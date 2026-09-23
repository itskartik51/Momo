package com.personal.momo.UI_Screens.Call

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Headset
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.personal.momo.Cache.CacheManager
import com.personal.momo.UI_Screens.bounceClick
import kotlinx.coroutines.delay

@Composable
fun ActiveCallScreen(
    onEndCall: () -> Unit
) {
    val context = LocalContext.current
    val avatarUrl by CacheManager.avatarUrlFlow.collectAsState()
    val currentUserId by CacheManager.appUserIdFlow.collectAsState()
    val partnerDisplayName = if (currentUserId.equals("Momo", ignoreCase = true)) "Kanu" else "Momo"

    var secondsElapsed by remember { mutableIntStateOf(0) }
    var isHoldActive by remember { mutableStateOf(false) }

    LaunchedEffect(CallManager.isPeerConnected) {
        if (CallManager.isPeerConnected) {
            secondsElapsed = 0
            while (true) {
                delay(1000L)
                secondsElapsed++
            }
        }
    }

    val latency = CallManager.latencyMs
    val dotColor by animateColorAsState(
        targetValue = when {
            latency <= 150 -> Color(0xFF4CAF50)
            latency <= 350 -> Color(0xFFFFC107)
            else -> Color(0xFFF44336)
        },
        animationSpec = tween(300),
        label = "LatencyColor"
    )

    val activeCircleBg = Color.White
    val activeCircleTint = Color(0xFF1E1F22)
    val inactiveCircleBg = Color(0xFF2C2D32)
    val inactiveCircleTint = Color.White

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121214))
    ) {
        // Top-Right Latency Capsule
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 42.dp, end = 20.dp)
                .clip(RoundedCornerShape(50.dp))
                .background(Color(0xFF242529))
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
            Text(
                text = "${if (latency > 0) latency else 120} ms",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFFB0B3B8)
            )
        }

        // Profile Section
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
                        contentDescription = "Avatar",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = partnerDisplayName,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = when {
                    isHoldActive -> "Call on Hold"
                    CallManager.isPeerConnected -> formatCallDuration(secondsElapsed)
                    CallManager.isConnecting -> "Connecting..."
                    else -> "Ringing..."
                },
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = if (isHoldActive) Color(0xFFFF9800) else Color(0xFF9E9E9E)
            )
        }

        // Bottom Solid Control Deck
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 16.dp, vertical = 24.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(32.dp),
            color = Color(0xFF1E1F22),
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize(animationSpec = tween(280))
                    .padding(horizontal = 16.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Fixed Row: Action Circular Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 1. Mic Toggle
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val isMicActive = CallManager.isMuted
                        Box(
                            modifier = Modifier
                                .size(62.dp)
                                .clip(CircleShape)
                                .background(if (isMicActive) activeCircleBg else inactiveCircleBg)
                                .bounceClick(scaleDown = 0.88f) {
                                    CallManager.toggleMute()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isMicActive) Icons.Default.MicOff else Icons.Default.Mic,
                                contentDescription = "Mute",
                                tint = if (isMicActive) activeCircleTint else inactiveCircleTint,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Text(
                            text = if (isMicActive) "Unmute" else "Mute",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFFB0B3B8)
                        )
                    }

                    // 2. Hold Toggle
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(62.dp)
                                .clip(CircleShape)
                                .background(if (isHoldActive) activeCircleBg else inactiveCircleBg)
                                .bounceClick(scaleDown = 0.88f) {
                                    isHoldActive = !isHoldActive
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Pause,
                                contentDescription = "Hold",
                                tint = if (isHoldActive) activeCircleTint else inactiveCircleTint,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Text(
                            text = "Hold",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFFB0B3B8)
                        )
                    }

                    // 3. Audio Route Indicator / Speaker Toggle
                    when {
                        // Strict Priority #1: Wired Earphones Connected -> Black Circle, Headset Icon (Indicator only)
                        AudioMan.isWiredConnected -> {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(62.dp)
                                        .clip(CircleShape)
                                        .background(inactiveCircleBg),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Headset,
                                        contentDescription = "Headphones",
                                        tint = inactiveCircleTint,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                                Text(
                                    text = "Headphones",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFFB0B3B8),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        // Priority #2: Bluetooth Headset Connected -> Black Circle, Bluetooth Icon (Indicator only)
                        AudioMan.isBluetoothConnected -> {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(62.dp)
                                        .clip(CircleShape)
                                        .background(inactiveCircleBg),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Bluetooth,
                                        contentDescription = "Bluetooth",
                                        tint = inactiveCircleTint,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                                Text(
                                    text = "Bluetooth",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFFB0B3B8),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        // Normal Mode: Interactive Speaker Button (Earpiece default, White when Speaker active)
                        else -> {
                            val isSpeakerActive = AudioMan.isSpeakerOn
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(62.dp)
                                        .clip(CircleShape)
                                        .background(if (isSpeakerActive) activeCircleBg else inactiveCircleBg)
                                        .bounceClick(scaleDown = 0.88f) {
                                            AudioMan.toggleSpeaker(context)
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.VolumeUp,
                                        contentDescription = "Speaker",
                                        tint = if (isSpeakerActive) activeCircleTint else inactiveCircleTint,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                                Text(
                                    text = "Speaker",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFFB0B3B8)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Centered Red Pill End Call Button
                Box(
                    modifier = Modifier
                        .width(148.dp)
                        .height(54.dp)
                        .clip(RoundedCornerShape(50.dp))
                        .background(Color(0xFFE53935))
                        .bounceClick(scaleDown = 0.92f) {
                            onEndCall()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CallEnd,
                        contentDescription = "End Call",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    }
}

private fun formatCallDuration(totalSeconds: Int): String {
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}
