package com.personal.momo.UI_Screens.Chat

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.VideoView
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.personal.momo.Cache.CacheManager
import com.personal.momo.UI_Screens.MomoPrimaryGradient
import com.personal.momo.UI_Screens.bounceClick
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
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
    val caption: String = ""
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

            if (timestamp > 0L && (now - timestamp) < STATUS_EXPIRY_MILLIS && mediaUrl.isNotBlank()) {
                list.add(
                    StatusItem(
                        id = id.ifBlank { child.key.orEmpty() },
                        senderId = senderId,
                        mediaUrl = mediaUrl,
                        mediaType = mediaType,
                        timestamp = timestamp,
                        caption = caption
                    )
                )
            } else if (timestamp > 0L && (now - timestamp) >= STATUS_EXPIRY_MILLIS) {
                child.ref.removeValue()
            }
        }

        return list.sortedBy { it.timestamp }
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
                            "caption" to caption.trim()
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
            input.close() ?: return null
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

@Composable
fun StatusViewerDialog(
    statuses: List<StatusItem>,
    displayName: String,
    avatarUrl: String?,
    initialIndex: Int = 0,
    onDismiss: () -> Unit
) {
    if (statuses.isEmpty()) {
        onDismiss()
        return
    }

    var currentIndex by remember { mutableIntStateOf(initialIndex.coerceIn(0, statuses.lastIndex)) }
    val currentStatus = statuses.getOrNull(currentIndex) ?: return

    var progress by remember { mutableFloatStateOf(0f) }
    var isPaused by remember { mutableStateOf(false) }

    val isVideo = currentStatus.mediaType == 2
    val displayDurationMs = if (isVideo) 15000L else 5000L

    BackHandler {
        onDismiss()
    }

    LaunchedEffect(currentIndex, isPaused) {
        val startTime = System.currentTimeMillis() - (progress * displayDurationMs).toLong()
        while (!isPaused && progress < 1f) {
            val elapsed = System.currentTimeMillis() - startTime
            progress = (elapsed.toFloat() / displayDurationMs.toFloat()).coerceIn(0f, 1f)
            delay(16L)
        }

        if (!isPaused && progress >= 1f) {
            if (currentIndex < statuses.lastIndex) {
                progress = 0f
                currentIndex++
            } else {
                onDismiss()
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .pointerInput(currentIndex) {
                    detectTapGestures(
                        onPress = {
                            isPaused = true
                            tryAwaitRelease()
                            isPaused = false
                        },
                        onTap = { offset ->
                            val screenWidth = size.width
                            if (offset.x < screenWidth * 0.32f) {
                                if (currentIndex > 0) {
                                    progress = 0f
                                    currentIndex--
                                } else {
                                    progress = 0f
                                }
                            } else {
                                if (currentIndex < statuses.lastIndex) {
                                    progress = 0f
                                    currentIndex++
                                } else {
                                    onDismiss()
                                }
                            }
                        }
                    )
                }
        ) {
            if (isVideo) {
                var videoViewRef by remember { mutableStateOf<VideoView?>(null) }

                DisposableEffect(currentStatus.mediaUrl) {
                    onDispose {
                        videoViewRef?.stopPlayback()
                        videoViewRef = null
                    }
                }

                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { context ->
                        VideoView(context).apply {
                            layoutParams = FrameLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            setVideoURI(Uri.parse(currentStatus.mediaUrl))
                            setOnPreparedListener { mp ->
                                mp.isLooping = true
                                start()
                            }
                            videoViewRef = this
                        }
                    },
                    update = { view ->
                        if (isPaused) {
                            view.pause()
                        } else {
                            if (!view.isPlaying) view.start()
                        }
                    }
                )
            } else {
                AsyncImage(
                    model = currentStatus.mediaUrl,
                    contentDescription = "Status Image",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .background(Color.Black.copy(alpha = 0.45f))
                    .statusBarsPadding()
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    for (i in statuses.indices) {
                        val segmentProgress = when {
                            i < currentIndex -> 1f
                            i == currentIndex -> progress
                            else -> 0f
                        }

                        LinearProgressIndicator(
                            progress = { segmentProgress },
                            modifier = Modifier
                                .weight(1f)
                                .height(2.5.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            color = Color.White,
                            trackColor = Color.White.copy(alpha = 0.30f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!avatarUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = avatarUrl,
                                contentDescription = "Status Author",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.70f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = displayName,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Text(
                            text = formatStatusTime(currentStatus.timestamp),
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.75f)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.35f))
                            .bounceClick(scaleDown = 0.94f) {
                                onDismiss()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            if (currentStatus.caption.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .background(Color.Black.copy(alpha = 0.65f))
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = currentStatus.caption,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White,
                        lineHeight = 20.sp
                    )
                }
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
