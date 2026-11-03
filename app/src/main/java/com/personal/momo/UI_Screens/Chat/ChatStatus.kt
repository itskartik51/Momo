package com.personal.momo.UI_Screens.Chat

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaScannerConnection
import android.net.Uri
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
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.personal.momo.Cache.CacheManager
import com.personal.momo.UI_Screens.MomoPrimaryGradient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
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

data class StatusItem(
    val id: String = "",
    val senderId: Int = 0,
    val mediaUrl: String = "",
    val mediaType: Int = 1, // 1: Photo, 2: Video
    val timestamp: Long = 0L,
    val caption: String = "",
    val viewCount: Int = 0
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

    @Volatile
    private var isListening = false

    fun startListening(context: Context) {
        if (isListening) return
        isListening = true

        val currentUserId = CacheManager.getAppUserId(context)
        val isMomo = currentUserId.equals("Momo", ignoreCase = true)
        val myCode = if (isMomo) 2 else 1
        val partnerCode = if (isMomo) 1 else 2

        val rootRef = database.getReference(RTDB_PATH_STATUS)

        rootRef.child(myCode.toString()).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                _myStatusesFlow.value = parseAndFilterStatuses(snapshot)
            }

            override fun onCancelled(error: DatabaseError) {}
        })

        rootRef.child(partnerCode.toString()).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                _partnerStatusesFlow.value = parseAndFilterStatuses(snapshot)
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun parseAndFilterStatuses(snapshot: DataSnapshot): List<StatusItem> {
        val now = System.currentTimeMillis()
        val list = mutableListOf<StatusItem>()

        for (child in snapshot.children) {
            val id = child.child("id").getValue(String::class.java).orEmpty()
            val senderId = child.child("senderId").getValue(Int::class.java) ?: 0
            val mediaUrl = child.child("mediaUrl").getValue(String::class.java).orEmpty()
            val mediaType = child.child("mediaType").getValue(Int::class.java) ?: 1
            val timestamp = child.child("timestamp").getValue(Long::class.java) ?: 0L
            val caption = child.child("caption").getValue(String::class.java).orEmpty()
            val viewCount = child.child("viewCount").getValue(Int::class.java) ?: 0

            if (timestamp > 0L && (now - timestamp) < STATUS_EXPIRY_MILLIS && mediaUrl.isNotBlank()) {
                list.add(
                    StatusItem(
                        id = id.ifBlank { child.key.orEmpty() },
                        senderId = senderId,
                        mediaUrl = mediaUrl,
                        mediaType = mediaType,
                        timestamp = timestamp,
                        caption = caption,
                        viewCount = viewCount
                    )
                )
            } else if (timestamp > 0L && (now - timestamp) >= STATUS_EXPIRY_MILLIS) {
                child.ref.removeValue()
            }
        }

        return list.sortedBy { it.timestamp }
    }

    fun incrementViewCount(statusItem: StatusItem) {
        if (statusItem.id.isBlank() || statusItem.senderId <= 0) return
        database.getReference(RTDB_PATH_STATUS)
            .child(statusItem.senderId.toString())
            .child(statusItem.id)
            .child("viewCount")
            .setValue(ServerValue.increment(1))
    }

    fun saveStatusToUpdates(
        context: Context,
        statusItem: StatusItem,
        onResult: (Boolean) -> Unit
    ) {
        val appContext = context.applicationContext
        scope.launch {
            val success = try {
                val updatesDir = MomoStorage.getUpdatesDir(appContext)
                val ext = if (statusItem.mediaType == 2) "mp4" else "jpg"
                val cleanId = statusItem.id.ifBlank { "STATUS_${statusItem.timestamp}" }
                val destFile = File(updatesDir, "${cleanId}.$ext")

                if (destFile.exists() && destFile.length() > 0L) {
                    true
                } else {
                    val url = URL(statusItem.mediaUrl)
                    val conn = url.openConnection() as HttpURLConnection
                    conn.connectTimeout = 15000
                    conn.readTimeout = 20000
                    conn.instanceFollowRedirects = true

                    if (conn.responseCode in 200..299) {
                        val tempFile = File(updatesDir, "${cleanId}_tmp.$ext")
                        conn.inputStream.use { input ->
                            FileOutputStream(tempFile).use { output ->
                                input.copyTo(output)
                            }
                        }
                        if (tempFile.length() > 0L) {
                            tempFile.renameTo(destFile)
                            MediaScannerConnection.scanFile(
                                appContext,
                                arrayOf(destFile.absolutePath),
                                null,
                                null
                            )
                            true
                        } else {
                            tempFile.delete()
                            false
                        }
                    } else {
                        false
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }

            withContext(Dispatchers.Main) {
                onResult(success)
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
        val currentUserId = CacheManager.getAppUserId(context)
        val isMomo = currentUserId.equals("Momo", ignoreCase = true)
        val myCode = if (isMomo) 2 else 1

        onProgress(true)

        scope.launch {
            try {
                val statusId = "STATUS_${System.currentTimeMillis()}"
                val extension = if (isVideo) "mp4" else "jpg"
                val storageRef = storage.reference
                    .child("status_media")
                    .child(myCode.toString())
                    .child("$statusId.$extension")

                val uploadTask = if (isVideo) {
                    storageRef.putFile(mediaUri)
                } else {
                    val compressedBytes = compressImageUri(context, mediaUri)
                    if (compressedBytes != null) {
                        storageRef.putBytes(compressedBytes)
                    } else {
                        storageRef.putFile(mediaUri)
                    }
                }

                uploadTask.addOnSuccessListener {
                    storageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                        val statusMap = hashMapOf<String, Any>(
                            "id" to statusId,
                            "senderId" to myCode,
                            "mediaUrl" to downloadUri.toString(),
                            "mediaType" to (if (isVideo) 2 else 1),
                            "timestamp" to System.currentTimeMillis(),
                            "caption" to caption.trim(),
                            "viewCount" to 0
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
                    }.addOnFailureListener {
                        onProgress(false)
                        onComplete(false)
                    }
                }.addOnFailureListener {
                    onProgress(false)
                    onComplete(false)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    onProgress(false)
                    onComplete(false)
                }
            }
        }
    }

    private fun compressImageUri(context: Context, uri: Uri): ByteArray? {
        return try {
            val input = context.contentResolver.openInputStream(uri) ?: return null
            val original = BitmapFactory.decodeStream(input)
            input.close()
            if (original == null) return null

            val maxDimension = 1280
            val width = original.width
            val height = original.height

            val scaled = if (width > maxDimension || height > maxDimension) {
                val ratio = width.toFloat() / height.toFloat()
                val targetW = if (ratio > 1) maxDimension else (maxDimension * ratio).toInt()
                val targetH = if (ratio > 1) (maxDimension / ratio).toInt() else maxDimension
                Bitmap.createScaledBitmap(original, targetW, targetH, true)
            } else {
                original
            }

            val baos = ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.JPEG, 85, baos)
            baos.toByteArray()
        } catch (_: Exception) {
            null
        }
    }
}

@Composable
fun SegmentedStatusRing(
    statusCount: Int,
    modifier: Modifier = Modifier,
    ringGap: Dp = 3.dp,
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
                    val gapAngle = if (statusCount > 8) 3.5f else 5.5f
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
        val timeStr = dt.format(DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH)).lowercase(Locale.ENGLISH)

        when (targetDate) {
            today -> "Today, $timeStr"
            today.minusDays(1) -> "Yesterday, $timeStr"
            else -> "${dt.format(DateTimeFormatter.ofPattern("dd MMM", Locale.ENGLISH))}, $timeStr"
        }
    } catch (_: Exception) {
        ""
    }
}
