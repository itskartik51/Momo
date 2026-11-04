package com.personal.momo.UI_Screens.Chat

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.MutableData
import com.google.firebase.database.Transaction
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.personal.momo.Cache.CacheManager
import com.personal.momo.UI_Screens.Chat.Media.FirebaseMediaManager
import com.personal.momo.UI_Screens.Chat.Media.MediaCompressor
import com.personal.momo.UI_Screens.Chat.Media.MediaCrypto
import com.personal.momo.UI_Screens.MomoPrimaryGradient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

typealias StatusProgressMap = Map<String, Float>

data class StatusItem(
    val id: String = "",
    val senderId: Int = 0,
    val mediaUrl: String = "",
    val mediaType: Int = 1, // 1: Photo, 2: Video
    val timestamp: Long = 0L,
    val caption: String = "",
    val viewCount: Int = 0,
    val blurPreview: String? = null,
    val fileId: String? = null,
    val cryptoKey: String? = null,
    val cryptoIv: String? = null
)

object StatusEngine {
    private const val RTDB_PATH_STATUS = "momo_chat/status"
    private const val STATUS_EXPIRY_MILLIS = 24 * 60 * 60 * 1000L

    private val database by lazy { FirebaseDatabase.getInstance() }
    private val storage by lazy { FirebaseStorage.getInstance() }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _myStatusesFlow = MutableStateFlow<List<StatusItem>>(emptyList())
    val myStatusesFlow: StateFlow<List<StatusItem>> = _myStatusesFlow.asStateFlow()

    private val _partnerStatusesFlow = MutableStateFlow<List<StatusItem>>(emptyList())
    val partnerStatusesFlow: StateFlow<List<StatusItem>> = _partnerStatusesFlow.asStateFlow()

    private val _downloadProgressFlow = MutableStateFlow<StatusProgressMap>(emptyMap())
    val downloadProgressFlow: StateFlow<StatusProgressMap> = _downloadProgressFlow.asStateFlow()

    private val activeDownloadIds = mutableSetOf<String>()

    @Volatile
    private var isListening = false

    fun getCacheFile(context: Context, status: StatusItem): File {
        val baseMomoDir = MomoStorage.getMomoDir(context)
        val cacheDir = File(baseMomoDir, "Media/Updates/.cache").apply {
            if (!exists()) mkdirs()
        }
        val ext = if (status.mediaType == 2) "mp4" else "jpg"
        return File(cacheDir, "${status.id}.$ext")
    }

    fun isStatusCached(context: Context, status: StatusItem): Boolean {
        val file = getCacheFile(context, status)
        return file.exists() && file.length() > 0L
    }

