package com.vyora.music.data.discord

import android.os.Build
import android.util.Base64
import com.google.gson.JsonArray
import com.google.gson.JsonNull
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.vyora.music.data.ytmusic.SongItem
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.Collections
import java.util.LinkedHashMap
import java.util.Locale
import java.util.UUID
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import kotlin.random.Random

/**
 * Mesin Discord Rich Presence (Gateway v10 + External Assets Proxy) bawaan VYORA.
 *
 * Menampilkan kartu "Listening to VYORA" lengkap dengan:
 * - Gambar sampul album resolusi tinggi (`mp:external/...`)
 * - Judul lagu (`details`) & Nama Artis (`state`)
 * - Bar durasi berjalan real-time (`timestamps.start` & `timestamps.end`, otomatis mengikuti speed)
 * - Tombol opsional "Dengarkan Lagu" ke YouTube Music
 */
class DiscordRpcClient(
    private val prefsStore: DiscordPreferencesStore,
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .pingInterval(25, TimeUnit.SECONDS)
        .build()

    private val _status = MutableStateFlow(DiscordConnectionStatus.DISCONNECTED)
    val status: StateFlow<DiscordConnectionStatus> = _status.asStateFlow()

    // LRU cache 128 entri untuk URL sampul -> mp:external/...
    private val artworkCache: MutableMap<String, String> = Collections.synchronizedMap(
        object : LinkedHashMap<String, String>(64, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, String>?): Boolean =
                size > 128
        }
    )

    private val wsGeneration = AtomicInteger(0)
    private val activitySeq = AtomicLong(0)

    @Volatile private var activeToken: String = ""
    @Volatile private var webSocket: WebSocket? = null
    @Volatile private var sessionEstablished: Boolean = false
    @Volatile private var sessionId: String? = null
    @Volatile private var resumeGatewayUrl: String? = null
    @Volatile private var lastSeq: Int = 0
    @Volatile private var reconnectAttempts: Int = 0
    @Volatile private var lastPresencePayload: JsonObject? = null

    private var heartbeatJob: Job? = null
    private var reconnectJob: Job? = null
    private var assetJob: Job? = null

    private val superPropertiesBase64: String by lazy {
        val obj = JsonObject().apply {
            addProperty("os", "Android")
            addProperty("browser", "Discord Android")
            addProperty("device", Build.DEVICE ?: "Android")
            addProperty("system_locale", Locale.getDefault().toString())
            addProperty("client_version", CLIENT_VERSION)
            addProperty("release_channel", "googleRelease")
            addProperty("device_vendor_id", UUID.randomUUID().toString())
            addProperty("client_uuid", UUID.randomUUID().toString())
            addProperty("client_launch_id", UUID.randomUUID().toString())
            addProperty("os_version", Build.VERSION.RELEASE ?: "14")
            addProperty("os_sdk_version", Build.VERSION.SDK_INT.toString())
            addProperty("client_build_number", CLIENT_BUILD_NUMBER)
            add("client_event_source", JsonNull.INSTANCE)
            addProperty("design_id", 0)
        }
        Base64.encodeToString(obj.toString().toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
    }

    /** Memvalidasi token & mengambil profil `@me` Discord secara langsung. */
    suspend fun verifyAndFetchProfile(rawToken: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val clean = DiscordPreferencesStore.sanitizeToken(rawToken)
            require(clean.length >= 24) { "Token Discord tidak valid" }
            val req = Request.Builder()
                .url("https://discord.com/api/v9/users/@me")
                .header("Authorization", clean)
                .header("User-Agent", USER_AGENT)
                .header("X-Super-Properties", superPropertiesBase64)
                .build()
            httpClient.newCall(req).execute().use { resp ->
                if (resp.code == 401 || resp.code == 403) {
                    throw IllegalStateException("Token Discord tidak valid atau sudah kedaluwarsa")
                }
                if (!resp.isSuccessful) {
                    throw IllegalStateException("Discord API merespons HTTP ${resp.code}")
                }
                val user = JsonParser.parseString(resp.body?.string() ?: "{}").asJsonObject
                applyUserJson(user)
                if (activeToken != clean) {
                    disconnectInternal(resetStatus = false)
                    artworkCache.clear()
                }
                prefsStore.setToken(clean)
                ensureConnected(clean)
            }
        }
    }

    /**
     * Dipanggil dari `PlaybackService` setiap kali lagu berganti, di-seek, speed berubah,
     * atau preferensi Discord berubah.
     */
    fun updateNowPlaying(
        song: SongItem?,
        isPlaying: Boolean,
        positionMs: Long,
        durationMs: Long,
        playbackSpeed: Float = 1f,
    ) {
        val prefs = prefsStore.state.value
        if (!prefs.isActive) {
            disconnect()
            return
        }
        if (song == null || !isPlaying) {
            clearPresence()
            return
        }

        val seq = activitySeq.incrementAndGet()
        val startCallRealMs = System.currentTimeMillis()
        ensureConnected(prefs.token)

        val rawLargeUrl = resolveBestArtworkUrl(song)
        val cachedLarge = artworkCache[rawLargeUrl]
        val rawSmallUrl = if (prefs.showVyoraBadge) VYORA_BADGE_URL else null
        val cachedSmall = rawSmallUrl?.let { artworkCache[it] }

        // Fase 1: Kirim langsung status Listening (pakai mp:external dari cache jika sudah siap)
        val initialPayload = buildListeningPresenceJson(
            song = song,
            prefs = prefs,
            positionMs = positionMs,
            durationMs = durationMs,
            playbackSpeed = playbackSpeed,
            resolvedLargeImage = cachedLarge,
            resolvedSmallImage = if (cachedLarge != null) cachedSmall else null,
            nowEpochMs = startCallRealMs,
        )
        sendPresencePayload(initialPayload)

        // Fase 2: Jika sampul album atau badge logo VYORA belum ada di cache,
        // upload ke /external-assets lalu perbarui kartu lengkap dengan logo VYORA!
        val needsLargeUpload = cachedLarge == null
        val needsSmallUpload = rawSmallUrl != null && cachedSmall == null
        if (needsLargeUpload || needsSmallUpload) {
            assetJob?.cancel()
            assetJob = scope.launch {
                val finalLarge = cachedLarge ?: uploadWithLogoFallback(prefs.token, rawLargeUrl)?.also {
                    artworkCache[rawLargeUrl] = it
                }
                val finalSmall = if (rawSmallUrl == null) {
                    null
                } else {
                    cachedSmall ?: uploadWithLogoFallback(prefs.token, rawSmallUrl)?.also {
                        artworkCache[rawSmallUrl] = it
                    }
                }

                if ((finalLarge != null || finalSmall != null) &&
                    seq == activitySeq.get() &&
                    prefsStore.state.value.isActive
                ) {
                    val nowRealMs = System.currentTimeMillis()
                    val elapsedTrackMs = ((nowRealMs - startCallRealMs) * playbackSpeed.coerceIn(0.25f, 4.0f))
                        .toLong()
                        .coerceAtLeast(0L)
                    val adjustedPosMs = if (durationMs > 0L) {
                        (positionMs + elapsedTrackMs).coerceAtMost(durationMs)
                    } else {
                        positionMs + elapsedTrackMs
                    }
                    val updatedPayload = buildListeningPresenceJson(
                        song = song,
                        prefs = prefsStore.state.value,
                        positionMs = adjustedPosMs,
                        durationMs = durationMs,
                        playbackSpeed = playbackSpeed,
                        resolvedLargeImage = finalLarge,
                        resolvedSmallImage = finalSmall,
                        nowEpochMs = nowRealMs,
                    )
                    sendPresencePayload(updatedPayload)
                }
            }
        }
    }

    /** Menghapus status Listening saat musik dijeda (pause) atau dihentikan. */
    fun clearPresence() {
        activitySeq.incrementAndGet()
        assetJob?.cancel()
        lastPresencePayload = null
        val ws = webSocket
        if (ws != null && sessionEstablished) {
            val d = JsonObject().apply {
                add("activities", JsonArray())
                addProperty("since", 0L)
                addProperty("status", "online")
                addProperty("afk", false)
            }
            sendGatewayFrame(ws, OP_PRESENCE_UPDATE, d)
            _status.value = DiscordConnectionStatus.CONNECTED
        }
    }

    fun disconnect() {
        disconnectInternal(resetStatus = true)
    }

    private fun disconnectInternal(resetStatus: Boolean) {
        wsGeneration.incrementAndGet()
        activitySeq.incrementAndGet()
        heartbeatJob?.cancel()
        reconnectJob?.cancel()
        assetJob?.cancel()
        lastPresencePayload = null
        sessionEstablished = false
        sessionId = null
        resumeGatewayUrl = null
        lastSeq = 0
        reconnectAttempts = 0
        val old = webSocket
        webSocket = null
        runCatching { old?.close(1000, "Client closed") }
        if (resetStatus) {
            _status.value = DiscordConnectionStatus.DISCONNECTED
        }
    }

    private fun ensureConnected(token: String) {
        if (token.isBlank()) return
        if (activeToken != token) {
            disconnectInternal(resetStatus = false)
            artworkCache.clear()
            activeToken = token
        }
        if (webSocket != null) return
        openGatewaySocket(token)
    }

    private fun openGatewaySocket(token: String) {
        val gen = wsGeneration.incrementAndGet()
        heartbeatJob?.cancel()
        reconnectJob?.cancel()
        sessionEstablished = false
        if (_status.value != DiscordConnectionStatus.INVALID_TOKEN) {
            _status.value = DiscordConnectionStatus.CONNECTING
        }

        val targetUrl = resumeGatewayUrl?.takeIf { it.startsWith("wss://") } ?: GATEWAY_URL
        val localeTag = Locale.getDefault().toString().replace('_', '-')
        val req = Request.Builder()
            .url(targetUrl)
            .header("User-Agent", USER_AGENT)
            .header("Accept-Language", localeTag)
            .build()

        webSocket = httpClient.newWebSocket(
            req,
            object : WebSocketListener() {
                override fun onMessage(webSocket: WebSocket, text: String) {
                    if (gen != wsGeneration.get()) return
                    runCatching {
                        val root = JsonParser.parseString(text).asJsonObject
                        val op = root.get("op")?.takeIf { !it.isJsonNull }?.asInt ?: -1
                        val s = root.get("s")?.takeIf { !it.isJsonNull }?.asInt
                        if (s != null && s > 0) lastSeq = s
                        val t = root.get("t")?.takeIf { !it.isJsonNull }?.asString
                        val d = root.get("d")

                        when (op) {
                            OP_HELLO -> {
                                val interval = d?.takeIf { it.isJsonObject }
                                    ?.asJsonObject
                                    ?.get("heartbeat_interval")
                                    ?.asLong ?: 41_250L
                                handleHello(webSocket, gen, token, interval)
                            }
                            OP_DISPATCH -> handleDispatch(t, d)
                            OP_HEARTBEAT -> sendHeartbeat(webSocket)
                            OP_RECONNECT -> runCatching { webSocket.close(4000, "Reconnect") }
                            OP_INVALID_SESSION -> {
                                val resumable = d?.takeIf { it.isJsonPrimitive }?.asBoolean ?: false
                                if (!resumable) {
                                    sessionId = null
                                    resumeGatewayUrl = null
                                    lastSeq = 0
                                }
                                runCatching { webSocket.close(4000, "Invalid session") }
                            }
                            OP_HEARTBEAT_ACK -> Unit
                        }
                    }
                }

                override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                    runCatching { webSocket.close(code, reason) }
                    onSocketDisconnected(gen, code)
                }

                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    onSocketDisconnected(gen, code)
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    onSocketDisconnected(gen, response?.code ?: -1)
                }
            },
        )
    }

    private fun handleHello(ws: WebSocket, gen: Int, token: String, intervalMs: Long) {
        val resumableId = sessionId
        if (!resumableId.isNullOrBlank() && lastSeq > 0) {
            sendResume(ws, token, resumableId, lastSeq)
            sendHeartbeat(ws)
        } else {
            // Sesi baru: kirim IDENTIFY + HEARTBEAT pertama langsung tanpa delay
            // supaya event READY turun dalam <0.5 detik!
            sendIdentify(ws, token)
            sendHeartbeat(ws)
        }

        heartbeatJob?.cancel()
        heartbeatJob = scope.launch {
            while (isActive && gen == wsGeneration.get()) {
                val jitterFactor = 0.95 + Random.nextDouble() * 0.10
                delay((intervalMs * jitterFactor).toLong().coerceAtLeast(5_000L))
                if (gen == wsGeneration.get()) {
                    sendHeartbeat(ws)
                }
            }
        }
    }

    private fun handleDispatch(eventType: String?, dataElement: com.google.gson.JsonElement?) {
        when (eventType) {
            "READY" -> {
                val obj = dataElement?.takeIf { it.isJsonObject }?.asJsonObject
                sessionId = obj?.get("session_id")?.takeIf { !it.isJsonNull }?.asString
                resumeGatewayUrl = obj?.get("resume_gateway_url")?.takeIf { !it.isJsonNull }?.asString
                obj?.getAsJsonObject("user")?.let { applyUserJson(it) }
                sessionEstablished = true
                reconnectAttempts = 0
                val pending = lastPresencePayload
                if (pending != null) {
                    webSocket?.let { sendGatewayFrame(it, OP_PRESENCE_UPDATE, pending) }
                    _status.value = DiscordConnectionStatus.BROADCASTING
                } else {
                    _status.value = DiscordConnectionStatus.CONNECTED
                }
            }
            "RESUMED" -> {
                sessionEstablished = true
                reconnectAttempts = 0
                val pending = lastPresencePayload
                if (pending != null) {
                    webSocket?.let { sendGatewayFrame(it, OP_PRESENCE_UPDATE, pending) }
                    _status.value = DiscordConnectionStatus.BROADCASTING
                } else {
                    _status.value = DiscordConnectionStatus.CONNECTED
                }
            }
        }
    }

    private fun onSocketDisconnected(gen: Int, code: Int) {
        if (gen != wsGeneration.get()) return
        heartbeatJob?.cancel()
        sessionEstablished = false
        webSocket = null

        if (code == 4004) {
            // 4004 = Authentication failed (token tidak sah / dicabut)
            _status.value = DiscordConnectionStatus.INVALID_TOKEN
            return
        }
        if (code in setOf(4010, 4011, 4012, 4013, 4014)) {
            _status.value = DiscordConnectionStatus.ERROR
            return
        }

        val prefs = prefsStore.state.value
        if (!prefs.isActive) {
            _status.value = DiscordConnectionStatus.DISCONNECTED
            return
        }

        if (reconnectAttempts >= MAX_RECONNECT_ATTEMPTS) {
            _status.value = DiscordConnectionStatus.ERROR
            return
        }

        val attempt = reconnectAttempts++
        val baseDelayMs = (1_500L shl attempt.coerceAtMost(5)).coerceAtMost(45_000L)
        _status.value = DiscordConnectionStatus.ERROR
        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            delay(baseDelayMs)
            val latest = prefsStore.state.value
            if (latest.isActive && webSocket == null) {
                openGatewaySocket(latest.token)
            }
        }
    }

    private fun sendHeartbeat(ws: WebSocket) {
        val payload = JsonObject().apply {
            addProperty("op", OP_HEARTBEAT)
            if (lastSeq > 0) addProperty("d", lastSeq) else add("d", JsonNull.INSTANCE)
        }
        ws.send(payload.toString())
    }

    private fun sendIdentify(ws: WebSocket, token: String) {
        val props = JsonObject().apply {
            addProperty("os", "Android")
            addProperty("browser", "Discord Android")
            addProperty("device", Build.MODEL ?: "Android")
            addProperty("system_locale", Locale.getDefault().toString())
            addProperty("client_version", CLIENT_VERSION)
            addProperty("release_channel", "googleRelease")
            addProperty("os_version", Build.VERSION.RELEASE ?: "14")
            addProperty("os_sdk_version", Build.VERSION.SDK_INT.toString())
            addProperty("client_build_number", CLIENT_BUILD_NUMBER)
        }
        val clientState = JsonObject().apply {
            add("guild_versions", JsonObject())
            addProperty("highest_last_message_id", "0")
            addProperty("read_state_version", 0)
            addProperty("user_guild_settings_version", -1)
            addProperty("user_settings_version", -1)
            addProperty("private_channels_version", "0")
            addProperty("api_code_version", 0)
        }
        val d = JsonObject().apply {
            addProperty("token", token)
            add("properties", props)
            addProperty("capabilities", 16381)
            addProperty("compress", false)
            add("client_state", clientState)
        }
        sendGatewayFrame(ws, OP_IDENTIFY, d)
    }

    private fun sendResume(ws: WebSocket, token: String, sid: String, seq: Int) {
        val d = JsonObject().apply {
            addProperty("token", token)
            addProperty("session_id", sid)
            addProperty("seq", seq)
        }
        sendGatewayFrame(ws, OP_RESUME, d)
    }

    private fun sendPresencePayload(d: JsonObject) {
        lastPresencePayload = d
        val ws = webSocket
        if (ws != null && sessionEstablished) {
            sendGatewayFrame(ws, OP_PRESENCE_UPDATE, d)
            _status.value = DiscordConnectionStatus.BROADCASTING
        }
    }

    private fun sendGatewayFrame(ws: WebSocket, op: Int, d: JsonObject) {
        val root = JsonObject().apply {
            addProperty("op", op)
            add("d", d)
        }
        ws.send(root.toString())
    }

    private fun applyUserJson(user: JsonObject) {
        val id = user.get("id")?.takeIf { !it.isJsonNull }?.asString.orEmpty()
        val uname = user.get("username")?.takeIf { !it.isJsonNull }?.asString.orEmpty()
        val gname = user.get("global_name")?.takeIf { !it.isJsonNull }?.asString.orEmpty()
        val avatarHash = user.get("avatar")?.takeIf { !it.isJsonNull }?.asString.orEmpty()
        val avatarUrl = if (id.isNotBlank() && avatarHash.isNotBlank()) {
            "https://cdn.discordapp.com/avatars/$id/$avatarHash.png?size=128"
        } else {
            ""
        }
        if (uname.isNotBlank()) {
            prefsStore.updateUserProfile(
                username = uname,
                displayName = gname.ifBlank { uname },
                avatarUrl = avatarUrl,
            )
        }
    }

    private suspend fun uploadWithLogoFallback(token: String, imageUrl: String): String? {
        val primary = uploadExternalAsset(token, imageUrl)
        if (primary != null) return primary
        if (imageUrl == VYORA_BADGE_URL || imageUrl == VYORA_COVER_FALLBACK_URL) {
            return uploadExternalAsset(token, VYORA_LEGACY_LOGO_URL)
        }
        return null
    }

    private suspend fun uploadExternalAsset(token: String, imageUrl: String): String? {
        if (imageUrl.startsWith("mp:")) return imageUrl
        if (!imageUrl.startsWith("http")) return null
        return try {
            val reqJson = JsonObject().apply {
                add("urls", JsonArray().apply { add(imageUrl) })
            }
            val req = Request.Builder()
                .url("https://discord.com/api/v9/applications/$APPLICATION_ID/external-assets")
                .header("Authorization", token)
                .header("User-Agent", USER_AGENT)
                .header("X-Super-Properties", superPropertiesBase64)
                .post(reqJson.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()

            httpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return null
                val arr = JsonParser.parseString(resp.body?.string() ?: "[]")
                if (!arr.isJsonArray || arr.asJsonArray.size() == 0) return null
                val path = arr.asJsonArray[0].asJsonObject
                    .get("external_asset_path")
                    ?.takeIf { !it.isJsonNull }
                    ?.asString
                path?.takeIf { it.isNotBlank() }?.let { "mp:$it" }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }
    }

    companion object {
        const val APPLICATION_ID = "1379051016007454760"
        private const val GATEWAY_URL = "wss://gateway.discord.gg/?v=10&encoding=json"
        private const val CLIENT_VERSION = "314.13 - Stable"
        private const val CLIENT_BUILD_NUMBER = 314013
        private const val USER_AGENT = "Discord-Android/$CLIENT_BUILD_NUMBER;RNA"
        private const val MAX_RECONNECT_ATTEMPTS = 7
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

        private const val OP_DISPATCH = 0
        private const val OP_HEARTBEAT = 1
        private const val OP_IDENTIFY = 2
        private const val OP_PRESENCE_UPDATE = 3
        private const val OP_RESUME = 6
        private const val OP_RECONNECT = 7
        private const val OP_INVALID_SESSION = 9
        private const val OP_HELLO = 10
        private const val OP_HEARTBEAT_ACK = 11

        private const val ACTIVITY_TYPE_LISTENING = 2

        /** Badge bundar logo VYORA untuk pojok kanan bawah sampul (`assets.small_image`). */
        internal const val VYORA_BADGE_URL =
            "https://raw.githubusercontent.com/SanzzAza/Musikku/main/assets/discord/vyora_badge.png?v=1"

        /** Sampul cadangan logo VYORA untuk lagu lokal MP3/FLAC yang tidak punya URL sampul online. */
        internal const val VYORA_COVER_FALLBACK_URL =
            "https://raw.githubusercontent.com/SanzzAza/Musikku/main/assets/discord/vyora_cover_fallback.png?v=1"

        private const val VYORA_LEGACY_LOGO_URL =
            "https://raw.githubusercontent.com/SanzzAza/Musikku/main/app/src/main/res/drawable-nodpi/vyora_logo.png"

        internal fun resolveBestArtworkUrl(song: SongItem): String {
            val thumb = song.thumbnail?.trim()
            if (!thumb.isNullOrBlank() && (thumb.startsWith("https://") || thumb.startsWith("http://"))) {
                return thumb
            }
            if (song.localUri == null && song.id.isNotBlank()) {
                return "https://i.ytimg.com/vi/${song.id}/hqdefault.jpg"
            }
            return VYORA_COVER_FALLBACK_URL
        }

        /**
         * Murni Kotlin (mudah diuji di unit test):
         * menyusun payload `d` untuk `OP_PRESENCE_UPDATE` tipe `LISTENING` (2) lengkap dengan
         * sampul lagu (`large_image`), badge logo VYORA (`small_image`), dan start/end timestamps
         * agar bar durasi `02:55 ━━━── 03:40` berjalan mulus di profil Discord.
         */
        internal fun buildListeningPresenceJson(
            song: SongItem,
            prefs: DiscordRpcPreferences,
            positionMs: Long,
            durationMs: Long,
            playbackSpeed: Float = 1f,
            resolvedLargeImage: String? = null,
            resolvedSmallImage: String? = null,
            nowEpochMs: Long = System.currentTimeMillis(),
        ): JsonObject {
            val activityName = when (prefs.listeningMode) {
                DiscordListeningMode.VYORA -> "VYORA"
                DiscordListeningMode.ARTIST -> song.artistsText.ifBlank { "VYORA" }
                DiscordListeningMode.YOUTUBE_MUSIC -> "YouTube Music"
            }

            val safeSpeed = playbackSpeed.coerceIn(0.25f, 4.0f)
            val safePos = positionMs.coerceAtLeast(0L)
            val totalDuration = durationMs.takeIf { it > 0L }
                ?: song.durationSec?.takeIf { it > 0 }?.let { it * 1000L }
                ?: 0L

            val activity = JsonObject().apply {
                addProperty("name", activityName)
                addProperty("type", ACTIVITY_TYPE_LISTENING)
                addProperty("application_id", APPLICATION_ID)
                addProperty("details", song.title.ifBlank { "Musik VYORA" }.take(128))
                addProperty("state", song.artistsText.ifBlank { "VYORA Music" }.take(128))

                val timestamps = JsonObject().apply {
                    val startEpoch = nowEpochMs - (safePos / safeSpeed).toLong()
                    addProperty("start", startEpoch)
                    if (totalDuration > safePos) {
                        val remainingRealMs = ((totalDuration - safePos) / safeSpeed).toLong()
                        addProperty("end", nowEpochMs + remainingRealMs)
                    } else if (totalDuration > 0L) {
                        addProperty("end", startEpoch + (totalDuration / safeSpeed).toLong())
                    }
                }
                add("timestamps", timestamps)

                val albumLabel = song.album?.name?.takeIf { it.isNotBlank() } ?: "VYORA Music"
                val hasLarge = !resolvedLargeImage.isNullOrBlank()
                val showSmall = prefs.showVyoraBadge && !resolvedSmallImage.isNullOrBlank()
                if (hasLarge || showSmall) {
                    add(
                        "assets",
                        JsonObject().apply {
                            if (hasLarge) {
                                addProperty("large_image", resolvedLargeImage)
                                addProperty("large_text", albumLabel.take(128))
                            }
                            if (showSmall) {
                                addProperty("small_image", resolvedSmallImage)
                                addProperty("small_text", "VYORA Music")
                            }
                        },
                    )
                }

                if (prefs.showListenButton && song.localUri == null && song.id.isNotBlank()) {
                    val watchUrl = "https://music.youtube.com/watch?v=${song.id}"
                    add("buttons", JsonArray().apply { add("Dengarkan Lagu") })
                    add(
                        "metadata",
                        JsonObject().apply {
                            add("button_urls", JsonArray().apply { add(watchUrl) })
                        },
                    )
                }
            }

            return JsonObject().apply {
                add("activities", JsonArray().apply { add(activity) })
                addProperty("since", nowEpochMs)
                addProperty("status", "online")
                addProperty("afk", false)
            }
        }
    }
}
