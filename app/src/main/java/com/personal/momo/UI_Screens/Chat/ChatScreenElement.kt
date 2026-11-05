package com.personal.momo.UI_Screens.Chat

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
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
import coil.compose.AsyncImage
import com.personal.momo.UI_Screens.MomoRedLight
import com.personal.momo.UI_Screens.bounceClick
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

internal val AquaMintColor = Color(0xFF43CEA2)
internal val GlowingPinkAccent = Color(0xFFFFC2D1)
internal val OutgoingDeepRoseBg = Color(0xFF5E081E).copy(alpha = 0.42f)
internal val HighlightFlashColor = Color(0x2825D366)

internal val ReplyIcon: ImageVector by lazy {
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

private val QuotePlayIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "QuotePlay",
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

internal val CloseReactionIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "CloseReaction",
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
internal fun ReactionPickerHeader(
    onCancel: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xFF171922),
        border = BorderStroke(width = 1.dp, color = Color(0xFF262936))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "React to message",
                fontSize = 14.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White.copy(alpha = 0.90f),
                style = TextStyle(
                    platformStyle = PlatformTextStyle(includeFontPadding = false)
                )
            )

            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .bounceClick(scaleDown = 0.88f) {
                        onCancel()
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = CloseReactionIcon,
                    contentDescription = "Cancel",
                    tint = Color.White.copy(alpha = 0.70f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
internal fun ReactionDetailsBottomSheet(
    message: ChatMessage,
    myCode: Int,
    partnerDisplayName: String,
    myAvatarUrl: String?,
    partnerAvatarUrl: String?,
    onRemoveMyReaction: () -> Unit,
    onDismiss: () -> Unit
) {
    val partnerCode = if (myCode == 1) 2 else 1
    val hasMyReaction = message.reactions.containsKey(myCode)
    val myReactionEmoji = message.reactions[myCode]
    val hasPartnerReaction = message.reactions.containsKey(partnerCode)
    val partnerReactionEmoji = message.reactions[partnerCode]

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onDismiss() }
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {}
                .navigationBarsPadding(),
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
            color = Color(0xFF171922),
            border = BorderStroke(1.dp, Color(0xFF262936))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .size(width = 36.dp, height = 4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color.White.copy(alpha = 0.25f))
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Reactions",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.90f),
                    modifier = Modifier.padding(start = 4.dp, bottom = 10.dp)
                )

                if (hasMyReaction && myReactionEmoji != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .bounceClick(scaleDown = 0.97f) {
                            onRemoveMyReaction()
                            onDismiss()
                        }
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            modifier = Modifier.size(40.dp),
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.12f)
                        ) {
                            if (!myAvatarUrl.isNullOrBlank()) {
                                AsyncImage(
                                    model = myAvatarUrl,
                                    contentDescription = "My Avatar",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Box(contentAlignment = Alignment.Center) {
                                    Text("Y", fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "You",
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                            Text(
                                text = "Tap to remove",
                                fontSize = 12.sp,
                                color = MomoRedLight
                            )
                        }

                        Text(
                            text = myReactionEmoji,
                            fontSize = 22.sp
                        )
                    }
                }

                if (hasPartnerReaction && partnerReactionEmoji != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            modifier = Modifier.size(40.dp),
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.12f)
                        ) {
                            if (!partnerAvatarUrl.isNullOrBlank()) {
                                AsyncImage(
                                    model = partnerAvatarUrl,
                                    contentDescription = "Partner Avatar",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        partnerDisplayName.take(1).uppercase(),
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = partnerDisplayName,
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        }

                        Text(
                            text = partnerReactionEmoji,
                            fontSize = 22.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
            }
        }
    }
}

@Composable
internal fun QuotedReplySnippet(
    replyMessage: ChatMessage,
    myCode: Int,
    partnerDisplayName: String,
    isOutgoing: Boolean,
    onReplyClick: (String) -> Unit
) {
    val context = LocalContext.current
    val isReplyFromMe = replyMessage.senderId == myCode
    val accentColor = if (isReplyFromMe) GlowingPinkAccent else AquaMintColor
    val senderTitle = if (isReplyFromMe) "You" else partnerDisplayName

    val isMedia = replyMessage.mediaType > 0
    val previewText = when {
        replyMessage.mediaType == 1 -> if (replyMessage.text.isNotBlank()) "📷 ${replyMessage.text}" else "📷 Photo"
        replyMessage.mediaType == 2 -> if (replyMessage.text.isNotBlank()) "🎥 ${replyMessage.text}" else "🎥 Video"
        else -> replyMessage.text.ifBlank { "Message" }
    }

    val mediaFile = remember(replyMessage.localPath) {
        val path = replyMessage.localPath
        if (path.isNullOrBlank()) null
        else {
            MediaRenderCache.getFile(path) ?: run {
                val resolved = MomoStorage.resolveLocalMediaFile(context, path)
                if (resolved != null) MediaRenderCache.putFile(path, resolved)
                resolved
            }
        }
    }

    val blurBitmap = remember(replyMessage.blurPreview) {
        val b64 = replyMessage.blurPreview
        if (b64.isNullOrBlank()) null
        else {
            MediaRenderCache.getBlurBitmap(b64) ?: run {
                val decoded = decodeBlurPreviewBitmap(b64)
                if (decoded != null) MediaRenderCache.putBlurBitmap(b64, decoded)
                decoded
            }
        }
    }

    val containerBg = if (isOutgoing) {
        OutgoingDeepRoseBg
    } else {
        Color.Black.copy(alpha = 0.28f)
    }

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(containerBg)
            .pointerInput(replyMessage.id) {
                detectTapGestures(
                    onTap = {
                        onReplyClick(replyMessage.id)
                    }
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
                .padding(start = 7.dp, end = if (isMedia) 4.dp else 8.dp, top = 4.dp, bottom = 4.dp)
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
                text = previewText,
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

        if (isMedia) {
            Box(
                modifier = Modifier
                    .padding(end = 4.dp, top = 4.dp, bottom = 4.dp)
                    .size(38.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.Black.copy(alpha = 0.35f)),
                contentAlignment = Alignment.Center
            ) {
                if (mediaFile != null || blurBitmap != null) {
                    AsyncImage(
                        model = mediaFile ?: blurBitmap,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }

                if (replyMessage.mediaType == 2) {
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.55f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = QuotePlayIcon,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(11.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun BubbleStatusTicks(status: Int) {
    val defaultTickColor = Color.White.copy(alpha = 0.80f)
    val readTickAquaMint = AquaMintColor

    Box(
        modifier = Modifier.width(18.dp),
        contentAlignment = Alignment.CenterEnd
    ) {
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
                        tint = readTickAquaMint,
                        modifier = Modifier.size(13.5.dp)
                    )
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = readTickAquaMint,
                        modifier = Modifier
                            .padding(start = 4.dp)
                            .size(13.5.dp)
                    )
                }
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

internal fun formatBubbleTime(timestamp: Long): String {
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

internal fun getPureEmojiCount(text: String): Int {
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
