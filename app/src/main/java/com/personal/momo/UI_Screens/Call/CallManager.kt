package com.personal.momo.UI_Screens.Call

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.Ringtone
import android.media.RingtoneManager
import android.media.ToneGenerator
import android.util.Base64
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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.personal.momo.Cache.CacheManager
import com.personal.momo.SecurityConfig
import com.personal.momo.UI_Screens.bounceClick
import io.agora.rtc2.ChannelMediaOptions
import io.agora.rtc2.Constants
import io.agora.rtc2.IRtcEngineEventHandler
import io.agora.rtc2.RtcEngine
import io.agora.rtc2.RtcEngineConfig
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.CRC32
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.random.Random

// Data Model for Past Call Records parsed from App/call_logs document fields
data class CallLogItem(
    val id: String = "",
    val callerId: Int = 1,
    val durationSeconds: Int = 0,
    val timestamp: Long = 0L
)

// Central Voice Engine & Signaling Controller
object CallManager {
    private const val AGORA_APP_ID = "8eb2889c463d4389af35fd64113508bc"
    private const val AGORA_PRIMARY_CERTIFICATE = "5f3a23a8b85d4d7694951ff7cbb79a2d"

    private var rtcEngine: RtcEngine? = null
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
    private val firestore by lazy { FirebaseFirestore.getInstance() }
    private var signalingListener: ListenerRegistration? = null
    private var callLogsListener: ListenerRegistration? = null

