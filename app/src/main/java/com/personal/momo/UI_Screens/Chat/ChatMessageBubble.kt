package com.personal.momo.UI_Screens.Chat

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.personal.momo.UI_Screens.MomoPrimaryGradient
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
internal fun MessageBubbleRow(
    message: ChatMessage,
    isOutgoing: Boolean,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    isLastInGroup: Boolean,
    onToggleSelect: (String) -> Unit
) {
    val haptic = LocalHapticFeedback.current

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
            horizontalArrangement = if (isOutgoing) Arrangement.End else Arrangement.Start
        ) {
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
                    BubbleMessageContent(
                        message = message,
                        isOutgoing = isOutgoing,
                        isLastInGroup = isLastInGroup,
                        formattedTime = formattedTime
                    )
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
    formattedTime: String
) {
    var textLayoutResult by remember(message.id, message.text) {
        mutableStateOf<TextLayoutResult?>(null)
    }

    val textColor = if (isOutgoing) Color.White else MaterialTheme.colorScheme.onSurface
    val timeColor = if (isOutgoing)
        Color.White.copy(alpha = 0.80f)
    else
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)

    Layout(
        content = {
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

        val maxTextWidth = (constraints.maxWidth - padStart - padEnd).coerceAtLeast(0)
        val textPlaceable = measurables[0].measure(
            constraints.copy(minWidth = 0, maxWidth = maxTextWidth, minHeight = 0)
        )

        if (!isLastInGroup || measurables.size < 2) {
            val totalWidth = padStart + textPlaceable.width + padEnd
            val totalHeight = padTop + textPlaceable.height + padBottom

            layout(totalWidth, totalHeight) {
                textPlaceable.placeRelative(padStart, padTop)
            }
        } else {
            val timePlaceable = measurables[1].measure(
                constraints.copy(minWidth = 0, minHeight = 0)
            )

            val lineCount = textLayoutResult?.lineCount ?: 1
            val lastLine = lineCount - 1
            val lastLineRight = textLayoutResult?.getLineRight(lastLine)?.toInt() ?: textPlaceable.width

            val inlineWidthNeeded = padStart + lastLineRight + horizontalGap + timePlaceable.width + timePadEnd
            val canFitInline = inlineWidthNeeded <= constraints.maxWidth

            if (canFitInline) {
                val baseBubbleWidth = padStart + textPlaceable.width + padEnd
                val totalWidth = maxOf(baseBubbleWidth, inlineWidthNeeded)
                val totalHeight = padTop + textPlaceable.height + padBottom

                layout(totalWidth, totalHeight) {
                    textPlaceable.placeRelative(padStart, padTop)

                    val timeX = totalWidth - timePlaceable.width - timePadEnd
                    val timeY = totalHeight - timePlaceable.height - timePadBottom
                    timePlaceable.placeRelative(timeX, timeY)
                }
            } else {
                val baseBubbleWidth = padStart + textPlaceable.width + padEnd
                val minTimeWidth = padStart + timePlaceable.width + timePadEnd
                val totalWidth = maxOf(baseBubbleWidth, minTimeWidth)
                val extraBottomSpace = (timePlaceable.height - (padBottom - timePadBottom)).coerceAtLeast(0) + 2.dp.roundToPx()
                val totalHeight = padTop + textPlaceable.height + padBottom + extraBottomSpace

                layout(totalWidth, totalHeight) {
                    textPlaceable.placeRelative(padStart, padTop)

                    val timeX = totalWidth - timePlaceable.width - timePadEnd
                    val timeY = totalHeight - timePlaceable.height - timePadBottom
                    timePlaceable.placeRelative(timeX, timeY)
                }
            }
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
