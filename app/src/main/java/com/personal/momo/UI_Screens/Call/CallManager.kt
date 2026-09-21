package com.personal.momo.UI_Screens.Call

import android.content.Context
import android.media.AudioManager
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.CallMade
import androidx.compose.material.icons.filled.CallMissed
import androidx.compose.material.icons.filled.CallReceived
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.outlined.VolumeMute
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.personal.momo.Cache.CacheManager
import com.personal.momo.UI_Screens.bounceClick
import io.agora.rtc2.ChannelMediaOptions
import io.agora.rtc2.Constants
import io.agora.rtc2.IRtcEngineEventHandler
import io.agora.rtc2.RtcEngine
import io.agora.rtc2.RtcEngineConfig
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Data Model for Past Call Records
data class CallLogItem(
    val id: String = "",
    val caller: String = "",
    val receiver: String = "",
    val status: String = "completed",
    val durationSeconds: Int = 0,
    val timestamp: Long = 0L
)

// Central Voice Engine & Signaling Controller
object CallManager {
    private const val AGORA_APP_ID = "8eb2889c463d4389af35fd64113508bc"
    private var rtcEngine: RtcEngine? = null

    var isCallActive by mutableStateOf(false)
    var isPeerConnected by mutableStateOf(false)
    var latencyMs by mutableIntStateOf(0)
    var isMuted by mutableStateOf(false)
    var isSpeakerOn by mutableStateOf(false)

    private var callStartTime: Long = 0L
    private val firestore by lazy { FirebaseFirestore.getInstance() }

    private val rtcEventHandler = object : IRtcEngineEventHandler() {
        override fun onJoinChannelSuccess(channel: String?, uid: Int, elapsed: Int) {
            isCallActive = true
            callStartTime = System.currentTimeMillis()
        }

        override fun onUserJoined(uid: Int, elapsed: Int) {
            isPeerConnected = true
        }

        override fun onUserOffline(uid: Int, reason: Int) {
            isPeerConnected = false
        }

        override fun onRtcStats(stats: RtcStats?) {
            stats?.let {
                latencyMs = if (it.gatewayRtt > 0) it.gatewayRtt else it.lastmileDelay
            }
        }
    }

    fun initEngine(context: Context) {
        if (rtcEngine == null) {
            try {
                val config = RtcEngineConfig().apply {
                    mContext = context.applicationContext
                    mAppId = AGORA_APP_ID
                    mEventHandler = rtcEventHandler
                }
                rtcEngine = RtcEngine.create(config)
                rtcEngine?.enableAudio()
                rtcEngine?.setChannelProfile(Constants.CHANNEL_PROFILE_COMMUNICATION)
            } catch (_: Exception) {
            }
        }
    }

    fun startCall(context: Context, callerName: String = "kanu", channelName: String = "momo_private_voice_room") {
        initEngine(context)
        isMuted = false
        isSpeakerOn = false
        latencyMs = 120

        val options = ChannelMediaOptions().apply {
            channelProfile = Constants.CHANNEL_PROFILE_COMMUNICATION
            clientRoleType = Constants.CLIENT_ROLE_BROADCASTER
            autoSubscribeAudio = true
            publishMicrophoneTrack = true
        }

        try {
            rtcEngine?.joinChannel(null, channelName, 0, options)

            // Signaling: Mark call status in Firestore
            val callData = hashMapOf(
                "caller" to callerName,
                "receiver" to "momo",
                "status" to "calling",
                "channelName" to channelName,
                "timestamp" to System.currentTimeMillis()
            )
            firestore.collection("calls").document("current_call").set(callData)
        } catch (_: Exception) {
        }
    }

    fun endCall(callerName: String = "kanu") {
        val duration = if (callStartTime > 0L) {
            ((System.currentTimeMillis() - callStartTime) / 1000).toInt()
        } else {
            0
        }

        try {
            rtcEngine?.leaveChannel()
        } catch (_: Exception) {
        }

        // Save Call Record in Firestore Logs
        val logRecord = hashMapOf(
            "caller" to callerName,
            "receiver" to "momo",
            "status" to if (isPeerConnected) "completed" else "missed",
            "durationSeconds" to duration,
            "timestamp" to System.currentTimeMillis()
        )
        firestore.collection("call_logs").add(logRecord)

        // Clear active call state
        firestore.collection("calls").document("current_call").update("status", "ended")

        isCallActive = false
        isPeerConnected = false
        callStartTime = 0L
        latencyMs = 0
    }

    fun toggleMute() {
        isMuted = !isMuted
        rtcEngine?.muteLocalAudioStream(isMuted)
    }

    fun toggleSpeaker(context: Context) {
        isSpeakerOn = !isSpeakerOn
        rtcEngine?.setEnableSpeakerphone(isSpeakerOn)
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        audioManager?.isSpeakerphoneOn = isSpeakerOn
    }

