package com.noctplayer.app.core.subtitle

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.subtitleDataStore by preferencesDataStore(name = "subtitle_settings")

data class SubtitleStyle(
    val delayMs: Long = 0L,
    val fontScale: Float = 1f,
    val textColorArgb: Int = 0xFFFFFFFF.toInt(),
    val backgroundOpacity: Float = 0.6f,
    val outlineStrength: Float = 1f // 0f = none, 1f = normal, 2f = strong
)

/**
 * Persists subtitle delay + style across sessions and app restarts.
 * Delay support here is display-side (positive delay = subtitles shown later);
 * see PROJECT_STATE.md for why negative delay is deferred.
 */
class SubtitlePrefsRepository(private val context: Context) {

    private object Keys {
        val DELAY_MS = longPreferencesKey("delay_ms")
        val FONT_SCALE = floatPreferencesKey("font_scale")
        val TEXT_COLOR = intPreferencesKey("text_color")
        val BG_OPACITY = floatPreferencesKey("bg_opacity")
        val OUTLINE = floatPreferencesKey("outline_strength")
    }

    val style: Flow<SubtitleStyle> = context.subtitleDataStore.data.map { prefs ->
        SubtitleStyle(
            delayMs = prefs[Keys.DELAY_MS] ?: 0L,
            fontScale = prefs[Keys.FONT_SCALE] ?: 1f,
            textColorArgb = prefs[Keys.TEXT_COLOR] ?: 0xFFFFFFFF.toInt(),
            backgroundOpacity = prefs[Keys.BG_OPACITY] ?: 0.6f,
            outlineStrength = prefs[Keys.OUTLINE] ?: 1f
        )
    }

    suspend fun setDelayMs(delayMs: Long) {
        context.subtitleDataStore.edit { it[Keys.DELAY_MS] = delayMs }
    }

    suspend fun setFontScale(scale: Float) {
        context.subtitleDataStore.edit { it[Keys.FONT_SCALE] = scale }
    }

    suspend fun setTextColor(argb: Int) {
        context.subtitleDataStore.edit { it[Keys.TEXT_COLOR] = argb }
    }

    suspend fun setBackgroundOpacity(opacity: Float) {
        context.subtitleDataStore.edit { it[Keys.BG_OPACITY] = opacity }
    }

    suspend fun setOutlineStrength(strength: Float) {
        context.subtitleDataStore.edit { it[Keys.OUTLINE] = strength }
    }
}
