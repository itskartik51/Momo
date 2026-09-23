package com.personal.momo.UI_Screens.Call

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
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

/**
 * Pure Coordinator: Connects UI, AudioMan, AgoraCallEngine, and FirestoreCallService.
 * Coordinates system-wide incoming call notifications (Heads-Up Banner + Full Screen Intent),
 * late-join architecture, and precise call logs without audio echoes.
 */
object CallManager {
    var isCallActive by mutableStateOf(false)
    var isIncomingCall by mutableStateOf(false)
    var isConnecting by mutableStateOf(false)
    var isPeerConnected by mutableStateOf(false)

    var incomingCallerName by mutableStateOf("Momo")
    var currentChannelName by mutableStateOf("momo_private_voice_room")
    var latencyMs by mutableIntStateOf(0)
    var isMuted by mutableStateOf(false)

    private var activeCallerCode: Int = 1
    private var connectedAtTimestamp: Long = 0L

    init {
        AgoraCallEngine.onJoinSuccess = { _, _ -> }

        AgoraCallEngine.onPeerJoined = {
            isConnecting = false
            isPeerConnected = true
            connectedAtTimestamp = System.currentTimeMillis()
            CallSounds.stopDialTone()
            FirestoreCallService.updateCallStatus("connected")
        }

        AgoraCallEngine.onPeerOffline = { _, _ ->
            isPeerConnected = false
        }

        AgoraCallEngine.onLatencyUpdated = { latency ->
            latencyMs = latency
        }

        AgoraCallEngine.onConnectionFailed = {
            CallSounds.stopDialTone()
        }
    }

    fun toggleMute() {
        isMuted = !isMuted
        AgoraCallEngine.setMute(isMuted)
    }

    fun startSignalingListener(context: Context) {
        val myId = CacheManager.getAppUserId(context).lowercase(Locale.ROOT)

        FirestoreCallService.startSignalingListener(
            myUserId = myId,
            onIncomingCall = { caller, channel, _ ->
                if (!isCallActive) {
                    incomingCallerName = if (caller.equals("kanu", ignoreCase = true)) "Kanu" else "Momo"
                    activeCallerCode = if (caller.equals("kanu", ignoreCase = true)) 1 else 2
                    currentChannelName = channel
                    isIncomingCall = true

                    AgoraCallEngine.preWarm(context)

                    // High-Priority Notification Channel plays native ringtone and wakes the locked screen
                    IncomingCallNotifier.show(context, incomingCallerName)
                }
            },
            onCallAccepted = {
                CallSounds.stopDialTone()
                IncomingCallNotifier.cancel(context)
                isConnecting = true
                AgoraCallEngine.joinRoom(currentChannelName)
            },
            onCallConnected = {
                CallSounds.releaseAll()
                IncomingCallNotifier.cancel(context)
                if (isCallActive) {
                    isPeerConnected = true
                    if (connectedAtTimestamp == 0L) {
                        connectedAtTimestamp = System.currentTimeMillis()
                    }
                }
            },
            onCallEnded = {
                CallSounds.releaseAll()
                IncomingCallNotifier.cancel(context)
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
        AgoraCallEngine.preWarm(context)
        isMuted = false
        latencyMs = 120
        currentChannelName = channelName
        isCallActive = true
        isConnecting = false
        isPeerConnected = false
        connectedAtTimestamp = 0L

        val myId = CacheManager.getAppUserId(context).lowercase(Locale.ROOT)
        val targetId = if (myId == "kanu") "momo" else "kanu"
        activeCallerCode = if (myId == "kanu") 1 else 2

        AudioMan.start(context)
        CallSounds.startDialTone()

        FirestoreCallService.sendCallSignal(
            caller = myId,
            receiver = targetId,
            channelName = channelName,
            timestamp = System.currentTimeMillis(),
            onSuccess = {},
            onFailure = {
                CallSounds.stopDialTone()
            }
        )
    }

    fun acceptIncomingCall(context: Context) {
        IncomingCallNotifier.cancel(context)
        CallSounds.stopIncomingRingtone()
        isIncomingCall = false
        isCallActive = true
        isConnecting = true
        isMuted = false
        latencyMs = 120

        AudioMan.start(context)
        AgoraCallEngine.joinRoom(currentChannelName)
        FirestoreCallService.updateCallStatus("accepted")
    }

    fun declineIncomingCall(context: Context) {
        IncomingCallNotifier.cancel(context)
        CallSounds.stopIncomingRingtone()
        isIncomingCall = false

        val cutTimestamp = System.currentTimeMillis()
        FirestoreCallService.logCall(cutTimestamp, activeCallerCode, 0)
        FirestoreCallService.updateCallStatus("ended")

        AudioMan.stop(context)
        AgoraCallEngine.leaveRoom()
    }

    fun endCall(context: Context) {
        IncomingCallNotifier.cancel(context)
        CallSounds.releaseAll()

        if (isPeerConnected && connectedAtTimestamp > 0L) {
            val durationSeconds = ((System.currentTimeMillis() - connectedAtTimestamp) / 1000).toInt()
            val finalDuration = if (durationSeconds > 0) durationSeconds else 1
            FirestoreCallService.logCall(connectedAtTimestamp, activeCallerCode, finalDuration)
        } else {
            val cutTimestamp = System.currentTimeMillis()
            FirestoreCallService.logCall(cutTimestamp, activeCallerCode, 0)
        }

        AudioMan.stop(context)
        AgoraCallEngine.leaveRoom()
        FirestoreCallService.updateCallStatus("ended")

        isCallActive = false
        isConnecting = false
        isPeerConnected = false
        connectedAtTimestamp = 0L
        latencyMs = 0
    }

    fun resetAudioAndCallState(context: Context) {
        IncomingCallNotifier.cancel(context)
        CallSounds.releaseAll()
        AudioMan.stop(context)
        AgoraCallEngine.leaveRoom()
        FirestoreCallService.updateCallStatus("ended")

        isCallActive = false
        isIncomingCall = false
        isConnecting = false
        isPeerConnected = false
        isMuted = false
        connectedAtTimestamp = 0L
        latencyMs = 0
    }

    private fun leaveCallSilently(context: Context) {
        IncomingCallNotifier.cancel(context)
        CallSounds.releaseAll()
        AudioMan.stop(context)
        AgoraCallEngine.leaveRoom()

        isCallActive = false
        isConnecting = false
        isPeerConnected = false
        connectedAtTimestamp = 0L
        latencyMs = 0
    }

    fun observeCallLogs(onLogsUpdated: (List<CallLogItem>) -> Unit) =
        FirestoreCallService.observeCallLogs(onLogsUpdated)
}

@Composable
fun CallScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val avatarUrl by CacheManager.avatarUrlFlow.collectAsState()
    val callLogs = remember { mutableStateListOf<CallLogItem>() }
    val currentUserId by CacheManager.appUserIdFlow.collectAsState()
    val partnerDisplayName = if (currentUserId.equals("Momo", ignoreCase = true)) "Kanu" else "Momo"

    var pendingCallAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) pendingCallAction?.invoke()
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
                        .bounceClick(scaleDown = 0.94f) { onBack() },
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

            DialerCard(
                avatarUrl = avatarUrl,
                partnerName = partnerName,
                onStartCall = onStartCall,
                onResetCall = onResetCall
            )

            Spacer(modifier = Modifier.height(24.dp))

            CallLogSection(
                callLogs = callLogs,
                currentUserId = currentUserId,
                partnerName = partnerName,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