    fun observeCallLogs(onLogsUpdated: (List<CallLogItem>) -> Unit): ListenerRegistration {
        return firestore.collection("call_logs")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(25)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                val logs = snapshot.documents.mapNotNull { doc ->
                    val caller = doc.getString("caller") ?: ""
                    val receiver = doc.getString("receiver") ?: ""
                    val status = doc.getString("status") ?: "completed"
                    val duration = doc.getLong("durationSeconds")?.toInt() ?: 0
                    val timestamp = doc.getLong("timestamp") ?: 0L
                    CallLogItem(
                        id = doc.id,
                        caller = caller,
                        receiver = receiver,
                        status = status,
                        durationSeconds = duration,
                        timestamp = timestamp
                    )
                }
                onLogsUpdated(logs)
            }
    }
}

// Unified Composable Screen (Hub & Dialer + Active Calling View)
@Composable
fun CallScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val avatarUrl by CacheManager.avatarUrlFlow.collectAsState()
    val callLogs = remember { mutableStateListOf<CallLogItem>() }

    // Observe Firestore Call Logs
    DisposableEffect(Unit) {
        val listener = CallManager.observeCallLogs { updatedList ->
            callLogs.clear()
            callLogs.addAll(updatedList)
        }
        onDispose {
            listener.remove()
        }
    }

    if (CallManager.isCallActive) {
        // Active Ongoing Call Screen
        ActiveCallView(
            avatarUrl = avatarUrl,
            onEndCall = {
                CallManager.endCall()
            }
        )
    } else {
        // Call Hub & Dialer Screen
        CallHubView(
            avatarUrl = avatarUrl,
            callLogs = callLogs,
            onBack = onBack,
            onStartCall = {
                CallManager.startCall(context)
            }
        )
    }
}

@Composable
fun CallHubScreen(onBack: () -> Unit) = CallScreen(onBack = onBack)

@Composable
private fun CallHubView(
    avatarUrl: String?,
    callLogs: List<CallLogItem>,
    onBack: () -> Unit,
    onStartCall: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top App Bar
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 2.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .bounceClick(scaleDown = 0.88f) {
                            onBack()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = "Momo Call Hub",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            // Contact Profile & Direct Call Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                tonalElevation = 1.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surface)
                            .border(2.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!avatarUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = avatarUrl,
                                contentDescription = "Momo Avatar",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Momo",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = "Private Voice Calling",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    // Start Voice Call Action Button
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Color(0xFFFF5E7E), Color(0xFFFF9966))
                                )
                            )
                            .bounceClick(scaleDown = 0.94f) {
                                onStartCall()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Call,
                                contentDescription = "Call",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Start Voice Call",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Recent Calls History Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = "Recent History",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "Recent Calls",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (callLogs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No call history yet",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(callLogs, key = { it.id }) { item ->
                        CallLogRow(item = item)
                    }
                }
            }
        }
    }
}

@Composable
private fun CallLogRow(item: CallLogItem) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val iconColor = when {
                    item.status == "missed" -> Color(0xFFF44336)
                    item.caller == "kanu" -> Color(0xFF4CAF50)
                    else -> Color(0xFF2196F3)
                }

                val iconVector = when {
                    item.status == "missed" -> Icons.Default.CallMissed
                    item.caller == "kanu" -> Icons.Default.CallMade
                    else -> Icons.Default.CallReceived
                }

                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(iconColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = iconVector,
                        contentDescription = "Type",
                        tint = iconColor,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Column {
                    Text(
                        text = "Momo",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = formatLogTimestamp(item.timestamp),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text(
                text = if (item.status == "missed") "Missed" else formatLogDuration(item.durationSeconds),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = if (item.status == "missed") Color(0xFFF44336) else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ActiveCallView(
    avatarUrl: String?,
    onEndCall: () -> Unit
) {
    val context = LocalContext.current
    var secondsElapsed by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1000L)
            secondsElapsed++
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(2.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
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

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Momo",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = when {
                    CallManager.isPeerConnected -> formatLogDuration(secondsElapsed)
                    else -> "Ringing..."
                },
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Delay / Latency Indicator Badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
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
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Active Bottom Call Action Controls
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp),
            horizontalArrangement = Arrangement.spacedBy(28.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Mute Toggle
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(if (CallManager.isMuted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                    .bounceClick(scaleDown = 0.88f) {
                        CallManager.toggleMute()
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (CallManager.isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                    contentDescription = "Mute",
                    tint = if (CallManager.isMuted) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
            }

            // End Call Button
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFF44336))
                    .bounceClick(scaleDown = 0.88f) {
                        onEndCall()
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CallEnd,
                    contentDescription = "End Call",
                    tint = Color.White,
                    modifier = Modifier.size(30.dp)
                )
            }

            // Speaker Toggle
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(if (CallManager.isSpeakerOn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                    .bounceClick(scaleDown = 0.88f) {
                        CallManager.toggleSpeaker(context)
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (CallManager.isSpeakerOn) Icons.Default.VolumeUp else Icons.Outlined.VolumeMute,
                    contentDescription = "Speaker",
                    tint = if (CallManager.isSpeakerOn) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

private fun formatLogDuration(totalSeconds: Int): String {
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}

private fun formatLogTimestamp(timestamp: Long): String {
    if (timestamp == 0L) return ""
    val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
