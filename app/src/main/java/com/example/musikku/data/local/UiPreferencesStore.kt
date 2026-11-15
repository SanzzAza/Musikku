package com.example.musikku.data.local

import android.content.Context
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
        return UiPreferences(
            style = style,
            dynamicCoverTheme = prefs.getBoolean(KEY_DYNAMIC_COVER, true),
            collapseNavbarOnScroll = prefs.getBoolean(KEY_COLLAPSE_NAV, true),
            fontStyle = font,
        )
    }

    fun setStyle(style: UiStyle) = update { it.copy(style = style) }

    fun setDynamicCoverTheme(enabled: Boolean) = update { it.copy(dynamicCoverTheme = enabled) }

    fun setCollapseNavbarOnScroll(enabled: Boolean) = update { it.copy(collapseNavbarOnScroll = enabled) }

    fun setFontStyle(font: AppFontStyle) = update { it.copy(fontStyle = font) }

    private fun update(transform: (UiPreferences) -> UiPreferences) {
        val next = transform(_state.value)
        _state.value = next
        prefs.edit()
            .putString(KEY_STYLE, next.style.name)
            .putBoolean(KEY_DYNAMIC_COVER, next.dynamicCoverTheme)
            .putBoolean(KEY_COLLAPSE_NAV, next.collapseNavbarOnScroll)
            .putString(KEY_FONT, next.fontStyle.name)
            .apply()
    }

    private companion object {
        const val KEY_STYLE = "ui_style"
        const val KEY_DYNAMIC_COVER = "dynamic_cover_theme"
        const val KEY_COLLAPSE_NAV = "collapse_navbar_on_scroll"
        const val KEY_FONT = "font_style"
    }
}
