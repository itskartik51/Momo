package com.personal.momo.UI_Screens.Call

import android.content.Context
import android.media.AudioManager
import android.media.Ringtone
import android.media.RingtoneManager
import android.media.ToneGenerator

object CallSounds {
    private var toneGenerator: ToneGenerator? = null
    private var incomingRingtone: Ringtone? = null

    fun startDialTone() {
        try {
            stopDialTone()
            // Uses STREAM_RING to avoid occupying the VoIP telephony communication pipeline
            toneGenerator = ToneGenerator(AudioManager.STREAM_RING, 80)
            toneGenerator?.startTone(ToneGenerator.TONE_SUP_RINGTONE)
        } catch (_: Exception) {
        }
    }

    fun stopDialTone() {
        try {
            toneGenerator?.stopTone()
            toneGenerator?.release()
        } catch (_: Exception) {
        }
        toneGenerator = null
    }

    fun startIncomingRingtone(context: Context) {
        try {
            stopIncomingRingtone()
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            incomingRingtone = RingtoneManager.getRingtone(context.applicationContext, uri)
            incomingRingtone?.play()
        } catch (_: Exception) {
        }
    }

    fun stopIncomingRingtone() {
        try {
            incomingRingtone?.stop()
        } catch (_: Exception) {
        }
        incomingRingtone = null
    }

    fun releaseAll() {
        stopDialTone()
        stopIncomingRingtone()
    }
}
