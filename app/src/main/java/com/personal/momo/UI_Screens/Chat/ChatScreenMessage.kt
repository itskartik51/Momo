package com.personal.momo.UI_Screens.Chat

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.personal.momo.UI_Screens.MomoPrimaryGradient
import com.personal.momo.UI_Screens.bounceClick
import kotlinx.coroutines.launch
import kotlin.math.abs

private val MomoCrimsonHighlightBase = Color(0xFFFF2E63)
private val TailExtensionWidth = 6.dp
private val BubbleCornerRadius = 12.dp

internal class WhatsAppBubbleShape(
    private val isOutgoing: Boolean,
    private val hasTail: Boolean,
    private val cornerRadius: Dp = BubbleCornerRadius,
    private val tailWidth: Dp = TailExtensionWidth,
    private val tailHeight: Dp = 10.dp
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val w = size.width
        val h = size.height
        val tw = if (hasTail) with(density) { tailWidth.toPx() } else 0f
        val th = with(density) { tailHeight.toPx() }.coerceAtMost(h * 0.65f)
        val bodyWidth = (w - tw).coerceAtLeast(1f)
        val r = with(density) { cornerRadius.toPx() }.coerceAtMost(minOf(bodyWidth / 2f, h / 2f))
        val tipR = with(density) { 2.dp.toPx() }

        val path = Path().apply {
            if (!hasTail) {
                addRoundRect(
                    androidx.compose.ui.geometry.RoundRect(
                        rect = Rect(0f, 0f, w, h),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(r, r)
                    )
                )
            } else if (isOutgoing) {
                val right = w - tw
                moveTo(r, 0f)
                lineTo(w - tipR, 0f)
                quadraticBezierTo(w, 0f, w - (tipR * 0.35f), tipR * 1.25f)
                quadraticBezierTo(right + (tw * 0.22f), th * 0.55f, right, th)
                lineTo(right, h - r)
                arcTo(
                    rect = Rect(right - 2f * r, h - 2f * r, right, h),
                    startAngleDegrees = 0f,
                    sweepAngleDegrees = 90f,
                    forceMoveTo = false
                )
                lineTo(r, h)
                arcTo(
                    rect = Rect(0f, h - 2f * r, 2f * r, h),
                    startAngleDegrees = 90f,
                    sweepAngleDegrees = 90f,
                    forceMoveTo = false
                )
                lineTo(0f, r)
                arcTo(
                    rect = Rect(0f, 0f, 2f * r, 2f * r),
                    startAngleDegrees = 180f,
                    sweepAngleDegrees = 90f,
                    forceMoveTo = false
                )
                close()
            } else {
                val left = tw
                moveTo(tipR, 0f)
                lineTo(w - r, 0f)
                arcTo(
                    rect = Rect(w - 2f * r, 0f, w, 2f * r),
                    startAngleDegrees = 270f,
                    sweepAngleDegrees = 90f,
                    forceMoveTo = false
                )
                lineTo(w, h - r)
                arcTo(
                    rect = Rect(w - 2f * r, h - 2f * r, w, h),
                    startAngleDegrees = 0f,
                    sweepAngleDegrees = 90f,
                    forceMoveTo = false
                )
                lineTo(left + r, h)
                arcTo(
                    rect = Rect(left, h - 2f * r, left + 2f * r, h),
                    startAngleDegrees = 90f,
                    sweepAngleDegrees = 90f,
                    forceMoveTo = false
                )
                lineTo(left, th)
                quadraticBezierTo(left - (tw * 0.22f), th * 0.55f, tipR * 0.35f, tipR * 1.25f)
                quadraticBezierTo(0f, 0f, tipR, 0f)
                close()
            }
        }
        return Outline.Generic(path)
    }
}

