package com.personal.momo.UI_Screens.Chat

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.ExifInterface
import android.media.MediaMetadataRetriever
import android.util.Base64
import android.util.LruCache
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.personal.momo.UI_Screens.Chat.Media.MediaCompressor
import com.personal.momo.UI_Screens.Chat.Preference.PrefPlayer
import com.personal.momo.UI_Screens.bounceClick
import kotlinx.coroutines.delay
import java.io.File
import java.util.Locale

internal object MediaRenderCache {
    private val ratioCache = LruCache<String, Float>(300)
    private val blurCache = LruCache<String, Bitmap>(150)
    private val fileCache = LruCache<String, File>(300)

    fun getAspectRatio(id: String): Float? = ratioCache.get(id)
    fun putAspectRatio(id: String, ratio: Float) { ratioCache.put(id, ratio) }

    fun getBlurBitmap(key: String): Bitmap? = blurCache.get(key)
    fun putBlurBitmap(key: String, bitmap: Bitmap) { blurCache.put(key, bitmap) }

    fun getFile(path: String): File? = fileCache.get(path)
    fun putFile(path: String, file: File) { fileCache.put(path, file) }
}

private val PlayArrowIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "PlayArrow",
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

private val DownloadArrowIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "DownloadArrow",
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

@Composable
internal fun MediaBubbleMessageContent(
    message: ChatMessage,
    replyToMessage: ChatMessage? = null,
    isOutgoing: Boolean,
    isLastInGroup: Boolean,
    formattedTime: String,
    myCode: Int,
    partnerDisplayName: String,
    bubbleShape: Shape,
    isSelectionMode: Boolean,
    onToggleSelect: (String) -> Unit,
    onReplyClick: (String) -> Unit
) {
    val context = LocalContext.current
    val hasReply = replyToMessage != null
    val hasCaption = message.text.isNotBlank()

    var mediaFile by remember(message.id, message.localPath, message.status) {
        val path = message.localPath
        val resolved = if (path.isNullOrBlank()) null else {
            val cached = MediaRenderCache.getFile(path)
            if (cached != null && cached.exists() && cached.length() > 0L) {
                cached
            } else {
                val direct = MomoStorage.resolveLocalMediaFile(context, path)
                if (direct != null && direct.exists() && direct.length() > 0L) {
                    MediaRenderCache.putFile(path, direct)
                    direct
                } else null
            }
        }
        mutableStateOf(resolved)
    }

    LaunchedEffect(message.id, message.localPath, mediaFile) {
        if (mediaFile == null && !message.localPath.isNullOrBlank()) {
            for (attempt in 1..25) {
                delay(80L)
                val path = message.localPath ?: break
                val found = MomoStorage.resolveLocalMediaFile(context, path)
                if (found != null && found.exists() && found.length() > 0L) {
                    MediaRenderCache.putFile(path, found)
                    mediaFile = found
                    break
                }
            }
        }
    }

    val blurBitmap = remember(message.blurPreview) {
        val b64 = message.blurPreview
        if (b64.isNullOrBlank()) null
        else {
            MediaRenderCache.getBlurBitmap(b64) ?: run {
                val decoded = decodeBlurPreviewBitmap(b64)
                if (decoded != null) MediaRenderCache.putBlurBitmap(b64, decoded)
                decoded
            }
        }
    }

    val mediaAspectRatio = remember(message.id, mediaFile, blurBitmap) {
        val active = mediaFile
        val ratio = when {
            active != null -> calculateMediaAspectRatio(active, message.mediaType == 2)
            blurBitmap != null && blurBitmap.width > 0 && blurBitmap.height > 0 -> {
                (blurBitmap.width.toFloat() / blurBitmap.height.toFloat()).coerceIn(0.68f, 1.75f)
            }
            else -> 1f
        }
        MediaRenderCache.putAspectRatio(message.id, ratio)
        ratio
    }

    val progressMap by ChatEngine.mediaProgressFlow.collectAsState()
    val mediaProgress = progressMap[message.id]
    val targetProgress = mediaProgress?.progress?.times(100f)?.coerceIn(0f, 100f) ?: 0f
    val animatedProgress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = tween(durationMillis = 140, easing = LinearEasing),
        label = "mediaProgressAnim"
    )

    var showPrefPlayer by remember { mutableStateOf(false) }

    if (showPrefPlayer && mediaFile != null) {
        PrefPlayer(
            mediaFile = mediaFile!!,
            isVideo = message.mediaType == 2,
            onDismiss = { showPrefPlayer = false }
        )
    }

    val framePadding = 3.5.dp

    Column(
        modifier = Modifier.width(255.dp)
    ) {
        if (hasReply && replyToMessage != null) {
            Box(
                modifier = Modifier.padding(
                    start = framePadding,
                    end = framePadding,
                    top = framePadding,
                    bottom = 2.dp
                )
            ) {
                QuotedReplySnippet(
                    replyMessage = replyToMessage,
                    myCode = myCode,
                    partnerDisplayName = partnerDisplayName,
                    isOutgoing = isOutgoing,
                    onReplyClick = onReplyClick
                )
            }
        }

        val innerMediaClipShape = if (hasCaption) {
            RoundedCornerShape(
                topStart = 9.dp,
                topEnd = 9.dp,
                bottomStart = 4.dp,
                bottomEnd = 4.dp
            )
        } else {
            RoundedCornerShape(9.dp)
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = framePadding,
                    end = framePadding,
                    top = if (hasReply) 2.dp else framePadding,
                    bottom = if (hasCaption) 2.dp else framePadding
                )
                .aspectRatio(mediaAspectRatio)
                .clip(innerMediaClipShape)
                .background(Color.Black.copy(alpha = 0.28f))
                .pointerInput(message.id, isSelectionMode, mediaFile != null) {
                    detectTapGestures(
                        onTap = {
                            if (isSelectionMode) {
                                onToggleSelect(message.id)
                            } else if (mediaFile != null) {
                                showPrefPlayer = true
                            }
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            if (mediaFile != null) {
                AsyncImage(
                    model = mediaFile,
                    contentDescription = if (message.mediaType == 2) "Video Preview" else "Photo Preview",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                if (message.mediaType == 2) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.55f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = PlayArrowIcon,
                            contentDescription = "Play Video",
                            tint = Color.White,
                            modifier = Modifier
                                .size(26.dp)
                                .padding(start = 2.dp)
                        )
                    }
                }
            } else {
                if (blurBitmap != null) {
                    Image(
                        bitmap = blurBitmap.asImageBitmap(),
                        contentDescription = "Blurred Preview",
                        modifier = Modifier
                            .fillMaxSize()
                            .blur(16.dp),
                        contentScale = ContentScale.Crop
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.22f))
                    )
                }

                if (mediaProgress != null) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.55f)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(26.dp),
                            color = Color.White,
                            strokeWidth = 2.5.dp
                        )
                    }
                } else {
                    val sizeLabel = if (message.fileSize > 0L) {
                        MediaCompressor.formatFileSize(message.fileSize)
                    } else {
                        if (message.mediaType == 2) "Video" else "Photo"
                    }

                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(22.dp))
                            .background(Color.Black.copy(alpha = 0.62f))
                            .bounceClick(scaleDown = 0.94f) {
                                if (isSelectionMode) {
                                    onToggleSelect(message.id)
                                } else {
                                    ChatEngine.downloadMediaMessage(context, message)
                                }
                            }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = DownloadArrowIcon,
                            contentDescription = "Download Media",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )

                        Text(
                            text = sizeLabel,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White.copy(alpha = 0.95f),
                            style = TextStyle(
                                platformStyle = PlatformTextStyle(includeFontPadding = false)
                            )
                        )
                    }
                }
            }

            if (mediaProgress != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 6.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.Black.copy(alpha = 0.55f))
                        .padding(horizontal = 8.dp, vertical = 2.5.dp)
                ) {
                    Text(
                        text = String.format(Locale.ENGLISH, "%s : %.1f%%", mediaProgress.stage, animatedProgress),
                        fontSize = 10.5.sp,
                        color = Color.White.copy(alpha = 0.95f),
                        style = TextStyle(
                            platformStyle = PlatformTextStyle(includeFontPadding = false)
                        )
                    )
                }
            }

            if (!hasCaption && isLastInGroup) {
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 7.dp, bottom = 6.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.Black.copy(alpha = 0.55f))
                        .padding(horizontal = 6.5.dp, vertical = 2.5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        text = formattedTime,
                        fontSize = 10.5.sp,
                        color = Color.White.copy(alpha = 0.95f),
                        style = TextStyle(
                            platformStyle = PlatformTextStyle(includeFontPadding = false)
                        )
                    )

                    if (isOutgoing) {
                        BubbleStatusTicks(status = message.status)
                    }
                }
            }
        }

        if (hasCaption) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 10.dp, end = 9.dp, top = 4.dp, bottom = 6.dp)
            ) {
                EmojiText(
                    text = message.text,
                    fontSize = 15.sp,
                    color = if (isOutgoing) Color.White else MaterialTheme.colorScheme.onSurface,
                    lineHeight = 20.sp,
                    style = TextStyle(
                        platformStyle = PlatformTextStyle(includeFontPadding = false)
                    )
                )

                if (isLastInGroup) {
                    Spacer(modifier = Modifier.height(3.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.5.dp)
                        ) {
                            Text(
                                text = formattedTime,
                                fontSize = 11.sp,
                                color = if (isOutgoing)
                                    Color.White.copy(alpha = 0.80f)
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                                style = TextStyle(
                                    platformStyle = PlatformTextStyle(includeFontPadding = false)
                                )
                            )

                            if (isOutgoing) {
                                BubbleStatusTicks(status = message.status)
                            }
                        }
                    }
                }
            }
        }
    }
}

