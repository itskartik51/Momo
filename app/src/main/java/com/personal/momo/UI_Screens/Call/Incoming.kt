package com.personal.momo.UI_Screens.Call

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.os.PowerManager
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectVerticalDragGestures
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.personal.momo.MainActivity
import com.personal.momo.R
import com.personal.momo.UI_Screens.bounceClick
import kotlin.math.roundToInt

@Composable
fun IncomingCallView(
    avatarUrl: String?,
    callerName: String,
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121214))
    ) {
        // Top Profile Section: Exactly matches ActiveCallScreen coordinates to eliminate layout shift
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 96.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(116.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF242529))
                    .border(2.dp, Color(0xFF383A40), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (!avatarUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = avatarUrl,
                        contentDescription = "Incoming Avatar",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = callerName,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Incoming Voice Call...",
                fontSize = 15.sp,
                color = Color(0xFF9E9E9E),
                fontWeight = FontWeight.Medium
            )
        }

        // Bottom Action Deck: Floating & Pulsing Swipe-Up Buttons
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 44.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SwipeUpCallButton(
                isAccept = false,
                label = "Decline",
                buttonColor = Color(0xFFFF4F4F),
                waveColor = Color(0xFFFF8787),
                onTrigger = onDecline
            )

            SwipeUpCallButton(
                isAccept = true,
                label = "Accept",
                buttonColor = Color(0xFF50FF8A),
                waveColor = Color(0xFF87FFAF),
                onTrigger = onAccept
            )
        }
    }
}

@Composable
private fun SwipeUpCallButton(
    isAccept: Boolean,
    label: String,
    buttonColor: Color,
    waveColor: Color,
    onTrigger: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "ButtonAnimations")

    // 1. Bobbing / Floating Movement (Vertical Sine Wave)
    val bobbingOffset by infiniteTransition.animateFloat(
        initialValue = -5f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "BobbingOffset"
    )

    // 2. Phone Icon Wobble / Vibration (-4 deg to +4 deg)
    val phoneWobble by infiniteTransition.animateFloat(
        initialValue = -4f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(120, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PhoneWobble"
    )

    // 3. Continuous 3-Layer Expanding Ripple Waves
    val waveProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "WaveProgress"
    )

    // 4. Subtle Upward Arrow Bounce for Gestural Affordance
    val arrowBounce by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -6f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ArrowBounce"
    )

    // Swipe-Up Drag Handling
    val density = LocalDensity.current
    val thresholdPx = with(density) { 70.dp.toPx() }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }

    val animatedDragOffset by animateFloatAsState(
        targetValue = dragOffsetY,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "DragAnimation"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.offset {
            IntOffset(0, (animatedDragOffset + bobbingOffset).roundToInt())
        }
    ) {
        // Subtle Upward Gesture Indicator
        Icon(
            imageVector = Icons.Default.KeyboardArrowUp,
            contentDescription = "Swipe up",
            tint = buttonColor.copy(alpha = 0.85f),
            modifier = Modifier
                .size(24.dp)
                .offset(y = arrowBounce.dp)
        )

        Spacer(modifier = Modifier.height(2.dp))

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(108.dp)
                .pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onDragEnd = {
                            if (dragOffsetY <= -thresholdPx) {
                                onTrigger()
                            }
                            dragOffsetY = 0f
                        },
                        onDragCancel = {
                            dragOffsetY = 0f
                        },
                        onVerticalDrag = { change, dragAmount ->
                            change.consume()
                            dragOffsetY = (dragOffsetY + dragAmount).coerceIn(-thresholdPx * 1.4f, 0f)
                            if (dragOffsetY <= -thresholdPx) {
                                onTrigger()
                                dragOffsetY = 0f
                            }
                        }
                    )
                }
        ) {
            // Triple-Layer Waves Render
            val wavePhases = listOf(0.0f, 0.33f, 0.66f)
            wavePhases.forEach { phase ->
                val progress = (waveProgress + phase) % 1.0f
                val scale = 1.0f + (progress * 0.58f)
                val alpha = (1.0f - progress) * 0.42f

                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            this.alpha = alpha
                        }
                        .background(waveColor, CircleShape)
                )
            }

            // Main Interactive Circle
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(buttonColor)
                    .bounceClick(scaleDown = 0.90f) {
                        onTrigger()
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isAccept) Icons.Default.Call else Icons.Default.CallEnd,
                    contentDescription = label,
                    tint = if (isAccept) Color(0xFF121214) else Color.White,
                    modifier = Modifier
                        .size(30.dp)
                        .graphicsLayer {
                            rotationZ = phoneWobble
                        }
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFFD0D3D8)
        )
    }
}

object IncomingCallNotifier {
    const val CHANNEL_ID = "momo_call_v2"
    const val NOTIFICATION_ID = 9110
    const val ACTION_ACCEPT = "com.personal.momo.action.ACCEPT_CALL"
    const val ACTION_DECLINE = "com.personal.momo.action.DECLINE_CALL"
    const val ACTION_INCOMING_CALL = "com.personal.momo.action.INCOMING_CALL"

    @SuppressLint("InvalidWakeLockTag")
    fun show(context: Context, callerName: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Instant hardware screen turn-on via WakeLock
        try {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            @Suppress("DEPRECATION")
            val wakeLock = powerManager.newWakeLock(
                PowerManager.SCREEN_BRIGHT_WAKE_LOCK or
                PowerManager.ACQUIRE_CAUSES_WAKEUP or
                PowerManager.ON_AFTER_RELEASE,
                "momo:incoming_call_wake"
            )
            wakeLock.acquire(10_000L)
        } catch (_: Exception) {}

        val ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                .build()

            val channel = NotificationChannel(
                CHANNEL_ID,
                "Incoming Voice Calls",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "High-priority incoming call banner and lock screen wake up"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 800, 500, 800)
                setSound(ringtoneUri, audioAttributes)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }
            notificationManager.createNotificationChannel(channel)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permission = ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS)
            if (permission != PackageManager.PERMISSION_GRANTED) return
        }

        // Direct Incoming Call UI Intent (Opens IncomingCallView directly over lock screen)
        val fullScreenIntent = Intent(context, MainActivity::class.java).apply {
            action = ACTION_INCOMING_CALL
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            context,
            101,
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Silent Decline Action (BroadcastReceiver cuts call without launching activity)
        val declineIntent = Intent(context, CallActionReceiver::class.java).apply {
            action = ACTION_DECLINE
        }
        val declinePendingIntent = PendingIntent.getBroadcast(
            context,
            102,
            declineIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Direct Accept Action
        val acceptIntent = Intent(context, MainActivity::class.java).apply {
            action = ACTION_ACCEPT
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val acceptPendingIntent = PendingIntent.getActivity(
            context,
            103,
            acceptIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(callerName)
            .setContentText("Incoming Voice Call...")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setAutoCancel(true)
            .setOngoing(true)
            .setSound(ringtoneUri)
            .setContentIntent(fullScreenPendingIntent)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .addAction(R.mipmap.ic_launcher, "Decline", declinePendingIntent)
            .addAction(R.mipmap.ic_launcher, "Accept", acceptPendingIntent)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    fun cancel(context: Context) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(NOTIFICATION_ID)
    }
}

class CallActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        val ctx = context ?: return
        if (intent?.action == IncomingCallNotifier.ACTION_DECLINE) {
            IncomingCallNotifier.cancel(ctx)
            CallManager.declineIncomingCall(ctx)
        }
    }
}
