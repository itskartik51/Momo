package com.personal.momo.UI_Screens.Finder

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.exp
import kotlin.math.sin

class CompassSoundPlayer(context: Context) {
    private val soundPool: SoundPool = SoundPool.Builder()
        .setMaxStreams(2)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()

    private var lightSoundId: Int = 0
    private var heavySoundId: Int = 0

    init {
        try {
            val lightFile = File(context.cacheDir, "momo_tick_light.wav")
            val heavyFile = File(context.cacheDir, "momo_tick_heavy.wav")

            if (!lightFile.exists() || lightFile.length() == 0L) {
                generateWav(lightFile, frequency = 2500.0, durationMs = 6, decay = 550.0, volume = 0.5f)
            }
            if (!heavyFile.exists() || heavyFile.length() == 0L) {
                generateWav(heavyFile, frequency = 950.0, durationMs = 15, decay = 250.0, volume = 0.9f)
            }

            lightSoundId = soundPool.load(lightFile.absolutePath, 1)
            heavySoundId = soundPool.load(heavyFile.absolutePath, 1)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun playLight() {
        if (lightSoundId != 0) {
            soundPool.play(lightSoundId, 0.5f, 0.5f, 1, 0, 1.0f)
        }
    }

    fun playHeavy() {
        if (heavySoundId != 0) {
            soundPool.play(heavySoundId, 0.85f, 0.85f, 1, 0, 1.0f)
        }
    }

    fun release() {
        try {
            soundPool.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun generateWav(file: File, frequency: Double, durationMs: Int, decay: Double, volume: Float) {
        val sampleRate = 44100
        val numSamples = (sampleRate * durationMs) / 1000
        val shortBuffer = ShortArray(numSamples)

        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val env = exp(-decay * t)
            val s = (sin(2.0 * Math.PI * frequency * t) * env * Short.MAX_VALUE * volume).toInt()
            shortBuffer[i] = s.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }

        val dataSize = numSamples * 2
        val totalSize = 36 + dataSize
        val byteBuffer = ByteBuffer.allocate(44 + dataSize).order(ByteOrder.LITTLE_ENDIAN)

        // Standard 44-byte WAV header
        byteBuffer.put("RIFF".toByteArray(Charsets.US_ASCII))
        byteBuffer.putInt(totalSize)
        byteBuffer.put("WAVE".toByteArray(Charsets.US_ASCII))
        byteBuffer.put("fmt ".toByteArray(Charsets.US_ASCII))
        byteBuffer.putInt(16)
        byteBuffer.putShort(1)
        byteBuffer.putShort(1)
        byteBuffer.putInt(sampleRate)
        byteBuffer.putInt(sampleRate * 2)
        byteBuffer.putShort(2)
        byteBuffer.putShort(16)
        byteBuffer.put("data".toByteArray(Charsets.US_ASCII))
        byteBuffer.putInt(dataSize)

        for (sample in shortBuffer) {
            byteBuffer.putShort(sample)
        }

        FileOutputStream(file).use { fos ->
            fos.write(byteBuffer.array())
        }
    }
}