internal fun decodeBlurPreviewBitmap(base64Str: String?): Bitmap? {
    if (base64Str.isNullOrBlank()) return null
    return try {
        val bytes = Base64.decode(base64Str, Base64.NO_WRAP)
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    } catch (_: Exception) {
        null
    }
}

private fun calculateMediaAspectRatio(file: File?, isVideo: Boolean): Float {
    if (file == null || !file.exists() || file.length() == 0L) return 1f
    return try {
        if (isVideo) {
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(file.absolutePath)
                val wStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
                val hStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
                val rotStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)
                val rawW = wStr?.toIntOrNull() ?: 0
                val rawH = hStr?.toIntOrNull() ?: 0
                val rotation = rotStr?.toIntOrNull() ?: 0
                val isRotated = rotation == 90 || rotation == 270
                val actualW = if (isRotated) rawH else rawW
                val actualH = if (isRotated) rawW else rawH
                if (actualW > 0 && actualH > 0) {
                    (actualW.toFloat() / actualH.toFloat()).coerceIn(0.68f, 1.75f)
                } else 1f
            } finally {
                try {
                    retriever.release()
                } catch (_: Exception) {}
            }
        } else {
            val boundsOptions = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeFile(file.absolutePath, boundsOptions)
            val rawW = boundsOptions.outWidth
            val rawH = boundsOptions.outHeight
            if (rawW > 0 && rawH > 0) {
                var rotation = 0
                try {
                    val exif = ExifInterface(file.absolutePath)
                    val orientation = exif.getAttributeInt(
                        ExifInterface.TAG_ORIENTATION,
                        ExifInterface.ORIENTATION_NORMAL
                    )
                    rotation = when (orientation) {
                        ExifInterface.ORIENTATION_ROTATE_90,
                        ExifInterface.ORIENTATION_ROTATE_270 -> 90
                        else -> 0
                    }
                } catch (_: Exception) {}
                val isRotated = rotation == 90
                val actualW = if (isRotated) rawH else rawW
                val actualH = if (isRotated) rawW else rawH
                (actualW.toFloat() / actualH.toFloat()).coerceIn(0.68f, 1.75f)
            } else 1f
        }
    } catch (_: Exception) {
        1f
    }
}
