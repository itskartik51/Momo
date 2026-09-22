package com.personal.momo.UI_Screens.Call

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.personal.momo.Cache.CacheManager
import com.personal.momo.UI_Screens.bounceClick
import java.util.Locale

// Master Controller & UI Orchestrator
object CallManager {
    var isCallActive by mutableStateOf(false)
    var isIncomingCall by mutableStateOf(false)
    var incomingCallerName by mutableStateOf("Momo")
    var currentChannelName by mutableStateOf("momo_private_voice_room")

    var isPeerConnected by mutableStateOf(false)
    var latencyMs by mutableIntStateOf(0)
    var isMuted by mutableStateOf(false)
    var isSpeakerOn by mutableStateOf(false)

    private var callStartTime: Long = 0L
    private var activeCallTimestampKey: Long = 0L

    init {
        // Wire Agora callbacks directly into master state
        AgoraCallEngine.onJoinChannelSuccess = { _, _ ->
            isCallActive = true
            callStartTime = System.currentTimeMillis()
        }

        AgoraCallEngine.onUserJoined = {
            isPeerConnected = true
            CallSounds.stopDialTone()
        }

        AgoraCallEngine.onUserOffline = { _, _ ->
            isPeerConnected = false
        }

        AgoraCallEngine.onLatencyUpdated = { latency ->
            latencyMs = latency
        }

        AgoraCallEngine.onErrorOccurred = { _ -> }

        AgoraCallEngine.onConnectionFailed = { _ ->
            CallSounds.stopDialTone()
        }
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
                    CallSounds.startIncomingRingtone(context)
                }
            },
            onCallConnected = {
                CallSounds.releaseAll()
                if (isCallActive) {
                    isPeerConnected = true
                }
            },
            onCallEnded = {
                CallSounds.releaseAll()
                if (isCallActive) {
                    leaveCallSilently(context)
                }
                isIncomingCall = false
            }
        )
    }

    fun stopSignalingListener() {
        CallSounds.releaseAll()
        FirestoreCallService.stopSignalingListener()
    }

    fun startCall(context: Context, channelName: String = "momo_private_voice_room") {
        AgoraCallEngine.initEngine(context)
        isMuted = false
        isSpeakerOn = false
        latencyMs = 120
        currentChannelName = channelName

        val myId = CacheManager.getAppUserId(context).lowercase(Locale.ROOT)
        val targetId = if (myId == "kanu") "momo" else "kanu"
        activeCallTimestampKey = System.currentTimeMillis()

        CallSounds.startDialTone()

        val token = AgoraCallEngine.buildAgoraToken(channelName)
        AgoraCallEngine.joinChannel(channelName, token)

        FirestoreCallService.sendCallSignal(
            caller = myId,
            receiver = targetId,
            channelName = channelName,
            timestamp = activeCallTimestampKey,
            onSuccess = {},
            onFailure = {
                CallSounds.stopDialTone()
            }
        )
    }

    fun acceptIncomingCall(context: Context) {
        CallSounds.stopIncomingRingtone()
        isIncomingCall = false
        AgoraCallEngine.initEngine(context)
        isMuted = false
        isSpeakerOn = false
        latencyMs = 120

        val token = AgoraCallEngine.buildAgoraToken(currentChannelName)
        AgoraCallEngine.joinChannel(currentChannelName, token)

        FirestoreCallService.updateCallStatus("connected")
    }

    fun declineIncomingCall(context: Context) {
        CallSounds.stopIncomingRingtone()
        isIncomingCall = false
        val callerCode = if (incomingCallerName.equals("Kanu", ignoreCase = true)) 1 else 2

        FirestoreCallService.updateCallStatus("ended")
        val logKey = if (activeCallTimestampKey > 0L) activeCallTimestampKey else System.currentTimeMillis()
        FirestoreCallService.logCall(logKey, callerCode, 0)
    }

    fun endCall(context: Context) {
        CallSounds.releaseAll()

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
    }

    fun resetAudioAndCallState(context: Context) {
        CallSounds.releaseAll()

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
    }

    private fun leaveCallSilently(context: Context) {
        CallSounds.releaseAll()
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
                    text = "Call",
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
            Spacer(modifier = Modifier.height(18.dp))

            // Modular Dialer Card
            DialerCard(
                avatarUrl = avatarUrl,
                partnerName = partnerName,
                onStartCall = onStartCall,
                onResetCall = onResetCall
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Modular Call History Section
            CallLogSection(
                callLogs = callLogs,
                currentUserId = currentUserId,
                partnerName = partnerName,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
