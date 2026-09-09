package com.personal.momo.UI_Screens

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Brush
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

// Centralized Momo Brand Gradients
val MomoRedDark = Color(0xFFC91D3B)
val MomoRedLight = Color(0xFFFF5E79)

val MomoPrimaryGradient = Brush.verticalGradient(
    colors = listOf(
        MomoRedDark,  // Gehra Lal (Top)
        MomoRedLight  // Luminous Soft Lal (Bottom)
    )
)

// Endless River Gradient (Aqua to Ocean Blue)
val Gradient2 = Brush.verticalGradient(
    colors = listOf(
        Color(0xFF43CEA2), // Aqua Mint (Top)
        Color(0xFF185A9D)  // Deep Ocean Blue (Bottom)
    )
)

// Vine Gradient (Emerald Green to Deep Forest)
val Gradient3 = Brush.verticalGradient(
    colors = listOf(
        Color(0xFF00BF8F), // Bright Emerald (Top)
        Color(0xFF001510)  // Deep Forest Green (Bottom)
    )
)

// Virgin America Gradient (Violet to Crimson Red)
val Gradient4 = Brush.horizontalGradient(
    colors = listOf(
        Color(0xFF7B4397), // Deep Violet (Left)
        Color(0xFFDC2430)  // Vivid Crimson Red (Right)
    )
)

// Purple Bliss Gradient (Teal Cyan to Deep Aubergine)
val Gradient5 = Brush.verticalGradient(
    colors = listOf(
        Color(0xFF0B8793), // Vibrant Teal Cyan (Top)
        Color(0xFF360033)  // Deep Purple (Bottom)
    )
)

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
