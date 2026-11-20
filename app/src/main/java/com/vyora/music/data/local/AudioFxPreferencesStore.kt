package com.vyora.music.data.local

import android.content.Context
import com.google.gson.Gson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class VibePreset(val label: String, val subtitle: String, val speed: Float, val pitch: Float, val defaultReverb: ReverbMode) {
    NORMAL("Normal", "1.00× Asli", 1.00f, 1.00f, ReverbMode.OFF),
    SLOWED_VIBE("Slowed + Reverb", "0.86× Hangat", 0.86f, 0.86f, ReverbMode.STUDIO),
    DEEP_SLOWED("Deep Slowed", "0.78× Melankolis", 0.78f, 0.80f, ReverbMode.CONCERT_HALL),
    NIGHTCORE("Nightcore", "1.18× Energik", 1.18f, 1.18f, ReverbMode.OFF),
    FAST_TEMPO("Tempo Cepat", "1.25× Nada Tetap", 1.25f, 1.00f, ReverbMode.OFF),
    CUSTOM("Kustom", "Atur bebas", 1.00f, 1.00f, ReverbMode.OFF),
}

/**
 * Preset Equalizer 5-Band (60Hz, 230Hz, 910Hz, 3.6kHz, 14kHz) dalam satuan dB (-12..+12).
 */
enum class EqPreset(val label: String, val gainsDb: List<Float>) {
    FLAT("Netral (Flat)", listOf(0f, 0f, 0f, 0f, 0f)),
    VYORA_SIGNATURE("VYORA Signature", listOf(4.5f, 2.0f, -0.5f, 2.5f, 4.0f)),
    TWS_AUTOEQ("TWS Harman (AutoEQ)", listOf(3.5f, 1.0f, -1.5f, 3.0f, 2.0f)),
    BASS_BOOST("Bass Boost", listOf(7.0f, 4.5f, 0.0f, 0.5f, 1.5f)),
    VOCAL_CLEAR("Vokal Jernih", listOf(-1.5f, 0.5f, 4.5f, 4.0f, 1.5f)),
    POP("Pop Modern", listOf(2.0f, 3.0f, 1.5f, 2.5f, 3.0f)),
    ROCK("Rock & Band", listOf(5.0f, 2.5f, -1.5f, 3.0f, 5.0f)),
    HIP_HOP("Hip-Hop & R&B", listOf(6.5f, 3.5f, -1.0f, 1.5f, 3.0f)),
    ACOUSTIC("Akustik Hangat", listOf(2.5f, 2.0f, 2.5f, 3.5f, 2.5f)),
    ELECTRONIC("EDM & Club", listOf(6.0f, 2.5f, 0.0f, 2.5f, 5.5f)),
    LOFI_WARM("Lo-Fi Vinyl", listOf(3.5f, 3.0f, 1.0f, -2.5f, -4.5f)),
    CUSTOM("Kustom", listOf(0f, 0f, 0f, 0f, 0f)),
}

enum class LoudnessMode(val label: String, val hint: String, val targetGainMb: Int) {
    OFF("Mati", "Volume asli lagu", 0),
    GENTLE("Lembut", "+3 dB seimbang", 300),
    STANDARD("Standar", "+6 dB rata & tegas", 600),
    STRONG("Kuat", "+9 dB ekstra lantang", 900),
}

enum class ReverbMode(val label: String) {
    OFF("Mati"),
    SMALL_ROOM("Kamar"),
    STUDIO("Studio"),
    CONCERT_HALL("Aula Besar"),
}

data class AudioFxPreferences(
    val vibePreset: VibePreset = VibePreset.NORMAL,
    val speed: Float = 1.00f,
    val pitch: Float = 1.00f,
    val linkPitchToSpeed: Boolean = true,
    val eqEnabled: Boolean = false,
    val eqPreset: EqPreset = EqPreset.VYORA_SIGNATURE,
    val bandGainsDb: List<Float> = EqPreset.VYORA_SIGNATURE.gainsDb,
    val bassBoostPercent: Int = 0,
    val virtualizerPercent: Int = 0,
    val loudnessMode: LoudnessMode = LoudnessMode.OFF,
    val reverbMode: ReverbMode = ReverbMode.OFF,
) {
    val isAnyActive: Boolean
        get() = eqEnabled ||
            kotlin.math.abs(speed - 1f) > 0.01f ||
            kotlin.math.abs(pitch - 1f) > 0.01f ||
            bassBoostPercent > 0 ||
            virtualizerPercent > 0 ||
            loudnessMode != LoudnessMode.OFF ||
            reverbMode != ReverbMode.OFF
}

