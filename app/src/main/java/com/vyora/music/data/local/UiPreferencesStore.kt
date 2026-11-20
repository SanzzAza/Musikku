package com.vyora.music.data.local

import android.content.Context
import com.vyora.music.data.ytmusic.AudioQuality
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class UiStyle(val label: String, val tagline: String) {
    LIQUID_GLASS("Liquid Glass", "Kaca bening melayang, efek blur & lensa"),
    EDITORIAL("Editorial", "Matte obsidian elegan, garis tegas & tenang"),
}

enum class ThemeMode(val label: String, val hint: String) {
    AUTO("Otomatis", "Ikut pengaturan sistem"),
    LIGHT("Terang", "Mutiara lembut"),
    DARK("Gelap", "Obsidian ungu"),
    BLACK("Hitam Pekat", "Hemat baterai OLED"),
}

enum class AppFontStyle(val label: String, val preview: String) {
    MODERN("Plus Jakarta", "Geometris modern"),
    EDITORIAL("Editorial", "Judul Playfair Serif"),
    CLASSIC_SERIF("Playfair Serif", "Semua teks serif"),
    MONO_CLEAN("JetBrains Studio", "Teknis & presisi"),
}

enum class StreamCacheLimit(val label: String, val maxBytes: Long) {
    OFF("Mati", 0L),
    MB_256("256 MB", 256L * 1024L * 1024L),
    MB_512("512 MB", 512L * 1024L * 1024L),
    GB_1("1 GB", 1024L * 1024L * 1024L),
    GB_2("2 GB", 2048L * 1024L * 1024L),
}

data class UiPreferences(
    val style: UiStyle = UiStyle.LIQUID_GLASS,
    val themeMode: ThemeMode = ThemeMode.AUTO,
    val dynamicCoverTheme: Boolean = true,
    val collapseNavbarOnScroll: Boolean = true,
    val fontStyle: AppFontStyle = AppFontStyle.MODERN,
    val audioQuality: AudioQuality = AudioQuality.HIGH,
    val dataSaverOnMobile: Boolean = false,
    val skipSilence: Boolean = false,
    val streamCacheLimit: StreamCacheLimit = StreamCacheLimit.MB_512,
) {
    val isGlass: Boolean get() = style == UiStyle.LIQUID_GLASS
}

/** Menyimpan preferensi tampilan, kualitas stream, dan batas auto-cache audio. */
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
        val mode = prefs.getString(KEY_THEME_MODE, null)
            ?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
            ?: ThemeMode.AUTO
        val quality = prefs.getString(KEY_QUALITY, null)
            ?.let { runCatching { AudioQuality.valueOf(it) }.getOrNull() }
            ?: AudioQuality.HIGH
        val cacheLimit = prefs.getString(KEY_CACHE_LIMIT, null)
            ?.let { runCatching { StreamCacheLimit.valueOf(it) }.getOrNull() }
            ?: StreamCacheLimit.MB_512
        return UiPreferences(
            style = style,
            themeMode = mode,
            dynamicCoverTheme = prefs.getBoolean(KEY_DYNAMIC_COVER, true),
            collapseNavbarOnScroll = prefs.getBoolean(KEY_COLLAPSE_NAV, true),
            fontStyle = font,
            audioQuality = quality,
            dataSaverOnMobile = prefs.getBoolean(KEY_DATA_SAVER, false),
            skipSilence = prefs.getBoolean(KEY_SKIP_SILENCE, false),
            streamCacheLimit = cacheLimit,
        )
    }

    fun setStyle(style: UiStyle) = update { it.copy(style = style) }

    fun setThemeMode(mode: ThemeMode) = update { it.copy(themeMode = mode) }

    fun setDynamicCoverTheme(enabled: Boolean) = update { it.copy(dynamicCoverTheme = enabled) }

    fun setCollapseNavbarOnScroll(enabled: Boolean) = update { it.copy(collapseNavbarOnScroll = enabled) }

    fun setFontStyle(font: AppFontStyle) = update { it.copy(fontStyle = font) }

    fun setAudioQuality(quality: AudioQuality) = update { it.copy(audioQuality = quality) }

    fun setDataSaverOnMobile(enabled: Boolean) = update { it.copy(dataSaverOnMobile = enabled) }

    fun setSkipSilence(enabled: Boolean) = update { it.copy(skipSilence = enabled) }

    fun setStreamCacheLimit(limit: StreamCacheLimit) = update { it.copy(streamCacheLimit = limit) }

    private fun update(transform: (UiPreferences) -> UiPreferences) {
        val next = transform(_state.value)
        _state.value = next
        prefs.edit()
            .putString(KEY_STYLE, next.style.name)
            .putString(KEY_THEME_MODE, next.themeMode.name)
            .putBoolean(KEY_DYNAMIC_COVER, next.dynamicCoverTheme)
            .putBoolean(KEY_COLLAPSE_NAV, next.collapseNavbarOnScroll)
            .putString(KEY_FONT, next.fontStyle.name)
            .putString(KEY_QUALITY, next.audioQuality.name)
            .putBoolean(KEY_DATA_SAVER, next.dataSaverOnMobile)
            .putBoolean(KEY_SKIP_SILENCE, next.skipSilence)
            .putString(KEY_CACHE_LIMIT, next.streamCacheLimit.name)
            .apply()
    }

    private companion object {
        const val KEY_STYLE = "ui_style"
        const val KEY_THEME_MODE = "theme_mode"
        const val KEY_DYNAMIC_COVER = "dynamic_cover_theme"
        const val KEY_COLLAPSE_NAV = "collapse_navbar_on_scroll"
        const val KEY_FONT = "font_style"
        const val KEY_QUALITY = "audio_quality"
        const val KEY_DATA_SAVER = "data_saver_mobile"
        const val KEY_SKIP_SILENCE = "skip_silence"
        const val KEY_CACHE_LIMIT = "stream_cache_limit"
    }
}
