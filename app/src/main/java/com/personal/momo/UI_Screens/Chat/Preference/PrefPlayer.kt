package com.personal.momo.UI_Screens.Chat.Preference

import android.net.Uri
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.VideoView
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
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
import com.personal.momo.UI_Screens.MomoPrimaryGradient
import com.personal.momo.UI_Screens.bounceClick
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale
import kotlin.math.abs

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

private val MoreVertIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "MoreVert",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).addPath(
        pathData = PathParser().parsePathString(
            "M12 8c1.1 0 2-.9 2-2s-.9-2-2-2-2 .9-2 2 .9 2 2 2zm0 2c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2zm0 6c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2z"
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

private val StatusSendIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "StatusSend",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).addPath(
        pathData = PathParser().parsePathString(
            "M2.01 21L23 12 2.01 3 2 10l15 2-15 2z"
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
    displayName: String,
    avatarUrl: String?,
    isOwnStatus: Boolean,
    initialIndex: Int = 0,
    onReplySend: ((status: StatusItem, replyText: String) -> Unit)? = null,
    onDismiss: () -> Unit
) {
    if (statuses.isEmpty()) {
        onDismiss()
        return
    }

    val context = LocalContext.current
    val density = LocalDensity.current
    val focusManager = LocalFocusManager.current
    val coroutineScope = rememberCoroutineScope()
    val configuration = LocalConfiguration.current

    var currentIndex by remember { mutableIntStateOf(initialIndex.coerceIn(0, statuses.lastIndex)) }
    val currentStatus = statuses.getOrNull(currentIndex) ?: statuses.last()

    var progress by remember(currentIndex) { mutableFloatStateOf(0f) }
    var isUserPaused by remember { mutableStateOf(false) }
    var isMenuOpen by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    var isImmersiveCleanMode by remember { mutableStateOf(false) }
    var isCaptionExpanded by remember(currentStatus.id) { mutableStateOf(false) }
    var isCaptionOverflowing by remember(currentStatus.id) { mutableStateOf(false) }

    var replyInputText by remember { mutableStateOf("") }
    var isReplyFocused by remember { mutableStateOf(false) }
    var showReplySentToast by remember { mutableStateOf(false) }

    val imeBottom = WindowInsets.ime.getBottom(density)
    val navBarsBottom = WindowInsets.navigationBars.getBottom(density)
    val bottomInsetDp = with(density) { maxOf(imeBottom, navBarsBottom).toDp() }
    val isKeyboardOpen = imeBottom > 0

    LaunchedEffect(isKeyboardOpen) {
        if (!isKeyboardOpen && isReplyFocused) {
            focusManager.clearFocus()
            isReplyFocused = false
        }
    }

    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    val animatedDragOffsetY by animateFloatAsState(
        targetValue = dragOffsetY,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "dragOffsetYAnim"
    )

    var isVideoBuffering by remember(currentIndex) { mutableStateOf(currentStatus.mediaType == 2) }
    var isSavedLocally by remember(currentStatus.id) { mutableStateOf(false) }

    val downloadProgressMap by StatusEngine.downloadProgressFlow.collectAsState()
    val activeDownloadProgress = downloadProgressMap[currentStatus.id]
    val isMediaDownloaded = StatusEngine.isStatusCached(context, currentStatus)

    val cachedLocalFile = remember(currentStatus.id, isMediaDownloaded) {
        if (isMediaDownloaded) StatusEngine.getCacheFile(context, currentStatus) else null
    }

    val blurBitmap = remember(currentStatus.blurPreview) {
        StatusEngine.decodeBlur(currentStatus.blurPreview)
    }

    val countedStatusIds = remember { mutableSetOf<String>() }

    BackHandler {
        when {
            showDeleteConfirmDialog -> showDeleteConfirmDialog = false
            isMenuOpen -> isMenuOpen = false
            isKeyboardOpen || isReplyFocused || replyInputText.isNotEmpty() -> {
                focusManager.clearFocus()
                replyInputText = ""
                isReplyFocused = false
            }
            isCaptionExpanded -> isCaptionExpanded = false
            isImmersiveCleanMode -> isImmersiveCleanMode = false
            else -> onDismiss()
        }
    }

    LaunchedEffect(currentStatus.id, isOwnStatus, isMediaDownloaded) {
        if (!isOwnStatus && currentStatus.id.isNotBlank() && countedStatusIds.add(currentStatus.id)) {
            StatusEngine.incrementStatusViewCount(currentStatus)
        }
        if (!isMediaDownloaded) {
            StatusEngine.ensureStatusDownloaded(context, currentStatus)
        }
    }

    LaunchedEffect(showReplySentToast) {
        if (showReplySentToast) {
            delay(1500L)
            showReplySentToast = false
        }
    }

    val isReplying = isKeyboardOpen || isReplyFocused
    val isPausedEffective = isUserPaused || isMenuOpen || showDeleteConfirmDialog || !isMediaDownloaded || isImmersiveCleanMode || isCaptionExpanded || isReplying
    val isVideo = currentStatus.mediaType == 2
    val photoDurationMs = 10_000L

    LaunchedEffect(currentIndex, isPausedEffective, isVideo, isMediaDownloaded) {
        if (!isVideo && isMediaDownloaded) {
            val startTime = System.currentTimeMillis() - (progress * photoDurationMs).toLong()
            while (!isPausedEffective && progress < 1f) {
                val elapsed = System.currentTimeMillis() - startTime
                progress = (elapsed.toFloat() / photoDurationMs.toFloat()).coerceIn(0f, 1f)
                delay(16L)
            }
            if (!isPausedEffective && progress >= 1f) {
                if (currentIndex < statuses.lastIndex) {
                    currentIndex++
                } else {
                    onDismiss()
                }
            }
        }
    }

    val backgroundAlpha = remember(animatedDragOffsetY) {
        (1f - (animatedDragOffsetY / 650f)).coerceIn(0.4f, 1f)
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
                .background(Color.Black.copy(alpha = backgroundAlpha))
                .graphicsLayer {
                    translationY = animatedDragOffsetY
                }
                .pointerInput(currentIndex, isImmersiveCleanMode, isCaptionExpanded, isKeyboardOpen, isReplyFocused) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        var isDrag = false
                        var totalDragY = 0f
                        var isHold = false

                        val holdJob = coroutineScope.launch {
                            delay(200L)
                            if (!isDrag && !isImmersiveCleanMode && !isCaptionExpanded && !isKeyboardOpen && !isReplyFocused) {
                                isUserPaused = true
                                isHold = true
                            }
                        }

                        try {
                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                if (!change.pressed) break

                                val deltaY = change.position.y - down.position.y
                                val deltaX = change.position.x - down.position.x

                                if (abs(deltaY) > 20f && abs(deltaY) > abs(deltaX)) {
                                    isDrag = true
                                    holdJob.cancel()
                                    if (isHold) {
                                        isUserPaused = false
                                        isHold = false
                                    }

                                    if (deltaY > 0f && !isCaptionExpanded && !isKeyboardOpen && !isReplyFocused) {
                                        totalDragY = deltaY
                                        dragOffsetY = deltaY
                                        change.consume()
                                    } else if (deltaY < -35f && !isImmersiveCleanMode && !isCaptionExpanded && !isKeyboardOpen && !isReplyFocused) {
                                        isImmersiveCleanMode = true
                                        change.consume()
                                    }
                                }
                            }
                        } finally {
                            holdJob.cancel()
                            if (isHold) {
                                isUserPaused = false
                            }
                        }

                        if (isDrag) {
                            if (totalDragY > 130.dp.toPx()) {
                                onDismiss()
                            } else {
                                dragOffsetY = 0f
                            }
                        } else {
                            if (!down.isConsumed) {
                                if (isKeyboardOpen || isReplyFocused) {
                                    focusManager.clearFocus()
                                    isReplyFocused = false
                                } else if (isImmersiveCleanMode) {
                                    isImmersiveCleanMode = false
                                } else if (isCaptionExpanded) {
                                    isCaptionExpanded = false
                                } else {
                                    val screenWidth = size.width
                                    val tapX = down.position.x
                                    if (tapX < screenWidth * 0.32f) {
                                        if (currentIndex > 0) currentIndex-- else progress = 0f
                                    } else {
                                        if (currentIndex < statuses.lastIndex) currentIndex++ else onDismiss()
                                    }
                                }
                            }
                        }
                    }
                }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .height(configuration.screenHeightDp.dp),
                contentAlignment = Alignment.Center
            ) {
                if (cachedLocalFile != null && cachedLocalFile.exists() && cachedLocalFile.length() > 0L) {
                    if (isVideo) {
                        var videoViewRef by remember(currentStatus.id) { mutableStateOf<VideoView?>(null) }
                        var videoDurationMs by remember(currentStatus.id) { mutableLongStateOf(0L) }

                        DisposableEffect(currentStatus.id) {
                            onDispose {
                                videoViewRef?.stopPlayback()
                                videoViewRef = null
                            }
                        }

                        LaunchedEffect(currentStatus.id, isPausedEffective, isVideoBuffering, videoDurationMs) {
                            while (!isPausedEffective && !isVideoBuffering && videoDurationMs > 0L) {
                                val vv = videoViewRef
                                if (vv != null && vv.isPlaying) {
                                    val pos = vv.currentPosition.toLong().coerceAtLeast(0L)
                                    progress = (pos.toFloat() / videoDurationMs.toFloat()).coerceIn(0f, 1f)
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
                                    setVideoPath(cachedLocalFile.absolutePath)
                                    setOnPreparedListener { mp ->
                                        isVideoBuffering = false
                                        videoDurationMs = mp.duration.toLong().coerceAtLeast(1L)
                                        mp.isLooping = false
                                        if (!isPausedEffective) start()
                                    }
                                    setOnCompletionListener {
                                        progress = 1f
                                        if (currentIndex < statuses.lastIndex) {
                                            currentIndex++
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
                                if (isPausedEffective) {
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
                                modifier = Modifier.size(42.dp)
                            )
                        }
                    } else {
                        AsyncImage(
                            model = cachedLocalFile,
                            contentDescription = "Status Photo",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    }
                } else {
                    if (blurBitmap != null) {
                        Image(
                            bitmap = blurBitmap.asImageBitmap(),
                            contentDescription = "Blurred Preview",
                            modifier = Modifier
                                .fillMaxSize()
                                .blur(20.dp),
                            contentScale = ContentScale.Crop
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.35f)),
                        contentAlignment = Alignment.Center
                    ) {
                        val progressPercent = ((activeDownloadProgress ?: 0f) * 100f).toInt().coerceIn(0, 100)
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color.Black.copy(alpha = 0.65f))
                                .border(1.dp, Color.White.copy(alpha = 0.20f), RoundedCornerShape(20.dp))
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                progress = { (activeDownloadProgress ?: 0f).coerceIn(0f, 1f) },
                                modifier = Modifier.size(16.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Download : $progressPercent%",
                                fontSize = 12.5.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = !isImmersiveCleanMode,
                enter = fadeIn(animationSpec = tween(220)),
                exit = fadeOut(animationSpec = tween(220)),
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.45f))
                        .statusBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        for (i in statuses.indices) {
                            val segmentFill = when {
                                i < currentIndex -> 1f
                                i == currentIndex -> progress
                                else -> 0f
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(2.5.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(Color.White.copy(alpha = 0.30f))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth(segmentFill.coerceIn(0f, 1f))
                                        .background(Color.White)
                                )
                            }
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

                        Spacer(modifier = Modifier.width(4.dp))

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
                                fontWeight = FontWeight.Normal,
                                color = Color.White.copy(alpha = 0.78f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = TextStyle(
                                    platformStyle = PlatformTextStyle(includeFontPadding = false),
                                    lineHeight = 14.sp
                                )
                            )
                        }

                        if (isOwnStatus && currentStatus.viewCount > 0) {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color.White.copy(alpha = 0.15f))
                                    .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(14.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = EyeStatusIcon,
                                    contentDescription = "Views",
                                    tint = Color.White,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = currentStatus.viewCount.toString(),
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                            }

                            Spacer(modifier = Modifier.width(6.dp))
                        }

                        Box {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .bounceClick(scaleDown = 0.94f) {
                                        isMenuOpen = true
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = MoreVertIcon,
                                    contentDescription = "Options",
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            DropdownMenu(
                                expanded = isMenuOpen,
                                onDismissRequest = { isMenuOpen = false },
                                modifier = Modifier.widthIn(min = 105.dp, max = 135.dp),
                                shape = RoundedCornerShape(14.dp),
                                containerColor = Color(0xFF222328),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
                            ) {
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = if (isSavedLocally) "Saved" else "Save",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = if (isSavedLocally) Color(0xFF25D366) else Color.White
                                        )
                                    },
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
                                    modifier = Modifier.height(38.dp),
                                    onClick = {
                                        isMenuOpen = false
                                        if (!isSavedLocally) {
                                            StatusEngine.saveStatusToDevice(context, currentStatus) { success ->
                                                if (success) isSavedLocally = true
                                            }
                                        }
                                    }
                                )

                                if (isOwnStatus) {
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = "Delete",
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = Color(0xFFFF4D4D)
                                            )
                                        },
                                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
                                        modifier = Modifier.height(38.dp),
                                        onClick = {
                                            isMenuOpen = false
                                            showDeleteConfirmDialog = true
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(bottom = bottomInsetDp)
            ) {
                if (currentStatus.caption.isNotBlank()) {
                    AnimatedVisibility(
                        visible = !isImmersiveCleanMode,
                        enter = fadeIn(animationSpec = tween(220)),
                        exit = fadeOut(animationSpec = tween(220)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.Black.copy(alpha = 0.70f))
                                .padding(horizontal = 20.dp, vertical = 12.dp)
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                val scrollState = rememberScrollState()
                                val textModifier = if (isCaptionExpanded) {
                                    Modifier
                                        .fillMaxWidth()
                                        .heightIn(max = 240.dp)
                                        .verticalScroll(scrollState)
                                } else {
                                    Modifier.fillMaxWidth()
                                }

                                Text(
                                    text = currentStatus.caption,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.White,
                                    lineHeight = 21.sp,
                                    maxLines = if (isCaptionExpanded) Int.MAX_VALUE else 3,
                                    overflow = TextOverflow.Ellipsis,
                                    onTextLayout = { textLayoutResult ->
                                        if (!isCaptionExpanded) {
                                            isCaptionOverflowing = textLayoutResult.hasVisualOverflow || textLayoutResult.lineCount > 3
                                        }
                                    },
                                    modifier = textModifier
                                )

                                if (isCaptionOverflowing || isCaptionExpanded) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = if (isCaptionExpanded) "Show less" else "Read more",
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF34B7F1),
                                        modifier = Modifier
                                            .bounceClick(scaleDown = 0.94f) {
                                                isCaptionExpanded = !isCaptionExpanded
                                            }
                                            .padding(vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                if (!isOwnStatus && !isImmersiveCleanMode) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.Black.copy(alpha = 0.55f))
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 44.dp, max = 100.dp)
                                .clip(RoundedCornerShape(22.dp))
                                .background(Color(0xFF171922).copy(alpha = 0.90f))
                                .border(1.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(22.dp))
                                .padding(start = 16.dp, end = 6.dp, top = 4.dp, bottom = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(vertical = 4.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                if (replyInputText.isEmpty()) {
                                    Text(
                                        text = "Reply...",
                                        fontSize = 15.sp,
                                        color = Color.White.copy(alpha = 0.50f)
                                    )
                                }

                                BasicTextField(
                                    value = replyInputText,
                                    onValueChange = { replyInputText = it },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .onFocusChanged { isReplyFocused = it.isFocused },
                                    textStyle = TextStyle(
                                        fontSize = 15.sp,
                                        color = Color.White
                                    ),
                                    keyboardOptions = KeyboardOptions(
                                        capitalization = KeyboardCapitalization.Sentences,
                                        autoCorrect = true,
                                        imeAction = ImeAction.Default
                                    ),
                                    cursorBrush = SolidColor(Color.White)
                                )
                            }

                            if (replyInputText.trim().isNotEmpty()) {
                                Spacer(modifier = Modifier.width(6.dp))

                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(MomoPrimaryGradient)
                                        .bounceClick(scaleDown = 0.94f) {
                                            val textToSend = replyInputText.trim()
                                            if (textToSend.isNotEmpty()) {
                                                onReplySend?.invoke(currentStatus, textToSend)
                                                replyInputText = ""
                                                focusManager.clearFocus()
                                                isReplyFocused = false
                                                showReplySentToast = true
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = StatusSendIcon,
                                        contentDescription = "Send Reply",
                                        tint = Color.White,
                                        modifier = Modifier
                                            .size(16.dp)
                                            .padding(start = 1.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = showReplySentToast,
                enter = fadeIn(animationSpec = tween(180)),
                exit = fadeOut(animationSpec = tween(180)),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = bottomInsetDp + 18.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF222328).copy(alpha = 0.92f))
                        .border(1.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 18.dp, vertical = 9.dp)
                ) {
                    Text(
                        text = "Reply sent",
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White
                    )
                }
            }

            if (showDeleteConfirmDialog) {
                Dialog(
                    onDismissRequest = { showDeleteConfirmDialog = false },
                    properties = DialogProperties(usePlatformDefaultWidth = false)
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 26.dp),
                        shape = RoundedCornerShape(24.dp),
                        color = Color(0xFF222328),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.10f))
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 22.dp, vertical = 22.dp)
                        ) {
                            Text(
                                text = "Delete update?",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "This status update will be deleted for everyone.",
                                fontSize = 13.5.sp,
                                color = Color.White.copy(alpha = 0.70f),
                                lineHeight = 18.5.sp
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .bounceClick(scaleDown = 0.94f) {
                                            showDeleteConfirmDialog = false
                                        }
                                        .padding(horizontal = 14.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        text = "Cancel",
                                        fontSize = 14.5.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color.White.copy(alpha = 0.85f)
                                    )
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(MomoPrimaryGradient)
                                        .bounceClick(scaleDown = 0.94f) {
                                            showDeleteConfirmDialog = false
                                            StatusEngine.deleteStatus(context, currentStatus) {
                                                if (statuses.size <= 1) {
                                                    onDismiss()
                                                } else {
                                                    if (currentIndex >= statuses.lastIndex) {
                                                        currentIndex = (currentIndex - 1).coerceAtLeast(0)
                                                    }
                                                }
                                            }
                                        }
                                        .padding(horizontal = 22.dp, vertical = 10.dp)
                                ) {
                                    Text(
                                        text = "Delete",
                                        fontSize = 14.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PhotoViewerContent(
    mediaTarget: Any?,
    onToggleControls: () -> Unit
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = {
                        if (scale > 1.05f) {
                            scale = 1f
                            offset = Offset.Zero
                        } else {
                            scale = 2.5f
                            offset = Offset.Zero
                        }
                    },
                    onTap = {
                        onToggleControls()
                    }
                )
            }
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    val newScale = (scale * zoom).coerceIn(1f, 5f)
                    scale = newScale
                    if (newScale > 1f) {
                        offset += pan
                    } else {
                        offset = Offset.Zero
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = mediaTarget,
            contentDescription = "Photo Preview",
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offset.x
                    translationY = offset.y
                },
            contentScale = ContentScale.Fit
        )
    }
}

@Composable
private fun VideoPlayerContent(
    uri: Uri?,
    controlsVisible: Boolean,
    onToggleControls: () -> Unit
) {
    var videoViewRef by remember { mutableStateOf<VideoView?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var isBuffering by remember { mutableStateOf(true) }
    var currentPositionMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(0L) }
    var isDraggingSlider by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose {
            videoViewRef?.stopPlayback()
            videoViewRef = null
        }
    }

    LaunchedEffect(isPlaying, isDraggingSlider) {
        while (isPlaying && !isDraggingSlider) {
            videoViewRef?.let { vv ->
                if (vv.isPlaying) {
                    currentPositionMs = vv.currentPosition.toLong().coerceAtLeast(0L)
                }
            }
            delay(200L)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onToggleControls() }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        if (uri != null) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    VideoView(context).apply {
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        setVideoURI(uri)
                        setOnPreparedListener { mp ->
                            isBuffering = false
                            durationMs = mp.duration.toLong().coerceAtLeast(0L)
                            start()
                            isPlaying = true
                        }
                        setOnCompletionListener {
                            isPlaying = false
                            currentPositionMs = durationMs
                        }
                        setOnErrorListener { _, _, _ ->
                            isBuffering = false
                            true
                        }
                        videoViewRef = this
                    }
                },
                update = { view ->
                    videoViewRef = view
                }
            )
        }

        if (isBuffering) {
            CircularProgressIndicator(
                color = Color.White,
                strokeWidth = 3.dp,
                modifier = Modifier.size(44.dp)
            )
        }

        AnimatedVisibility(
            visible = controlsVisible && !isBuffering,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.55f))
                    .border(1.dp, Color.White.copy(alpha = 0.25f), CircleShape)
                    .bounceClick(scaleDown = 0.94f) {
                        val vv = videoViewRef ?: return@bounceClick
                        if (isPlaying) {
                            vv.pause()
                            isPlaying = false
                        } else {
                            if (currentPositionMs >= durationMs && durationMs > 0L) {
                                vv.seekTo(0)
                                currentPositionMs = 0L
                            }
                            vv.start()
                            isPlaying = true
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isPlaying) PauseIcon else PlayIcon,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
        }

        AnimatedVisibility(
            visible = controlsVisible && !isBuffering,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.65f))
                    .navigationBarsPadding()
                    .padding(horizontal = 18.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatDuration(currentPositionMs),
                        fontSize = 12.5.sp,
                        color = Color.White.copy(alpha = 0.85f)
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    Slider(
                        value = if (durationMs > 0L) (currentPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f,
                        onValueChange = { fraction ->
                            isDraggingSlider = true
                            currentPositionMs = (fraction * durationMs).toLong()
                        },
                        onValueChangeFinished = {
                            videoViewRef?.seekTo(currentPositionMs.toInt())
                            isDraggingSlider = false
                        },
                        colors = SliderDefaults.colors(
                            thumbColor = Color.White,
                            activeTrackColor = Color(0xFFFF2E63),
                            inactiveTrackColor = Color.White.copy(alpha = 0.25f)
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    Text(
                        text = formatDuration(durationMs),
                        fontSize = 12.5.sp,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }
            }
        }
    }
}

private fun formatDuration(millis: Long): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.ENGLISH, "%02d:%02d", minutes, seconds)
}
