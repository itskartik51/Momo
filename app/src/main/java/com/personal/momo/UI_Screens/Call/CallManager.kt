package com.personal.momo.UI_Screens.Call

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.Ringtone
import android.media.RingtoneManager
import android.media.ToneGenerator
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.personal.momo.Cache.CacheManager
import com.personal.momo.UI_Screens.bounceClick
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Master Controller & UI Orchestrator
object CallManager {
    private var toneGenerator: ToneGenerator? = null
    private var incomingRingtone: Ringtone? = null

    var isCallActive by mutableStateOf(false)
    var isIncomingCall by mutableStateOf(false)
    var incomingCallerName by mutableStateOf("Momo")
    var currentChannelName by mutableStateOf("momo_private_voice_room")

    var isPeerConnected by mutableStateOf(false)
    var latencyMs by mutableIntStateOf(0)
    var isMuted by mutableStateOf(false)
    var isSpeakerOn by mutableStateOf(false)

    var diagnosticStatus by mutableStateOf("Idle / Ready")

    private var callStartTime: Long = 0L
    private var activeCallTimestampKey: Long = 0L

    init {
        // Wire Agora callbacks directly into master state
        AgoraCallEngine.onJoinChannelSuccess = { channel, _ ->
            isCallActive = true
            callStartTime = System.currentTimeMillis()
            diagnosticStatus = "Agora: Channel Joined ($channel)"
        }

        AgoraCallEngine.onUserJoined = {
            isPeerConnected = true
            stopDialTone()
            diagnosticStatus = "Agora: Partner Connected!"
        }

        AgoraCallEngine.onUserOffline = { _, reason ->
            isPeerConnected = false
            diagnosticStatus = "Agora: Partner Offline (reason: $reason)"
        }

        AgoraCallEngine.onLatencyUpdated = { latency ->
            latencyMs = latency
        }

        AgoraCallEngine.onErrorOccurred = { err ->
            diagnosticStatus = "Agora Error: $err"
        }

        AgoraCallEngine.onConnectionFailed = { reason ->
            stopDialTone()
            diagnosticStatus = "Agora Connection Failed: reason $reason"
        }
    }

    private fun startDialTone() {
        try {
            stopDialTone()
            toneGenerator = ToneGenerator(AudioManager.STREAM_VOICE_CALL, 80)
            toneGenerator?.startTone(ToneGenerator.TONE_SUP_RINGTONE)
        } catch (_: Exception) {
        }
    }

    private fun stopDialTone() {
        try {
            toneGenerator?.stopTone()
            toneGenerator?.release()
        } catch (_: Exception) {
        }
        toneGenerator = null
    }

    private fun startIncomingRingtone(context: Context) {
        try {
            stopIncomingRingtone()
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            incomingRingtone = RingtoneManager.getRingtone(context.applicationContext, uri)
            incomingRingtone?.play()
        } catch (_: Exception) {
        }
    }

    private fun stopIncomingRingtone() {
        try {
            incomingRingtone?.stop()
        } catch (_: Exception) {
        }
        incomingRingtone = null
    }

    fun startSignalingListener(context: Context) {
        val myId = CacheManager.getAppUserId(context).lowercase(Locale.ROOT)

        FirestoreCallService.startSignalingListener(
            myUserId = myId,
            onIncomingCall = { caller, channel, timestamp ->
                if (!isCallActive) {
                    incomingCallerName = if (caller.equals("kanu", ignoreCase = true)) "Kanu" else "Momo"
                    currentChannelName = channel
                    isIncomingCall = true
                    activeCallTimestampKey = timestamp
                    startIncomingRingtone(context)
                    diagnosticStatus = "Incoming call from $incomingCallerName!"
                }
            },
            onCallConnected = {
                stopDialTone()
                stopIncomingRingtone()
                if (isCallActive) {
                    isPeerConnected = true
                    diagnosticStatus = "Call connected with partner"
                }
            },
            onCallEnded = {
                stopDialTone()
                stopIncomingRingtone()
                if (isCallActive) {
                    leaveCallSilently(context)
                }
                isIncomingCall = false
                diagnosticStatus = "Call ended"
            },
            onError = { err ->
                diagnosticStatus = "Signaling: $err"
            }
        )
    }

    fun stopSignalingListener() {
        stopDialTone()
        stopIncomingRingtone()
        FirestoreCallService.stopSignalingListener()
    }

    fun startCall(context: Context, channelName: String = "momo_private_voice_room") {
        AgoraCallEngine.initEngine(context) { err -> diagnosticStatus = "Agora Init: $err" }
        isMuted = false
        isSpeakerOn = false
        latencyMs = 120
        currentChannelName = channelName

        val myId = CacheManager.getAppUserId(context).lowercase(Locale.ROOT)
        val targetId = if (myId == "kanu") "momo" else "kanu"
        activeCallTimestampKey = System.currentTimeMillis()

        startDialTone()

        val token = AgoraCallEngine.buildAgoraToken(channelName)
        diagnosticStatus = "Joining Agora Channel..."
        AgoraCallEngine.joinChannel(channelName, token)

        diagnosticStatus = "Sending Call Signal to $targetId..."
        FirestoreCallService.sendCallSignal(
            caller = myId,
            receiver = targetId,
            channelName = channelName,
            timestamp = activeCallTimestampKey,
            onSuccess = { diagnosticStatus = "Signal sent! Ringing..." },
            onFailure = { e ->
                stopDialTone()
                diagnosticStatus = "Firestore Signal Error: ${e.localizedMessage}"
            }
        )
    }

    fun acceptIncomingCall(context: Context) {
        stopIncomingRingtone()
        isIncomingCall = false
        AgoraCallEngine.initEngine(context) { err -> diagnosticStatus = "Agora Init: $err" }
        isMuted = false
        isSpeakerOn = false
        latencyMs = 120

        val token = AgoraCallEngine.buildAgoraToken(currentChannelName)
        diagnosticStatus = "Accepting & Joining Agora..."
        AgoraCallEngine.joinChannel(currentChannelName, token)

        FirestoreCallService.updateCallStatus("connected",
            onSuccess = { diagnosticStatus = "Accepted. Status: connected" },
            onFailure = { e -> diagnosticStatus = "Accept Error: ${e.localizedMessage}" }
        )
    }

    fun declineIncomingCall(context: Context) {
        stopIncomingRingtone()
        isIncomingCall = false
        val callerCode = if (incomingCallerName.equals("Kanu", ignoreCase = true)) 1 else 2

        FirestoreCallService.updateCallStatus("ended")
        val logKey = if (activeCallTimestampKey > 0L) activeCallTimestampKey else System.currentTimeMillis()
        FirestoreCallService.logCall(logKey, callerCode, 0)
        diagnosticStatus = "Call declined"
    }

    fun endCall(context: Context) {
        stopDialTone()
        stopIncomingRingtone()

        val duration = if (callStartTime > 0L) {
            ((System.currentTimeMillis() - callStartTime) / 1000).toInt()
        } else {
            0
        }

        val myId = CacheManager.getAppUserId(context).lowercase(Locale.ROOT)
        val callerCode = if (myId == "kanu") 1 else 2
        val statusDuration = if (isPeerConnected) (if (duration > 0) duration else 1) else 0

        AgoraCallEngine.leaveChannel()

        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            audioManager?.let { am ->
                am.isSpeakerphoneOn = false
                am.mode = AudioManager.MODE_NORMAL
            }
        } catch (_: Exception) {
        }

        val logKey = if (activeCallTimestampKey > 0L) activeCallTimestampKey else System.currentTimeMillis()
        FirestoreCallService.logCall(logKey, callerCode, statusDuration)
        FirestoreCallService.updateCallStatus("ended")

        isCallActive = false
        isPeerConnected = false
        callStartTime = 0L
        activeCallTimestampKey = 0L
        latencyMs = 0
        diagnosticStatus = "Call ended"
    }

    fun resetAudioAndCallState(context: Context) {
        stopDialTone()
        stopIncomingRingtone()

        AgoraCallEngine.leaveChannel()

        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            audioManager?.let { am ->
                am.isSpeakerphoneOn = false
                am.isMicrophoneMute = false
                am.mode = AudioManager.MODE_NORMAL
            }
        } catch (_: Exception) {
        }

        FirestoreCallService.updateCallStatus("ended")

        isCallActive = false
        isIncomingCall = false
        isPeerConnected = false
        isMuted = false
        isSpeakerOn = false
        callStartTime = 0L
        activeCallTimestampKey = 0L
        latencyMs = 0
        diagnosticStatus = "Reset complete: Mic released, Audio normal"
    }

    private fun leaveCallSilently(context: Context) {
        stopDialTone()
        stopIncomingRingtone()
        AgoraCallEngine.leaveChannel()

        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            audioManager?.let { am ->
                am.isSpeakerphoneOn = false
                am.mode = AudioManager.MODE_NORMAL
            }
        } catch (_: Exception) {
        }

        isCallActive = false
        isPeerConnected = false
        callStartTime = 0L
        activeCallTimestampKey = 0L
        latencyMs = 0
    }

    fun toggleMute() {
        isMuted = !isMuted
        AgoraCallEngine.setMute(isMuted)
    }

    fun toggleSpeaker(context: Context) {
        isSpeakerOn = !isSpeakerOn
        AgoraCallEngine.setSpeaker(context, isSpeakerOn)
    }

    fun observeCallLogs(onLogsUpdated: (List<CallLogItem>) -> Unit) =
        FirestoreCallService.observeCallLogs(onLogsUpdated)
}

