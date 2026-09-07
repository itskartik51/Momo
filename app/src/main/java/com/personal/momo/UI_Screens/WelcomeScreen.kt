package com.personal.momo.UI_Screens

import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PathMeasure
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.personal.momo.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Teeno presentation styles:
 * - OVERLAY: Logo style (hello cursive MOMO typography ke andar 72-77% width span par draw hoga)
 * - INLINE: Side-by-side (hello aur MOMO ek horizontal line me)
 * - STACKED: Vertical elegance (hello upar aur MOMO theek uske niche)
 */
enum class WelcomeLayoutMode {
    OVERLAY,
    INLINE,
    STACKED
}

private val WelcomeMomoGradient = Brush.verticalGradient(
    colors = listOf(
        Color(0xFFC91D3B), // Dark Crimson Red (Top)
        Color(0xFFFF5E79)  // Luminous Coral Red (Bottom)
    )
)

private val WelcomeMomoBoldFont = FontFamily(Font(R.font.momo_bold))

@Composable
fun WelcomeScreen(
    layoutMode: WelcomeLayoutMode = WelcomeLayoutMode.OVERLAY,
    onAnimationFinished: () -> Unit = {}
) {
    val currentOnFinished = rememberUpdatedState(onAnimationFinished)
    val isDark = isSystemInDarkTheme()

    // Animation Controllers
    val drawProgress = remember { Animatable(0f) }
    val momoAlpha = remember { Animatable(0f) }
    val momoScale = remember { Animatable(0.94f) }
    val glowAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        // Step 1: Ambient backdrop glow
        launch {
            glowAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 500)
            )
        }

        // Step 2: Cursive handwriting stroke draw
        launch {
            drawProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 1150, easing = FastOutSlowInEasing)
            )
        }

        // Step 3: MOMO reveal synchronized with hello finish
        delay(850)
        launch {
            momoAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 400)
            )
        }
        launch {
            momoScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
        }

        // Step 4: Settle pause and navigate to HomeScreen
        delay(950)
        currentOnFinished.value()
    }

    val strokeColor = MaterialTheme.colorScheme.onBackground
    val backgroundColor = MaterialTheme.colorScheme.background
    val primaryAccent = MaterialTheme.colorScheme.primary

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        // Ambient Radial Glow using dynamic Theme Tokens
        Box(
            modifier = Modifier
                .size(460.dp)
                .alpha(glowAlpha.value)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            primaryAccent.copy(alpha = if (isDark) 0.16f else 0.05f),
                            primaryAccent.copy(alpha = if (isDark) 0.06f else 0.02f),
                            Color.Transparent
                        )
                    )
                )
        )

        // Selected Layout Presentation
        when (layoutMode) {
            WelcomeLayoutMode.OVERLAY -> {
                OverlayLayout(
                    drawProgress = drawProgress.value,
                    momoAlpha = momoAlpha.value,
                    momoScale = momoScale.value,
                    strokeColor = strokeColor
                )
            }

            WelcomeLayoutMode.INLINE -> {
                InlineLayout(
                    drawProgress = drawProgress.value,
                    momoAlpha = momoAlpha.value,
                    momoScale = momoScale.value,
                    strokeColor = strokeColor
                )
            }

            WelcomeLayoutMode.STACKED -> {
                StackedLayout(
                    drawProgress = drawProgress.value,
                    momoAlpha = momoAlpha.value,
                    momoScale = momoScale.value,
                    strokeColor = strokeColor
                )
            }
        }
    }
}

/**
 * Mode 1: Exact Brand Logo Overlap Layout
 * MOMO is locked at 76.sp.
 * hello is calibrated to 218.dp width (74.5-75% coverage of MOMO)
 * with vertical height strictly locked at 48.dp.
 */
@Composable
private fun OverlayLayout(
    drawProgress: Float,
    momoAlpha: Float,
    momoScale: Float,
    strokeColor: Color
) {
    Box(
        contentAlignment = Alignment.Center
    ) {
        // Hero Base: MOMO Typography locked at 76.sp
        Text(
            text = "MOMO",
            fontFamily = WelcomeMomoBoldFont,
            fontSize = 76.sp,
            letterSpacing = 1.5.sp,
            style = TextStyle(brush = WelcomeMomoGradient),
            modifier = Modifier
                .graphicsLayer {
                    alpha = momoAlpha
                    scaleX = momoScale
                    scaleY = momoScale
                }
        )

        // Overlay: Cursive Hello calibrated to 72-77% span (218.dp) with locked 48.dp height
        AppleCursiveHelloCanvas(
            modifier = Modifier
                .size(width = 218.dp, height = 48.dp),
            progress = drawProgress,
            strokeColor = strokeColor,
            strokeWidth = 2.dp
        )
    }
}

/**
 * Mode 2: Side-by-Side Inline Layout
 */
@Composable
private fun InlineLayout(
    drawProgress: Float,
    momoAlpha: Float,
    momoScale: Float,
    strokeColor: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        AppleCursiveHelloCanvas(
            modifier = Modifier
                .size(width = 160.dp, height = 34.dp),
            progress = drawProgress,
            strokeColor = strokeColor,
            strokeWidth = 1.8.dp
        )

        Spacer(modifier = Modifier.width(10.dp))

        Text(
            text = "MOMO",
            fontFamily = WelcomeMomoBoldFont,
            fontSize = 44.sp,
            letterSpacing = 1.sp,
            style = TextStyle(brush = WelcomeMomoGradient),
            modifier = Modifier
                .graphicsLayer {
                    alpha = momoAlpha
                    scaleX = momoScale
                    scaleY = momoScale
                }
        )
    }
}

