package com.frankensteinlab.app.data

import android.content.Context

class AppPreferences(context: Context) {
    private val prefs = context.getSharedPreferences("preferences_v1", Context.MODE_PRIVATE)

    enum class Theme { SYSTEM, LIGHT, DARK }
    enum class MangaMode { CONTINUOUS_VERTICAL, PAGED_HORIZONTAL }

    fun theme(): Theme = runCatching {
        Theme.valueOf(prefs.getString(KEY_THEME, Theme.SYSTEM.name) ?: Theme.SYSTEM.name)
    }.getOrDefault(Theme.SYSTEM)

    fun setTheme(value: Theme) = prefs.edit().putString(KEY_THEME, value.name).apply()

    fun mangaMode(): MangaMode = runCatching {
        MangaMode.valueOf(prefs.getString(KEY_MANGA_MODE, MangaMode.CONTINUOUS_VERTICAL.name)
            ?: MangaMode.CONTINUOUS_VERTICAL.name)
    }.getOrDefault(MangaMode.CONTINUOUS_VERTICAL)

    fun setMangaMode(value: MangaMode) = prefs.edit().putString(KEY_MANGA_MODE, value.name).apply()

    fun novelFontSize(): Int = prefs.getInt(KEY_NOVEL_FONT_SIZE, 18).coerceIn(14, 32)

    fun setNovelFontSize(value: Int) = prefs.edit().putInt(KEY_NOVEL_FONT_SIZE, value.coerceIn(14, 32)).apply()

    fun reset() = prefs.edit().clear().apply()

    private companion object {
        const val KEY_THEME = "theme"
        const val KEY_MANGA_MODE = "manga_mode"
        const val KEY_NOVEL_FONT_SIZE = "novel_font_size"
    }
}
