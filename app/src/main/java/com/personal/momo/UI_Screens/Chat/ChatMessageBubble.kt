package com.personal.momo.UI_Screens.Chat

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Schedule
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
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.personal.momo.UI_Screens.MomoPrimaryGradient
import com.personal.momo.UI_Screens.MomoRedLight
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

private val AquaMintColor = Color(0xFF43CEA2)

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

@Composable
internal fun MessageBubbleRow(
    message: ChatMessage,
    isOutgoing: Boolean,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    isLastInGroup: Boolean,
    myCode: Int,
    partnerDisplayName: String,
    onToggleSelect: (String) -> Unit,
    onSwipeToReply: (ChatMessage) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()

    val triggerThresholdPx = with(density) { 45.dp.toPx() }
    val maxDragPx = with(density) { 68.dp.toPx() }

    val offsetX = remember { Animatable(0f) }
    var isTriggered by remember { mutableStateOf(false) }

    val draggableState = rememberDraggableState { delta ->
        if (!isSelectionMode) {
            val current = offsetX.value
            val target = (current + delta).coerceIn(0f, maxDragPx)
            coroutineScope.launch {
                offsetX.snapTo(target)
            }
            if (target >= triggerThresholdPx && !isTriggered) {
                isTriggered = true
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            }
        }
    }

    val bubbleShape = if (isOutgoing) {
        RoundedCornerShape(
            topStart = 16.dp,
            topEnd = 4.dp,
            bottomStart = 16.dp,
            bottomEnd = if (isLastInGroup) 16.dp else 4.dp
        )
    } else {
        RoundedCornerShape(
            topStart = 4.dp,
            topEnd = 16.dp,
            bottomStart = if (isLastInGroup) 16.dp else 4.dp,
            bottomEnd = 16.dp
        )
    }

    val formattedTime = remember(message.timestamp) {
        formatBubbleTime(message.timestamp)
    }

    val bottomSpacing = if (isLastInGroup) 6.dp else 2.dp

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (isSelected) Color(0x2825D366) else Color.Transparent
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
            .padding(start = 12.dp, end = 12.dp, top = 1.dp, bottom = bottomSpacing)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = if (isOutgoing) Arrangement.End else Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                contentAlignment = Alignment.CenterStart
            ) {
                if (offsetX.value > 0f) {
                    val progress = (offsetX.value / triggerThresholdPx).coerceIn(0f, 1f)
                    val iconScale = 0.5f + (0.5f * progress)
                    val iconAlpha = progress

                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .graphicsLayer {
                                scaleX = iconScale
                                scaleY = iconScale
                                alpha = iconAlpha
                                translationX = (offsetX.value * 0.35f)
                            }
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = ReplyIcon,
                            contentDescription = "Reply",
                            tint = if (isTriggered) MomoRedLight else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }

                Surface(
                    modifier = Modifier
                        .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                        .draggable(
                            state = draggableState,
                            orientation = Orientation.Horizontal,
                            enabled = !isSelectionMode,
                            onDragStopped = {
                                if (isTriggered) {
                                    onSwipeToReply(message)
                                }
                                isTriggered = false
                                coroutineScope.launch {
                                    offsetX.animateTo(
                                        targetValue = 0f,
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioMediumBouncy,
                                            stiffness = Spring.StiffnessLow
                                        )
                                    )
                                }
                            }
                        )
                        .widthIn(max = 290.dp),
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
                        BubbleMessageContent(
                            message = message,
                            isOutgoing = isOutgoing,
                            isLastInGroup = isLastInGroup,
                            formattedTime = formattedTime,
                            myCode = myCode,
                            partnerDisplayName = partnerDisplayName
                        )
                    }
                }
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
    partnerDisplayName: String
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
                    replyToText = message.replyToText ?: "",
                    replyToSenderId = message.replyToSenderId,
                    myCode = myCode,
                    partnerDisplayName = partnerDisplayName,
                    isOutgoing = isOutgoing
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
        val padStart = 11.dp.roundToPx()
        val padEnd = 11.dp.roundToPx()
        val padTop = 6.dp.roundToPx()
        val padBottom = 6.dp.roundToPx()

        val timePadEnd = 7.dp.roundToPx()
        val timePadBottom = 2.dp.roundToPx()
        val horizontalGap = 7.dp.roundToPx()
        val quoteSpacing = 4.dp.roundToPx()

        val maxContentWidth = (constraints.maxWidth - padStart - padEnd).coerceAtLeast(0)

        var mIndex = 0
        val quoteMeasurable = if (hasReply) measurables[mIndex++] else null
        val textMeasurable = measurables[mIndex++]
        val timeMeasurable = if (isLastInGroup && measurables.size > mIndex) measurables[mIndex] else null

        val unconstrainedQuotePlaceable = quoteMeasurable?.measure(
            constraints.copy(minWidth = 0, maxWidth = maxContentWidth, minHeight = 0)
        )

        val textPlaceable = textMeasurable.measure(
            constraints.copy(minWidth = 0, maxWidth = maxContentWidth, minHeight = 0)
        )

        val timePlaceable = timeMeasurable?.measure(
            constraints.copy(minWidth = 0, minHeight = 0)
        )

        val lineCount = textLayoutResult?.lineCount ?: 1
        val lastLine = lineCount - 1
        val lastLineRight = textLayoutResult?.getLineRight(lastLine)?.toInt() ?: textPlaceable.width

        val quoteWidth = unconstrainedQuotePlaceable?.width ?: 0
        val quoteHeight = if (unconstrainedQuotePlaceable != null) unconstrainedQuotePlaceable.height + quoteSpacing else 0

        if (timePlaceable == null) {
            val contentWidth = maxOf(quoteWidth, textPlaceable.width)
            val totalWidth = padStart + contentWidth + padEnd
            val totalHeight = padTop + quoteHeight + textPlaceable.height + padBottom

            val finalQuotePlaceable = quoteMeasurable?.measure(
                constraints.copy(minWidth = contentWidth, maxWidth = contentWidth)
            ) ?: unconstrainedQuotePlaceable

            layout(totalWidth, totalHeight) {
                finalQuotePlaceable?.placeRelative(padStart, padTop)
                textPlaceable.placeRelative(padStart, padTop + quoteHeight)
            }
        } else {
            val inlineWidthNeeded = padStart + lastLineRight + horizontalGap + timePlaceable.width + timePadEnd
            val canFitInline = inlineWidthNeeded <= constraints.maxWidth

            if (canFitInline) {
                val baseContentWidth = maxOf(quoteWidth, textPlaceable.width)
                val baseBubbleWidth = padStart + baseContentWidth + padEnd
                val totalWidth = maxOf(baseBubbleWidth, inlineWidthNeeded)
                val totalHeight = padTop + quoteHeight + textPlaceable.height + padBottom

                val finalContentWidth = (totalWidth - padStart - padEnd).coerceAtLeast(0)
                val finalQuotePlaceable = quoteMeasurable?.measure(
                    constraints.copy(minWidth = finalContentWidth, maxWidth = finalContentWidth)
                ) ?: unconstrainedQuotePlaceable

                layout(totalWidth, totalHeight) {
                    finalQuotePlaceable?.placeRelative(padStart, padTop)
                    textPlaceable.placeRelative(padStart, padTop + quoteHeight)

                    val timeX = totalWidth - timePlaceable.width - timePadEnd
                    val timeY = totalHeight - timePlaceable.height - timePadBottom
                    timePlaceable.placeRelative(timeX, timeY)
                }
            } else {
                val baseContentWidth = maxOf(quoteWidth, textPlaceable.width)
                val baseBubbleWidth = padStart + baseContentWidth + padEnd
                val minTimeWidth = padStart + timePlaceable.width + timePadEnd
                val totalWidth = maxOf(baseBubbleWidth, minTimeWidth)

                val extraBottomSpace = (timePlaceable.height - (padBottom - timePadBottom)).coerceAtLeast(0) + 2.dp.roundToPx()
                val totalHeight = padTop + quoteHeight + textPlaceable.height + padBottom + extraBottomSpace

                val finalContentWidth = (totalWidth - padStart - padEnd).coerceAtLeast(0)
                val finalQuotePlaceable = quoteMeasurable?.measure(
                    constraints.copy(minWidth = finalContentWidth, maxWidth = finalContentWidth)
                ) ?: unconstrainedQuotePlaceable

                layout(totalWidth, totalHeight) {
                    finalQuotePlaceable?.placeRelative(padStart, padTop)
                    textPlaceable.placeRelative(padStart, padTop + quoteHeight)

                    val timeX = totalWidth - timePlaceable.width - timePadEnd
                    val timeY = totalHeight - timePlaceable.height - timePadBottom
                    timePlaceable.placeRelative(timeX, timeY)
                }
            }
        }
    }
}

@Composable
private fun QuotedReplySnippet(
    replyToText: String,
    replyToSenderId: Int?,
    myCode: Int,
    partnerDisplayName: String,
    isOutgoing: Boolean
) {
    val isReplyFromMe = replyToSenderId == myCode
    val accentColor = if (isReplyFromMe) MomoRedLight else AquaMintColor
    val senderTitle = if (isReplyFromMe) "You" else partnerDisplayName

    val containerBg = if (isOutgoing) {
        Color.Black.copy(alpha = 0.22f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(containerBg)
    ) {
        Box(
            modifier = Modifier
                .width(3.5.dp)
                .heightIn(min = 34.dp)
                .background(accentColor)
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
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
                color = if (isOutgoing)
                    Color.White.copy(alpha = 0.85f)
                else
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
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