// Master Composable router for external callers (e.g. HomeScreen.kt)
@Composable
fun CallScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val avatarUrl by CacheManager.avatarUrlFlow.collectAsState()
    val callLogs = remember { mutableStateListOf<CallLogItem>() }
    val currentUserId by CacheManager.appUserIdFlow.collectAsState()
    val partnerDisplayName = if (currentUserId.equals("Momo", ignoreCase = true)) "Kanu" else "Momo"

    var pendingCallAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            pendingCallAction?.invoke()
        }
        pendingCallAction = null
    }

    fun executeWithMicPermission(action: () -> Unit) {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            action()
        } else {
            pendingCallAction = action
            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    DisposableEffect(Unit) {
        CallManager.startSignalingListener(context)
        val logsListener = CallManager.observeCallLogs { updatedList ->
            callLogs.clear()
            callLogs.addAll(updatedList)
        }
        onDispose {
            logsListener.remove()
        }
    }

    when {
        CallManager.isIncomingCall -> {
            IncomingCallView(
                avatarUrl = avatarUrl,
                callerName = CallManager.incomingCallerName,
                onAccept = {
                    executeWithMicPermission {
                        CallManager.acceptIncomingCall(context)
                    }
                },
                onDecline = {
                    CallManager.declineIncomingCall(context)
                }
            )
        }
        CallManager.isCallActive -> {
            ActiveCallScreen(
                onEndCall = {
                    CallManager.endCall(context)
                }
            )
        }
        else -> {
            CallHubView(
                avatarUrl = avatarUrl,
                partnerName = partnerDisplayName,
                currentUserId = currentUserId,
                callLogs = callLogs,
                onBack = onBack,
                onStartCall = {
                    executeWithMicPermission {
                        CallManager.startCall(context)
                    }
                },
                onResetCall = {
                    CallManager.resetAudioAndCallState(context)
                }
            )
        }
    }
}

@Composable
fun CallHubScreen(onBack: () -> Unit) = CallScreen(onBack = onBack)

@Composable
private fun IncomingCallView(
    avatarUrl: String?,
    callerName: String,
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
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
                    .size(116.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(2.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
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

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = callerName,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Incoming Voice Call...",
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium
            )
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 54.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF44336))
                        .bounceClick(scaleDown = 0.88f) {
                            onDecline()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CallEnd,
                        contentDescription = "Decline Call",
                        tint = Color.White,
                        modifier = Modifier.size(30.dp)
                    )
                }
                Text(
                    text = "Decline",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF4CAF50))
                        .bounceClick(scaleDown = 0.88f) {
                            onAccept()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Call,
                        contentDescription = "Accept Call",
                        tint = Color.White,
                        modifier = Modifier.size(30.dp)
                    )
                }
                Text(
                    text = "Accept",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun CallHubView(
    avatarUrl: String?,
    partnerName: String,
    currentUserId: String,
    callLogs: List<CallLogItem>,
    onBack: () -> Unit,
    onStartCall: () -> Unit,
    onResetCall: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
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
                    text = "$partnerName Call Hub",
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
            Spacer(modifier = Modifier.height(14.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = "Diagnostic",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Device: $currentUserId | ${CallManager.diagnosticStatus}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

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
                                contentDescription = "$partnerName Avatar",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = partnerName,
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

                    Spacer(modifier = Modifier.height(10.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                            .bounceClick(scaleDown = 0.94f) {
                                onResetCall()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CallEnd,
                                contentDescription = "Reset Call",
                                tint = Color(0xFFF44336),
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Cancel / Reset Audio",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

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

            Spacer(modifier = Modifier.height(10.dp))

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
                        CallLogRow(item = item, currentUserId = currentUserId, partnerName = partnerName)
                    }
                }
            }
        }
    }
}

@Composable
private fun CallLogRow(item: CallLogItem, currentUserId: String, partnerName: String) {
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
                val isMyIdKanu = currentUserId.equals("kanu", ignoreCase = true)
                val isOutgoing = (isMyIdKanu && item.callerId == 1) || (!isMyIdKanu && item.callerId == 2)
                val isMissed = item.durationSeconds == 0

                val iconColor = when {
                    isMissed -> Color(0xFFF44336)
                    isOutgoing -> Color(0xFF4CAF50)
                    else -> Color(0xFF2196F3)
                }

                val iconVector = when {
                    isMissed -> Icons.Default.CallMissed
                    isOutgoing -> Icons.Default.CallMade
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
                        text = partnerName,
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
                text = if (item.durationSeconds == 0) "Missed" else formatLogDuration(item.durationSeconds),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = if (item.durationSeconds == 0) Color(0xFFF44336) else MaterialTheme.colorScheme.onSurfaceVariant
            )
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
