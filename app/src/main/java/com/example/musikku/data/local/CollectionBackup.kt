package com.example.musikku.data.local

import com.example.musikku.data.ytmusic.AlbumRef
import com.example.musikku.data.ytmusic.ArtistRef
import com.example.musikku.data.ytmusic.SongItem
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.io.ByteArrayOutputStream
import java.io.InputStream

/** File portabel, bukan unduhan audio. Impor diverifikasi sebelum mengubah koleksi. */
data class CollectionBackup(val version: Int = 1, val favorites: List<SongItem>, val playlists: List<UserPlaylist>)

object CollectionBackupCodec {
    const val MAX_BYTES = 5 * 1024 * 1024
    private const val MAX_SONGS = 20_000
    private const val MAX_PLAYLISTS = 2_000
    private val gson = Gson()

    fun encode(backup: CollectionBackup): String = gson.toJson(backup)

    fun readLimited(input: InputStream): String {
        val out = ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        while (true) {
            val n = input.read(buffer)
            if (n < 0) break
            require(out.size() + n <= MAX_BYTES) { "File backup terlalu besar (maksimal 5 MB)." }
            out.write(buffer, 0, n)
        }
        return out.toString("UTF-8")
    }

    fun decode(json: String): CollectionBackup {
        require(json.toByteArray(Charsets.UTF_8).size <= MAX_BYTES) { "File backup terlalu besar." }
        val root = JsonParser.parseString(json).takeIf { it.isJsonObject }?.asJsonObject
            ?: throw IllegalArgumentException("Format backup tidak valid.")
        require(root.get("version")?.isJsonPrimitive == true && root.get("version").asJsonPrimitive.isNumber && root.get("version").asString == "1") { "Versi backup tidak didukung." }
        var total = 0
        fun songs(array: JsonArray): List<SongItem> = array.map { item ->
            require(++total <= MAX_SONGS && item.isJsonObject) { "Daftar lagu tidak valid atau terlalu besar." }
            val o = item.asJsonObject
            val id = o.requiredString("id")
            val title = o.requiredString("title")
            val artistsElement = o.get("artists")
            require(artistsElement == null || artistsElement.isJsonArray) { "Data artis tidak valid." }
            val artists = artistsElement?.asJsonArray?.map { artist ->
                require(artist.isJsonObject) { "Data artis tidak valid." }
                ArtistRef(artist.asJsonObject.optionalString("id"), artist.asJsonObject.requiredString("name", false))
            }.orEmpty()
            val albumElement = o.get("album")
            val album = if (albumElement == null || albumElement.isJsonNull) null else {
                require(albumElement.isJsonObject) { "Data album tidak valid." }
                AlbumRef(albumElement.asJsonObject.optionalString("id"), albumElement.asJsonObject.requiredString("name", false))
            }
            val duration = o.get("durationSec")?.let { require(it.isJsonPrimitive && it.asJsonPrimitive.isNumber); it.asInt } ?: 0
            require(duration >= 0) { "Durasi tidak valid." }
            SongItem(id, title, artists, album, duration, o.optionalString("thumbnail"))
        }.distinctBy { it.id }
        val favorites = songs(root.requiredArray("favorites"))
        val playlistArray = root.requiredArray("playlists")
        require(playlistArray.size() <= MAX_PLAYLISTS) { "Terlalu banyak playlist." }
        val playlists = playlistArray.map { item ->
            require(item.isJsonObject) { "Playlist tidak valid." }
            val p = item.asJsonObject
            UserPlaylist(id = p.requiredString("id"), name = p.requiredString("name"), songs = songs(p.requiredArray("songs")),
                createdAt = p.get("createdAt")?.let { require(it.isJsonPrimitive && it.asJsonPrimitive.isNumber); it.asLong } ?: 0L)
        }
        require(playlists.map { it.id }.distinct().size == playlists.size) { "ID playlist ganda." }
        return CollectionBackup(favorites = favorites, playlists = playlists)
    }

    private fun JsonObject.requiredArray(name: String): JsonArray = get(name)?.takeIf { it.isJsonArray }?.asJsonArray
        ?: throw IllegalArgumentException("Data $name tidak valid.")
    private fun JsonObject.requiredString(name: String, nonblank: Boolean = true): String {
        val value = get(name)
        require(value != null && value.isJsonPrimitive && value.asJsonPrimitive.isString) { "Data $name tidak valid." }
        val text = value.asString
        require(text.length <= 4096 && (!nonblank || text.isNotBlank())) { "Data $name tidak valid." }
        return text
    }
    private fun JsonObject.optionalString(name: String): String? {
        val value = get(name) ?: return null
        return if (value.isJsonNull) null else requiredString(name, false)
    }
}
