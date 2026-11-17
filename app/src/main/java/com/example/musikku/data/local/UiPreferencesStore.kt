package com.example.musikku.data.local

import android.content.Context
import com.example.musikku.data.ytmusic.AudioQuality
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class UiStyle(val label: String, val tagline: String) {
    LIQUID_GLASS("Liquid Glass", "Kaca bening melayang, efek blur & lensa"),
    EDITORIAL("Editorial", "Matte obsidian hangat, garis tegas & tenang"),
}

enum class AppFontStyle(val label: String, val preview: String) {
    MODERN("Modern", "Bersih & modern"),
    EDITORIAL("Editorial", "Judul serif elegan"),
    CLASSIC_SERIF("Klasik Serif", "Semua teks serif"),
    MONO_CLEAN("Studio", "Rapi & teknis"),
}

data class UiPreferences(
    val style: UiStyle = UiStyle.LIQUID_GLASS,
    val dynamicCoverTheme: Boolean = true,
    val collapseNavbarOnScroll: Boolean = true,
    val fontStyle: AppFontStyle = AppFontStyle.MODERN,
    val audioQuality: AudioQuality = AudioQuality.HIGH,
    val dataSaverOnMobile: Boolean = false,
    val skipSilence: Boolean = false,
) {
    val isGlass: Boolean get() = style == UiStyle.LIQUID_GLASS
}

/** Menyimpan preferensi tampilan (Liquid Glass / Editorial, tema ikut cover, navbar melipat, font). */
class UiPreferencesStore(context: Context) {

    private val prefs = context.getSharedPreferences("ui_prefs", Context.MODE_PRIVATE)

    private val _state = MutableStateFlow(load())
    val state: StateFlow<UiPreferences> = _state.asStateFlow()

    private fun load(): UiPreferences {
        val style = prefs.getString(KEY_STYLE, null)
            ?.let { runCatching { UiStyle.valueOf(it) }.getOrNull() }
            ?: UiStyle.LIQUID_GLASS
        val font = prefs.getString(KEY_FONT, null)
            ?.let { runCatching { AppFontStyle.valueOf(it) }.getOrNull() }
            ?: AppFontStyle.MODERN
        val quality = prefs.getString(KEY_QUALITY, null)
            ?.let { runCatching { AudioQuality.valueOf(it) }.getOrNull() }
            ?: AudioQuality.HIGH
        return UiPreferences(
            style = style,
            dynamicCoverTheme = prefs.getBoolean(KEY_DYNAMIC_COVER, true),
            collapseNavbarOnScroll = prefs.getBoolean(KEY_COLLAPSE_NAV, true),
            fontStyle = font,
            audioQuality = quality,
            dataSaverOnMobile = prefs.getBoolean(KEY_DATA_SAVER, false),
            skipSilence = prefs.getBoolean(KEY_SKIP_SILENCE, false),
        )
    }

    fun setStyle(style: UiStyle) = update { it.copy(style = style) }

    fun setDynamicCoverTheme(enabled: Boolean) = update { it.copy(dynamicCoverTheme = enabled) }

    fun setCollapseNavbarOnScroll(enabled: Boolean) = update { it.copy(collapseNavbarOnScroll = enabled) }

    fun setFontStyle(font: AppFontStyle) = update { it.copy(fontStyle = font) }

    fun setAudioQuality(quality: AudioQuality) = update { it.copy(audioQuality = quality) }

    fun setDataSaverOnMobile(enabled: Boolean) = update { it.copy(dataSaverOnMobile = enabled) }

    fun setSkipSilence(enabled: Boolean) = update { it.copy(skipSilence = enabled) }

    private fun update(transform: (UiPreferences) -> UiPreferences) {
        val next = transform(_state.value)
        _state.value = next
        prefs.edit()
            .putString(KEY_STYLE, next.style.name)
            .putBoolean(KEY_DYNAMIC_COVER, next.dynamicCoverTheme)
            .putBoolean(KEY_COLLAPSE_NAV, next.collapseNavbarOnScroll)
            .putString(KEY_FONT, next.fontStyle.name)
            .putString(KEY_QUALITY, next.audioQuality.name)
            .putBoolean(KEY_DATA_SAVER, next.dataSaverOnMobile)
            .putBoolean(KEY_SKIP_SILENCE, next.skipSilence)
            .apply()
    }

    private companion object {
        const val KEY_STYLE = "ui_style"
        const val KEY_DYNAMIC_COVER = "dynamic_cover_theme"
        const val KEY_COLLAPSE_NAV = "collapse_navbar_on_scroll"
        const val KEY_FONT = "font_style"
        const val KEY_QUALITY = "audio_quality"
        const val KEY_DATA_SAVER = "data_saver_mobile"
        const val KEY_SKIP_SILENCE = "skip_silence"
    }
}
