package com.noctplayer.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val NoctColorScheme = darkColorScheme(
    primary = NoctAccent,
    background = NoctBackground,
    surface = NoctSurface,
    surfaceVariant = NoctSurfaceVariant,
    onBackground = NoctOnSurface,
    onSurface = NoctOnSurface,
    error = NoctError
)

@Composable
fun NoctPlayerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = NoctColorScheme,
        typography = NoctTypography,
        content = content
    )
}
