package com.personal.momo.UI_Screens.Chat

import android.content.Intent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.personal.momo.UI_Screens.MomoPrimaryGradient
import com.personal.momo.UI_Screens.bounceClick
import kotlinx.coroutines.launch
import java.io.File
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val AquaMintColor = Color(0xFF43CEA2)
private val GlowingPinkAccent = Color(0xFFFFC2D1)
private val OutgoingDeepRoseBg = Color(0xFF5E081E).copy(alpha = 0.42f)
private val HighlightFlashColor = Color(0x2825D366)

private val ReplyIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "Reply",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).addPath(
        pathData = PathParser().parsePathString(
            "M10 9V5l-7 7 7 7v-4.1c5 0 8.5 1.6 11 5.1-1-5-4-10-11-11z"
        ).toNodes(),
        fill = SolidColor(Color.White)
    ).build()
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

private val CloseIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "Close",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).addPath(
        pathData = PathParser().parsePathString(
            "M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12z"
        ).toNodes(),
        fill = SolidColor(Color.White)
    ).build()
}

@Composable
internal fun MessageBubbleRow(
    message: ChatMessage,
    isOutgoing: Boolean,
    isSelected: Boolean,
    isHighlighted: Boolean,
    isSelectionMode: Boolean,
    isLastInGroup: Boolean,
    isLastInSenderGroup: Boolean = isLastInGroup,
    myCode: Int,
    partnerDisplayName: String,
    onToggleSelect: (String) -> Unit,
    onSwipeToReply: (ChatMessage) -> Unit,
    onReplyClick: (String) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()

    val triggerThresholdPx = with(density) { 45.dp.toPx() }

    val offsetX = remember { Animatable(0f) }
    var isTriggered by remember { mutableStateOf(false) }

    val draggableState = rememberDraggableState { delta ->
        if (!isSelectionMode) {
            val current = offsetX.value
            val newRaw = (current + delta).coerceAtLeast(0f)
            coroutineScope.launch {
                offsetX.snapTo(newRaw)
            }
            if (newRaw >= triggerThresholdPx && !isTriggered) {
                isTriggered = true
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            } else if (newRaw < triggerThresholdPx && isTriggered) {
                isTriggered = false
            }
        }
    }

    val bubbleShape = if (isOutgoing) {
        RoundedCornerShape(
            topStart = 8.dp,
            topEnd = 2.dp,
            bottomStart = 8.dp,
            bottomEnd = if (isLastInSenderGroup) 8.dp else 2.dp
        )
    } else {
        RoundedCornerShape(
            topStart = 2.dp,
            topEnd = 8.dp,
            bottomStart = if (isLastInSenderGroup) 8.dp else 2.dp,
            bottomEnd = 8.dp
        )
    }

    val formattedTime = remember(message.timestamp) {
        formatBubbleTime(message.timestamp)
    }

    val rowBackgroundColor by animateColorAsState(
        targetValue = when {
            isSelected -> HighlightFlashColor
            isHighlighted -> HighlightFlashColor
            else -> Color.Transparent
        },
        animationSpec = tween(durationMillis = if (isHighlighted) 120 else 600),
        label = "rowHighlightAnimation"
    )

    val bottomSpacing = if (isLastInSenderGroup) 8.dp else 2.dp

    val hasReply = !message.replyToText.isNullOrBlank()
    val isMedia = message.mediaType > 0
    val pureEmojiCount = remember(message.text) { if (!isMedia) getPureEmojiCount(message.text) else 0 }
    val isFloatingEmoji = !hasReply && !isMedia && (pureEmojiCount in 1..3)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(rowBackgroundColor)
            .draggable(
                state = draggableState,
                orientation = Orientation.Horizontal,
                enabled = !isSelectionMode,
                onDragStopped = {
                    val wasTriggered = isTriggered
                    if (wasTriggered) {
                        onSwipeToReply(message)
                    }
                    coroutineScope.launch {
                        offsetX.animateTo(
                            targetValue = 0f,
                            animationSpec = tween(
                                durationMillis = 180,
                                easing = FastOutSlowInEasing
                            )
                        )
                        isTriggered = false
                    }
                }
            )
            .pointerInput(message.id, isSelectionMode) {
                detectTapGestures(
                    onLongPress = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onToggleSelect(message.id)
                    },
                    onTap = {
                        if (isSelectionMode) {
                            onToggleSelect(message.id)
                        }
                    }
                )
            }
            .padding(start = 8.dp, end = 8.dp, top = 1.dp, bottom = bottomSpacing)
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 14.dp)
                .size(0.dp)
                .wrapContentSize(unbounded = true, align = Alignment.Center)
                .graphicsLayer {
                    val raw = offsetX.value
                    val progress = (raw / triggerThresholdPx).coerceIn(0f, 1f)
                    scaleX = 0.45f + (0.55f * progress)
                    scaleY = 0.45f + (0.55f * progress)
                    alpha = progress
                    translationX = (raw * 0.18f)
                }
        ) {
            Icon(
                imageVector = ReplyIcon,
                contentDescription = "Reply",
                tint = if (isTriggered) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                },
                modifier = Modifier.size(20.dp)
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    val raw = offsetX.value
                    val effective = if (raw <= triggerThresholdPx) {
                        raw
                    } else {
                        triggerThresholdPx + (raw - triggerThresholdPx) * 0.28f
                    }
                    translationX = effective
                },
            horizontalArrangement = if (isOutgoing) Arrangement.End else Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isFloatingEmoji) {
                FloatingEmojiContent(
                    text = message.text.trim(),
                    emojiCount = pureEmojiCount,
                    isOutgoing = isOutgoing,
                    formattedTime = formattedTime,
                    status = message.status
                )
            } else {
                Surface(
                    modifier = Modifier.widthIn(max = 290.dp),
                    shape = bubbleShape,
                    color = if (isOutgoing) Color.Transparent else MaterialTheme.colorScheme.surface,
                    border = if (isOutgoing) null else BorderStroke(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                    ),
                    shadowElevation = 1.dp
                ) {
                    Box(
                        modifier = Modifier.then(
                            if (isOutgoing) Modifier.background(MomoPrimaryGradient) else Modifier
                        )
                    ) {
                        if (message.mediaType > 0) {
                            MediaBubbleMessageContent(
                                message = message,
                                isOutgoing = isOutgoing,
                                isLastInGroup = isLastInGroup,
                                formattedTime = formattedTime,
                                myCode = myCode,
                                partnerDisplayName = partnerDisplayName,
                                bubbleShape = bubbleShape,
                                onReplyClick = onReplyClick
                            )
                        } else {
                            BubbleMessageContent(
                                message = message,
                                isOutgoing = isOutgoing,
                                isLastInGroup = isLastInGroup,
                                formattedTime = formattedTime,
                                myCode = myCode,
                                partnerDisplayName = partnerDisplayName,
                                onReplyClick = onReplyClick
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MediaBubbleMessageContent(
    message: ChatMessage,
    isOutgoing: Boolean,
    isLastInGroup: Boolean,
    formattedTime: String,
    myCode: Int,
    partnerDisplayName: String,
    bubbleShape: RoundedCornerShape,
    onReplyClick: (String) -> Unit
) {
    val context = LocalContext.current
    val hasReply = !message.replyToText.isNullOrBlank()
    val hasCaption = message.text.isNotBlank()

    val mediaFile = remember(message.localPath) {
        message.localPath?.let { fileName ->
            File(File(context.filesDir, "chat_media"), fileName).takeIf { it.exists() && it.length() > 0L }
        }
    }

    var showFullScreenPreview by remember { mutableStateOf(false) }

    if (showFullScreenPreview && mediaFile != null) {
        Dialog(
            onDismissRequest = { showFullScreenPreview = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = mediaFile,
                    contentDescription = "Full Preview",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )

                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 42.dp, end = 20.dp)
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.65f))
                        .bounceClick(scaleDown = 0.94f) {
                            showFullScreenPreview = false
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = CloseIcon,
                        contentDescription = "Close",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }

    Column(
        modifier = Modifier.widthIn(max = 290.dp)
    ) {
        if (hasReply) {
            Box(
                modifier = Modifier.padding(start = 6.dp, end = 6.dp, top = 6.dp, bottom = 4.dp)
            ) {
                QuotedReplySnippet(
                    replyToId = message.replyToId,
                    replyToText = message.replyToText ?: "",
                    replyToSenderId = message.replyToSenderId,
                    myCode = myCode,
                    partnerDisplayName = partnerDisplayName,
                    isOutgoing = isOutgoing,
                    onReplyClick = onReplyClick
                )
            }
        }

        val mediaClipShape = if (hasCaption) {
            RoundedCornerShape(
                topStart = bubbleShape.topStart,
                topEnd = bubbleShape.topEnd,
                bottomStart = 4.dp,
                bottomEnd = 4.dp
            )
        } else {
            bubbleShape
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 180.dp, max = 260.dp)
                .padding(
                    start = if (hasCaption) 4.dp else 0.dp,
                    end = if (hasCaption) 4.dp else 0.dp,
                    top = if (hasCaption || hasReply) 4.dp else 0.dp,
                    bottom = if (hasCaption) 0.dp else 0.dp
                )
                .clip(mediaClipShape)
                .background(Color.Black.copy(alpha = 0.25f))
                .bounceClick(scaleDown = 0.97f) {
                    if (mediaFile != null) {
                        if (message.mediaType == 2) {
                            try {
                                val uri = FileProvider.getUriForFile(
                                    context,
                                    "${context.packageName}.provider",
                                    mediaFile
                                )
                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                    setDataAndType(uri, "video/*")
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(intent, "Play Video"))
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        } else {
                            showFullScreenPreview = true
                        }
                    }
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

                if (!hasCaption && isLastInGroup) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 8.dp, bottom = 6.dp)
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
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(30.dp),
                        color = if (isOutgoing) Color.White else MaterialTheme.colorScheme.primary,
                        strokeWidth = 2.5.dp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = if (message.mediaType == 2) "Loading video..." else "Loading photo...",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.75f),
                        style = TextStyle(
                            platformStyle = PlatformTextStyle(includeFontPadding = false)
                        )
                    )
                }
            }
        }

        if (hasCaption) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 10.dp, end = 8.dp, top = 6.dp, bottom = 5.dp)
            ) {
                Text(
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

@Composable
private fun FloatingEmojiContent(
    text: String,
    emojiCount: Int,
    isOutgoing: Boolean,
    formattedTime: String,
    status: Int
) {
    val emojiFontSize = when (emojiCount) {
        1 -> 48.sp
        2 -> 34.sp
        else -> 26.sp
    }
    val emojiLineHeight = when (emojiCount) {
        1 -> 54.sp
        2 -> 40.sp
        else -> 32.sp
    }

    Column(
        horizontalAlignment = if (isOutgoing) Alignment.End else Alignment.Start,
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            fontSize = emojiFontSize,
            lineHeight = emojiLineHeight,
            style = TextStyle(
                platformStyle = PlatformTextStyle(includeFontPadding = false)
            )
        )

        Spacer(modifier = Modifier.height(2.dp))

        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(
                    if (isOutgoing) Color(0xFF2B0914).copy(alpha = 0.70f)
                    else Color.Black.copy(alpha = 0.45f)
                )
                .padding(horizontal = 6.5.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.5.dp)
        ) {
            Text(
                text = formattedTime,
                fontSize = 10.5.sp,
                color = Color.White.copy(alpha = 0.90f),
                style = TextStyle(
                    platformStyle = PlatformTextStyle(includeFontPadding = false)
                )
            )

            if (isOutgoing) {
                BubbleStatusTicks(status = status)
            }
        }
    }
}

