@file:OptIn(UnstableApi::class)

package com.personal.momo.UI_Screens.Chat.Media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Base64
import androidx.media3.common.Effect
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.FrameDropEffect
import androidx.media3.effect.Presentation
import androidx.media3.transformer.Composition
import androidx.media3.transformer.DefaultEncoderFactory
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.ProgressHolder
import androidx.media3.transformer.Transformer
import androidx.media3.transformer.VideoEncoderSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.roundToInt

object MediaCompressor {

    private const val MAX_IMAGE_DIMENSION = 1600
    private const val DEFAULT_IMAGE_QUALITY = 80
    private const val MAX_VIDEO_SHORT_SIDE = 720
    private const val MAX_VIDEO_LONG_SIDE = 1280
    private const val TARGET_VIDEO_BITRATE = 1_800_000
    private const val TARGET_VIDEO_FPS = 30f
    private const val SMART_SKIP_MAX_IMAGE_SIZE_BYTES = 153_600L
    private const val BLUR_THUMB_MAX_DIMENSION = 36

    suspend fun compressImage(
        context: Context,
        inputUri: Uri,
        maxDimension: Int = MAX_IMAGE_DIMENSION,
        quality: Int = DEFAULT_IMAGE_QUALITY
    ): File = withContext(Dispatchers.IO) {
        val boundsOptions = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }

        context.contentResolver.openInputStream(inputUri)?.use { inputStream ->
            BitmapFactory.decodeStream(inputStream, null, boundsOptions)
        } ?: throw IllegalArgumentException("Cannot open image input stream for URI: $inputUri")

        val rawWidth = boundsOptions.outWidth
        val rawHeight = boundsOptions.outHeight
        if (rawWidth <= 0 || rawHeight <= 0) {
            throw IllegalArgumentException("Invalid image dimensions from URI: $inputUri")
        }

