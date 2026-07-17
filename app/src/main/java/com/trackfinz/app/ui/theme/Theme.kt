package com.trackfinz.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary          = Teal500,
    onPrimary        = White,
    primaryContainer = Teal400.copy(alpha = 0.15f),
    secondary        = Emerald500,
    onSecondary      = White,
    background       = SoftGray100,
    onBackground     = Navy900,
    surface          = White,
    onSurface        = Navy900,
    surfaceVariant   = SoftGray200,
    outline          = SoftGray400,
    error            = ExpenseRed
)

private val DarkColorScheme = darkColorScheme(
    primary          = Teal400,
    onPrimary        = Navy900,
    primaryContainer = Teal600.copy(alpha = 0.25f),
    secondary        = Emerald400,
    onSecondary      = Navy900,
    background       = Navy900,
    onBackground     = White,
    surface          = Navy800,
    onSurface        = White,
    surfaceVariant   = Navy700,
    outline          = SoftGray600,
    error            = ExpenseRed
)

@Composable
fun TrackFinzTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? android.app.Activity)?.window ?: return@SideEffect
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = TrackFinzTypography,
        content     = content
    )
}