class AudioFxPreferencesStore(context: Context) {

    private val prefs = context.getSharedPreferences("vyora_audio_fx", Context.MODE_PRIVATE)
    private val gson = Gson()

    private val _state = MutableStateFlow(load())
    val state: StateFlow<AudioFxPreferences> = _state.asStateFlow()

    private fun load(): AudioFxPreferences {
        val raw = prefs.getString(KEY_JSON, null) ?: return AudioFxPreferences()
        return runCatching {
            val parsed = gson.fromJson(raw, AudioFxPreferences::class.java) ?: return AudioFxPreferences()
            val cleanBands = if (parsed.bandGainsDb.size == 5) {
                parsed.bandGainsDb.map { it.coerceIn(-12f, 12f) }
            } else {
                parsed.eqPreset.gainsDb
            }
            parsed.copy(
                speed = parsed.speed.coerceIn(0.50f, 2.00f),
                pitch = parsed.pitch.coerceIn(0.50f, 2.00f),
                bandGainsDb = cleanBands,
                bassBoostPercent = parsed.bassBoostPercent.coerceIn(0, 100),
                virtualizerPercent = parsed.virtualizerPercent.coerceIn(0, 100),
            )
        }.getOrDefault(AudioFxPreferences())
    }

    fun applyVibePreset(preset: VibePreset) = update { cur ->
        if (preset == VibePreset.CUSTOM) {
            cur.copy(vibePreset = VibePreset.CUSTOM)
        } else {
            cur.copy(
                vibePreset = preset,
                speed = preset.speed,
                pitch = preset.pitch,
                reverbMode = preset.defaultReverb,
            )
        }
    }

    fun setSpeed(speed: Float) = update { cur ->
        val clamped = (kotlin.math.round(speed * 100f) / 100f).coerceIn(0.50f, 2.00f)
        cur.copy(
            vibePreset = VibePreset.CUSTOM,
            speed = clamped,
            pitch = if (cur.linkPitchToSpeed) clamped else cur.pitch,
        )
    }

    fun setPitch(pitch: Float) = update { cur ->
        val clamped = (kotlin.math.round(pitch * 100f) / 100f).coerceIn(0.50f, 2.00f)
        cur.copy(
            vibePreset = VibePreset.CUSTOM,
            pitch = clamped,
        )
    }

    fun setLinkPitchToSpeed(linked: Boolean) = update { cur ->
        cur.copy(
            linkPitchToSpeed = linked,
            pitch = if (linked) cur.speed else cur.pitch,
        )
    }

    fun setEqEnabled(enabled: Boolean) = update { it.copy(eqEnabled = enabled) }

    fun applyEqPreset(preset: EqPreset) = update { cur ->
        cur.copy(
            eqEnabled = true,
            eqPreset = preset,
            bandGainsDb = if (preset == EqPreset.CUSTOM) cur.bandGainsDb else preset.gainsDb,
        )
    }

    fun setBandGain(index: Int, gainDb: Float) = update { cur ->
        if (index !in 0..4) return@update cur
        val nextBands = cur.bandGainsDb.toMutableList()
        nextBands[index] = (kotlin.math.round(gainDb * 2f) / 2f).coerceIn(-12f, 12f)
        cur.copy(
            eqEnabled = true,
            eqPreset = EqPreset.CUSTOM,
            bandGainsDb = nextBands,
        )
    }

    fun setBassBoost(percent: Int) = update { it.copy(bassBoostPercent = percent.coerceIn(0, 100)) }

    fun setVirtualizer(percent: Int) = update { it.copy(virtualizerPercent = percent.coerceIn(0, 100)) }

    fun setLoudness(mode: LoudnessMode) = update { it.copy(loudnessMode = mode) }

    fun setReverb(mode: ReverbMode) = update { it.copy(reverbMode = mode) }

    fun resetAll() = update { AudioFxPreferences() }

    private fun update(transform: (AudioFxPreferences) -> AudioFxPreferences) {
        val next = transform(_state.value)
        _state.value = next
        prefs.edit().putString(KEY_JSON, gson.toJson(next)).apply()
    }

    private companion object {
        const val KEY_JSON = "audio_fx_prefs_v1"
    }
}