@Composable
private fun BubbleMessageContent(
    message: ChatMessage,
    isOutgoing: Boolean,
    isLastInGroup: Boolean,
    formattedTime: String,
    myCode: Int,
    partnerDisplayName: String,
    onReplyClick: (String) -> Unit
) {
    var textLayoutResult by remember(message.id, message.text) {
        mutableStateOf<TextLayoutResult?>(null)
    }

    val textColor = if (isOutgoing) Color.White else MaterialTheme.colorScheme.onSurface
    val timeColor = if (isOutgoing)
        Color.White.copy(alpha = 0.80f)
    else
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)

    val hasReply = !message.replyToText.isNullOrBlank()

    Layout(
        content = {
            if (hasReply) {
                QuotedReplySnippet(
                    replyToId = message.replyToId,
                    replyToText = message.replyToText ?: "",
                    replyToSenderId = message.replyToSenderId,
                    myCode = myCode,
                    partnerDisplayName = partnerDisplayName,
                    isOutgoing = isOutgoing,
                    onReplyClick = onReplyClick
                )
            }

            Text(
                text = message.text,
                fontSize = 15.5.sp,
                color = textColor,
                lineHeight = 21.5.sp,
                style = TextStyle(
                    platformStyle = PlatformTextStyle(includeFontPadding = false)
                ),
                onTextLayout = { textLayoutResult = it }
            )

            if (isLastInGroup) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.5.dp)
                ) {
                    Text(
                        text = formattedTime,
                        fontSize = 11.sp,
                        color = timeColor,
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
    ) { measurables, constraints ->
        val bubblePadStart = 8.dp.roundToPx()
        val bubblePadEnd = 8.dp.roundToPx()
        val bubblePadTop = if (hasReply) 4.dp.roundToPx() else 5.dp.roundToPx()
        val bubblePadBottom = 5.dp.roundToPx()

        val quoteMargin = 4.dp.roundToPx()
        val quoteSpacing = 4.dp.roundToPx()

        val timePadEnd = 6.dp.roundToPx()
        val timePadBottom = 2.dp.roundToPx()
        val horizontalGap = 6.dp.roundToPx()

        val maxTextWidth = (constraints.maxWidth - bubblePadStart - bubblePadEnd).coerceAtLeast(0)
        val maxQuoteWidth = (constraints.maxWidth - quoteMargin * 2).coerceAtLeast(0)

        var mIndex = 0
        val quoteMeasurable = if (hasReply) measurables[mIndex++] else null
        val textMeasurable = measurables[mIndex++]
        val timeMeasurable = if (isLastInGroup && measurables.size > mIndex) measurables[mIndex] else null

        val textPlaceable = textMeasurable.measure(
            constraints.copy(minWidth = 0, maxWidth = maxTextWidth, minHeight = 0)
        )

        val timePlaceable = timeMeasurable?.measure(
            constraints.copy(minWidth = 0, minHeight = 0)
        )

        val lineCount = textLayoutResult?.lineCount ?: 1
        val lastLine = lineCount - 1
        val lastLineRight = textLayoutResult?.getLineRight(lastLine)?.toInt() ?: textPlaceable.width

        val timeWidth = timePlaceable?.width ?: 0
        val timeHeight = timePlaceable?.height ?: 0

        val inlineWidthNeeded = bubblePadStart + lastLineRight + horizontalGap + timeWidth + timePadEnd
        val canFitInline = timePlaceable == null || inlineWidthNeeded <= constraints.maxWidth

        val textNeededWidth = if (canFitInline) {
            maxOf(textPlaceable.width, (inlineWidthNeeded - bubblePadStart - bubblePadEnd).coerceAtLeast(0))
        } else {
            maxOf(textPlaceable.width, (timeWidth + timePadEnd - bubblePadEnd).coerceAtLeast(0))
        }

        val desiredQuoteWidth = if (hasReply) {
            val minQuoteWidth = (textNeededWidth + bubblePadStart + bubblePadEnd - quoteMargin * 2).coerceAtLeast(140.dp.roundToPx())
            minOf(minQuoteWidth, maxQuoteWidth)
        } else 0

        val quotePlaceable = quoteMeasurable?.measure(
            constraints.copy(
                minWidth = desiredQuoteWidth,
                maxWidth = maxQuoteWidth,
                minHeight = 0
            )
        )

        val quoteWidth = quotePlaceable?.width ?: 0
        val quoteHeight = if (quotePlaceable != null) quotePlaceable.height + quoteSpacing else 0

        val baseBubbleWidth = if (hasReply) {
            maxOf(quoteWidth + quoteMargin * 2, textPlaceable.width + bubblePadStart + bubblePadEnd)
        } else {
            textPlaceable.width + bubblePadStart + bubblePadEnd
        }

        if (timePlaceable == null) {
            val totalWidth = baseBubbleWidth
            val totalHeight = bubblePadTop + quoteHeight + textPlaceable.height + bubblePadBottom

            layout(totalWidth, totalHeight) {
                quotePlaceable?.placeRelative(quoteMargin, bubblePadTop)
                textPlaceable.placeRelative(bubblePadStart, bubblePadTop + quoteHeight)
            }
        } else if (canFitInline) {
            val totalWidth = maxOf(baseBubbleWidth, inlineWidthNeeded)
            val totalHeight = bubblePadTop + quoteHeight + textPlaceable.height + bubblePadBottom

            layout(totalWidth, totalHeight) {
                quotePlaceable?.placeRelative(quoteMargin, bubblePadTop)
                textPlaceable.placeRelative(bubblePadStart, bubblePadTop + quoteHeight)

                val timeX = totalWidth - timeWidth - timePadEnd
                val timeY = totalHeight - timeHeight - timePadBottom
                timePlaceable.placeRelative(timeX, timeY)
            }
        } else {
            val minTimeWidth = bubblePadStart + timeWidth + timePadEnd
            val totalWidth = maxOf(baseBubbleWidth, minTimeWidth)

            val extraBottomSpace = (timeHeight - (bubblePadBottom - timePadBottom)).coerceAtLeast(0) + 2.dp.roundToPx()
            val totalHeight = bubblePadTop + quoteHeight + textPlaceable.height + bubblePadBottom + extraBottomSpace

            layout(totalWidth, totalHeight) {
                quotePlaceable?.placeRelative(quoteMargin, bubblePadTop)
                textPlaceable.placeRelative(bubblePadStart, bubblePadTop + quoteHeight)

                val timeX = totalWidth - timeWidth - timePadEnd
                val timeY = totalHeight - timeHeight - timePadBottom
                timePlaceable.placeRelative(timeX, timeY)
            }
        }
    }
}

