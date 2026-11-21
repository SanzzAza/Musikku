package com.vyora.music.data.discord

import com.vyora.music.data.ytmusic.ArtistRef
import com.vyora.music.data.ytmusic.SongItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DiscordRpcClientTest {

    @Test
    fun `sanitizeToken strips quotes and Bearer prefix`() {
        assertEquals("abc123tokenXYZ", DiscordPreferencesStore.sanitizeToken("  \"abc123tokenXYZ\" "))
        assertEquals("abc123tokenXYZ", DiscordPreferencesStore.sanitizeToken("Bearer abc123tokenXYZ"))
    }

    @Test
    fun `buildListeningPresenceJson creates type 2 LISTENING card with live timestamps and mp asset`() {
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
            showListenButton = true,
        )

        val now = 1_700_000_000_000L
        val payload = DiscordRpcClient.buildListeningPresenceJson(
            song = song,
            prefs = prefs,
            positionMs = 175_000L, // 02:55
            durationMs = 220_000L, // 03:40
            playbackSpeed = 1.0f,
            resolvedLargeImage = "mp:external/sample_hash/https/i.ytimg.com/vi/LjhCEhWiKXk/hqdefault.jpg",
            nowEpochMs = now,
        )

        val activities = payload.getAsJsonArray("activities")
        assertEquals(1, activities.size())
        val act = activities[0].asJsonObject

        assertEquals("VYORA", act.get("name").asString)
        assertEquals(2, act.get("type").asInt) // 2 = LISTENING ("Listening to VYORA")
        assertEquals("Just the Way You Are", act.get("details").asString)
        assertEquals("Bruno Mars", act.get("state").asString)

        val ts = act.getAsJsonObject("timestamps")
        assertEquals(now - 175_000L, ts.get("start").asLong)
        assertEquals(now + 45_000L, ts.get("end").asLong)

        val assets = act.getAsJsonObject("assets")
        assertTrue(assets.get("large_image").asString.startsWith("mp:external/"))

        val buttons = act.getAsJsonArray("buttons")
        assertEquals("Dengarkan Lagu", buttons[0].asString)
    }
}