/**
 * Mode 3: Vertically Stacked Layout
 */
@Composable
private fun StackedLayout(
    drawProgress: Float,
    momoAlpha: Float,
    momoScale: Float,
    strokeColor: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        AppleCursiveHelloCanvas(
            modifier = Modifier
                .size(width = 190.dp, height = 40.dp),
            progress = drawProgress,
            strokeColor = strokeColor,
            strokeWidth = 1.8.dp
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "MOMO",
            fontFamily = WelcomeMomoBoldFont,
            fontSize = 54.sp,
            letterSpacing = 1.2.sp,
            style = TextStyle(brush = WelcomeMomoGradient),
            modifier = Modifier
                .graphicsLayer {
                    alpha = momoAlpha
                    scaleX = momoScale
                    scaleY = momoScale
                }
        )
    }
}

/**
 * Precision Cursive Handwriting Engine.
 * - 'h': Upward diagonal entry, straight vertical stem drop, rounded arch shoulder.
 * - 'e': Authentic eyelet loop crossing back over entry stroke.
 * - 'l' & 'l': Ascender loops aligned within waistline.
 * - 'o': Round oval finish with horizontal flourish across final letter.
 */
@Composable
fun AppleCursiveHelloCanvas(
    modifier: Modifier = Modifier,
    progress: Float = 1f,
    strokeColor: Color,
    strokeWidth: Dp = 2.dp
) {
    val density = LocalDensity.current
    val strokeWidthPx = with(density) { strokeWidth.toPx() }

    val fullPath = remember {
        android.graphics.Path().apply {
            // --- Entry & 'h' ---
            moveTo(20f, 75f)
            cubicTo(42f, 75f, 62f, 70f, 82f, 45f)
            cubicTo(92f, 30f, 98f, 15f, 106f, 15f)
            cubicTo(112f, 15f, 112f, 24f, 108f, 40f)
            cubicTo(104f, 56f, 98f, 74f, 96f, 85f)
            cubicTo(96f, 68f, 106f, 53f, 122f, 53f)
            cubicTo(134f, 53f, 140f, 64f, 140f, 75f)
            cubicTo(140f, 82f, 144f, 85f, 152f, 85f)

            // --- Ligature to 'e' & 'e' Loop ---
            cubicTo(168f, 85f, 188f, 82f, 206f, 66f)
            cubicTo(216f, 58f, 226f, 52f, 232f, 52f)
            cubicTo(238f, 52f, 240f, 60f, 234f, 68f)
            cubicTo(225f, 76f, 210f, 82f, 206f, 84f)
            cubicTo(214f, 86f, 226f, 86f, 242f, 82f)

            // --- Ligature to first 'l' & 'l1' ---
            cubicTo(258f, 78f, 276f, 64f, 290f, 42f)
            cubicTo(300f, 26f, 308f, 15f, 316f, 15f)
            cubicTo(322f, 15f, 322f, 25f, 317f, 45f)
            cubicTo(311f, 66f, 306f, 78f, 312f, 85f)
            cubicTo(316f, 87f, 324f, 86f, 336f, 80f)

            // --- Ligature to second 'l' & 'l2' ---
            cubicTo(350f, 74f, 368f, 58f, 380f, 38f)
            cubicTo(388f, 24f, 396f, 15f, 404f, 15f)
            cubicTo(410f, 15f, 410f, 25f, 405f, 45f)
            cubicTo(399f, 66f, 395f, 78f, 401f, 85f)
            cubicTo(405f, 87f, 414f, 86f, 424f, 80f)

            // --- Ligature to 'o', 'o' Oval & Exit Flourish ---
            cubicTo(434f, 74f, 442f, 62f, 452f, 55f)
            cubicTo(462f, 48f, 474f, 52f, 478f, 62f)
            cubicTo(482f, 72f, 476f, 85f, 464f, 85f)
            cubicTo(452f, 85f, 446f, 74f, 450f, 63f)
            cubicTo(454f, 54f, 464f, 52f, 474f, 56f)
            cubicTo(486f, 60f, 500f, 62f, 515f, 60f)
        }
    }

    val pathMeasure = remember(fullPath) { PathMeasure(fullPath, false) }
    val pathLength = remember(pathMeasure) { pathMeasure.length }

    val segmentPath = remember { android.graphics.Path() }
    val matrix = remember { Matrix() }

    val strokeColorArgb = strokeColor.toArgb()
    val paint = remember(strokeWidthPx, strokeColorArgb) {
        Paint().apply {
            color = strokeColorArgb
            style = Paint.Style.STROKE
            this.strokeWidth = strokeWidthPx
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            isAntiAlias = true
        }
    }

    Canvas(modifier = modifier) {
        val clampedProgress = progress.coerceIn(0f, 1f)
        if (clampedProgress > 0f) {
            segmentPath.reset()
            pathMeasure.getSegment(0f, pathLength * clampedProgress, segmentPath, true)

            val scaleX = size.width / 525f
            val scaleY = size.height / 100f
            matrix.reset()
            matrix.setScale(scaleX, scaleY)
            segmentPath.transform(matrix)

            drawIntoCanvas { canvas ->
                canvas.nativeCanvas.drawPath(segmentPath, paint)
            }
        }
    }
}
