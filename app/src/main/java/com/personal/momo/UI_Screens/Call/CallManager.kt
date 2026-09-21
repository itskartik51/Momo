package com.personal.momo.UI_Screens.Call

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.agora.rtc2.Constants
import io.agora.rtc2.IRtcEngineEventHandler
import io.agora.rtc2.RtcEngine
import io.agora.rtc2.RtcEngineConfig
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.TreeMap
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object CallManager {

    private const val APP_ID = "8eb2889c463d4389af35fd64113508bc"
    private const val APP_CERTIFICATE = "5f3a23a8b85d4d7694951ff7cbb79a2d"

    private var rtcEngine: RtcEngine? = null

    // Reactive State for UI
    var isCallActive by mutableStateOf(false)
        private set

    var isMuted by mutableStateOf(false)
        private set

    var isSpeakerOn by mutableStateOf(true)
        private set

    var latencyMs by mutableIntStateOf(0)
        private set

    var isPeerConnected by mutableStateOf(false)
        private set

    private val rtcEventHandler = object : IRtcEngineEventHandler() {
        override fun onJoinChannelSuccess(channel: String?, uid: Int, elapsed: Int) {
            isCallActive = true
        }

        override fun onUserJoined(uid: Int, elapsed: Int) {
            isPeerConnected = true
        }

        override fun onUserOffline(uid: Int, reason: Int) {
            isPeerConnected = false
        }

        override fun onRtcStats(stats: RtcStats?) {
            stats?.let {
                latencyMs = it.lastmileDelay
            }
        }

        override fun onLeaveChannel(stats: RtcStats?) {
            isCallActive = false
            isPeerConnected = false
            latencyMs = 0
        }

        override fun onError(err: Int) {
            if (err == Constants.ERR_TOKEN_EXPIRED || err == Constants.ERR_INVALID_TOKEN) {
                leaveCall()
            }
        }
    }

    fun initEngine(context: Context) {
        if (rtcEngine != null) return
        try {
            val config = RtcEngineConfig().apply {
                mContext = context.applicationContext
                mAppId = APP_ID
                mEventHandler = rtcEventHandler
                mChannelProfile = Constants.CHANNEL_PROFILE_COMMUNICATION
            }
            rtcEngine = RtcEngine.create(config).apply {
                enableAudio()
                setDefaultAudioRoutetoSpeakerphone(true)
                setEnableSpeakerphone(true)
            }
        } catch (e: Exception) {
            rtcEngine = null
        }
    }

    fun joinCall(context: Context, channelName: String, uid: Int = 0) {
        if (rtcEngine == null) {
            initEngine(context)
        }

        val token = generateRtcToken(
            appId = APP_ID,
            appCertificate = APP_CERTIFICATE,
            channelName = channelName,
            uid = uid,
            expirationTimeInSeconds = 86400
        )

        rtcEngine?.let { engine ->
            engine.setEnableSpeakerphone(isSpeakerOn)
            engine.muteLocalAudioStream(isMuted)
            engine.joinChannel(token, channelName, "", uid)
        }
    }

    fun leaveCall() {
        try {
            rtcEngine?.leaveChannel()
        } catch (_: Exception) {
        } finally {
            isCallActive = false
            isPeerConnected = false
            latencyMs = 0
            isMuted = false
            isSpeakerOn = true
        }
    }

    fun toggleMute() {
        val nextState = !isMuted
        rtcEngine?.muteLocalAudioStream(nextState)
        isMuted = nextState
    }

    fun toggleSpeaker() {
        val nextState = !isSpeakerOn
        rtcEngine?.setEnableSpeakerphone(nextState)
        isSpeakerOn = nextState
    }

    fun release() {
        leaveCall()
        RtcEngine.destroy()
        rtcEngine = null
    }

    // In-App Agora Token Builder (RTC Token v006 - Zero external server dependency)
    private fun generateRtcToken(
        appId: String,
        appCertificate: String,
        channelName: String,
        uid: Int,
        expirationTimeInSeconds: Int
    ): String {
        return try {
            val currentTimestamp = (System.currentTimeMillis() / 1000).toInt()
            val privilegeExpiredTs = currentTimestamp + expirationTimeInSeconds
            val salt = (1..99999999).random()

            val messages = TreeMap<Short, Int>()
            messages[1.toShort()] = privilegeExpiredTs

            val baos = ByteArrayOutputStream()
            val dos = DataOutputStream(baos)
            dos.writeShort(messages.size)
            for ((key, value) in messages) {
                dos.writeShort(key.toInt())
                dos.writeInt(value)
            }
            val messageBytes = baos.toByteArray()

            val signStream = ByteArrayOutputStream()
            signStream.write(appId.toByteArray(Charsets.UTF_8))
            signStream.write(channelName.toByteArray(Charsets.UTF_8))
            val uidString = if (uid == 0) "" else uid.toString()
            signStream.write(uidString.toByteArray(Charsets.UTF_8))
            signStream.write(messageBytes)

            val signature = hmacSha256(appCertificate.toByteArray(Charsets.UTF_8), signStream.toByteArray())

            val contentStream = ByteArrayOutputStream()
            writeBytesWithLength(contentStream, signature)
            writeBytesWithLength(contentStream, appId.toByteArray(Charsets.UTF_8))
            contentStream.write(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(salt).array())
            contentStream.write(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(privilegeExpiredTs).array())
            writeBytesWithLength(contentStream, messageBytes)

            val finalSignature = hmacSha256(appCertificate.toByteArray(Charsets.UTF_8), contentStream.toByteArray())
            val tokenBuffer = ByteArrayOutputStream()
            writeBytesWithLength(tokenBuffer, finalSignature)
            tokenBuffer.write(contentStream.toByteArray())

            val base64Token = android.util.Base64.encodeToString(tokenBuffer.toByteArray(), android.util.Base64.NO_WRAP)
            "006$appId$base64Token"
        } catch (e: Exception) {
            ""
        }
    }

    private fun writeBytesWithLength(stream: ByteArrayOutputStream, bytes: ByteArray) {
        stream.write(ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort(bytes.size.toShort()).array())
        stream.write(bytes)
    }

    private fun hmacSha256(key: ByteArray, message: ByteArray): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(key, "HmacSHA256"))
        return mac.doFinal(message)
    }
}
