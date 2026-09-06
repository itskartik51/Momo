package com.personal.momo.UI_Screens

import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PathMeasure
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.personal.momo.Cache.CacheManager
import com.personal.momo.UI_Screens.Calendar.MomoCalendar
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val BellIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "Bell",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).addPath(
        pathData = PathParser().parsePathString(
            "M12 22c1.1 0 2-.9 2-2h-4c0 1.1.89 2 2 2zm6-6v-5c0-3.07-1.64-5.64-4.5-6.32V4c0-.83-.67-1.5-1.5-1.5s-1.5.67-1.5 1.5v.68C7.63 5.36 6 7.92 6 11v5l-2 2v1h16v-1l-2-2z"
        ).toNodes(),
        fill = SolidColor(Color.White)
    ).build()
}

val MomoLogoGradient = Brush.verticalGradient(
    colors = listOf(
        Color(0xFFC91D3B), // Dark Crimson Red
        Color(0xFFFF5E79)  // Luminous Coral Red
    )
)

@Composable
fun HomeScreen() {
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        CacheManager.init(context)
    }

    val avatarUrl by CacheManager.avatarUrlFlow.collectAsState()

    var isIntroFinished by rememberSaveable { mutableStateOf(false) }

    val drawProgress = remember { Animatable(if (isIntroFinished) 1f else 0f) }
    val momoAlpha = remember { Animatable(if (isIntroFinished) 1f else 0f) }
    val momoScale = remember { Animatable(if (isIntroFinished) 1f else 0.8f) }
    val introOverlayAlpha = remember { Animatable(if (isIntroFinished) 0f else 1f) }
    val homeContentAlpha = remember { Animatable(if (isIntroFinished) 1f else 0f) }
    val homeOffsetY = remember { Animatable(if (isIntroFinished) 0f else 28f) }

    LaunchedEffect(Unit) {
        if (!isIntroFinished) {
            launch {
                drawProgress.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 1100, easing = FastOutSlowInEasing)
                )
            }

            delay(750)
            launch {
                momoAlpha.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 450)
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

            delay(850)
            launch {
                introOverlayAlpha.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(durationMillis = 550, easing = LinearOutSlowInEasing)
                )
            }
            launch {
                homeContentAlpha.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 550, easing = LinearOutSlowInEasing)
                )
            }
            launch {
                homeOffsetY.animateTo(
                    targetValue = 0f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                )
            }

            delay(600)
            isIntroFinished = true
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Main Home Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .alpha(homeContentAlpha.value)
                .offset(y = homeOffsetY.value.dp)
        ) {
            HomeHeader(avatarUrl = avatarUrl)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color(0xFF060709))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 16.dp)
                ) {
                    MomoCalendar()
                }
            }
        }

        // Apple-style Opening Intro Splash
        if (!isIntroFinished) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(introOverlayAlpha.value)
                    .background(Color(0xFF060709)),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    CursiveHelloText(
                        modifier = Modifier
                            .width(135.dp)
                            .height(55.dp),
                        progress = drawProgress.value,
                        strokeWidth = 3.8.dp
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    Text(
                        text = "MOMO",
                        fontSize = 38.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp,
                        style = TextStyle(brush = MomoLogoGradient),
                        modifier = Modifier
                            .graphicsLayer {
                                alpha = momoAlpha.value
                                scaleX = momoScale.value
                                scaleY = momoScale.value
                            }
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeHeader(avatarUrl: String?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left Profile Avatar & Aesthetic Brand Greeting
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface)
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (!avatarUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = avatarUrl,
                        contentDescription = "Profile Avatar",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                CursiveHelloText(
                    modifier = Modifier
                        .width(52.dp)
                        .height(22.dp),
                    progress = 1f,
                    strokeWidth = 2.dp
                )

                Spacer(modifier = Modifier.width(6.dp))

                Text(
                    text = "MOMO",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.2.sp,
                    style = TextStyle(brush = MomoLogoGradient)
                )
            }
        }

        // Right Action Buttons
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .bounceClick(scaleDown = 0.88f) {
                        // Notifications action
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = BellIcon,
                    contentDescription = "Notifications",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp)
                )
            }

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .bounceClick(scaleDown = 0.88f) {
                        // Options menu action
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "More Options",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

/**
 * Continuous single-stroke Apple-style cursive handwriting component.
 */
@Composable
fun CursiveHelloText(
    modifier: Modifier = Modifier,
    progress: Float = 1f,
    strokeWidth: Dp = 2.5.dp
) {
    val density = LocalDensity.current
    val strokeWidthPx = with(density) { strokeWidth.toPx() }

    val fullPath = remember {
        android.graphics.Path().apply {
            // 'h'
            moveTo(14f, 62f)
            cubicTo(14f, 62f, 22f, 38f, 32f, 16f)
            cubicTo(38f, 4f, 45f, 6f, 41f, 22f)
            cubicTo(35f, 42f, 29f, 68f, 29f, 68f)
            cubicTo(29f, 68f, 37f, 44f, 49f, 44f)
            cubicTo(59f, 44f, 63f, 54f, 63f, 66f)
            // 'e'
            cubicTo(63f, 66f, 69f, 50f, 79f, 48f)
            cubicTo(89f, 46f, 91f, 56f, 83f, 62f)
            cubicTo(75f, 68f, 71f, 64f, 79f, 58f)
            // first 'l'
            cubicTo(79f, 58f, 89f, 62f, 96f, 48f)
            cubicTo(106f, 26f, 115f, 8f, 119f, 8f)
            cubicTo(123f, 8f, 119f, 24f, 111f, 46f)
            cubicTo(105f, 62f, 103f, 68f, 109f, 68f)
            // second 'l'
            cubicTo(109f, 68f, 119f, 62f, 126f, 48f)
            cubicTo(136f, 26f, 145f, 8f, 149f, 8f)
            cubicTo(153f, 8f, 149f, 24f, 141f, 46f)
            cubicTo(135f, 62f, 133f, 68f, 139f, 68f)
            // 'o'
            cubicTo(139f, 68f, 147f, 56f, 159f, 50f)
            cubicTo(171f, 44f, 181f, 52f, 181f, 60f)
            cubicTo(181f, 70f, 166f, 72f, 157f, 62f)
            cubicTo(151f, 55f, 159f, 48f, 171f, 50f)
            // flick finish
            cubicTo(177f, 51f, 187f, 48f, 195f, 48f)
        }
    }

    val pathMeasure = remember(fullPath) { PathMeasure(fullPath, false) }
    val pathLength = remember(pathMeasure) { pathMeasure.length }

    val segmentPath = remember { android.graphics.Path() }
    val matrix = remember { Matrix() }

    val paint = remember(strokeWidthPx) {
        Paint().apply {
            color = android.graphics.Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = strokeWidthPx
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

            val scaleX = size.width / 205f
            val scaleY = size.height / 76f
            matrix.reset()
            matrix.setScale(scaleX, scaleY)
            segmentPath.transform(matrix)

            drawIntoCanvas { canvas ->
                canvas.nativeCanvas.drawPath(segmentPath, paint)
            }
        }
    }
}
