@file:OptIn(UnstableApi::class)

package com.personal.momo.UI_Screens.Chat.Media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.media3.common.Effect
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.Presentation
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.ProgressHolder
import androidx.media3.transformer.Transformer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
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

        var inSampleSize = 1
        val maxOriginalDimension = maxOf(rawWidth, rawHeight)
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

        val cacheFolder = File(context.cacheDir, "media_cache").apply {
            if (!exists()) mkdirs()
        }
        val outputImageFile = File(cacheFolder, "IMG_${UUID.randomUUID().toString().take(8)}.jpg")

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
                            /* audioProcessors = */ emptyList(),
                            /* videoEffects = */ videoEffects
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

            val transformer = Transformer.Builder(context)
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