        var rotationDegrees = 0
        try {
            context.contentResolver.openInputStream(inputUri)?.use { inputStream ->
                val exif = ExifInterface(inputStream)
                val orientation = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
                rotationDegrees = when (orientation) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270
                    else -> 0
                }
            }
        } catch (_: Exception) {
            rotationDegrees = 0
        }

        val cacheFolder = File(context.cacheDir, "media_cache").apply {
            if (!exists()) mkdirs()
        }
        val outputImageFile = File(cacheFolder, "IMG_${UUID.randomUUID().toString().take(8)}.jpg")

        val inputSizeBytes = getUriFileSize(context, inputUri)
        val maxOriginalDimension = maxOf(rawWidth, rawHeight)

        if (inputSizeBytes in 1..SMART_SKIP_MAX_IMAGE_SIZE_BYTES &&
            maxOriginalDimension <= maxDimension &&
            rotationDegrees == 0
        ) {
            context.contentResolver.openInputStream(inputUri)?.use { inputStream ->
                FileOutputStream(outputImageFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            return@withContext outputImageFile
        }

        var inSampleSize = 1
        if (maxOriginalDimension > maxDimension) {
            val halfMax = maxOriginalDimension / 2
            while ((halfMax / inSampleSize) >= maxDimension) {
                inSampleSize *= 2
            }
        }

        val decodeOptions = BitmapFactory.Options().apply {
            this.inSampleSize = inSampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }

        val sampledBitmap = context.contentResolver.openInputStream(inputUri)?.use { inputStream ->
            BitmapFactory.decodeStream(inputStream, null, decodeOptions)
        } ?: throw IllegalStateException("Failed to decode sampled bitmap")

        val currentMax = maxOf(sampledBitmap.width, sampledBitmap.height)
        val scaledBitmap = if (currentMax > maxDimension) {
            val scaleFactor = maxDimension.toFloat() / currentMax
            val destWidth = (sampledBitmap.width * scaleFactor).roundToInt().coerceAtLeast(1)
            val destHeight = (sampledBitmap.height * scaleFactor).roundToInt().coerceAtLeast(1)
            Bitmap.createScaledBitmap(sampledBitmap, destWidth, destHeight, true).also {
                if (it != sampledBitmap) {
                    sampledBitmap.recycle()
                }
            }
        } else {
            sampledBitmap
        }

        val finalOrientedBitmap = if (rotationDegrees != 0) {
            val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
            Bitmap.createBitmap(
                scaledBitmap,
                0,
                0,
                scaledBitmap.width,
                scaledBitmap.height,
                matrix,
                true
            ).also {
                if (it != scaledBitmap) {
                    scaledBitmap.recycle()
                }
            }
        } else {
            scaledBitmap
        }

        FileOutputStream(outputImageFile).use { outputStream ->
            finalOrientedBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
        }
        finalOrientedBitmap.recycle()

        outputImageFile
    }

    suspend fun compressVideo(
        context: Context,
        inputUri: Uri,
        onProgress: ((Float) -> Unit)? = null
    ): File = withContext(Dispatchers.Main) {
        val cacheFolder = File(context.cacheDir, "media_cache").apply {
            if (!exists()) mkdirs()
        }
        val outputVideoFile = File(cacheFolder, "VID_${UUID.randomUUID().toString().take(8)}.mp4")

        val videoEffects = mutableListOf<Effect>()

        videoEffects.add(FrameDropEffect.createDefaultFrameDropEffect(TARGET_VIDEO_FPS))

        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, inputUri)
            val widthStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
            val heightStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
            val rotationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)

            val rawW = widthStr?.toIntOrNull() ?: 0
            val rawH = heightStr?.toIntOrNull() ?: 0
            val rotation = rotationStr?.toIntOrNull() ?: 0

            val isRotated = rotation == 90 || rotation == 270
            val actualW = if (isRotated) rawH else rawW
            val actualH = if (isRotated) rawW else rawH

            if (actualW > 0 && actualH > 0) {
                val isPortrait = actualH > actualW
                val targetHeight = if (isPortrait) {
                    if (actualW > MAX_VIDEO_SHORT_SIDE) {
                        val computed = ((actualH * MAX_VIDEO_SHORT_SIDE.toFloat()) / actualW).roundToInt()
                        if (computed % 2 != 0) computed + 1 else computed
                    } else {
                        if (actualH % 2 != 0) actualH + 1 else actualH
                    }
                } else {
                    if (actualH > MAX_VIDEO_SHORT_SIDE) {
                        MAX_VIDEO_SHORT_SIDE
                    } else {
                        if (actualH % 2 != 0) actualH + 1 else actualH
                    }
                }

                if (targetHeight != actualH && targetHeight in 240..MAX_VIDEO_LONG_SIDE) {
                    videoEffects.add(Presentation.createForHeight(targetHeight))
                }
            }
        } catch (_: Exception) {
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {
            }
        }

        val mediaItem = MediaItem.fromUri(inputUri)
        val editedMediaItem = EditedMediaItem.Builder(mediaItem)
            .apply {
                if (videoEffects.isNotEmpty()) {
                    setEffects(
                        Effects(
                            emptyList(),
                            videoEffects
                        )
                    )
                }
            }
            .build()

        suspendCancellableCoroutine { continuation ->
            val listener = object : Transformer.Listener {
                override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                    if (continuation.isActive) {
                        continuation.resume(outputVideoFile)
                    }
                }

                override fun onError(
                    composition: Composition,
                    exportResult: ExportResult,
                    exportException: ExportException
                ) {
                    if (outputVideoFile.exists()) {
                        outputVideoFile.delete()
                    }
                    if (continuation.isActive) {
                        continuation.resumeWithException(exportException)
                    }
                }
            }

            val videoEncoderSettings = VideoEncoderSettings.Builder()
                .setBitrate(TARGET_VIDEO_BITRATE)
                .build()

            val encoderFactory = DefaultEncoderFactory.Builder(context.applicationContext)
                .setRequestedVideoEncoderSettings(videoEncoderSettings)
                .setEnableFallback(true)
                .build()

            val transformer = Transformer.Builder(context.applicationContext)
                .setEncoderFactory(encoderFactory)
                .addListener(listener)
                .setVideoMimeType(MimeTypes.VIDEO_H264)
                .setAudioMimeType(MimeTypes.AUDIO_AAC)
                .build()

            continuation.invokeOnCancellation {
                transformer.cancel()
                if (outputVideoFile.exists()) {
                    outputVideoFile.delete()
                }
            }

            if (onProgress != null) {
                val progressHolder = ProgressHolder()
                launch {
                    while (continuation.isActive) {
                        val state = transformer.getProgress(progressHolder)
                        if (state == Transformer.PROGRESS_STATE_AVAILABLE) {
                            onProgress(progressHolder.progress / 100f)
                        }
                        delay(120)
                    }
                }
            }

            transformer.start(editedMediaItem, outputVideoFile.absolutePath)
        }
    }

    fun generateBlurThumbnail(file: File, isVideo: Boolean): String? {
        if (!file.exists() || file.length() == 0L) return null
        return try {
            val rawBitmap: Bitmap? = if (isVideo) {
                val retriever = MediaMetadataRetriever()
                try {
                    retriever.setDataSource(file.absolutePath)
                    retriever.getFrameAtTime(0L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                        ?: retriever.frameAtTime
                } finally {
                    try {
                        retriever.release()
                    } catch (_: Exception) {}
                }
            } else {
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(file.absolutePath, bounds)
                val maxDim = maxOf(bounds.outWidth, bounds.outHeight)
                var sample = 1
                while (maxDim / sample > BLUR_THUMB_MAX_DIMENSION * 2) {
                    sample *= 2
                }
                val opts = BitmapFactory.Options().apply {
                    inSampleSize = sample
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                }
                val decoded = BitmapFactory.decodeFile(file.absolutePath, opts)
                if (decoded != null) {
                    var rotation = 0
                    try {
                        val exif = ExifInterface(file.absolutePath)
                        rotation = when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                            ExifInterface.ORIENTATION_ROTATE_90 -> 90
                            ExifInterface.ORIENTATION_ROTATE_180 -> 180
                            ExifInterface.ORIENTATION_ROTATE_270 -> 270
                            else -> 0
                        }
                    } catch (_: Exception) {}
                    if (rotation != 0) {
                        val matrix = Matrix().apply { postRotate(rotation.toFloat()) }
                        Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true).also {
                            if (it != decoded) decoded.recycle()
                        }
                    } else {
                        decoded
                    }
                } else null
            }

            if (rawBitmap == null || rawBitmap.width <= 0 || rawBitmap.height <= 0) return null

            val maxSide = maxOf(rawBitmap.width, rawBitmap.height)
            val scale = BLUR_THUMB_MAX_DIMENSION.toFloat() / maxSide
            val targetW = (rawBitmap.width * scale).roundToInt().coerceAtLeast(4)
            val targetH = (rawBitmap.height * scale).roundToInt().coerceAtLeast(4)

            val tinyBitmap = Bitmap.createScaledBitmap(rawBitmap, targetW, targetH, true)
            if (tinyBitmap != rawBitmap) {
                rawBitmap.recycle()
            }

            val blurredTiny = applyBoxBlur(tinyBitmap, radius = 2)
            if (blurredTiny != tinyBitmap) {
                tinyBitmap.recycle()
            }

            val out = ByteArrayOutputStream()
            blurredTiny.compress(Bitmap.CompressFormat.JPEG, 55, out)
            blurredTiny.recycle()

            Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
        } catch (_: Exception) {
            null
        }
    }

    private fun applyBoxBlur(src: Bitmap, radius: Int): Bitmap {
        val w = src.width
        val h = src.height
        val pixels = IntArray(w * h)
        val result = IntArray(w * h)
        src.getPixels(pixels, 0, w, 0, 0, w, h)

        repeat(2) {
            for (y in 0 until h) {
                for (x in 0 until w) {
                    var rSum = 0
                    var gSum = 0
                    var bSum = 0
                    var count = 0

                    for (dy in -radius..radius) {
                        val ny = (y + dy).coerceIn(0, h - 1)
                        for (dx in -radius..radius) {
                            val nx = (x + dx).coerceIn(0, w - 1)
                            val c = pixels[ny * w + nx]
                            rSum += (c shr 16) and 0xFF
                            gSum += (c shr 8) and 0xFF
                            bSum += c and 0xFF
                            count++
                        }
                    }

                    val r = rSum / count
                    val g = gSum / count
                    val b = bSum / count
                    result[y * w + x] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
                }
            }
            System.arraycopy(result, 0, pixels, 0, pixels.size)
        }

        val outBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        outBitmap.setPixels(pixels, 0, w, 0, 0, w, h)
        return outBitmap
    }

    private fun getUriFileSize(context: Context, uri: Uri): Long {
        return try {
            context.contentResolver.openFileDescriptor(uri, "r")?.use {
                it.statSize
            } ?: -1L
        } catch (_: Exception) {
            -1L
        }
    }

    fun clearMediaCache(context: Context) {
        try {
            val cacheFolder = File(context.cacheDir, "media_cache")
            if (cacheFolder.exists()) {
                cacheFolder.deleteRecursively()
            }
        } catch (_: Exception) {
        }
    }

    fun formatFileSize(bytes: Long): String {
        if (bytes <= 0L) return "0 B"
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        return when {
            mb >= 1.0 -> String.format(java.util.Locale.ENGLISH, "%.1f MB", mb)
            kb >= 1.0 -> String.format(java.util.Locale.ENGLISH, "%.0f KB", kb)
            else -> "$bytes B"
        }
    }
}
