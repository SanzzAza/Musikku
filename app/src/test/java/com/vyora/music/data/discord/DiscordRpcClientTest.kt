package com.vyora.music.data.discord

import com.vyora.music.data.ytmusic.ArtistRef
import com.vyora.music.data.ytmusic.SongItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DiscordRpcClientTest {

    @Test
    fun `sanitizeToken strips quotes and Bearer prefix`() {
        assertEquals("abc123tokenXYZ", DiscordPreferencesStore.sanitizeToken("  \"abc123tokenXYZ\" "))
        assertEquals("abc123tokenXYZ", DiscordPreferencesStore.sanitizeToken("Bearer abc123tokenXYZ"))
    }

    @Test
    fun `buildListeningPresenceJson attaches VYORA small_image badge and live timestamps`() {
        val song = SongItem(
            id = "LjhCEhWiKXk",
            title = "Just the Way You Are",
            artists = listOf(ArtistRef(id = "UC1", name = "Bruno Mars")),
            thumbnail = "https://i.ytimg.com/vi/LjhCEhWiKXk/hqdefault.jpg",
            durationSec = 220,
        )
        val prefs = DiscordRpcPreferences(
            token = "valid_token_sample_1234567890",
            enabled = true,
            listeningMode = DiscordListeningMode.VYORA,
            showVyoraBadge = true,
            showListenButton = true,
        )

        val now = 1_700_000_000_000L
        val payload = DiscordRpcClient.buildListeningPresenceJson(
            song = song,
            prefs = prefs,
            positionMs = 175_000L,
            durationMs = 220_000L,
            playbackSpeed = 1.0f,
            resolvedLargeImage = "mp:external/sample_hash/https/i.ytimg.com/vi/LjhCEhWiKXk/hqdefault.jpg",
            resolvedSmallImage = "mp:external/badge_hash/https/raw.githubusercontent.com/SanzzAza/Musikku/main/assets/discord/vyora_badge.png",
            nowEpochMs = now,
        )

        val activities = payload.getAsJsonArray("activities")
        assertEquals(1, activities.size())
        val act = activities[0].asJsonObject

        assertEquals("VYORA", act.get("name").asString)
        assertEquals(2, act.get("type").asInt)
        assertEquals("Just the Way You Are", act.get("details").asString)
        assertEquals("Bruno Mars", act.get("state").asString)

        val ts = act.getAsJsonObject("timestamps")
        assertEquals(now - 175_000L, ts.get("start").asLong)
        assertEquals(now + 45_000L, ts.get("end").asLong)

        val assets = act.getAsJsonObject("assets")
        assertTrue(assets.get("large_image").asString.startsWith("mp:external/"))
        assertTrue(assets.get("small_image").asString.startsWith("mp:external/"))
        assertEquals("VYORA Music", assets.get("small_text").asString)

        val buttons = act.getAsJsonArray("buttons")
        assertEquals("Dengarkan Lagu", buttons[0].asString)
    }

    @Test
    fun `buildListeningPresenceJson omits small_image when showVyoraBadge is false`() {
        val song = SongItem(
            id = "LjhCEhWiKXk",
            title = "The One That Got Away",
            artists = listOf(ArtistRef(id = "UC2", name = "Katy Perry")),
        )
        val prefs = DiscordRpcPreferences(
            token = "valid_token_sample_1234567890",
            enabled = true,
            showVyoraBadge = false,
        )

        val payload = DiscordRpcClient.buildListeningPresenceJson(
            song = song,
            prefs = prefs,
            positionMs = 72_000L,
            durationMs = 227_000L,
            resolvedLargeImage = "mp:external/cover/https/i.ytimg.com/vi/LjhCEhWiKXk/hqdefault.jpg",
            resolvedSmallImage = "mp:external/badge/vyora_badge.png",
        )

        val assets = payload.getAsJsonArray("activities")[0].asJsonObject.getAsJsonObject("assets")
        assertTrue(assets.has("large_image"))
        assertFalse(assets.has("small_image"))
    }

    @Test
    fun `resolveBestArtworkUrl falls back to VYORA logo cover for local files without online thumbnail`() {
        val localSong = SongItem(
            id = "local_42",
            title = "Offline Track",
            artists = listOf(ArtistRef(id = null, name = "Local Artist")),
            thumbnail = "content://media/external/audio/albumart/42",
            localUri = "content://media/external/audio/media/99",
        )
        assertEquals(
            DiscordRpcClient.VYORA_COVER_FALLBACK_URL,
            DiscordRpcClient.resolveBestArtworkUrl(localSong),
        )
    }
}
