package com.vyora.music.data.local

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.vyora.music.data.ytmusic.AlbumRef
import com.vyora.music.data.ytmusic.ArtistRef
import com.vyora.music.data.ytmusic.SongItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Membaca file musik yang sudah ada di HP lewat MediaStore.
 *
 * Hasilnya dipetakan ke [SongItem] dengan `localUri`, sehingga bisa masuk ke
 * antrean pemutar yang sama dengan lagu YouTube Music.
 */
class LocalMusicRepository(private val context: Context) {

    companion object {
        const val LOCAL_ID_PREFIX = "local:"
        private const val MAX_SONGS = 5_000
    }

    suspend fun load(): List<SongItem> = withContext(Dispatchers.IO) {
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DISPLAY_NAME,
        )
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC"

        val songs = ArrayList<SongItem>()
        runCatching {
            context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                null,
                sortOrder,
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val displayCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)

                while (cursor.moveToNext() && songs.size < MAX_SONGS) {
                    val mediaId = cursor.getLong(idCol)
                    val durationMs = if (cursor.isNull(durationCol)) 0L else cursor.getLong(durationCol)
                    // Abaikan potongan pendek (nada dering, notifikasi) agar daftar tidak penuh sampah.
                    if (durationMs < 30_000L) continue

                    val title = cursor.getString(titleCol).orEmpty().ifBlank {
                        cursor.getString(displayCol).orEmpty().substringBeforeLast('.')
                    }.ifBlank { "Tanpa judul" }

                    val rawArtist = cursor.getString(artistCol).orEmpty()
                    // MediaStore memakai "<unknown>" untuk file tanpa tag artis.
                    val artist = if (rawArtist.isBlank() || rawArtist.equals("<unknown>", true)) {
                        "Artis tidak dikenal"
                    } else rawArtist

                    val album = cursor.getString(albumCol).orEmpty()
                    val albumId = if (cursor.isNull(albumIdCol)) -1L else cursor.getLong(albumIdCol)

                    val fileUri = ContentUris.withAppendedId(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        mediaId,
                    )

                    songs.add(
                        SongItem(
                            id = "$LOCAL_ID_PREFIX$mediaId",
                            title = title,
                            artists = listOf(ArtistRef(id = null, name = artist)),
                            album = album.takeIf { it.isNotBlank() }?.let { AlbumRef(id = null, name = it) },
                            durationSec = (durationMs / 1000L).toInt(),
                            thumbnail = albumArtUri(albumId),
                            localUri = fileUri.toString(),
                        )
                    )
                }
            }
        }
        songs
    }

    private fun albumArtUri(albumId: Long): String? {
        if (albumId <= 0) return null
        return ContentUris.withAppendedId(
            Uri.parse("content://media/external/audio/albumart"),
            albumId,
        ).toString()
    }
}
