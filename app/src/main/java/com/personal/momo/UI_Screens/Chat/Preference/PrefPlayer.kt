package com.personal.momo.UI_Screens.Chat.Preference

import android.net.Uri
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.VideoView
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.personal.momo.UI_Screens.Chat.StatusEngine
import com.personal.momo.UI_Screens.Chat.StatusItem
import com.personal.momo.UI_Screens.Chat.formatStatusTime
import com.personal.momo.UI_Screens.bounceClick
import kotlinx.coroutines.delay
import java.io.File
import java.util.Locale

private val BackArrowIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "BackArrow",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).addPath(
        pathData = PathParser().parsePathString(
            "M20 11H7.83l5.59-5.59L12 4l-8 8 8 8 1.41-1.41L7.83 13H20v-2z"
        ).toNodes(),
        fill = SolidColor(Color.White)
    ).build()
}

private val PlayIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "Play",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).addPath(
        pathData = PathParser().parsePathString(
            "M8 5v14l11-7z"
        ).toNodes(),
        fill = SolidColor(Color.White)
    ).build()
}

private val PauseIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "Pause",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).addPath(
        pathData = PathParser().parsePathString(
            "M6 19h4V5H6v14zm8-14v14h4V5h-4z"
        ).toNodes(),
        fill = SolidColor(Color.White)
    ).build()
}

private val DownloadStatusIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "DownloadStatus",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).addPath(
        pathData = PathParser().parsePathString(
            "M19 9h-4V3H9v6H5l7 7 7-7zM5 18v2h14v-2H5z"
        ).toNodes(),
        fill = SolidColor(Color.White)
    ).build()
}

private val EyeStatusIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "EyeStatus",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).addPath(
        pathData = PathParser().parsePathString(
            "M12 4.5C7 4.5 2.73 7.61 1 12c1.73 4.39 6 7.5 11 7.5s9.27-3.11 11-7.5c-1.73-4.39-6-7.5-11-7.5zM12 17c-2.76 0-5-2.24-5-5s2.24-5 5-5 5 2.24 5 5-2.24 5-5 5zm0-8c-1.66 0-3 1.34-3 3s1.34 3 3 3 3-1.34 3-3-1.34-3-3-3z"
        ).toNodes(),
        fill = SolidColor(Color.White)
    ).build()
}

