package com.noctplayer.app.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val NoctColorScheme = darkColorScheme(
    primary = NoctAccent,
    onPrimary = NoctBackground,
    secondary = NoctSecondary,
    background = NoctBackground,
    surface = NoctSurface,
    surfaceVariant = NoctSurfaceVariant,
    surfaceContainer = NoctSurfaceContainer,
    surfaceContainerHigh = NoctSurfaceElevated,
    onBackground = NoctOnSurface,
    onSurface = NoctOnSurface,
    onSurfaceVariant = NoctOnSurfaceMuted,
    outline = NoctOutline,
    error = NoctError
)

@Composable
fun NoctPlayerTheme(content: @Composable () -> Unit) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val activity = view.context as? Activity
            activity?.window?.let { window ->
                // App is AMOLED-dark only: system bar icons should always render light.
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
            }
        }
    }
    MaterialTheme(
        colorScheme = NoctColorScheme,
        typography = NoctTypography,
        content = content
    )
}