@Composable
private fun QuotedReplySnippet(
    replyToId: String?,
    replyToText: String,
    replyToSenderId: Int?,
    myCode: Int,
    partnerDisplayName: String,
    isOutgoing: Boolean,
    onReplyClick: (String) -> Unit
) {
    val isReplyFromMe = replyToSenderId == myCode
    val accentColor = if (isReplyFromMe) GlowingPinkAccent else AquaMintColor
    val senderTitle = if (isReplyFromMe) "You" else partnerDisplayName

    val containerBg = if (isOutgoing) {
        OutgoingDeepRoseBg
    } else {
        Color.Black.copy(alpha = 0.28f)
    }

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(containerBg)
            .pointerInput(replyToId) {
                detectTapGestures(
                    onTap = {
                        if (!replyToId.isNullOrEmpty()) {
                            onReplyClick(replyToId)
                        }
                    }
                )
            }
            .height(IntrinsicSize.Min)
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .fillMaxHeight()
                .background(accentColor)
        )

        Column(
            modifier = Modifier
                .padding(start = 7.dp, end = 8.dp, top = 4.dp, bottom = 4.dp)
                .widthIn(min = 120.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = senderTitle,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = accentColor,
                maxLines = 1,
                style = TextStyle(
                    platformStyle = PlatformTextStyle(includeFontPadding = false)
                )
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = replyToText,
                fontSize = 12.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = Color.White.copy(alpha = 0.85f),
                lineHeight = 16.sp,
                style = TextStyle(
                    platformStyle = PlatformTextStyle(includeFontPadding = false)
                )
            )
        }
    }
}

