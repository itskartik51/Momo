package com.personal.momo.UI_Screens.Chat

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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.personal.momo.UI_Screens.MomoPrimaryGradient
import kotlinx.coroutines.launch

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
        animationSpec = tween(
            durationMillis = when {
                isSelected -> 100
                isHighlighted -> 450
                else -> 300
            }
        ),
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
                                isSelectionMode = isSelectionMode,
                                onToggleSelect = onToggleSelect,
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
