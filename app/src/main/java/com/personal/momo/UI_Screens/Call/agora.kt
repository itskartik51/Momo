package com.personal.momo.UI_Screens.Call

import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.util.Base64
import io.agora.rtc2.ChannelMediaOptions
import io.agora.rtc2.Constants
import io.agora.rtc2.IRtcEngineEventHandler
import io.agora.rtc2.RtcEngine
import io.agora.rtc2.RtcEngineConfig
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.zip.CRC32
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.random.Random

enum class AudioRoute {
    BLUETOOTH,
    SPEAKER,
    PHONE
}

object AgoraCallEngine {
    private const val AGORA_APP_ID = "8eb2889c463d4389af35fd64113508bc"
    private const val AGORA_PRIMARY_CERTIFICATE = "5f3a23a8b85d4d7694951ff7cbb79a2d"

    var rtcEngine: RtcEngine? = null
        private set

    // Callbacks to notify master CallManager
    var onJoinChannelSuccess: ((channel: String?, uid: Int) -> Unit)? = null
    var onUserJoined: ((uid: Int) -> Unit)? = null
    var onUserOffline: ((uid: Int, reason: Int) -> Unit)? = null
    var onLatencyUpdated: ((latencyMs: Int) -> Unit)? = null
    var onAudioRouteChanged: ((routing: Int) -> Unit)? = null
    var onErrorOccurred: ((err: Int) -> Unit)? = null
    var onConnectionFailed: ((reason: Int) -> Unit)? = null

    private val rtcEventHandler = object : IRtcEngineEventHandler() {
        override fun onJoinChannelSuccess(channel: String?, uid: Int, elapsed: Int) {
            onJoinChannelSuccess?.invoke(channel, uid)
        }

        override fun onUserJoined(uid: Int, elapsed: Int) {
            onUserJoined?.invoke(uid)
        }

        override fun onUserOffline(uid: Int, reason: Int) {
            onUserOffline?.invoke(uid, reason)
        }

        override fun onRtcStats(stats: RtcStats?) {
            stats?.let {
                val latency = if (it.gatewayRtt > 0) it.gatewayRtt else it.lastmileDelay
                onLatencyUpdated?.invoke(latency)
            }
        }

        override fun onAudioRouteChanged(routing: Int) {
            onAudioRouteChanged?.invoke(routing)
        }

        override fun onError(err: Int) {
            onErrorOccurred?.invoke(err)
        }

        override fun onConnectionStateChanged(state: Int, reason: Int) {
            if (state == 5) {
                onConnectionFailed?.invoke(reason)
            }
        }
    }

    fun initEngine(context: Context, onError: (String) -> Unit = {}): Boolean {
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
                return true
            } catch (e: Exception) {
                onError(e.localizedMessage ?: "Unknown Init Error")
                return false
            }
        }
        return true
    }

    fun buildAgoraToken(channelName: String, uid: Int = 0): String {
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
        } catch (_: Exception) {
            ""
        }
    }

    fun joinChannel(channelName: String, token: String): Int {
        val options = ChannelMediaOptions().apply {
            channelProfile = Constants.CHANNEL_PROFILE_COMMUNICATION
            clientRoleType = Constants.CLIENT_ROLE_BROADCASTER
            autoSubscribeAudio = true
            publishMicrophoneTrack = true
        }
        return rtcEngine?.joinChannel(token, channelName, 0, options) ?: -1
    }

    fun leaveChannel() {
        try {
            rtcEngine?.leaveChannel()
        } catch (_: Exception) {
        }
    }

    fun setMute(isMuted: Boolean) {
        rtcEngine?.muteLocalAudioStream(isMuted)
    }

    fun checkBluetoothConnected(context: Context): Boolean {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return false
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
                devices.any {
                    it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
                    it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
                    (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && it.type == AudioDeviceInfo.TYPE_BLE_HEADSET)
                }
            } else {
                @Suppress("DEPRECATION")
                audioManager.isBluetoothScoOn || audioManager.isBluetoothA2dpOn
            }
        } catch (_: Exception) {
            false
        }
    }

    fun getConnectedBluetoothName(context: Context): String {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return "Bluetooth"
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
                val btDevice = devices.firstOrNull {
                    it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
                    it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
                    (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && it.type == AudioDeviceInfo.TYPE_BLE_HEADSET)
                }
                val name = btDevice?.productName?.toString()
                if (!name.isNullOrBlank()) name else "Bluetooth"
            } else {
                "Bluetooth"
            }
        } catch (_: Exception) {
            "Bluetooth"
        }
    }

    fun setAudioRoute(context: Context, route: AudioRoute) {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return

        when (route) {
            AudioRoute.SPEAKER -> {
                rtcEngine?.setEnableSpeakerphone(true)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val speakerDevice = audioManager.availableCommunicationDevices.firstOrNull {
                        it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER
                    }
                    if (speakerDevice != null) {
                        audioManager.setCommunicationDevice(speakerDevice)
                    } else {
                        audioManager.isSpeakerphoneOn = true
                    }
                } else {
                    try {
                        audioManager.stopBluetoothSco()
                        audioManager.isBluetoothScoOn = false
                    } catch (_: Exception) {
                    }
                    audioManager.isSpeakerphoneOn = true
                }
            }
            AudioRoute.BLUETOOTH -> {
                rtcEngine?.setEnableSpeakerphone(false)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val btDevice = audioManager.availableCommunicationDevices.firstOrNull {
                        it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
                        it.type == AudioDeviceInfo.TYPE_BLE_HEADSET
                    }
                    if (btDevice != null) {
                        audioManager.setCommunicationDevice(btDevice)
                    } else {
                        try {
                            audioManager.startBluetoothSco()
                            audioManager.isBluetoothScoOn = true
                        } catch (_: Exception) {
                        }
                    }
                } else {
                    audioManager.isSpeakerphoneOn = false
                    try {
                        audioManager.startBluetoothSco()
                        audioManager.isBluetoothScoOn = true
                    } catch (_: Exception) {
                    }
                }
            }
            AudioRoute.PHONE -> {
                rtcEngine?.setEnableSpeakerphone(false)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val earpieceDevice = audioManager.availableCommunicationDevices.firstOrNull {
                        it.type == AudioDeviceInfo.TYPE_BUILTIN_EARPIECE
                    }
                    if (earpieceDevice != null) {
                        audioManager.setCommunicationDevice(earpieceDevice)
                    } else {
                        audioManager.clearCommunicationDevice()
                        audioManager.isSpeakerphoneOn = false
                    }
                } else {
                    try {
                        audioManager.stopBluetoothSco()
                        audioManager.isBluetoothScoOn = false
                    } catch (_: Exception) {
                    }
                    audioManager.isSpeakerphoneOn = false
                }
            }
        }
    }

    fun resetAudio(context: Context) {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                audioManager.clearCommunicationDevice()
            }
            audioManager.stopBluetoothSco()
            audioManager.isBluetoothScoOn = false
            audioManager.isSpeakerphoneOn = false
            audioManager.isMicrophoneMute = false
            audioManager.mode = AudioManager.MODE_NORMAL
        } catch (_: Exception) {
        }
    }

    fun setSpeaker(context: Context, isSpeakerOn: Boolean) {
        if (isSpeakerOn) {
            setAudioRoute(context, AudioRoute.SPEAKER)
        } else {
            if (checkBluetoothConnected(context)) {
                setAudioRoute(context, AudioRoute.BLUETOOTH)
            } else {
                setAudioRoute(context, AudioRoute.PHONE)
            }
        }
    }
}
