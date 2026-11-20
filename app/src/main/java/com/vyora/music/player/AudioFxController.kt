package com.vyora.music.player

import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.media.audiofx.PresetReverb
import android.media.audiofx.Virtualizer
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.vyora.music.data.local.AudioFxPreferences
import com.vyora.music.data.local.LoudnessMode
import com.vyora.music.data.local.ReverbMode

/**
 * Mesin pemroses efek audio real-time VYORA (terhubung langsung ke ExoPlayer):
 * - Kecepatan & Nada (`PlaybackParameters`: Slowed + Reverb, Nightcore, Tempo Cepat)
 * - 5-Band Equalizer + Preset AutoEQ
 * - Bass Boost & 3D Virtualizer
 * - Normalisasi / Penguat Suara (`LoudnessEnhancer`)
 * - Ruang Reverb (`PresetReverb`)
 *
 * Semua pemanggilan DSP dibungkus `runCatching` agar aman di seluruh ROM Android.
 */
@OptIn(UnstableApi::class)
class AudioFxController(private val player: ExoPlayer) {

    private var boundSessionId: Int = C.AUDIO_SESSION_ID_UNSET
    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null
    private var loudnessEnhancer: LoudnessEnhancer? = null
    private var presetReverb: PresetReverb? = null
    private var latestPrefs: AudioFxPreferences = AudioFxPreferences()

    private val listener = object : Player.Listener {
        override fun onAudioSessionIdChanged(audioSessionId: Int) {
            rebindSession(audioSessionId)
            applyDsp(latestPrefs)
        }
    }

    fun attach(initialPrefs: AudioFxPreferences) {
        latestPrefs = initialPrefs
        player.addListener(listener)
        rebindSession(player.audioSessionId)
        apply(initialPrefs)
    }

    fun apply(prefs: AudioFxPreferences) {
        latestPrefs = prefs
        runCatching {
            val targetParams = PlaybackParameters(
                prefs.speed.coerceIn(0.5f, 2.0f),
                prefs.pitch.coerceIn(0.5f, 2.0f),
            )
            if (player.playbackParameters != targetParams) {
                player.playbackParameters = targetParams
            }
        }
        if (player.audioSessionId != boundSessionId) {
            rebindSession(player.audioSessionId)
        }
        applyDsp(prefs)
    }

    private fun rebindSession(sessionId: Int) {
        if (sessionId == boundSessionId) return
        releaseFx()
        boundSessionId = sessionId
        if (sessionId == C.AUDIO_SESSION_ID_UNSET || sessionId <= 0) return

        equalizer = runCatching { Equalizer(0, sessionId) }.getOrNull()
        bassBoost = runCatching { BassBoost(0, sessionId) }.getOrNull()
        virtualizer = runCatching { Virtualizer(0, sessionId) }.getOrNull()
        loudnessEnhancer = runCatching { LoudnessEnhancer(sessionId) }.getOrNull()
        presetReverb = runCatching { PresetReverb(0, sessionId) }.getOrNull()
    }

    private fun applyDsp(prefs: AudioFxPreferences) {
        // 1. 5-Band Equalizer
        equalizer?.let { eq ->
            runCatching {
                val range = eq.bandLevelRange // millibels, biasanya [-1500, 1500]
                val minMb = range.getOrElse(0) { -1200 }.toInt()
                val maxMb = range.getOrElse(1) { 1200 }.toInt()
                val numBands = eq.numberOfBands.toInt()
                for (b in 0 until numBands) {
                    val mappedIdx = if (numBands <= 1) 0 else ((b * 4) / (numBands - 1)).coerceIn(0, 4)
                    val db = if (prefs.eqEnabled) prefs.bandGainsDb.getOrElse(mappedIdx) { 0f } else 0f
                    val targetMb = (db * 100f).toInt().coerceIn(minMb, maxMb).toShort()
                    eq.setBandLevel(b.toShort(), targetMb)
                }
                eq.enabled = prefs.eqEnabled
            }
        }

        // 2. Bass Boost (0..1000)
        bassBoost?.let { bb ->
            runCatching {
                val active = prefs.bassBoostPercent > 0
                if (bb.strengthSupported) {
                    bb.setStrength((prefs.bassBoostPercent * 10).coerceIn(0, 1000).toShort())
                }
                bb.enabled = active
            }
        }

        // 3. 3D Virtualizer (0..1000)
        virtualizer?.let { virt ->
            runCatching {
                val active = prefs.virtualizerPercent > 0
                if (virt.strengthSupported) {
                    virt.setStrength((prefs.virtualizerPercent * 10).coerceIn(0, 1000).toShort())
                }
                virt.enabled = active
            }
        }

        // 4. Loudness Enhancer / Normalisasi Suara
        loudnessEnhancer?.let { le ->
            runCatching {
                val active = prefs.loudnessMode != LoudnessMode.OFF
                le.setTargetGain(if (active) prefs.loudnessMode.targetGainMb else 0)
                le.enabled = active
            }
        }

        // 5. Studio Reverb
        presetReverb?.let { rev ->
            runCatching {
                val preset = when (prefs.reverbMode) {
                    ReverbMode.OFF -> PresetReverb.PRESET_NONE
                    ReverbMode.SMALL_ROOM -> PresetReverb.PRESET_SMALLROOM
                    ReverbMode.STUDIO -> PresetReverb.PRESET_MEDIUMROOM
                    ReverbMode.CONCERT_HALL -> PresetReverb.PRESET_LARGEHALL
                }
                rev.preset = preset
                rev.enabled = prefs.reverbMode != ReverbMode.OFF
            }
        }
    }

    private fun releaseFx() {
        runCatching { equalizer?.release() }
        runCatching { bassBoost?.release() }
        runCatching { virtualizer?.release() }
        runCatching { loudnessEnhancer?.release() }
        runCatching { presetReverb?.release() }
        equalizer = null
        bassBoost = null
        virtualizer = null
        loudnessEnhancer = null
        presetReverb = null
    }

    fun release() {
        player.removeListener(listener)
        releaseFx()
        boundSessionId = C.AUDIO_SESSION_ID_UNSET
    }
}
