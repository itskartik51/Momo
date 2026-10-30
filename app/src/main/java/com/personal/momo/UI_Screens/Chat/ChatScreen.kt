package com.personal.momo.UI_Screens.Chat

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.personal.momo.Cache.CacheManager
import com.personal.momo.UI_Screens.MomoPrimaryGradient
import com.personal.momo.UI_Screens.bounceClick
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val ArrowBackIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "ArrowBack",
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

private val SendIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "Send",
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

data class ChatMessageItem(
    val id: String = System.currentTimeMillis().toString(),
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isOutgoing: Boolean,
    val deliveryStatus: MessageDeliveryStatus = MessageDeliveryStatus.SENT
)

@Composable
fun ChatScreen(
    onBack: () -> Unit = {},
    onSendMessage: (String) -> Unit = {}
) {
    val currentUserId by CacheManager.appUserIdFlow.collectAsState()
    val avatarUrl by CacheManager.avatarUrlFlow.collectAsState()
    val partnerDisplayName = if (currentUserId.equals("Momo", ignoreCase = true)) "Kanu" else "Momo"

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    var inputText by remember { mutableStateOf("") }

    // Standalone sample chat messages for instant layout preview
    val messagesList = remember {
        mutableStateListOf(
            ChatMessageItem(
                id = "1",
                text = "Hey! Did you reach safely?",
                timestamp = System.currentTimeMillis() - 1000 * 60 * 15,
                isOutgoing = false
            ),
            ChatMessageItem(
                id = "2",
                text = "Yes, just reached 5 mins ago. Setting up everything.",
                timestamp = System.currentTimeMillis() - 1000 * 60 * 10,
                isOutgoing = true,
                deliveryStatus = MessageDeliveryStatus.READ
            ),
            ChatMessageItem(
                id = "3",
                text = "Awesome! Let me know once you are free.",
                timestamp = System.currentTimeMillis() - 1000 * 60 * 5,
                isOutgoing = false
            )
        )
    }

    // Hardware back navigation handler
    BackHandler {
        onBack()
    }

    // Auto-scroll to latest message
    LaunchedEffect(messagesList.size) {
        if (messagesList.isNotEmpty()) {
            listState.animateScrollToItem(messagesList.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .navigationBarsPadding()
            .imePadding()
    ) {
        // Top Header
        ChatTopHeader(
            partnerName = partnerDisplayName,
            avatarUrl = avatarUrl,
            isOnline = true,
            onBack = onBack
        )

        // Messages Canvas
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 14.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(messagesList, key = { it.id }) { message ->
                MessageBubbleRow(message = message)
            }
        }

        // Bottom Typing Bar
        ChatInputBar(
            text = inputText,
            onTextChanged = { inputText = it },
            onSend = {
                val cleanText = inputText.trim()
                if (cleanText.isNotEmpty()) {
                    val newMsg = ChatMessageItem(
                        id = System.currentTimeMillis().toString(),
                        text = cleanText,
                        timestamp = System.currentTimeMillis(),
                        isOutgoing = true,
                        deliveryStatus = MessageDeliveryStatus.SENT
                    )
                    messagesList.add(newMsg)
                    onSendMessage(cleanText)
                    inputText = ""
                    coroutineScope.launch {
                        listState.animateScrollToItem(messagesList.size - 1)
                    }
                }
            }
        )
    }
}

@Composable
private fun ChatTopHeader(
    partnerName: String,
    avatarUrl: String?,
    isOnline: Boolean,
    onBack: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 3.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Back Action Button
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .bounceClick(scaleDown = 0.94f) {
                        onBack()
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = ArrowBackIcon,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Partner Avatar with Presence Indicator
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (!avatarUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = avatarUrl,
                        contentDescription = "Avatar",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(24.dp)
                    )
                }

                if (isOnline) {
                    Box(
                        modifier = Modifier
                            .size(11.dp)
                            .align(Alignment.BottomEnd)
                            .clip(CircleShape)
                            .background(Color(0xFF25D366))
                            .border(
                                width = 1.5.dp,
                                color = MaterialTheme.colorScheme.surface,
                                shape = CircleShape
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Name & Status Text
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = partnerName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (isOnline) "Online" else "Offline",
                    fontSize = 12.sp,
                    color = if (isOnline)
                        Color(0xFF25D366)
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun MessageBubbleRow(message: ChatMessageItem) {
    val bubbleShape = if (message.isOutgoing) {
        RoundedCornerShape(
            topStart = 16.dp,
            topEnd = 4.dp,
            bottomStart = 16.dp,
            bottomEnd = 16.dp
        )
    } else {
        RoundedCornerShape(
            topStart = 4.dp,
            topEnd = 16.dp,
            bottomStart = 16.dp,
            bottomEnd = 16.dp
        )
    }

    val formattedTime = remember(message.timestamp) {
        formatBubbleTime(message.timestamp)
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isOutgoing) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            modifier = Modifier.widthIn(max = 280.dp),
            shape = bubbleShape,
            color = if (message.isOutgoing)
                MaterialTheme.colorScheme.surfaceVariant
            else
                MaterialTheme.colorScheme.surface,
            border = if (message.isOutgoing) null else androidx.compose.foundation.BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
            ),
            shadowElevation = 1.dp
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(
                    text = message.text,
                    fontSize = 14.5.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(3.dp))

                Row(
                    modifier = Modifier.align(Alignment.End),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = formattedTime,
                        fontSize = 10.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )

                    if (message.isOutgoing) {
                        BubbleStatusTicks(status = message.deliveryStatus)
                    }
                }
            }
        }
    }
}

@Composable
private fun BubbleStatusTicks(status: MessageDeliveryStatus) {
    when (status) {
        MessageDeliveryStatus.SENT -> {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Sent",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                modifier = Modifier.size(13.dp)
            )
        }
        MessageDeliveryStatus.DELIVERED -> {
            Box(
                modifier = Modifier.width(17.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Delivered",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                    modifier = Modifier.size(13.dp)
                )
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                    modifier = Modifier
                        .padding(start = 4.dp)
                        .size(13.dp)
                )
            }
        }
        MessageDeliveryStatus.READ -> {
            val blueTickColor = Color(0xFF34B7F1)
            Box(
                modifier = Modifier.width(17.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Read",
                    tint = blueTickColor,
                    modifier = Modifier.size(13.dp)
                )
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = blueTickColor,
                    modifier = Modifier
                        .padding(start = 4.dp)
                        .size(13.dp)
                )
            }
        }
        MessageDeliveryStatus.NONE -> {}
    }
}

@Composable
private fun ChatInputBar(
    text: String,
    onTextChanged: (String) -> Unit,
    onSend: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Text Input Box
            Box(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 44.dp, max = 110.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(22.dp)
                    )
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                if (text.isEmpty()) {
                    Text(
                        text = "Message...",
                        fontSize = 14.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }

                BasicTextField(
                    value = text,
                    onValueChange = onTextChanged,
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = TextStyle(
                        fontSize = 14.5.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Rounded Send Button
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MomoPrimaryGradient)
                    .bounceClick(scaleDown = 0.94f) {
                        onSend()
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = SendIcon,
                    contentDescription = "Send",
                    tint = Color.White,
                    modifier = Modifier
                        .size(19.dp)
                        .padding(start = 2.dp)
                )
            }
        }
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
