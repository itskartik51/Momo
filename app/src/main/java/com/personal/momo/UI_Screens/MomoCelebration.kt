package com.personal.momo.UI_Screens

import android.graphics.Path
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.PathShape
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import nl.dionsegijn.konfetti.compose.KonfettiView
import nl.dionsegijn.konfetti.core.Party
import nl.dionsegijn.konfetti.core.Position
import nl.dionsegijn.konfetti.core.emitter.Emitter
import nl.dionsegijn.konfetti.core.models.Shape
import java.util.concurrent.TimeUnit

val MomoCelebrationColors = listOf(
    0xFFC91D3B.toInt(),
    0xFFFF5E79.toInt(),
    0xFFFFB800.toInt(),
    0xFFFF3B30.toInt(),
    0xFFFF9500.toInt(),
    0xFFE040FB.toInt()
)

object MomoCelebrationShapes {
    private fun createStarShape(): Shape {
        val path = Path().apply {
            val points = 5
            val outerRadius = 12f
            val innerRadius = 5.5f
            val cx = 12f
            val cy = 12f
            var angle = -Math.PI / 2.0
            val angleStep = Math.PI / points
            moveTo((cx + outerRadius * Math.cos(angle)).toFloat(), (cy + outerRadius * Math.sin(angle)).toFloat())
            for (i in 1 until points * 2) {
                angle += angleStep
                val r = if (i % 2 == 0) outerRadius else innerRadius
                lineTo((cx + r * Math.cos(angle)).toFloat(), (cy + r * Math.sin(angle)).toFloat())
            }
            close()
        }
        val drawable = ShapeDrawable(PathShape(path, 24f, 24f))
        return Shape.DrawableShape(drawable, tint = true)
    }

    val shapes: List<Shape> by lazy {
        try {
            listOf(
                Shape.Square,
                Shape.Circle,
                Shape.Rectangle(0.25f),
                createStarShape()
            )
        } catch (_: Throwable) {
            listOf(Shape.Square, Shape.Circle, Shape.Rectangle(0.25f))
        }
    }
}

/**
 * Screen-wide realistic bottom-center cannon blast triggered on Milestone anniversaries.
 */
@Composable
fun MomoBottomCannonCelebration(
    trigger: Boolean,
    modifier: Modifier = Modifier
) {
    var celebrationParties by remember { mutableStateOf<List<Party>>(emptyList()) }
    var hasCelebratedToday by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(trigger) {
        if (trigger && !hasCelebratedToday) {
            hasCelebratedToday = true
            celebrationParties = listOf(
                Party(
                    speed = 16f,
                    maxSpeed = 44f,
                    damping = 0.89f,
                    angle = 270,
                    spread = 82,
                    colors = MomoCelebrationColors,
                    shapes = MomoCelebrationShapes.shapes,
                    timeToLive = 3800L,
                    position = Position.Relative(0.5, 1.0),
                    emitter = Emitter(duration = 150, TimeUnit.MILLISECONDS).max(120)
                )
            )
        }
    }

    if (celebrationParties.isNotEmpty()) {
        KonfettiView(
            modifier = modifier.fillMaxSize(),
            parties = celebrationParties
        )
    }
}

/**
 * Gentle, slow-drifting colorful snowfall overlay for milestone event cards.
 */
@Composable
fun MomoMilestoneSnowfall(
    modifier: Modifier = Modifier
) {
    val snowParties = remember {
        listOf(
            Party(
                speed = 0.6f,
                maxSpeed = 2.2f,
                damping = 0.95f,
                angle = 90,
                spread = 60,
                colors = MomoCelebrationColors,
                shapes = MomoCelebrationShapes.shapes,
                timeToLive = 3200L,
                position = Position.Relative(0.0, 0.0).between(Position.Relative(1.0, 0.0)),
                emitter = Emitter(duration = 5, TimeUnit.SECONDS).perSecond(12)
            )
        )
    }

    KonfettiView(
        modifier = modifier,
        parties = snowParties
    )
}
