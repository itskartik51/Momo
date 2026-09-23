package com.personal.momo.UI_Screens.Call

import android.content.Context
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

enum class CallAudioDevice {
    EARPIECE,
    SPEAKER,
    WIRED,
    BLUETOOTH
}

/**
 * Dedicated Hardware Audio Controller for Momo VoIP.
 * Strictly enforces hardware priority:
 * 1. Wired Earphones (Highest Priority)
 * 2. Bluetooth Headset
 * 3. Normal Mode (Earpiece by default, toggled to Speaker only on explicit user click)
 */
object AudioMan {
    var currentDevice by mutableStateOf(CallAudioDevice.EARPIECE)
    var isSpeakerOn by mutableStateOf(false)
    var isWiredConnected by mutableStateOf(false)
    var isBluetoothConnected by mutableStateOf(false)

    private var isSpeakerManuallySelected = false
    private var audioDeviceCallback: AudioDeviceCallback? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private var debounceRunnable: Runnable? = null

    fun start(context: Context) {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
        audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
        isSpeakerManuallySelected = false

        registerCallback(context)
        refreshAndRoute(context)
    }

    fun stop(context: Context) {
        unregisterCallback(context)
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                audioManager.clearCommunicationDevice()
            } else {
                @Suppress("DEPRECATION")
                audioManager.isSpeakerphoneOn = false
                try {
                    audioManager.stopBluetoothSco()
                    @Suppress("DEPRECATION")
                    audioManager.isBluetoothScoOn = false
                } catch (_: Exception) {}
            }
            audioManager.isMicrophoneMute = false
            audioManager.mode = AudioManager.MODE_NORMAL
        } catch (_: Exception) {}

        isSpeakerManuallySelected = false
        currentDevice = CallAudioDevice.EARPIECE
        isSpeakerOn = false
        isWiredConnected = false
        isBluetoothConnected = false
    }

    fun toggleSpeaker(context: Context) {
        // Locked when a physical peripheral (Wired or BT) is plugged in
        if (isWiredConnected || isBluetoothConnected) return

        isSpeakerManuallySelected = !isSpeakerManuallySelected
        refreshAndRoute(context)
    }

    private fun registerCallback(context: Context) {
        if (audioDeviceCallback != null) return
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            audioDeviceCallback = object : AudioDeviceCallback() {
                override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>?) {
                    debounceRefresh(context)
                }

                override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>?) {
                    debounceRefresh(context)
                }
            }
            audioManager.registerAudioDeviceCallback(audioDeviceCallback, null)
        }
    }

    private fun unregisterCallback(context: Context) {
        debounceRunnable?.let { mainHandler.removeCallbacks(it) }
        debounceRunnable = null

        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && audioDeviceCallback != null) {
            audioManager.unregisterAudioDeviceCallback(audioDeviceCallback)
        }
        audioDeviceCallback = null
    }

    private fun debounceRefresh(context: Context) {
        debounceRunnable?.let { mainHandler.removeCallbacks(it) }
        debounceRunnable = Runnable {
            refreshAndRoute(context)
        }
        mainHandler.postDelayed(debounceRunnable!!, 300L)
    }

    fun refreshAndRoute(context: Context) {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return

        if (audioManager.mode != AudioManager.MODE_IN_COMMUNICATION) {
            audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
        }

        val outputs = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS).toList()
        } else {
            emptyList()
        }

        val wiredDevice = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            outputs.firstOrNull {
                it.type == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
                it.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
                (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && it.type == AudioDeviceInfo.TYPE_USB_HEADSET)
            }
        } else null

        val hasWired = wiredDevice != null || (outputs.isEmpty() && @Suppress("DEPRECATION") audioManager.isWiredHeadsetOn)
        isWiredConnected = hasWired

        val btDevice = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            outputs.firstOrNull {
                it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
                it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
                (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && it.type == AudioDeviceInfo.TYPE_BLE_HEADSET)
            }
        } else null

        val hasBt = btDevice != null || (outputs.isEmpty() && (@Suppress("DEPRECATION") audioManager.isBluetoothScoOn || @Suppress("DEPRECATION") audioManager.isBluetoothA2dpOn))
        isBluetoothConnected = hasBt

        // Strict Priority: Wired > Bluetooth > Speaker (if user tapped) > Earpiece
        if (hasWired) {
            routeToWired(audioManager)
        } else if (hasBt) {
            routeToBluetooth(audioManager)
        } else {
            if (isSpeakerManuallySelected) {
                routeToSpeaker(audioManager)
            } else {
                routeToEarpiece(audioManager)
            }
        }
    }

    private fun routeToWired(audioManager: AudioManager) {
        currentDevice = CallAudioDevice.WIRED
        isSpeakerOn = false

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            audioManager.clearCommunicationDevice()
            val target = audioManager.availableCommunicationDevices.firstOrNull {
                it.type == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
                it.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
                it.type == AudioDeviceInfo.TYPE_USB_HEADSET
            }
            if (target != null) {
                audioManager.setCommunicationDevice(target)
            }
        } else {
            @Suppress("DEPRECATION")
            audioManager.isSpeakerphoneOn = false
            try {
                audioManager.stopBluetoothSco()
                @Suppress("DEPRECATION")
                audioManager.isBluetoothScoOn = false
            } catch (_: Exception) {}
        }
    }

    private fun routeToBluetooth(audioManager: AudioManager) {
        currentDevice = CallAudioDevice.BLUETOOTH
        isSpeakerOn = false

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            audioManager.clearCommunicationDevice()
            val target = audioManager.availableCommunicationDevices.firstOrNull {
                it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
                it.type == AudioDeviceInfo.TYPE_BLE_HEADSET
            }
            if (target != null) {
                audioManager.setCommunicationDevice(target)
            } else {
                mainHandler.postDelayed({
                    val retry = audioManager.availableCommunicationDevices.firstOrNull {
                        it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
                        it.type == AudioDeviceInfo.TYPE_BLE_HEADSET
                    }
                    if (retry != null) {
                        audioManager.setCommunicationDevice(retry)
                    }
                }, 250L)
            }
        } else {
            @Suppress("DEPRECATION")
            audioManager.isSpeakerphoneOn = false
            try {
                audioManager.startBluetoothSco()
                @Suppress("DEPRECATION")
                audioManager.isBluetoothScoOn = true
            } catch (_: Exception) {}
        }
    }

    private fun routeToSpeaker(audioManager: AudioManager) {
        currentDevice = CallAudioDevice.SPEAKER
        isSpeakerOn = true

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            audioManager.clearCommunicationDevice()
            val target = audioManager.availableCommunicationDevices.firstOrNull {
                it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER
            }
            if (target != null) {
                audioManager.setCommunicationDevice(target)
            }
        } else {
            try {
                audioManager.stopBluetoothSco()
                @Suppress("DEPRECATION")
                audioManager.isBluetoothScoOn = false
            } catch (_: Exception) {}
            @Suppress("DEPRECATION")
            audioManager.isSpeakerphoneOn = true
        }
    }

    private fun routeToEarpiece(audioManager: AudioManager) {
        currentDevice = CallAudioDevice.EARPIECE
        isSpeakerOn = false

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            audioManager.clearCommunicationDevice()
            val target = audioManager.availableCommunicationDevices.firstOrNull {
                it.type == AudioDeviceInfo.TYPE_BUILTIN_EARPIECE
            }
            if (target != null) {
                audioManager.setCommunicationDevice(target)
            }
        } else {
            try {
                audioManager.stopBluetoothSco()
                @Suppress("DEPRECATION")
                audioManager.isBluetoothScoOn = false
            } catch (_: Exception) {}
            @Suppress("DEPRECATION")
            audioManager.isSpeakerphoneOn = false
        }
    }
}