@Composable
private fun BubbleStatusTicks(status: Int) {
    val defaultTickColor = Color.White.copy(alpha = 0.80f)
    val blueTickColor = Color(0xFF34B7F1)

    when (status) {
        0 -> {
            Icon(
                imageVector = Icons.Outlined.Schedule,
                contentDescription = "Pending",
                tint = defaultTickColor,
                modifier = Modifier.size(13.5.dp)
            )
        }
        1 -> {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Sent",
                tint = defaultTickColor,
                modifier = Modifier.size(13.5.dp)
            )
        }
        2 -> {
            Box(
                modifier = Modifier.width(17.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Delivered",
                    tint = defaultTickColor,
                    modifier = Modifier.size(13.5.dp)
                )
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = defaultTickColor,
                    modifier = Modifier
                        .padding(start = 4.dp)
                        .size(13.5.dp)
                )
            }
        }
        3 -> {
            Box(
                modifier = Modifier.width(17.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Read",
                    tint = blueTickColor,
                    modifier = Modifier.size(13.5.dp)
                )
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = blueTickColor,
                    modifier = Modifier
                        .padding(start = 4.dp)
                        .size(13.5.dp)
                )
            }
        }
    }
}

private fun isEmojiCodePoint(cp: Int): Boolean {
    return (cp in 0x1F600..0x1F64F) ||
        (cp in 0x1F300..0x1F5FF) ||
        (cp in 0x1F680..0x1F6FF) ||
        (cp in 0x1F700..0x1F77F) ||
        (cp in 0x1F780..0x1F7FF) ||
        (cp in 0x1F800..0x1F8FF) ||
        (cp in 0x1F900..0x1F9FF) ||
        (cp in 0x1FA00..0x1FA6F) ||
        (cp in 0x1FA70..0x1FAFF) ||
        (cp in 0x2600..0x26FF) ||
        (cp in 0x2700..0x27BF) ||
        (cp in 0x2300..0x23FF) ||
        (cp in 0x2B50..0x2B55) ||
        (cp in 0x203C..0x2049) ||
        (cp in 0x2194..0x21AA) ||
        (cp in 0x2934..0x2935) ||
        (cp in 0x3297..0x3299) ||
        (cp in 0x1F1E6..0x1F1FF)
}

