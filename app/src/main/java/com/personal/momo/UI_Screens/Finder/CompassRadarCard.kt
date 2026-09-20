package com.personal.momo.UI_Screens.Finder

import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.personal.momo.Cache.CacheManager
import com.personal.momo.Proximity.ProximityMath
import java.util.Locale
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun CompassRadarCard(
    partnerInfo: CacheManager.UserLocationInfo,
    selfInfo: CacheManager.UserLocationInfo,
    distanceMeters: Int?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var isScreenActive by remember { mutableStateOf(true) }
    var continuousAzimuth by remember { mutableFloatStateOf(0f) }
    var lastRawAzimuth by remember { mutableFloatStateOf(0f) }

    val soundPlayer = remember { CompassSoundPlayer(context) }

    // Scoped Lifecycle Observer: Stop sensors & audio on Pause / Stop / Minimize
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    isScreenActive = true
                }
                Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP -> {
                    isScreenActive = false
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            isScreenActive = false
            soundPlayer.release()
        }
    }

    // Hardware Rotation Vector Sensor
    DisposableEffect(isScreenActive) {
        if (!isScreenActive) {
            return@DisposableEffect onDispose {}
        }

        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

        val listener = object : SensorEventListener {
            val rotationMatrix = FloatArray(9)
            val orientationAngles = FloatArray(3)

            override fun onSensorChanged(event: SensorEvent) {
                if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
                    SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                    SensorManager.getOrientation(rotationMatrix, orientationAngles)
                    val rawDeg = ((Math.toDegrees(orientationAngles[0].toDouble()) + 360.0) % 360.0).toFloat()

                    var diff = rawDeg - lastRawAzimuth
                    if (diff > 180f) diff -= 360f
                    if (diff < -180f) diff += 360f

                    continuousAzimuth += diff
                    lastRawAzimuth = rawDeg
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        if (rotationSensor != null) {
            sensorManager.registerListener(listener, rotationSensor, SensorManager.SENSOR_DELAY_UI)
        }

        onDispose {
            sensorManager.unregisterListener(listener)
        }
    }

    val animatedAzimuth by animateFloatAsState(
        targetValue = continuousAzimuth,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "compassSmoothSpring"
    )

    val normalizedAzimuth = remember(animatedAzimuth) {
        ((animatedAzimuth % 360f) + 360f) % 360f
    }

    // Unified Geodesic Bearing from centralized ProximityMath
    val partnerBearing = remember(
        selfInfo.latitude,
        selfInfo.longitude,
        partnerInfo.latitude,
        partnerInfo.longitude
    ) {
        val sLat = selfInfo.latitude
        val sLng = selfInfo.longitude
        val pLat = partnerInfo.latitude
        val pLng = partnerInfo.longitude

        if (sLat != null && sLng != null && pLat != null && pLng != null &&
            (sLat != 0.0 || sLng != 0.0)
        ) {
            ProximityMath.calculateBearing(sLat, sLng, pLat, pLng)
        } else {
            null
        }
    }

    // Audio Tick bound to animated heading crossing 30° / 90°
    var lastStep by remember { mutableStateOf<Int?>(null) }
    var lastPlayedBoundary by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(animatedAzimuth, isScreenActive) {
        if (!isScreenActive) return@LaunchedEffect

        val currentStep = floor(animatedAzimuth / 30.0).toInt()

        lastPlayedBoundary?.let { boundary ->
            val dist = abs(animatedAzimuth - boundary)
            if (dist >= 4.0f) {
                lastPlayedBoundary = null
            }
        }

        if (lastStep == null) {
            lastStep = currentStep
        } else if (currentStep != lastStep) {
            val stepDiff = abs(currentStep - lastStep!!)
            if (stepDiff <= 3) {
                val boundary = if (currentStep > lastStep!!) currentStep * 30 else (currentStep + 1) * 30

                if (boundary != lastPlayedBoundary) {
                    val normalizedBoundary = ((boundary % 360) + 360) % 360
                    if (normalizedBoundary % 90 == 0) {
                        soundPlayer.playHeavy()
                    } else {
                        soundPlayer.playLight()
                    }
                    lastPlayedBoundary = boundary
                }
            }
            lastStep = currentStep
        }
    }

    // Current Phone Live Heading Direction via Centralized ProximityMath
    val liveHeadingDirection = remember(normalizedAzimuth) {
        ProximityMath.getCardinalDirection(normalizedAzimuth.toDouble())
    }

    // Partner's Real World Direction via Centralized ProximityMath
    val partnerCardinalDirection = remember(partnerBearing) {
        if (partnerBearing != null) ProximityMath.getCardinalDirection(partnerBearing.toDouble()) else "--"
    }

    val formattedDistance = remember(distanceMeters) {
        formatDistanceLabel(distanceMeters)
    }

    val deviationAngle = remember(normalizedAzimuth, partnerBearing) {
        if (partnerBearing != null) {
            val diff = abs(normalizedAzimuth - partnerBearing)
            if (diff > 180f) 360f - diff else diff
        } else 0f
    }

    val isTargetLocked = partnerBearing != null && deviationAngle <= 12f

    val partnerDisplayName = partnerInfo.name.ifBlank { "Partner" }

    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariantColor = MaterialTheme.colorScheme.onSurfaceVariant
    val primaryColor = MaterialTheme.colorScheme.primary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Dial Canvas
            Box(
                modifier = Modifier.size(260.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val center = this.center
                    val radius = (size.minDimension / 2f) - 38.dp.toPx()

                    val dialRotation = -animatedAzimuth

                    val diffCW = if (partnerBearing != null) {
                        ((normalizedAzimuth - partnerBearing + 360f) % 360f)
                    } else 0f

                    val isClockwiseShortest = diffCW <= 180f
                    val shortestSpan = if (isClockwiseShortest) diffCW else (360f - diffCW)

                    rotate(dialRotation, pivot = center) {
                        for (i in 0 until 120) {
                            val angleDeg = i * 3f
                            val angleRad = Math.toRadians(angleDeg.toDouble())

                            val isMajor = (i % 10 == 0)
                            val isNorthZero = (i == 0)

                            val isTickCovered = if (partnerBearing != null && shortestSpan > 1.5f) {
                                if (isClockwiseShortest) {
                                    val distFromMomo = ((angleDeg - partnerBearing + 360f) % 360f)
                                    distFromMomo <= diffCW
                                } else {
                                    val distFromNeedle = ((angleDeg - normalizedAzimuth + 360f) % 360f)
                                    distFromNeedle <= shortestSpan
                                }
                            } else false

                            val tickColor = if (isTickCovered && partnerBearing != null && shortestSpan > 0f) {
                                val fraction = if (isClockwiseShortest) {
                                    val distFromMomo = ((angleDeg - partnerBearing + 360f) % 360f)
                                    (distFromMomo / shortestSpan).coerceIn(0f, 1f)
                                } else {
                                    val distFromNeedle = ((angleDeg - normalizedAzimuth + 360f) % 360f)
                                    (1f - (distFromNeedle / shortestSpan)).coerceIn(0f, 1f)
                                }
                                lerp(primaryColor, tertiaryColor, fraction)
                            } else {
                                if (isMajor) onSurfaceColor.copy(alpha = 0.75f)
                                else onSurfaceVariantColor.copy(alpha = 0.35f)
                            }

                            val tickLength = if (isMajor) 9.dp.toPx() else 5.dp.toPx()
                            val strokeWidth = if (isTickCovered) {
                                if (isMajor) 2.2.dp.toPx() else 1.2.dp.toPx()
                            } else {
                                if (isMajor) 1.6.dp.toPx() else 0.8.dp.toPx()
                            }

                            val innerR = radius - tickLength
                            val startX = center.x + innerR * sin(angleRad).toFloat()
                            val startY = center.y - innerR * cos(angleRad).toFloat()

                            val endX = center.x + radius * sin(angleRad).toFloat()
                            val endY = center.y - radius * cos(angleRad).toFloat()

                            drawLine(
                                color = tickColor,
                                start = Offset(startX, startY),
                                end = Offset(endX, endY),
                                strokeWidth = strokeWidth,
                                cap = StrokeCap.Round
                            )

                            if (isMajor) {
                                val numberText = angleDeg.toInt().toString()
                                val numRadius = radius + 22.dp.toPx()
                                val numX = center.x + numRadius * sin(angleRad).toFloat()
                                val numY = center.y - numRadius * cos(angleRad).toFloat()

                                rotate(degrees = angleDeg, pivot = Offset(numX, numY)) {
                                    drawContext.canvas.nativeCanvas.drawText(
                                        numberText,
                                        numX,
                                        numY + 4.dp.toPx(),
                                        Paint().apply {
                                            color = if (isNorthZero) primaryColor.toArgb() else onSurfaceVariantColor.toArgb()
                                            textSize = 10.sp.toPx()
                                            textAlign = Paint.Align.CENTER
                                            typeface = Typeface.DEFAULT_BOLD
                                            isAntiAlias = true
                                        }
                                    )
                                }
                            }
                        }

                        // Partner Radar Pointer Line & Label at partnerBearing (24dp clearance)
                        if (partnerBearing != null) {
                            val momoRad = Math.toRadians(partnerBearing.toDouble())

                            val momoTickInner = radius - 12.dp.toPx()
                            val momoStartX = center.x + momoTickInner * sin(momoRad).toFloat()
                            val momoStartY = center.y - momoTickInner * cos(momoRad).toFloat()
                            val momoEndX = center.x + radius * sin(momoRad).toFloat()
                            val momoEndY = center.y - radius * cos(momoRad).toFloat()

                            drawLine(
                                color = primaryColor,
                                start = Offset(momoStartX, momoStartY),
                                end = Offset(momoEndX, momoEndY),
                                strokeWidth = 2.8.dp.toPx(),
                                cap = StrokeCap.Round
                            )

                            val momoLabelRadius = radius - 24.dp.toPx()
                            val momoX = center.x + momoLabelRadius * sin(momoRad).toFloat()
                            val momoY = center.y - momoLabelRadius * cos(momoRad).toFloat()

                            rotate(degrees = partnerBearing, pivot = Offset(momoX, momoY)) {
                                drawContext.canvas.nativeCanvas.drawText(
                                    partnerDisplayName,
                                    momoX,
                                    momoY + 4.dp.toPx(),
                                    Paint().apply {
                                        color = primaryColor.toArgb()
                                        textSize = 11.sp.toPx()
                                        textAlign = Paint.Align.CENTER
                                        typeface = Typeface.DEFAULT_BOLD
                                        isAntiAlias = true
                                    }
                                )
                            }
                        }
                    }

                    // Top Fixed Indicator Notch at 12 o'clock
                    drawLine(
                        color = tertiaryColor,
                        start = Offset(center.x, center.y - radius - 2.dp.toPx()),
                        end = Offset(center.x, center.y - radius + 12.dp.toPx()),
                        strokeWidth = 2.4.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }

                // Center Display: True Heading Degree + Live Direction
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "${normalizedAzimuth.roundToInt()}°",
                        fontSize = 34.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = liveHeadingDirection,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Primary Radar Title: Highlights Partner Direction & Distance
            val radarTitleText = if (partnerBearing != null) {
                if (isTargetLocked) {
                    "Facing $partnerDisplayName ($formattedDistance)"
                } else {
                    "$partnerDisplayName: $partnerCardinalDirection ($formattedDistance)"
                }
            } else {
                "$partnerDisplayName: -- ($formattedDistance)"
            }

            Text(
                text = radarTitleText,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = if (isTargetLocked) primaryColor else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

private fun formatDistanceLabel(meters: Int?): String {
    if (meters == null) return "--"
    return if (meters < 1000) {
        "$meters m"
    } else {
        String.format(Locale.ENGLISH, "%.1f km", meters / 1000.0)
    }
}
