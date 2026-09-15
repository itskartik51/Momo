package com.personal.momo.UI_Screens

import android.content.Context
import android.graphics.Path
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.PathShape
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.delay
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
 * Screen-wide realistic bottom-center cannon blast with zero-latency audio and hardware micro-haptics.
 */
@Composable
fun MomoBottomCannonCelebration(
    trigger: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    var celebrationParties by remember { mutableStateOf<List<Party>>(emptyList()) }
    var hasCelebratedToday by rememberSaveable { mutableStateOf(false) }

    var soundId by remember { mutableStateOf(0) }
    var isSoundLoaded by remember { mutableStateOf(false) }

    val vibrator = remember(context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    val soundPool = remember {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        SoundPool.Builder()
            .setMaxStreams(2)
            .setAudioAttributes(audioAttributes)
            .build().apply {
                setOnLoadCompleteListener { _, _, status ->
                    if (status == 0) {
                        isSoundLoaded = true
                    }
                }
            }
    }

    DisposableEffect(Unit) {
        val resId = context.resources.getIdentifier("popper", "raw", context.packageName)
        if (resId != 0) {
            soundId = soundPool.load(context, resId, 1)
        }
        onDispose {
            soundPool.release()
        }
    }

    LaunchedEffect(trigger) {
        if (trigger && !hasCelebratedToday) {
            hasCelebratedToday = true

            // Audio decode synchronization check
            val resId = context.resources.getIdentifier("popper", "raw", context.packageName)
            if (resId != 0) {
                var elapsed = 0L
                while (!isSoundLoaded && elapsed < 400L) {
                    delay(20)
                    elapsed += 20
                }
            }

            // 1. Hardware micro-tap (feather-light click, no motor spin)
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    vibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createOneShot(12L, 35))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(12L)
                }
            } catch (_: Throwable) {
            }

            // 2. Play zero-latency SoundPool audio
            if (isSoundLoaded && soundId != 0) {
                soundPool.play(soundId, 1f, 1f, 1, 0, 1f)
            }

            // 3. Upward cannon party blast triggered simultaneously
            celebrationParties = listOf(
                Party(
                    speed = 35f,
                    maxSpeed = 65f,
                    damping = 0.93f,
                    angle = 270,
                    spread = 65,
                    colors = MomoCelebrationColors,
                    shapes = MomoCelebrationShapes.shapes,
                    timeToLive = 5000L,
                    position = Position.Relative(0.5, 1.0),
                    emitter = Emitter(duration = 200, TimeUnit.MILLISECONDS).max(140)
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
 * Dense, continuous colorful snowfall overlay for milestone event cards.
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
                timeToLive = 3500L,
                position = Position.Relative(0.0, 0.0).between(Position.Relative(1.0, 0.0)),
                emitter = Emitter(duration = 100, TimeUnit.DAYS).perSecond(35)
            )
        )
    }

    KonfettiView(
        modifier = modifier,
        parties = snowParties
    )
}