    private val rtcEventHandler = object : IRtcEngineEventHandler() {
        override fun onJoinChannelSuccess(channel: String?, uid: Int, elapsed: Int) {
            isCallActive = true
            callStartTime = System.currentTimeMillis()
            diagnosticStatus = "Agora: Channel Joined ($channel)"
        }

        override fun onUserJoined(uid: Int, elapsed: Int) {
            isPeerConnected = true
            stopDialTone()
            diagnosticStatus = "Agora: Partner Connected!"
        }

        override fun onUserOffline(uid: Int, reason: Int) {
            isPeerConnected = false
            diagnosticStatus = "Agora: Partner Offline (reason: $reason)"
        }

        override fun onRtcStats(stats: RtcStats?) {
            stats?.let {
                latencyMs = if (it.gatewayRtt > 0) it.gatewayRtt else it.lastmileDelay
            }
        }

        override fun onError(err: Int) {
            diagnosticStatus = "Agora Error: $err"
        }

        override fun onConnectionStateChanged(state: Int, reason: Int) {
            if (state == 5) {
                stopDialTone()
                diagnosticStatus = "Agora Connection Failed: reason $reason"
            }
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

    private fun ensureAuth(onReady: () -> Unit) {
        val auth = FirebaseAuth.getInstance()
        if (auth.currentUser != null) {
            onReady()
        } else {
            if (SecurityConfig.AUTH_EMAIL.isNotBlank() && SecurityConfig.AUTH_PASS.isNotBlank()) {
                diagnosticStatus = "Authenticating Firebase..."
                auth.signInWithEmailAndPassword(SecurityConfig.AUTH_EMAIL, SecurityConfig.AUTH_PASS)
                    .addOnSuccessListener {
                        onReady()
                    }
                    .addOnFailureListener { e ->
                        diagnosticStatus = "Auth Failed: ${e.localizedMessage}"
                    }
            } else {
                onReady()
            }
        }
    }

    private fun buildAgoraToken(channelName: String, uid: Int = 0): String {
        return try {
            val currentTs = (System.currentTimeMillis() / 1000L).toInt()
            val privilegeTs = currentTs + 86400
            val salt = Random.nextInt(100000000) + 1
            val uidStr = if (uid == 0) "" else (uid.toLong() and 0xFFFFFFFFL).toString()

            val msgBuf = ByteBuffer.allocate(64).order(ByteOrder.LITTLE_ENDIAN)
            msgBuf.putInt(salt)
            msgBuf.putInt(privilegeTs)
            msgBuf.putShort(4.toShort())

            msgBuf.putShort(1.toShort())
            msgBuf.putInt(privilegeTs)
            msgBuf.putShort(2.toShort())
            msgBuf.putInt(privilegeTs)
            msgBuf.putShort(3.toShort())
            msgBuf.putInt(privilegeTs)
            msgBuf.putShort(4.toShort())
            msgBuf.putInt(privilegeTs)

            val msgBytes = ByteArray(msgBuf.position())
            msgBuf.flip()
            msgBuf.get(msgBytes)

            val mac = Mac.getInstance("HmacSHA256")
            mac.init(SecretKeySpec(AGORA_PRIMARY_CERTIFICATE.toByteArray(Charsets.UTF_8), "HmacSHA256"))
            mac.update(AGORA_APP_ID.toByteArray(Charsets.UTF_8))
            mac.update(channelName.toByteArray(Charsets.UTF_8))
            mac.update(uidStr.toByteArray(Charsets.UTF_8))
            mac.update(msgBytes)
            val signature = mac.doFinal()

            val crcChannel = (CRC32().apply { update(channelName.toByteArray(Charsets.UTF_8)) }.value and 0xFFFFFFFFL).toInt()
            val crcUid = (CRC32().apply { update(uidStr.toByteArray(Charsets.UTF_8)) }.value and 0xFFFFFFFFL).toInt()

            val contentBuf = ByteBuffer.allocate(2 + signature.size + 4 + 4 + 2 + msgBytes.size).order(ByteOrder.LITTLE_ENDIAN)
            contentBuf.putShort(signature.size.toShort())
            contentBuf.put(signature)
            contentBuf.putInt(crcChannel)
            contentBuf.putInt(crcUid)
            contentBuf.putShort(msgBytes.size.toShort())
            contentBuf.put(msgBytes)

            val base64 = Base64.encodeToString(contentBuf.array(), Base64.NO_WRAP)
            "006$AGORA_APP_ID$base64"
        } catch (e: Exception) {
            diagnosticStatus = "Token Gen Error: ${e.localizedMessage}"
            ""
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
                diagnosticStatus = "Agora Engine Initialized"
            } catch (e: Exception) {
                diagnosticStatus = "Agora Init Error: ${e.localizedMessage}"
            }
        }
    }

    fun startSignalingListener(context: Context) {
        if (signalingListener != null) return

        ensureAuth {
            val myId = CacheManager.getAppUserId(context).lowercase(Locale.ROOT)

            signalingListener = firestore.collection("App").document("current_call")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) return@addSnapshotListener
                    if (snapshot == null || !snapshot.exists()) return@addSnapshotListener

                    val status = snapshot.getString("status") ?: ""
                    val caller = snapshot.getString("caller") ?: ""
                    val receiver = snapshot.getString("receiver") ?: ""
                    val channel = snapshot.getString("channelName") ?: "momo_private_voice_room"
                    val timestamp = snapshot.getLong("timestamp") ?: 0L
                    val isRecent = (System.currentTimeMillis() - timestamp) < 60_000L

                    when (status) {
                        "calling" -> {
                            if (!isCallActive && isRecent && receiver.equals(myId, ignoreCase = true)) {
                                incomingCallerName = if (caller.equals("kanu", ignoreCase = true)) "Kanu" else "Momo"
                                currentChannelName = channel
                                isIncomingCall = true
                                activeCallTimestampKey = timestamp
                                startIncomingRingtone(context)
                                diagnosticStatus = "Incoming call from $incomingCallerName!"
                            }
                        }
                        "connected" -> {
                            stopDialTone()
                            stopIncomingRingtone()
                            if (isCallActive) {
                                isPeerConnected = true
                                diagnosticStatus = "Call connected with partner"
                            }
                        }
                        "ended" -> {
                            stopDialTone()
                            stopIncomingRingtone()
                            if (isCallActive) {
                                leaveCallSilently(context)
                            }
                            isIncomingCall = false
                            diagnosticStatus = "Call ended"
                        }
                    }
                }
        }
    }

    fun stopSignalingListener() {
        stopDialTone()
        stopIncomingRingtone()
        signalingListener?.remove()
        signalingListener = null
    }

    fun startCall(context: Context, channelName: String = "momo_private_voice_room") {
        initEngine(context)
        isMuted = false
        isSpeakerOn = false
        latencyMs = 120
        currentChannelName = channelName

        val myId = CacheManager.getAppUserId(context).lowercase(Locale.ROOT)
        val targetId = if (myId == "kanu") "momo" else "kanu"
        activeCallTimestampKey = System.currentTimeMillis()

        val options = ChannelMediaOptions().apply {
            channelProfile = Constants.CHANNEL_PROFILE_COMMUNICATION
            clientRoleType = Constants.CLIENT_ROLE_BROADCASTER
            autoSubscribeAudio = true
            publishMicrophoneTrack = true
        }

        startDialTone()

        try {
            val token = buildAgoraToken(channelName)
            diagnosticStatus = "Joining Agora Channel..."
            rtcEngine?.joinChannel(token, channelName, 0, options)

            ensureAuth {
                diagnosticStatus = "Sending Call Signal to $targetId..."
                val callData = hashMapOf(
                    "caller" to myId,
                    "receiver" to targetId,
                    "status" to "calling",
                    "channelName" to channelName,
                    "timestamp" to activeCallTimestampKey
                )
                firestore.collection("App").document("current_call").set(callData)
                    .addOnSuccessListener {
                        diagnosticStatus = "Signal sent! Ringing..."
                    }
                    .addOnFailureListener { e ->
                        stopDialTone()
                        diagnosticStatus = "Firestore Signal Error: ${e.localizedMessage}"
                    }
            }
        } catch (e: Exception) {
            stopDialTone()
            diagnosticStatus = "Call Start Error: ${e.localizedMessage}"
        }
    }

    fun acceptIncomingCall(context: Context) {
        stopIncomingRingtone()
        isIncomingCall = false
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
            val token = buildAgoraToken(currentChannelName)
            diagnosticStatus = "Accepting & Joining Agora..."
            rtcEngine?.joinChannel(token, currentChannelName, 0, options)

            ensureAuth {
                firestore.collection("App").document("current_call").update("status", "connected")
                    .addOnSuccessListener {
                        diagnosticStatus = "Accepted. Status: connected"
                    }
                    .addOnFailureListener { e ->
                        diagnosticStatus = "Accept Error: ${e.localizedMessage}"
                    }
            }
        } catch (e: Exception) {
            diagnosticStatus = "Accept Agora Error: ${e.localizedMessage}"
        }
    }

    fun declineIncomingCall(context: Context) {
        stopIncomingRingtone()
        isIncomingCall = false
        val callerCode = if (incomingCallerName.equals("Kanu", ignoreCase = true)) 1 else 2

        ensureAuth {
            firestore.collection("App").document("current_call").update("status", "ended")

            val logKey = if (activeCallTimestampKey > 0L) activeCallTimestampKey.toString() else System.currentTimeMillis().toString()
            val missedLogValue = listOf(callerCode, 0)

            firestore.collection("App").document("call_logs")
                .update(logKey, missedLogValue)
                .addOnFailureListener {
                    firestore.collection("App").document("call_logs")
                        .set(mapOf(logKey to missedLogValue), com.google.firebase.firestore.SetOptions.merge())
                }
            diagnosticStatus = "Call declined"
        }
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

        try {
            rtcEngine?.leaveChannel()
        } catch (_: Exception) {
        }

        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            audioManager?.let { am ->
                am.isSpeakerphoneOn = false
                am.mode = AudioManager.MODE_NORMAL
            }
        } catch (_: Exception) {
        }

        ensureAuth {
            val logKey = if (activeCallTimestampKey > 0L) activeCallTimestampKey.toString() else System.currentTimeMillis().toString()
            val logValue = listOf(callerCode, statusDuration)

            firestore.collection("App").document("call_logs")
                .update(logKey, logValue)
                .addOnFailureListener {
                    firestore.collection("App").document("call_logs")
                        .set(mapOf(logKey to logValue), com.google.firebase.firestore.SetOptions.merge())
                }

            firestore.collection("App").document("current_call").update("status", "ended")
        }

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

        try {
            rtcEngine?.leaveChannel()
        } catch (_: Exception) {
        }

        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            audioManager?.let { am ->
                am.isSpeakerphoneOn = false
                am.isMicrophoneMute = false
                am.mode = AudioManager.MODE_NORMAL
            }
        } catch (_: Exception) {
        }

        ensureAuth {
            try {
                firestore.collection("App").document("current_call").update("status", "ended")
            } catch (_: Exception) {
            }
        }

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

        try {
            rtcEngine?.leaveChannel()
        } catch (_: Exception) {
        }

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
        rtcEngine?.muteLocalAudioStream(isMuted)
    }

    fun toggleSpeaker(context: Context) {
        isSpeakerOn = !isSpeakerOn
        rtcEngine?.setEnableSpeakerphone(isSpeakerOn)
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        audioManager?.isSpeakerphoneOn = isSpeakerOn
    }

    fun observeCallLogs(onLogsUpdated: (List<CallLogItem>) -> Unit): ListenerRegistration {
        return firestore.collection("App").document("call_logs")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null || !snapshot.exists()) {
                    onLogsUpdated(emptyList())
                    return@addSnapshotListener
                }

                val dataMap = snapshot.data ?: emptyMap()
                val parsedList = mutableListOf<CallLogItem>()

                for ((key, value) in dataMap) {
                    val ts = key.toLongOrNull() ?: 0L
                    if (ts == 0L) continue

                    val list = value as? List<*> ?: continue
                    if (list.size >= 2) {
                        val callerId = (list[0] as? Number)?.toInt() ?: 1
                        val duration = (list[1] as? Number)?.toInt() ?: 0

                        parsedList.add(
                            CallLogItem(
                                id = key,
                                callerId = callerId,
                                durationSeconds = duration,
                                timestamp = ts
                            )
                        )
                    }
                }

                val sortedLogs = parsedList.sortedByDescending { it.timestamp }.take(100)
                onLogsUpdated(sortedLogs)
            }
    }
}

// Entry Composable for Call Module (Hub, Signaling & Screen Router)
@Composable
fun CallHubScreen(
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
            // Calling the independent CallScreen composable from CallScreen.kt
            CallScreen(
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