    fun decodeBlur(base64Str: String?): Bitmap? {
        if (base64Str.isNullOrBlank()) return null
        return try {
            val bytes = Base64.decode(base64Str, Base64.NO_WRAP)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (_: Exception) {
            null
        }
    }

    fun startListening(context: Context) {
        if (isListening) return
        isListening = true

        val appContext = context.applicationContext
        val currentUserId = CacheManager.getAppUserId(appContext)
        val isMomo = currentUserId.equals("Momo", ignoreCase = true)
        val myCode = if (isMomo) 2 else 1
        val partnerCode = if (isMomo) 1 else 2

        val rootRef = database.getReference(RTDB_PATH_STATUS)

        rootRef.child(myCode.toString()).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                _myStatusesFlow.value = parseAndFilterStatuses(appContext, snapshot)
            }

            override fun onCancelled(error: DatabaseError) {}
        })

        rootRef.child(partnerCode.toString()).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val partnerList = parseAndFilterStatuses(appContext, snapshot)
                _partnerStatusesFlow.value = partnerList

                partnerList.forEach { status ->
                    if (!isStatusCached(appContext, status)) {
                        ensureStatusDownloaded(appContext, status)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun parseAndFilterStatuses(context: Context, snapshot: DataSnapshot): List<StatusItem> {
        val now = System.currentTimeMillis()
        val list = mutableListOf<StatusItem>()

        for (child in snapshot.children) {
            val id = child.child("id").getValue(String::class.java).orEmpty().ifBlank { child.key.orEmpty() }
            val senderId = child.child("senderId").getValue(Int::class.java) ?: 0
            val mediaUrl = child.child("mediaUrl").getValue(String::class.java).orEmpty()
            val mediaType = child.child("mediaType").getValue(Int::class.java) ?: 1
            val timestamp = child.child("timestamp").getValue(Long::class.java) ?: 0L
            val caption = child.child("caption").getValue(String::class.java).orEmpty()
            val viewCount = child.child("viewCount").getValue(Int::class.java) ?: 0
            val blurPreview = child.child("blurPreview").getValue(String::class.java)
            val fileId = child.child("fileId").getValue(String::class.java)?.takeIf { it.isNotBlank() }
            val cryptoKey = child.child("cryptoKey").getValue(String::class.java)?.takeIf { it.isNotBlank() }
            val cryptoIv = child.child("cryptoIv").getValue(String::class.java)?.takeIf { it.isNotBlank() }

            val hasValidMediaSource = !fileId.isNullOrBlank() || mediaUrl.isNotBlank()

            if (timestamp > 0L && (now - timestamp) < STATUS_EXPIRY_MILLIS && hasValidMediaSource) {
                list.add(
                    StatusItem(
                        id = id,
                        senderId = senderId,
                        mediaUrl = mediaUrl,
                        mediaType = mediaType,
                        timestamp = timestamp,
                        caption = caption,
                        viewCount = viewCount,
                        blurPreview = blurPreview,
                        fileId = fileId,
                        cryptoKey = cryptoKey,
                        cryptoIv = cryptoIv
                    )
                )
            } else if (timestamp > 0L && (now - timestamp) >= STATUS_EXPIRY_MILLIS) {
                child.ref.removeValue()

                scope.launch {
                    try {
                        if (!fileId.isNullOrBlank()) {
                            FirebaseMediaManager.deleteMediaFromFirebase(fileId)
                        }
                        if (mediaUrl.isNotBlank()) {
                            val ext = if (mediaType == 2) "mp4" else "jpg"
                            storage.reference
                                .child("status_media")
                                .child(senderId.toString())
                                .child("$id.$ext")
                                .delete()
                        }

                        val cacheFile = getCacheFile(context, StatusItem(id = id, mediaType = mediaType))
                        if (cacheFile.exists()) cacheFile.delete()
                    } catch (_: Exception) {}
                }
            }
        }

        return list.sortedBy { it.timestamp }
    }

    fun ensureStatusDownloaded(context: Context, status: StatusItem) {
        val isEncrypted = !status.fileId.isNullOrBlank() && !status.cryptoKey.isNullOrBlank() && !status.cryptoIv.isNullOrBlank()
        if (status.id.isBlank() || (!isEncrypted && status.mediaUrl.isBlank())) return

        val appContext = context.applicationContext
        val cacheFile = getCacheFile(appContext, status)
        if (cacheFile.exists() && cacheFile.length() > 0L) return

        synchronized(activeDownloadIds) {
            if (activeDownloadIds.contains(status.id)) return
            activeDownloadIds.add(status.id)
        }

        scope.launch {
            try {
                downloadAndCacheInternal(appContext, status, cacheFile)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                synchronized(activeDownloadIds) {
                    activeDownloadIds.remove(status.id)
                }
                val updatedMap = _downloadProgressFlow.value.toMutableMap()
                updatedMap.remove(status.id)
                _downloadProgressFlow.value = updatedMap
            }
        }
    }

    private suspend fun downloadAndCacheInternal(
        context: Context,
        status: StatusItem,
        cacheFile: File
    ) {
        val isEncrypted = !status.fileId.isNullOrBlank() && !status.cryptoKey.isNullOrBlank() && !status.cryptoIv.isNullOrBlank()

        if (isEncrypted) {
            val tempEncFile = File(cacheFile.parentFile, "${status.id}.enc.tmp")
            val tempDecFile = File(cacheFile.parentFile, "${cacheFile.name}.tmp")

            try {
                val downloadResult = FirebaseMediaManager.downloadEncryptedMedia(
                    fileId = status.fileId!!,
                    targetFile = tempEncFile,
                    onProgress = { prog ->
                        val updatedMap = _downloadProgressFlow.value.toMutableMap()
                        updatedMap[status.id] = prog.coerceIn(0f, 1f)
                        _downloadProgressFlow.value = updatedMap
                    }
                )

                if (downloadResult.isSuccess && tempEncFile.exists() && tempEncFile.length() > 0L) {
                    MediaCrypto.decryptFile(
                        context = context,
                        encryptedFile = tempEncFile,
                        base64Key = status.cryptoKey!!,
                        base64Iv = status.cryptoIv!!,
                        destinationFile = tempDecFile
                    )

                    if (tempDecFile.exists() && tempDecFile.length() > 0L) {
                        tempDecFile.renameTo(cacheFile)
                    }
                }
            } finally {
                if (tempEncFile.exists()) tempEncFile.delete()
                if (tempDecFile.exists()) tempDecFile.delete()
            }
        } else if (status.mediaUrl.isNotBlank()) {
            val tempFile = File(cacheFile.parentFile, "${cacheFile.name}.tmp")
            try {
                val url = URL(status.mediaUrl)
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    connectTimeout = 15000
                    readTimeout = 20000
                    instanceFollowRedirects = true
                }

                if (conn.responseCode in 200..299) {
                    val totalLength = conn.contentLength.toFloat()
                    conn.inputStream.use { input ->
                        FileOutputStream(tempFile).use { output ->
                            val buffer = ByteArray(8192)
                            var read: Int
                            var totalRead = 0L

                            while (input.read(buffer).also { read = it } != -1) {
                                output.write(buffer, 0, read)
                                totalRead += read

                                if (totalLength > 0f) {
                                    val prog = (totalRead / totalLength).coerceIn(0f, 1f)
                                    val updatedMap = _downloadProgressFlow.value.toMutableMap()
                                    updatedMap[status.id] = prog
                                    _downloadProgressFlow.value = updatedMap
                                }
                            }
                        }
                    }

                    if (tempFile.exists() && tempFile.length() > 0L) {
                        tempFile.renameTo(cacheFile)
                    }
                }
            } finally {
                if (tempFile.exists()) tempFile.delete()
            }
        }
    }

    fun incrementStatusViewCount(status: StatusItem) {
        if (status.id.isBlank() || status.senderId <= 0) return
        val viewRef = database.getReference(RTDB_PATH_STATUS)
            .child(status.senderId.toString())
            .child(status.id)
            .child("viewCount")

        viewRef.runTransaction(object : Transaction.Handler {
            override fun doTransaction(currentData: MutableData): Transaction.Result {
                val current = currentData.getValue(Int::class.java) ?: 0
                currentData.value = current + 1
                return Transaction.success(currentData)
            }

            override fun onComplete(
                error: DatabaseError?,
                committed: Boolean,
                currentData: DataSnapshot?
            ) {}
        })
    }

    fun saveStatusToDevice(
        context: Context,
        status: StatusItem,
        onResult: (Boolean) -> Unit
    ) {
        val appContext = context.applicationContext
        scope.launch {
            try {
                val cacheFile = getCacheFile(appContext, status)
                val isVideo = status.mediaType == 2

                if (!cacheFile.exists() || cacheFile.length() <= 0L) {
                    downloadAndCacheInternal(appContext, status, cacheFile)
                }

                val saved = if (cacheFile.exists() && cacheFile.length() > 0L) {
                    MomoStorage.saveStatusMediaToUpdates(
                        context = appContext,
                        sourceFile = cacheFile,
                        statusId = status.id,
                        isVideo = isVideo
                    )
                } else {
                    false
                }

                withContext(Dispatchers.Main) { onResult(saved) }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) { onResult(false) }
            }
        }
    }

    fun deleteStatus(
        context: Context,
        status: StatusItem,
        onComplete: (Boolean) -> Unit
    ) {
        val appContext = context.applicationContext
        val currentUserId = CacheManager.getAppUserId(appContext)
        val isMomo = currentUserId.equals("Momo", ignoreCase = true)
        val myCode = if (isMomo) 2 else 1

        scope.launch {
            try {
                database.getReference(RTDB_PATH_STATUS)
                    .child(myCode.toString())
                    .child(status.id)
                    .removeValue()

                if (!status.fileId.isNullOrBlank()) {
                    FirebaseMediaManager.deleteMediaFromFirebase(status.fileId)
                }

                if (status.mediaUrl.isNotBlank()) {
                    val ext = if (status.mediaType == 2) "mp4" else "jpg"
                    storage.reference
                        .child("status_media")
                        .child(myCode.toString())
                        .child("${status.id}.$ext")
                        .delete()
                }

                val cacheFile = getCacheFile(appContext, status)
                if (cacheFile.exists()) cacheFile.delete()

                withContext(Dispatchers.Main) { onComplete(true) }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) { onComplete(false) }
            }
        }
    }

    fun uploadStatus(
        context: Context,
        mediaUri: Uri,
        isVideo: Boolean,
        caption: String,
        onProgress: (Boolean) -> Unit,
        onComplete: (Boolean) -> Unit
    ) {
        val appContext = context.applicationContext
        val currentUserId = CacheManager.getAppUserId(appContext)
        val isMomo = currentUserId.equals("Momo", ignoreCase = true)
        val myCode = if (isMomo) 2 else 1

        onProgress(true)

        scope.launch {
            var compressedFile: File? = null
            var encryptedFile: File? = null

            try {
                val statusId = "STATUS_${System.currentTimeMillis()}"
                val mediaTypeInt = if (isVideo) 2 else 1

                compressedFile = if (isVideo) {
                    MediaCompressor.compressVideo(appContext, mediaUri)
                } else {
                    MediaCompressor.compressImage(appContext, mediaUri)
                }

                val blurBase64 = MediaCompressor.generateBlurThumbnail(compressedFile, isVideo)

                val cacheFile = getCacheFile(
                    appContext,
                    StatusItem(id = statusId, mediaType = mediaTypeInt)
                )
                compressedFile.copyTo(cacheFile, overwrite = true)

                val encryptionResult = MediaCrypto.encryptFile(appContext, compressedFile)
                encryptedFile = encryptionResult.encryptedFile

                if (compressedFile.exists()) {
                    compressedFile.delete()
                }

                val uploadResult = FirebaseMediaManager.uploadEncryptedMedia(
                    encryptedFile = encryptedFile,
                    fileId = statusId
                )

                if (encryptedFile.exists()) {
                    encryptedFile.delete()
                }

                if (uploadResult.isSuccess) {
                    val statusMap = hashMapOf<String, Any>(
                        "id" to statusId,
                        "senderId" to myCode,
                        "mediaUrl" to "",
                        "fileId" to statusId,
                        "cryptoKey" to encryptionResult.base64Key,
                        "cryptoIv" to encryptionResult.base64Iv,
                        "mediaType" to mediaTypeInt,
                        "timestamp" to System.currentTimeMillis(),
                        "caption" to caption.trim(),
                        "viewCount" to 0,
                        "blurPreview" to (blurBase64 ?: "")
                    )

                    database.getReference(RTDB_PATH_STATUS)
                        .child(myCode.toString())
                        .child(statusId)
                        .setValue(statusMap)
                        .addOnSuccessListener {
                            onProgress(false)
                            onComplete(true)
                        }
                        .addOnFailureListener {
                            onProgress(false)
                            onComplete(false)
                        }
                } else {
                    withContext(Dispatchers.Main) {
                        onProgress(false)
                        onComplete(false)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                try {
                    if (compressedFile?.exists() == true) compressedFile.delete()
                    if (encryptedFile?.exists() == true) encryptedFile.delete()
                } catch (_: Exception) {}

                withContext(Dispatchers.Main) {
                    onProgress(false)
                    onComplete(false)
                }
            }
        }
    }
}

@Composable
fun SegmentedStatusRing(
    statusCount: Int,
    modifier: Modifier = Modifier,
    ringGap: Dp = 2.5.dp,
    strokeWidth: Dp = 2.5.dp,
    content: @Composable () -> Unit
) {
    if (statusCount <= 0) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            content()
        }
    } else {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.matchParentSize()) {
                val strokePx = strokeWidth.toPx()
                val arcSize = Size(size.width - strokePx, size.height - strokePx)
                val topLeft = Offset(strokePx / 2f, strokePx / 2f)

                if (statusCount == 1) {
                    drawArc(
                        brush = MomoPrimaryGradient,
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokePx, cap = StrokeCap.Round)
                    )
                } else {
                    val gapAngle = if (statusCount > 8) 6f else 9f
                    val totalGap = gapAngle * statusCount
                    val sweepPerSegment = (360f - totalGap) / statusCount

                    for (i in 0 until statusCount) {
                        val startAngle = -90f + i * (sweepPerSegment + gapAngle) + (gapAngle / 2f)
                        drawArc(
                            brush = MomoPrimaryGradient,
                            startAngle = startAngle,
                            sweepAngle = sweepPerSegment,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = strokePx, cap = StrokeCap.Round)
                        )
                    }
                }
            }

            Box(
                modifier = Modifier.padding(ringGap + strokeWidth),
                contentAlignment = Alignment.Center
            ) {
                content()
            }
        }
    }
}

fun formatStatusTime(timestamp: Long): String {
    if (timestamp <= 0L) return ""
    return try {
        val dt = LocalDateTime.ofInstant(
            Instant.ofEpochMilli(timestamp),
            ZoneId.systemDefault()
        )
        val today = LocalDate.now()
        val targetDate = dt.toLocalDate()
        val timeStr = dt.format(DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH))

        when (targetDate) {
            today -> "today at $timeStr"
            today.minusDays(1) -> "yesterday at $timeStr"
            else -> "${dt.format(DateTimeFormatter.ofPattern("dd MMM", Locale.ENGLISH))} at $timeStr"
        }
    } catch (_: Exception) {
        ""
    }
}
