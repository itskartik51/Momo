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
 * Teeno layout styles:
 * - OVERLAY: Logo style (hello cursive MOMO typography ke theek center par compact draw hoga)
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

        // Step 2: Apple cursive handwriting stroke
        launch {
            drawProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 1150, easing = FastOutSlowInEasing)
            )
        }

        // Step 3: Reveal MOMO right when hello finishes
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

        // Step 4: Settle pause and direct transition to HomeScreen
        delay(900)
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
 * MOMO is locked to 76.sp, with hello scaled to fit proportionally inside its frame.
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
        // Base: MOMO Typography locked at 76.sp
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

        // Overlay: Apple Cursive Handwriting scaled to 76.sp MOMO proportion
        AppleCursiveHelloCanvas(
            modifier = Modifier
                .size(width = 138.dp, height = 48.dp),
            progress = drawProgress,
            strokeColor = strokeColor,
            strokeWidth = 1.8.dp
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
                .size(width = 138.dp, height = 48.dp),
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
                .size(width = 150.dp, height = 52.dp),
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
 * Continuous single-stroke Apple cursive handwriting engine.
 * Mathematical cubic-bezier curves matching proper letter anatomy.
 */
@Composable
fun AppleCursiveHelloCanvas(
    modifier: Modifier = Modifier,
    progress: Float = 1f,
    strokeColor: Color,
    strokeWidth: Dp = 1.8.dp
) {
    val density = LocalDensity.current
    val strokeWidthPx = with(density) { strokeWidth.toPx() }

    val fullPath = remember {
        android.graphics.Path().apply {
            // 'h' - tall loop & baseline drop
            moveTo(38f, 142f)
            cubicTo(40f, 118f, 52f, 42f, 66f, 22f)
            cubicTo(73f, 12f, 81f, 18f, 78f, 32f)
            cubicTo(74f, 58f, 60f, 114f, 58f, 140f)
            cubicTo(58f, 115f, 70f, 88f, 88f, 88f)
            cubicTo(100f, 88f, 108f, 98f, 108f, 114f)
            cubicTo(108f, 126f, 106f, 134f, 114f, 138f)
            cubicTo(119f, 140f, 125f, 138f, 132f, 130f)

            // 'e' - eyelet loop
            cubicTo(138f, 122f, 152f, 86f, 164f, 86f)
            cubicTo(172f, 86f, 174f, 96f, 172f, 106f)
            cubicTo(168f, 122f, 150f, 135f, 138f, 138f)
            cubicTo(145f, 142f, 156f, 141f, 166f, 135f)
            cubicTo(176f, 128f, 184f, 116f, 192f, 104f)

            // first 'l' - ascender loop
            cubicTo(202f, 88f, 218f, 40f, 228f, 22f)
            cubicTo(235f, 12f, 242f, 18f, 240f, 32f)
            cubicTo(234f, 62f, 220f, 114f, 218f, 138f)
            cubicTo(218f, 143f, 224f, 144f, 232f, 138f)
            cubicTo(240f, 130f, 248f, 118f, 256f, 106f)

            // second 'l' - ascender loop
            cubicTo(266f, 88f, 282f, 40f, 292f, 22f)
            cubicTo(299f, 12f, 306f, 18f, 304f, 32f)
            cubicTo(298f, 62f, 284f, 114f, 282f, 138f)
            cubicTo(282f, 143f, 288f, 144f, 296f, 138f)
            cubicTo(304f, 130f, 314f, 116f, 322f, 104f)

            // 'o' - clean oval and exit flourish
            cubicTo(330f, 92f, 342f, 86f, 354f, 86f)
            cubicTo(370f, 86f, 380f, 98f, 380f, 114f)
            cubicTo(380f, 128f, 368f, 140f, 352f, 140f)
            cubicTo(338f, 140f, 328f, 126f, 328f, 112f)
            cubicTo(328f, 96f, 340f, 86f, 354f, 86f)
            cubicTo(362f, 86f, 370f, 90f, 376f, 96f)
            cubicTo(382f, 102f, 390f, 98f, 402f, 94f)
            cubicTo(412f, 90f, 422f, 88f, 434f, 88f)
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

            val scaleX = size.width / 445f
            val scaleY = size.height / 155f
            matrix.reset()
            matrix.setScale(scaleX, scaleY)
            segmentPath.transform(matrix)

            drawIntoCanvas { canvas ->
                canvas.nativeCanvas.drawPath(segmentPath, paint)
            }
        }
    }
}
