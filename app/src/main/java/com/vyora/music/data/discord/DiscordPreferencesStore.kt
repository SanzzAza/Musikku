package com.vyora.music.data.discord

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class DiscordListeningMode(val label: String, val subtitle: String) {
    VYORA("Listening to VYORA", "Tampil eksklusif \"Listening to VYORA\""),
    ARTIST("Listening to [Nama Artis]", "Judul kartu mengikuti nama penyanyi lagu aktif"),
    YOUTUBE_MUSIC("Listening to YouTube Music", "Tampil klasik \"Listening to YouTube Music\""),
}

data class DiscordRpcPreferences(
    val token: String = "",
    val enabled: Boolean = true,
    val listeningMode: DiscordListeningMode = DiscordListeningMode.VYORA,
    val showVyoraBadge: Boolean = true,
    val showListenButton: Boolean = true,
    val username: String = "",
    val displayName: String = "",
    val avatarUrl: String = "",
) {
    val isLoggedIn: Boolean get() = token.isNotBlank()
    val isActive: Boolean get() = isLoggedIn && enabled
}

enum class DiscordConnectionStatus(val label: String) {
    DISCONNECTED("Belum terhubung"),
    CONNECTING("Menghubungkan ke Discord…"),
    CONNECTED("Aktif • Siap menampilkan status"),
    BROADCASTING("Sedang menampilkan Listening di Discord"),
    INVALID_TOKEN("Sesi kedaluwarsa — silakan login ulang"),
    ERROR("Koneksi terputus — mencoba ulang…"),
}

class DiscordPreferencesStore(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(
        "vyora_discord_rpc",
        Context.MODE_PRIVATE,
    )

    private val _state = MutableStateFlow(load())
    val state: StateFlow<DiscordRpcPreferences> = _state.asStateFlow()

    private fun load(): DiscordRpcPreferences {
        val modeName = prefs.getString(KEY_MODE, DiscordListeningMode.VYORA.name)
        val mode = DiscordListeningMode.entries.firstOrNull { it.name == modeName }
            ?: DiscordListeningMode.VYORA
        return DiscordRpcPreferences(
            token = prefs.getString(KEY_TOKEN, "")?.trim().orEmpty(),
            enabled = prefs.getBoolean(KEY_ENABLED, true),
            listeningMode = mode,
            showVyoraBadge = prefs.getBoolean(KEY_SHOW_VYORA_BADGE, true),
            showListenButton = prefs.getBoolean(KEY_SHOW_BUTTON, true),
            username = prefs.getString(KEY_USERNAME, "").orEmpty(),
            displayName = prefs.getString(KEY_DISPLAY_NAME, "").orEmpty(),
            avatarUrl = prefs.getString(KEY_AVATAR_URL, "").orEmpty(),
        )
    }

    fun setToken(rawToken: String) {
        val clean = sanitizeToken(rawToken)
        if (clean.isBlank()) return
        prefs.edit()
            .putString(KEY_TOKEN, clean)
            .putBoolean(KEY_ENABLED, true)
            .apply()
        _state.value = _state.value.copy(token = clean, enabled = true)
    }

    fun updateUserProfile(username: String, displayName: String, avatarUrl: String) {
        prefs.edit()
            .putString(KEY_USERNAME, username)
            .putString(KEY_DISPLAY_NAME, displayName)
            .putString(KEY_AVATAR_URL, avatarUrl)
            .apply()
        _state.value = _state.value.copy(
            username = username,
            displayName = displayName,
            avatarUrl = avatarUrl,
        )
    }

    fun setEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_ENABLED, enabled).apply()
        _state.value = _state.value.copy(enabled = enabled)
    }

    fun setListeningMode(mode: DiscordListeningMode) {
        prefs.edit().putString(KEY_MODE, mode.name).apply()
        _state.value = _state.value.copy(listeningMode = mode)
    }

    fun setShowVyoraBadge(show: Boolean) {
        prefs.edit().putBoolean(KEY_SHOW_VYORA_BADGE, show).apply()
        _state.value = _state.value.copy(showVyoraBadge = show)
    }

    fun setShowListenButton(show: Boolean) {
        prefs.edit().putBoolean(KEY_SHOW_BUTTON, show).apply()
        _state.value = _state.value.copy(showListenButton = show)
    }

    fun logout() {
        prefs.edit()
            .remove(KEY_TOKEN)
            .remove(KEY_USERNAME)
            .remove(KEY_DISPLAY_NAME)
            .remove(KEY_AVATAR_URL)
            .apply()
        _state.value = _state.value.copy(
            token = "",
            username = "",
            displayName = "",
            avatarUrl = "",
        )
    }

    companion object {
        private const val KEY_TOKEN = "discord_token"
        private const val KEY_ENABLED = "discord_enabled"
        private const val KEY_MODE = "discord_listening_mode"
        private const val KEY_SHOW_VYORA_BADGE = "discord_show_vyora_badge"
        private const val KEY_SHOW_BUTTON = "discord_show_button"
        private const val KEY_USERNAME = "discord_username"
        private const val KEY_DISPLAY_NAME = "discord_display_name"
        private const val KEY_AVATAR_URL = "discord_avatar_url"

        internal fun sanitizeToken(raw: String): String =
            raw.trim().removeSurrounding("\"").removePrefix("Bearer ").trim()
    }
}
