package com.personal.momo.UI_Screens

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Momo Core Brand Palette
val MomoPrimaryDark = Color(0xFFFF334B)
val MomoPrimaryLight = Color(0xFFE5253F)

val BackgroundDark = Color(0xFF0F1015)
val BackgroundLight = Color(0xFFF8F9FA)

val SurfaceDark = Color(0xFF171922)
val SurfaceLight = Color(0xFFFFFFFF)

val OutlineVariantDark = Color(0xFF262936)
val OutlineVariantLight = Color(0xFFE2E4E9)

val TextPrimaryDark = Color(0xFFFFFFFF)
val TextPrimaryLight = Color(0xFF111318)

val TextSecondaryDark = Color(0xFF8A90A2)
val TextSecondaryLight = Color(0xFF6C7280)

private val DarkColorScheme = darkColorScheme(
    primary = MomoPrimaryDark,
    background = BackgroundDark,
    surface = SurfaceDark,
    outlineVariant = OutlineVariantDark,
    onPrimary = Color.White,
    onBackground = TextPrimaryDark,
    onSurface = TextPrimaryDark,
    onSurfaceVariant = TextSecondaryDark
)

private val LightColorScheme = lightColorScheme(
    primary = MomoPrimaryLight,
    background = BackgroundLight,
    surface = SurfaceLight,
    outlineVariant = OutlineVariantLight,
    onPrimary = Color.White,
    onBackground = TextPrimaryLight,
    onSurface = TextPrimaryLight,
    onSurfaceVariant = TextSecondaryLight
)

@Composable
fun MomoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