@Composable
fun PrefPlayer(
    mediaUri: Uri? = null,
    mediaPath: String? = null,
    mediaFile: File? = null,
    isVideo: Boolean = false,
    onDismiss: () -> Unit
) {
    BackHandler {
        onDismiss()
    }

    val resolvedUri = remember(mediaUri, mediaPath, mediaFile) {
        when {
            mediaUri != null -> mediaUri
            mediaFile != null && mediaFile.exists() -> Uri.fromFile(mediaFile)
            !mediaPath.isNullOrBlank() -> {
                if (mediaPath.startsWith("http://") || mediaPath.startsWith("https://") || mediaPath.startsWith("content://")) {
                    Uri.parse(mediaPath)
                } else {
                    val localFile = File(mediaPath)
                    if (localFile.exists()) Uri.fromFile(localFile) else Uri.parse(mediaPath)
                }
            }
            else -> null
        }
    }

    var areControlsVisible by remember { mutableStateOf(true) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            if (isVideo) {
                VideoPlayerContent(
                    uri = resolvedUri,
                    controlsVisible = areControlsVisible,
                    onToggleControls = { areControlsVisible = !areControlsVisible }
                )
            } else {
                PhotoViewerContent(
                    mediaTarget = resolvedUri ?: mediaPath ?: mediaFile,
                    onToggleControls = { areControlsVisible = !areControlsVisible }
                )
            }

            AnimatedVisibility(
                visible = areControlsVisible,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.TopStart)
            ) {
                Box(
                    modifier = Modifier
                        .statusBarsPadding()
                        .padding(top = 16.dp, start = 16.dp)
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.65f))
                        .border(1.dp, Color.White.copy(alpha = 0.20f), CircleShape)
                        .bounceClick(scaleDown = 0.94f) {
                            onDismiss()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = BackArrowIcon,
                        contentDescription = "Back",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun PrefPlayer(
    statuses: List<StatusItem>,
    isOwner: Boolean,
    displayName: String,
    avatarUrl: String?,
    initialIndex: Int = 0,
    onDismiss: () -> Unit
) {
    if (statuses.isEmpty()) {
        onDismiss()
        return
    }

    val context = LocalContext.current
    var currentIndex by remember { mutableIntStateOf(initialIndex.coerceIn(0, statuses.lastIndex)) }
    val safeIndex = currentIndex.coerceIn(0, statuses.lastIndex)
    val currentStatus = statuses.getOrNull(safeIndex) ?: return

    var progress by remember(safeIndex) { mutableFloatStateOf(0f) }
    var isPaused by remember { mutableStateOf(false) }
    var isVideoBuffering by remember(safeIndex) { mutableStateOf(currentStatus.mediaType == 2) }

    var isSaving by remember( currentStatus.id ) { mutableStateOf(false) }
    var isSaved by remember( currentStatus.id ) { mutableStateOf(false) }
    var saveFeedbackText by remember { mutableStateOf<String?>(null) }

    val isVideo = currentStatus.mediaType == 2
    val photoDurationMs = 10_000L

    BackHandler {
        onDismiss()
    }

    LaunchedEffect(currentStatus.id, isOwner) {
        if (!isOwner) {
            StatusEngine.incrementViewCount(currentStatus)
        }
    }

    LaunchedEffect(saveFeedbackText) {
        if (saveFeedbackText != null) {
            delay(1800L)
            saveFeedbackText = null
        }
    }

    LaunchedEffect(safeIndex, isPaused, isVideo) {
        if (!isVideo) {
            val startTime = System.currentTimeMillis() - (progress * photoDurationMs).toLong()
            while (!isPaused && progress < 1f) {
                val elapsed = System.currentTimeMillis() - startTime
                progress = (elapsed.toFloat() / photoDurationMs.toFloat()).coerceIn(0f, 1f)
                delay(16L)
            }
            if (!isPaused && progress >= 1f) {
                if (safeIndex < statuses.lastIndex) {
                    currentIndex = safeIndex + 1
                } else {
                    onDismiss()
                }
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .pointerInput(safeIndex, statuses.size) {
                    detectTapGestures(
                        onPress = {
                            isPaused = true
                            tryAwaitRelease()
                            isPaused = false
                        },
                        onTap = { offset ->
                            val width = size.width
                            if (offset.x < width * 0.30f) {
                                if (safeIndex > 0) {
                                    currentIndex = safeIndex - 1
                                } else {
                                    progress = 0f
                                }
                            } else {
                                if (safeIndex < statuses.lastIndex) {
                                    currentIndex = safeIndex + 1
                                } else {
                                    onDismiss()
                                }
                            }
                        }
                    )
                }
        ) {
            if (isVideo) {
                var videoViewRef by remember(currentStatus.id) { mutableStateOf<VideoView?>(null) }
                var videoDurationMs by remember(currentStatus.id) { mutableLongStateOf(0L) }

                DisposableEffect(currentStatus.id) {
                    onDispose {
                        videoViewRef?.stopPlayback()
                        videoViewRef = null
                    }
                }

                LaunchedEffect(currentStatus.id, isPaused, isVideoBuffering) {
                    while (!isPaused && !isVideoBuffering) {
                        val vv = videoViewRef
                        if (vv != null && vv.isPlaying && videoDurationMs > 0L) {
                            progress = (vv.currentPosition.toFloat() / videoDurationMs.toFloat()).coerceIn(0f, 1f)
                        }
                        delay(30L)
                    }
                }

                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        VideoView(ctx).apply {
                            layoutParams = FrameLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            setVideoURI(Uri.parse(currentStatus.mediaUrl))
                            setOnPreparedListener { mp ->
                                isVideoBuffering = false
                                videoDurationMs = mp.duration.toLong().coerceAtLeast(1L)
                                mp.isLooping = false
                                if (!isPaused) start()
                            }
                            setOnCompletionListener {
                                progress = 1f
                                if (safeIndex < statuses.lastIndex) {
                                    currentIndex = safeIndex + 1
                                } else {
                                    onDismiss()
                                }
                            }
                            setOnErrorListener { _, _, _ ->
                                isVideoBuffering = false
                                true
                            }
                            videoViewRef = this
                        }
                    },
                    update = { view ->
                        videoViewRef = view
                        if (isPaused) {
                            if (view.isPlaying) view.pause()
                        } else if (!isVideoBuffering && !view.isPlaying) {
                            view.start()
                        }
                    }
                )

                if (isVideoBuffering) {
                    CircularProgressIndicator(
                        color = Color.White,
                        strokeWidth = 3.dp,
                        modifier = Modifier
                            .size(42.dp)
                            .align(Alignment.Center)
                    )
                }
            } else {
                AsyncImage(
                    model = currentStatus.mediaUrl,
                    contentDescription = "Status Media",
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
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    for (i in statuses.indices) {
                        val segmentProgress = when {
                            i < safeIndex -> 1f
                            i == safeIndex -> progress
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
                            .size(36.dp)
                            .clip(CircleShape)
                            .bounceClick(scaleDown = 0.94f) {
                                onDismiss()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = BackArrowIcon,
                            contentDescription = "Back",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                            .border(1.dp, Color.White.copy(alpha = 0.25f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!avatarUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = avatarUrl,
                                contentDescription = "Uploader Avatar",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.75f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = displayName,
                            fontSize = 15.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = TextStyle(
                                platformStyle = PlatformTextStyle(includeFontPadding = false),
                                lineHeight = 18.sp
                            )
                        )

                        Spacer(modifier = Modifier.height(1.5.dp))

                        Text(
                            text = formatStatusTime(currentStatus.timestamp),
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.78f),
                            maxLines = 1,
                            style = TextStyle(
                                platformStyle = PlatformTextStyle(includeFontPadding = false),
                                lineHeight = 14.sp
                            )
                        )
                    }

                    if (isOwner && currentStatus.viewCount > 0) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.Black.copy(alpha = 0.50f))
                                .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(16.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = EyeStatusIcon,
                                contentDescription = "Views",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )

                            Spacer(modifier = Modifier.width(5.dp))

                            Text(
                                text = "${currentStatus.viewCount}",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.50f))
                            .border(1.dp, Color.White.copy(alpha = 0.20f), CircleShape)
                            .bounceClick(scaleDown = 0.94f) {
                                if (!isSaving && !isSaved) {
                                    isSaving = true
                                    StatusEngine.saveStatusToUpdates(context, currentStatus) { success ->
                                        isSaving = false
                                        if (success) {
                                            isSaved = true
                                            saveFeedbackText = "Saved to Updates"
                                        } else {
                                            saveFeedbackText = "Failed to save"
                                        }
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        when {
                            isSaving -> {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            isSaved -> {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Saved",
                                    tint = Color(0xFF25D366),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            else -> {
                                Icon(
                                    imageVector = DownloadStatusIcon,
                                    contentDescription = "Save to Updates",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
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
                        .padding(horizontal = 20.dp, vertical = 14.dp),
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

            AnimatedVisibility(
                visible = saveFeedbackText != null,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .
