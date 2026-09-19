package com.personal.momo.UI_Screens.Tracking

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.Typeface
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.AudioAttributes
import android.media.SoundPool
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
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
import com.personal.momo.Proximity.ProximityLocationService
import com.personal.momo.UI_Screens.bounceClick
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun TrackerScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var isLiveActive by remember { mutableStateOf(false) }
    val trackerState by CacheManager.trackerStateFlow.collectAsState()

    // Scoped Lifecycle: Auto-turn off Live mode on Back, Navigation, or App Minimize
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) {
                if (isLiveActive) {
                    isLiveActive = false
                    ProximityLocationService.stopLive(context)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            if (isLiveActive) {
                isLiveActive = false
                ProximityLocationService.stopLive(context)
            }
        }
    }

    BackHandler {
        if (isLiveActive) {
            isLiveActive = false
            ProximityLocationService.stopLive(context)
        }
        onBack()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // Header Top Bar
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 3.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left Section: Back Button + "Tracker" Title
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .bounceClick(scaleDown = 0.94f) {
                                    if (isLiveActive) {
                                        isLiveActive = false
                                        ProximityLocationService.stopLive(context)
                                    }
                                    onBack()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Text(
                            text = "Tracker",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Right Section: Pill Buttons (Live & Sync)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Pill 1: Live Toggle
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(
                                    if (isLiveActive) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                )
                                .then(
                                    if (isLiveActive) {
                                        Modifier.border(
                                            width = 1.dp,
                                            color = MaterialTheme.colorScheme.primary,
                                            shape = RoundedCornerShape(50)
                                        )
                                    } else Modifier
                                )
                                .bounceClick(scaleDown = 0.94f) {
                                    isLiveActive = !isLiveActive
                                    if (isLiveActive) {
                                        ProximityLocationService.startLive(context)
                                        Toast.makeText(context, "10s Live Tracking Active", Toast.LENGTH_SHORT).show()
                                    } else {
                                        ProximityLocationService.stopLive(context)
                                        Toast.makeText(context, "Live Tracking Stopped", Toast.LENGTH_SHORT).show()
                                    }
                                }
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                if (isLiveActive) {
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary)
                                    )
                                }

                                Text(
                                    text = "Live",
                                    fontSize = 12.sp,
                                    fontWeight = if (isLiveActive) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isLiveActive) MaterialTheme.colorScheme.onPrimaryContainer
                                    else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        // Pill 2: Sync One-Shot Action
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .bounceClick(scaleDown = 0.94f) {
                                    ProximityLocationService.forceSync(context)
                                    Toast.makeText(context, "Syncing location...", Toast.LENGTH_SHORT).show()
                                }
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Sync",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Card 1: Tabular Tracking Details Card
            Card(
                modifier = Modifier
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
                        .padding(horizontal = 16.dp, vertical = 16.dp)
                ) {
                    // Row 1: Partner Info
                    TrackerRowItem(item = trackerState.partnerInfo)

                    Spacer(modifier = Modifier.height(12.dp))

                    HorizontalDivider(
                        modifier = Modifier.fillMaxWidth(),
                        thickness = 0.8.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Row 2: Self Info
                    TrackerRowItem(item = trackerState.selfInfo)

                    Spacer(modifier = Modifier.height(14.dp))

                    HorizontalDivider(
                        modifier = Modifier.fillMaxWidth(),
                        thickness = 0.8.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Row 3: Inter-Device Distance
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        val distanceText = if (trackerState.distanceMeters != null) {
                            "Distance : ${trackerState.distanceMeters} m"
                        } else {
                            "Distance : -- m"
                        }

                        Text(
                            text = distanceText,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Card 2: Partner Compass & Radar Dial Card (ColorOS Minimal Style)
            CompassRadarCard(
                partnerInfo = trackerState.partnerInfo,
                selfInfo = trackerState.selfInfo,
                distanceMeters = trackerState.distanceMeters
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun CompassRadarCard(
    partnerInfo: CacheManager.UserLocationInfo,
    selfInfo: CacheManager.UserLocationInfo,
    distanceMeters: Int?
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var isScreenActive by remember { mutableStateOf(true) }
    var continuousAzimuth by remember { mutableFloatStateOf(0f) }
    var lastRawAzimuth by remember { mutableFloatStateOf(0f) }

    val soundPlayer = remember { CompassSoundPlayer(context) }

    // Scoped Lifecycle Observer: Turn off sensor & sounds when app is minimized or paused
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

    // Hardware Sensor Registration (Only active while screen is in foreground)
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

    val partnerBearing = remember(selfInfo.latitude, selfInfo.longitude, partnerInfo.latitude, partnerInfo.longitude) {
        if (selfInfo.latitude != null && selfInfo.longitude != null &&
            partnerInfo.latitude != null && partnerInfo.longitude != null
        ) {
            calculateBearing(selfInfo.latitude, selfInfo.longitude, partnerInfo.latitude, partnerInfo.longitude).toFloat()
        } else {
            null
        }
    }

    // Relative angle between phone orientation and partner
    val relativeAngle = remember(animatedAzimuth, partnerBearing) {
        if (partnerBearing != null) {
            val normalizedAzimuth = ((animatedAzimuth % 360f) + 360f) % 360f
            ((partnerBearing - normalizedAzimuth + 360f) % 360f).roundToInt()
        } else {
            null
        }
    }

    // Perfectly Synced Audio Tick: Directly bound to animated relative angle crossing 30° / 90°
    var lastStep by remember { mutableStateOf<Int?>(null) }
    var lastPlayedBoundary by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(animatedAzimuth, isScreenActive) {
        if (!isScreenActive) return@LaunchedEffect

        val continuousRelative = (partnerBearing ?: 0f) - animatedAzimuth
        val currentStep = floor(continuousRelative / 30.0).toInt()

        // Clear debounce lock when moved at least 4 degrees away from boundary
        lastPlayedBoundary?.let { boundary ->
            val dist = abs(continuousRelative - boundary)
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

    val cardinalDirection = remember(partnerBearing) {
        if (partnerBearing != null) getCardinalDirection(partnerBearing.toDouble()) else "--"
    }

    val formattedDistance = remember(distanceMeters) {
        formatDistanceLabel(distanceMeters)
    }

    val directionDistanceTitle = if (partnerBearing != null && distanceMeters != null) {
        "$cardinalDirection ($formattedDistance)"
    } else if (partnerBearing != null) {
        cardinalDirection
    } else {
        "--"
    }

    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariantColor = MaterialTheme.colorScheme.onSurfaceVariant
    val primaryColor = MaterialTheme.colorScheme.primary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary

    Card(
        modifier = Modifier
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
                .padding(vertical = 24.dp),
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

                    // Dial rotation: Rotates so that partner angle aligns with the top needle
                    val dialRotation = if (partnerBearing != null) {
                        partnerBearing - animatedAzimuth
                    } else {
                        -animatedAzimuth
                    }

                    // Top Needle position on the dial plate coordinate space
                    val needleAngleOnDial = ((-dialRotation % 360f) + 360f) % 360f
                    val isShortestPathClockwise = needleAngleOnDial <= 180f

                    rotate(dialRotation, pivot = center) {
                        // 120 Ticks: Major every 30° (10 ticks), Minor in between
                        for (i in 0 until 120) {
                            val angleDeg = i * 3f
                            val angleRad = Math.toRadians(angleDeg.toDouble())

                            val isMajor = (i % 10 == 0)
                            val isPartnerZero = (i == 0)

                            // Covered ticks: strictly connects 0° (Momo) to needleAngleOnDial (Top Needle)
                            val isTickCovered = if (isShortestPathClockwise) {
                                angleDeg in 0.0f..needleAngleOnDial
                            } else {
                                angleDeg >= needleAngleOnDial || angleDeg == 0f
                            }

                            // Dynamic Gradient sweep along the arc connecting Momo and Needle
                            val tickColor = if (isTickCovered) {
                                val fraction = if (isShortestPathClockwise) {
                                    if (needleAngleOnDial > 0f) (angleDeg / needleAngleOnDial).coerceIn(0f, 1f) else 0f
                                } else {
                                    val arcSpan = 360f - needleAngleOnDial
                                    if (arcSpan > 0f) {
                                        val distFromZero = if (angleDeg == 0f) 0f else (360f - angleDeg)
                                        (distFromZero / arcSpan).coerceIn(0f, 1f)
                                    } else 0f
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

                            // Radially aligned numbers (0, 30, 60 ... 330)
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
                                            color = if (isPartnerZero) primaryColor.toArgb() else onSurfaceVariantColor.toArgb()
                                            textSize = 10.sp.toPx()
                                            textAlign = Paint.Align.CENTER
                                            typeface = Typeface.DEFAULT_BOLD
                                            isAntiAlias = true
                                        }
                                    )
                                }
                            }
                        }

                        // Partner Name Tag at 0° Marker (Lowered inwards with 32dp clearance to prevent touching tick lines)
                        val partnerNameTag = partnerInfo.name.ifBlank { "Partner" }
                        val labelRadius = radius - 32.dp.toPx()
                        drawContext.canvas.nativeCanvas.drawText(
                            partnerNameTag,
                            center.x,
                            center.y - labelRadius,
                            Paint().apply {
                                color = primaryColor.toArgb()
                                textSize = 11.sp.toPx()
                                textAlign = Paint.Align.CENTER
                                typeface = Typeface.DEFAULT_BOLD
                                isAntiAlias = true
                            }
                        )
                    }

                    // Top Fixed Indicator Notch at 12 o'clock (ColorOS clean bar)
                    drawLine(
                        color = tertiaryColor,
                        start = Offset(center.x, center.y - radius - 2.dp.toPx()),
                        end = Offset(center.x, center.y - radius + 12.dp.toPx()),
                        strokeWidth = 2.4.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }

                // Center Big Angle Display
                Text(
                    text = if (relativeAngle != null) "$relativeAngle°" else "--°",
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Direction & Distance Label: e.g. North-East (510 m) / North-East (1.4 km)
            Text(
                text = directionDistanceTitle,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun TrackerRowItem(item: CacheManager.UserLocationInfo) {
    val context = LocalContext.current

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        // Line 1: Name (Click opens Google Maps) on Left, Time on Right
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val hasValidCoords = item.latitude != null && item.longitude != null

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .then(
                        if (hasValidCoords) {
                            Modifier.bounceClick(scaleDown = 0.94f) {
                                openGoogleMaps(context, item.latitude!!, item.longitude!!, item.name)
                            }
                        } else Modifier
                    )
            ) {
                Text(
                    text = item.name.ifBlank { "--" },
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
            }

            Text(
                text = formatTimestamp(item.timestamp),
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Line 2: Coordinates (Click copies to clipboard) on Left, Accuracy on Right
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val formattedCoords = formatCoordinates(item.latitude, item.longitude)
            val isClickable = item.latitude != null && item.longitude != null

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .then(
                        if (isClickable) {
                            Modifier.bounceClick(scaleDown = 0.95f) {
                                copyToClipboard(context, "${item.latitude}, ${item.longitude}")
                            }
                        } else Modifier
                    )
            ) {
                Text(
                    text = formattedCoords,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }

            Text(
                text = formatAccuracy(item.accuracy),
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

private class CompassSoundPlayer(context: Context) {
    private val soundPool: SoundPool = SoundPool.Builder()
        .setMaxStreams(2)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()

    private var lightSoundId: Int = 0
    private var heavySoundId: Int = 0

    init {
        try {
            val lightFile = File(context.cacheDir, "momo_tick_light.wav")
            val heavyFile = File(context.cacheDir, "momo_tick_heavy.wav")

            if (!lightFile.exists() || lightFile.length() == 0L) {
                generateWav(lightFile, frequency = 2500.0, durationMs = 6, decay = 550.0, volume = 0.5f)
            }
            if (!heavyFile.exists() || heavyFile.length() == 0L) {
                generateWav(heavyFile, frequency = 950.0, durationMs = 15, decay = 250.0, volume = 0.9f)
            }

            lightSoundId = soundPool.load(lightFile.absolutePath, 1)
            heavySoundId = soundPool.load(heavyFile.absolutePath, 1)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun playLight() {
        if (lightSoundId != 0) {
            soundPool.play(lightSoundId, 0.5f, 0.5f, 1, 0, 1.0f)
        }
    }

    fun playHeavy() {
        if (heavySoundId != 0) {
            soundPool.play(heavySoundId, 0.85f, 0.85f, 1, 0, 1.0f)
        }
    }

    fun release() {
        try {
            soundPool.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun generateWav(file: File, frequency: Double, durationMs: Int, decay: Double, volume: Float) {
        val sampleRate = 44100
        val numSamples = (sampleRate * durationMs) / 1000
        val shortBuffer = ShortArray(numSamples)

        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val env = exp(-decay * t)
            val s = (sin(2.0 * Math.PI * frequency * t) * env * Short.MAX_VALUE * volume).toInt()
            shortBuffer[i] = s.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }

        val dataSize = numSamples * 2
        val totalSize = 36 + dataSize
        val byteBuffer = ByteBuffer.allocate(44 + dataSize).order(ByteOrder.LITTLE_ENDIAN)

        // Standard 44-byte WAV header
        byteBuffer.put("RIFF".toByteArray(Charsets.US_ASCII))
        byteBuffer.putInt(totalSize)
        byteBuffer.put("WAVE".toByteArray(Charsets.US_ASCII))
        byteBuffer.put("fmt ".toByteArray(Charsets.US_ASCII))
        byteBuffer.putInt(16)
        byteBuffer.putShort(1)
        byteBuffer.putShort(1)
        byteBuffer.putInt(sampleRate)
        byteBuffer.putInt(sampleRate * 2)
        byteBuffer.putShort(2)
        byteBuffer.putShort(16)
        byteBuffer.put("data".toByteArray(Charsets.US_ASCII))
        byteBuffer.putInt(dataSize)

        for (sample in shortBuffer) {
            byteBuffer.putShort(sample)
        }

        FileOutputStream(file).use { fos ->
            fos.write(byteBuffer.array())
        }
    }
}

private fun calculateBearing(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val phi1 = Math.toRadians(lat1)
    val phi2 = Math.toRadians(lat2)
    val deltaLambda = Math.toRadians(lon2 - lon1)

    val y = sin(deltaLambda) * cos(phi2)
    val x = cos(phi1) * sin(phi2) - sin(phi1) * cos(phi2) * cos(deltaLambda)
    val theta = atan2(y, x)
    return (Math.toDegrees(theta) + 360.0) % 360.0
}

private fun getCardinalDirection(bearing: Double): String {
    val directions = arrayOf(
        "North", "North-East", "East", "South-East",
        "South", "South-West", "West", "North-West"
    )
    val index = ((bearing + 22.5) / 45.0).toInt() % 8
    return directions[index]
}

private fun formatDistanceLabel(meters: Int?): String {
    if (meters == null) return "--"
    return if (meters < 1000) {
        "$meters m"
    } else {
        String.format(Locale.ENGLISH, "%.1f km", meters / 1000.0)
    }
}

private fun openGoogleMaps(context: Context, lat: Double, lng: Double, label: String) {
    val geoUri = Uri.parse("geo:$lat,$lng?q=$lat,$lng(${Uri.encode(label)})")
    val mapIntent = Intent(Intent.ACTION_VIEW, geoUri).apply {
        setPackage("com.google.android.apps.maps")
    }

    try {
        context.startActivity(mapIntent)
    } catch (e: Exception) {
        val webUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=$lat,$lng")
        val fallbackIntent = Intent(Intent.ACTION_VIEW, webUri)
        context.startActivity(fallbackIntent)
    }
}

private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("Coordinates", text)
    clipboard.setPrimaryClip(clip)
    Toast.makeText(context, "Coordinates copied", Toast.LENGTH_SHORT).show()
}

private fun formatCoordinates(lat: Double?, lng: Double?): String {
    if (lat == null || lng == null) return "--"
    val latDir = if (lat >= 0.0) "N" else "S"
    val lngDir = if (lng >= 0.0) "E" else "W"
    return String.format(
        Locale.ENGLISH,
        "%.6f° %s, %.6f° %s",
        abs(lat),
        latDir,
        abs(lng),
        lngDir
    )
}

private fun formatAccuracy(accuracy: Float?): String {
    return if (accuracy != null) "~${accuracy.roundToInt()} m" else "--"
}

private fun formatTimestamp(timestamp: Long?): String {
    if (timestamp == null || timestamp == 0L) return "--"
    val formatter = SimpleDateFormat("hh:mm a", Locale.ENGLISH)
    return formatter.format(Date(timestamp))
}
