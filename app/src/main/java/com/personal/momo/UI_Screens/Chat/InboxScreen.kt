package com.personal.momo.UI_Screens.Chat

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.personal.momo.Cache.CacheManager
import com.personal.momo.UI_Screens.MomoPrimaryGradient
import com.personal.momo.UI_Screens.MomoRedLight
import com.personal.momo.UI_Screens.bounceClick
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale

private val InboxPendingClockIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "InboxPendingClock",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).addPath(
        pathData = PathParser().parsePathString(
            "M11.99 2C6.47 2 2 6.48 2 12s4.47 10 9.99 10C17.52 22 22 17.52 22 12S17.52 2 11.99 2zM12 20c-4.42 0-8-3.58-8-8s3.58-8 8-8 8 3.58 8 8-3.58 8-8 8zm.5-13H11v6l5.25 3.15.75-1.23-4.5-2.67z"
        ).toNodes(),
        fill = SolidColor(Color.White)
    ).build()
}

@Composable
fun InboxScreen(
    onChatClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val currentUserId by CacheManager.appUserIdFlow.collectAsState()
    val avatarUrl by CacheManager.avatarUrlFlow.collectAsState()
    val messages by ChatRepository.messagesFlow.collectAsState()

    val isMomo = currentUserId.equals("Momo", ignoreCase = true)
    val mySenderCode = if (isMomo) 2 else 1
    val partnerDisplayName = if (isMomo) "Kanu" else "Momo"

    LaunchedEffect(Unit) {
        ChatEngine.startEngine(context)
    }

    val lastMsg = messages.lastOrNull()
    val lastMessageText = remember(lastMsg) {
        if (lastMsg == null) {
            "Tap to start chatting"
        } else if (lastMsg.mediaType == 1) {
            if (lastMsg.text.isNotBlank()) "📷 ${lastMsg.text}" else "📷 Photo"
        } else if (lastMsg.mediaType == 2) {
            if (lastMsg.text.isNotBlank()) "🎥 ${lastMsg.text}" else "🎥 Video"
        } else {
            lastMsg.text.ifBlank { "Tap to start chatting" }
        }
    }

    val lastTimestamp = lastMsg?.timestamp ?: 0L
    val isOutgoing = lastMsg != null && lastMsg.senderId == mySenderCode
    val deliveryStatus = lastMsg?.status ?: -1
    val unreadCount = remember(messages, mySenderCode) {
        messages.count { it.senderId != mySenderCode && it.status < 3 }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        InboxChatRow(
            partnerName = partnerDisplayName,
            avatarUrl = avatarUrl,
            lastMessage = lastMessageText,
            timestamp = lastTimestamp,
            isOutgoing = isOutgoing,
            deliveryStatus = deliveryStatus,
            unreadCount = unreadCount,
            onClick = onChatClick
        )
    }
}

@Composable
private fun InboxChatRow(
    partnerName: String,
    avatarUrl: String?,
    lastMessage: String,
    timestamp: Long,
    isOutgoing: Boolean,
    deliveryStatus: Int,
    unreadCount: Int,
    onClick: () -> Unit
) {
    val formattedTime = remember(timestamp) {
        formatChatTimestamp(timestamp)
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .bounceClick(scaleDown = 0.94f) {
                onClick()
            },
        color = MaterialTheme.colorScheme.background
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (!avatarUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = avatarUrl,
                        contentDescription = "Partner Avatar",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Avatar Fallback",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = partnerName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(3.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isOutgoing && deliveryStatus in 0..3) {
                        MessageStatusTicks(status = deliveryStatus)
                        Spacer(modifier = Modifier.width(4.dp))
                    }

                    Text(
                        text = lastMessage,
                        fontSize = 13.5.sp,
                        fontWeight = if (unreadCount > 0) FontWeight.Medium else FontWeight.Normal,
                        color = if (unreadCount > 0)
                            MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f)
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Center
            ) {
                if (formattedTime.isNotEmpty()) {
                    Text(
                        text = formattedTime,
                        fontSize = 12.sp,
                        fontWeight = if (unreadCount > 0) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (unreadCount > 0)
                            MomoRedLight
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                if (unreadCount > 0) {
                    Box(
                        modifier = Modifier
                            .defaultMinSize(minWidth = 20.dp, minHeight = 20.dp)
                            .clip(CircleShape)
                            .background(MomoPrimaryGradient)
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (unreadCount > 99) "99+" else unreadCount.toString(),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            lineHeight = 11.sp
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }
    }
}

@Composable
private fun MessageStatusTicks(status: Int) {
    when (status) {
        0 -> {
            Icon(
                imageVector = InboxPendingClockIcon,
                contentDescription = "Pending",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(14.dp)
            )
        }
        1 -> {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Sent",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                modifier = Modifier.size(15.dp)
            )
        }
        2 -> {
            Box(
                modifier = Modifier.width(19.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Delivered",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                    modifier = Modifier.size(15.dp)
                )
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                    modifier = Modifier
                        .padding(start = 5.dp)
                        .size(15.dp)
                )
            }
        }
        3 -> {
            val blueTickColor = Color(0xFF34B7F1)
            Box(
                modifier = Modifier.width(19.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Read",
                    tint = blueTickColor,
                    modifier = Modifier.size(15.dp)
                )
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = blueTickColor,
                    modifier = Modifier
                        .padding(start = 5.dp)
                        .size(15.dp)
                )
            }
        }
    }
}

private fun formatChatTimestamp(timestamp: Long): String {
    if (timestamp <= 0L) return ""
    return try {
        val msgDateTime = LocalDateTime.ofInstant(
            Instant.ofEpochMilli(timestamp),
            ZoneId.systemDefault()
        )
        val msgDate = msgDateTime.toLocalDate()
        val today = LocalDate.now()

        if (msgDate.isEqual(today)) {
            val timeFormatter = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH)
            msgDateTime.format(timeFormatter).lowercase(Locale.ENGLISH)
        } else {
            val currentSunday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))
            val currentSaturday = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SATURDAY))

            val isCurrentWeek = !msgDate.isBefore(currentSunday) && !msgDate.isAfter(currentSaturday)

            if (isCurrentWeek) {
                val dayFormatter = DateTimeFormatter.ofPattern("EEEE", Locale.ENGLISH)
                msgDate.format(dayFormatter)
            } else {
                val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yy", Locale.ENGLISH)
                msgDate.format(dateFormatter)
            }
        }
    } catch (_: Exception) {
        ""
    }
}