private fun isEmojiModifierOrJoiner(cp: Int): Boolean {
    return cp == 0x200D ||
        (cp in 0xFE00..0xFE0F) ||
        (cp in 0x1F3FB..0x1F3FF) ||
        (cp in 0xE0020..0xE007F) ||
        cp == 0x20E3
}

private fun getPureEmojiCount(text: String): Int {
    val trimmed = text.trim()
    if (trimmed.isEmpty()) return 0
    var count = 0
    var i = 0
    val len = trimmed.length
    while (i < len) {
        val cp = trimmed.codePointAt(i)
        val charCount = Character.charCount(cp)

        if (Character.isWhitespace(cp)) {
            i += charCount
            continue
        }

        if (!isEmojiCodePoint(cp)) {
            return 0
        }

        count++
        i += charCount

        while (i < len) {
            val nextCp = trimmed.codePointAt(i)
            val nextCharCount = Character.charCount(nextCp)
            if (isEmojiModifierOrJoiner(nextCp)) {
                i += nextCharCount
                if (nextCp == 0x200D && i < len) {
                    val joinedCp = trimmed.codePointAt(i)
                    i += Character.charCount(joinedCp)
                }
            } else if (nextCp in 0x1F1E6..0x1F1FF && cp in 0x1F1E6..0x1F1FF) {
                i += nextCharCount
                break
            } else {
                break
            }
        }
    }
    return count
}

internal fun isSameMinute(t1: Long, t2: Long): Boolean {
    if (t1 <= 0L || t2 <= 0L) return false
    return try {
        val dt1 = LocalDateTime.ofInstant(Instant.ofEpochMilli(t1), ZoneId.systemDefault())
        val dt2 = LocalDateTime.ofInstant(Instant.ofEpochMilli(t2), ZoneId.systemDefault())
        dt1.year == dt2.year &&
            dt1.dayOfYear == dt2.dayOfYear &&
            dt1.hour == dt2.hour &&
            dt1.minute == dt2.minute
    } catch (_: Exception) {
        false
    }
}

private fun formatBubbleTime(timestamp: Long): String {
    if (timestamp <= 0L) return ""
    return try {
        val dt = LocalDateTime.ofInstant(
            Instant.ofEpochMilli(timestamp),
            ZoneId.systemDefault()
        )
        val formatter = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH)
        dt.format(formatter).lowercase(Locale.ENGLISH)
    } catch (_: Exception) {
        ""
    }
}
