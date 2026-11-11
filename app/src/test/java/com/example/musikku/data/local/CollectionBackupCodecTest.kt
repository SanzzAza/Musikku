package com.example.musikku.data.local

import com.example.musikku.data.ytmusic.ArtistRef
import com.example.musikku.data.ytmusic.SongItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.ByteArrayInputStream

class CollectionBackupCodecTest {
    private val song = SongItem("abc", "Judul", listOf(ArtistRef("artist", "Artis")), durationSec = 123)

    @Test fun roundTrip() {
        val original = CollectionBackup(favorites = listOf(song), playlists = listOf(UserPlaylist("p1", "Playlist", listOf(song), 10)))
        assertEquals(original, CollectionBackupCodec.decode(CollectionBackupCodec.encode(original)))
    }

    @Test fun missingArraysRejected() {
        assertThrows(IllegalArgumentException::class.java) { CollectionBackupCodec.decode("""{"version":1,"favorites":[]}""") }
    }

    @Test fun unsupportedVersionRejected() {
        assertThrows(IllegalArgumentException::class.java) { CollectionBackupCodec.decode("""{"version":2,"favorites":[],"playlists":[]}""") }
    }

    @Test fun malformedSongRejected() {
        assertThrows(IllegalArgumentException::class.java) { CollectionBackupCodec.decode("""{"version":1,"favorites":[{"id":null}],"playlists":[]}""") }
    }

    @Test fun duplicatePlaylistIdsRejected() {
        val p = """{"id":"p","name":"X","songs":[]}"""
        assertThrows(IllegalArgumentException::class.java) { CollectionBackupCodec.decode("""{"version":1,"favorites":[],"playlists":[$p,$p]}""") }
    }

    @Test fun sizeLimitEnforced() {
        val input = ByteArrayInputStream(ByteArray(CollectionBackupCodec.MAX_BYTES + 1))
        assertThrows(IllegalArgumentException::class.java) { CollectionBackupCodec.readLimited(input) }
    }
}