@Composable
internal fun MessageBubbleRow(
    message: ChatMessage,
    replyToMessage: ChatMessage? = null,
    replyToStatus: StatusItem? = null,
    isStatusActive: Boolean = true,
    isOutgoing: Boolean,
    isSelected: Boolean,
    isHighlighted: Boolean,
    isSelectionMode: Boolean,
    isFirstInSenderGroup: Boolean = true,
    isLastInGroup: Boolean,
    isLastInSenderGroup: Boolean = isLastInGroup,
    myCode: Int,
    partnerDisplayName: String,
    onToggleSelect: (String) -> Unit,
    onSwipeToReply: (ChatMessage) -> Unit,
    onReplyClick: (String) -> Unit,
    onDoubleTapReact: (String) -> Unit = {},
    onReactionBadgeClick: (ChatMessage) -> Unit = {}
) {
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()

    val triggerThresholdPx = with(density) { 45.dp.toPx() }

    val offsetX = remember { Animatable(0f) }
    var isTriggered by remember { mutableStateOf(false) }

    val bubbleShape = remember(isOutgoing, isFirstInSenderGroup) {
        WhatsAppBubbleShape(
            isOutgoing = isOutgoing,
            hasTail = isFirstInSenderGroup
        )
    }

    val formattedTime = remember(message.timestamp) {
        formatBubbleTime(message.timestamp)
    }

    val selectionAlpha by animateFloatAsState(
        targetValue = if (isSelected) 0.20f else 0f,
        animationSpec = tween(
            durationMillis = if (isSelected) 100 else 120,
            easing = FastOutSlowInEasing
        ),
        label = "selectionAlphaAnimation"
    )

    val highlightAlpha by animateFloatAsState(
        targetValue = if (isHighlighted) 0.26f else 0f,
        animationSpec = tween(
            durationMillis = if (isHighlighted) 140 else 850,
            easing = FastOutSlowInEasing
        ),
        label = "quoteHighlightAlphaAnimation"
    )

    val combinedHighlightAlpha = maxOf(selectionAlpha, highlightAlpha)
    val rowBackgroundColor = if (combinedHighlightAlpha > 0.001f) {
        MomoCrimsonHighlightBase.copy(alpha = combinedHighlightAlpha)
    } else {
        Color.Transparent
    }

    val hasReactions = message.reactions.isNotEmpty()
    val baseBottomSpacing = if (isLastInSenderGroup) 8.dp else 2.5.dp
    val bottomSpacing = if (hasReactions) baseBottomSpacing + 14.dp else baseBottomSpacing

    val isStatusReply = isStatusActive && (replyToStatus != null || message.replyToId?.startsWith("STATUS_") == true)
    val hasReply = (replyToMessage != null) || isStatusReply

    val isMedia = message.mediaType > 0
    val pureEmojiCount = remember(message.text) { if (!isMedia) getPureEmojiCount(message.text) else 0 }
    val isFloatingEmoji = !hasReply && !isMedia && (pureEmojiCount in 1..3)

    val nonTailIndentStart = if (!isOutgoing && !isFirstInSenderGroup) TailExtensionWidth else 0.dp
    val nonTailIndentEnd = if (isOutgoing && !isFirstInSenderGroup) TailExtensionWidth else 0.dp

    val innerTailPadStart = if (!isOutgoing && isFirstInSenderGroup) TailExtensionWidth else 0.dp
    val innerTailPadEnd = if (isOutgoing && isFirstInSenderGroup) TailExtensionWidth else 0.dp

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(rowBackgroundColor)
            .pointerInput(message.id, isSelectionMode) {
                if (isSelectionMode) return@pointerInput
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    var isHorizontalDrag = false
                    var isDirectionLocked = false
                    var totalX = 0f
                    var totalY = 0f

                    try {
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            if (!change.pressed) break

                            val dragX = change.position.x - change.previousPosition.x
                            val dragY = change.position.y - change.previousPosition.y

                            totalX += dragX
                            totalY += dragY

                            if (!isDirectionLocked) {
                                val absX = abs(totalX)
                                val absY = abs(totalY)
                                val touchSlop = viewConfiguration.touchSlop

                                if (absX > touchSlop || absY > touchSlop) {
                                    isDirectionLocked = true
                                    if (totalX > 0f && absX > absY * 1.6f) {
                                        isHorizontalDrag = true
                                    }
                                }
                            }

                            if (isHorizontalDrag) {
                                change.consume()
                                val current = offsetX.value
                                val newRaw = (current + dragX).coerceAtLeast(0f)
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
                    } finally {
                        if (isHorizontalDrag) {
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
                    }
                }
            }
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
                    },
                    onDoubleTap = {
                        if (!isSelectionMode) {
                            onDoubleTapReact(message.id)
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
                .padding(start = nonTailIndentStart, end = nonTailIndentEnd)
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
                Box(
                    modifier = Modifier.wrapContentSize()
                ) {
                    Surface(
                        modifier = Modifier.widthIn(max = 296.dp),
                        shape = bubbleShape,
                        color = if (isOutgoing) Color.Transparent else MaterialTheme.colorScheme.surface,
                        border = if (isOutgoing) null else BorderStroke(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.28f)
                        ),
                        shadowElevation = 1.dp
                    ) {
                        Box(
                            modifier = Modifier
                                .then(
                                    if (isOutgoing) Modifier.background(MomoPrimaryGradient) else Modifier
                                )
                                .padding(start = innerTailPadStart, end = innerTailPadEnd)
                        ) {
                            if (message.mediaType > 0) {
                                MediaBubbleMessageContent(
                                    message = message,
                                    replyToMessage = replyToMessage,
                                    isOutgoing = isOutgoing,
                                    isLastInGroup = isLastInGroup,
                                    formattedTime = formattedTime,
                                    myCode = myCode,
                                    partnerDisplayName = partnerDisplayName,
                                    bubbleShape = bubbleShape,
                                    isSelectionMode = isSelectionMode,
                                    onToggleSelect = onToggleSelect,
                                    onReplyClick = onReplyClick,
                                    onDoubleTapReact = onDoubleTapReact
                                )
                            } else {
                                BubbleMessageContent(
                                    message = message,
                                    replyToMessage = replyToMessage,
                                    replyToStatus = replyToStatus,
                                    isStatusReply = isStatusReply,
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

                    if (hasReactions) {
                        ReactionBadge(
                            reactions = message.reactions,
                            isOutgoing = isOutgoing,
                            isFirstInSenderGroup = isFirstInSenderGroup,
                            modifier = Modifier
                                .align(if (isOutgoing) Alignment.BottomEnd else Alignment.BottomStart)
                                .offset(
                                    x = if (isOutgoing) (-12).dp else (if (isFirstInSenderGroup) 16.dp else 10.dp),
                                    y = 17.dp
                                )
                                .bounceClick(scaleDown = 0.90f) {
                                    onReactionBadgeClick(message)
                                }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReactionBadge(
    reactions: Map<Int, String>,
    isOutgoing: Boolean,
    isFirstInSenderGroup: Boolean,
    modifier: Modifier = Modifier
) {
    val emojis = remember(reactions) { reactions.values.toList() }
    val distinctEmojis = remember(emojis) { emojis.distinct() }
    val isSingle = emojis.size == 1

    val badgeBgColor = if (isOutgoing) {
        Color(0xFF38101E)
    } else {
        Color(0xFF222634)
    }

    val cutoutBorderColor = Color(0xFF0D0E15)

    if (isSingle) {
        Box(
            modifier = modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(badgeBgColor)
                .border(
                    width = 1.5.dp,
                    color = cutoutBorderColor,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = emojis.first(),
                fontSize = 13.5.sp,
                textAlign = TextAlign.Center,
                style = TextStyle(
                    platformStyle = PlatformTextStyle(includeFontPadding = false)
                )
            )
        }
    } else {
        val badgeText = if (distinctEmojis.size == 1 && emojis.size > 1) {
            "${distinctEmojis[0]} 2"
        } else {
            distinctEmojis.joinToString(" ")
        }
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = badgeBgColor,
            border = BorderStroke(
                width = 1.5.dp,
                color = cutoutBorderColor
            ),
            shadowElevation = 1.dp,
            modifier = modifier
        ) {
            Text(
                text = badgeText,
                fontSize = 12.5.sp,
                color = Color.White,
                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.5.dp),
                style = TextStyle(
                    platformStyle = PlatformTextStyle(includeFontPadding = false)
                )
            )
        }
    }
}

@Composable
internal fun StatusReplySnippet(
    title: String = "Reply to status",
    caption: String,
    accentColor: Color,
    isOutgoing: Boolean,
    onClick: () -> Unit
) {
    val containerBg = if (isOutgoing) {
        OutgoingDeepRoseBg
    } else {
        Color.Black.copy(alpha = 0.28f)
    }

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(containerBg)
            .pointerInput(title, caption) {
                detectTapGestures(
                    onTap = { onClick() }
                )
            }
            .height(IntrinsicSize.Min),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .fillMaxHeight()
                .background(accentColor)
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 7.dp, end = 8.dp, top = 4.dp, bottom = 4.dp)
                .widthIn(min = 120.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = accentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = TextStyle(
                    platformStyle = PlatformTextStyle(includeFontPadding = false)
                )
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = caption.ifBlank { "Status" },
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
    replyToMessage: ChatMessage?,
    replyToStatus: StatusItem? = null,
    isStatusReply: Boolean = false,
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

    val hasReply = (replyToMessage != null) || isStatusReply

    val statusQuoteTitle = remember(replyToStatus, myCode, partnerDisplayName, isOutgoing) {
        if (replyToStatus != null) {
            val ownerName = if (replyToStatus.senderId == myCode) "You" else partnerDisplayName
            "$ownerName • Status"
        } else {
            if (isOutgoing) "$partnerDisplayName • Status" else "You • Status"
        }
    }

    val statusQuoteAccentColor = remember(replyToStatus, myCode, isOutgoing) {
        if (replyToStatus != null) {
            if (replyToStatus.senderId == myCode) GlowingPinkAccent else AquaMintColor
        } else {
            if (isOutgoing) AquaMintColor else GlowingPinkAccent
        }
    }

    val statusQuoteCaption = remember(replyToStatus, message.replyToText) {
        if (replyToStatus != null) {
            val typeIcon = if (replyToStatus.mediaType == 2) "🎥" else "📷"
            if (replyToStatus.caption.isNotBlank()) {
                "$typeIcon ${replyToStatus.caption}"
            } else {
                if (replyToStatus.mediaType == 2) "🎥 Video" else "📷 Photo"
            }
        } else {
            message.replyToText.orEmpty().ifBlank { "📷 Status" }
        }
    }

    Layout(
        content = {
            if (isStatusReply) {
                StatusReplySnippet(
                    title = statusQuoteTitle,
                    caption = statusQuoteCaption,
                    accentColor = statusQuoteAccentColor,
                    isOutgoing = isOutgoing,
                    onClick = { onReplyClick(message.replyToId ?: replyToStatus?.id.orEmpty()) }
                )
            } else if (replyToMessage != null) {
                QuotedReplySnippet(
                    replyMessage = replyToMessage,
                    myCode = myCode,
                    partnerDisplayName = partnerDisplayName,
                    isOutgoing = isOutgoing,
                    onReplyClick = onReplyClick
                )
            }

            Text(
                text = message.text,
                fontSize = 15.sp,
                color = textColor,
                lineHeight = 20.sp,
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
        val bubblePadStart = 10.dp.roundToPx()
        val bubblePadEnd = 10.dp.roundToPx()
        val bubblePadTop = if (hasReply) 5.dp.roundToPx() else 6.dp.roundToPx()
        val bubblePadBottom = 6.dp.roundToPx()

        val quoteMargin = 4.5.dp.roundToPx()
        val quoteSpacing = 4.5.dp.roundToPx()

        val timePadEnd = 8.5.dp.roundToPx()
        val timePadBottom = 4.dp.roundToPx()
        val horizontalGap = 7.dp.roundToPx()

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

            val extraBottomSpace = (timeHeight - (bubblePadBottom - timePadBottom)).coerceAtLeast(0) + 3.dp.roundToPx()
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
