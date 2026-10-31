package com.personal.momo.UI_Screens.Chat

import android.app.Activity
import android.graphics.drawable.ColorDrawable
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import coil.compose.AsyncImage
import com.personal.momo.Cache.CacheManager
import com.personal.momo.UI_Screens.bounceClick
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
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

private val CopyIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "ContentCopy",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).addPath(
        pathData = PathParser().parsePathString(
            "M16 1H4c-1.1 0-2 .9-2 2v14h2V3h12V1zm3 4H8c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h11c1.1 0 2-.9 2-2V7c0-1.1-.9-2-2-2zm0 16H8V7h11v14z"
        ).toNodes(),
        fill = SolidColor(Color.White)
    ).build()
}

private val DeleteIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "DeleteOutline",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).addPath(
        pathData = PathParser().parsePathString(
            "M6 19c0 1.1.9 2 2 2h8c1.1 0 2-.9 2-2V7H6v12zM19 4h-3.5l-1-1h-5l-1 1H5v2h14V4z"
        ).toNodes(),
        fill = SolidColor(Color.White)
    ).build()
}

@Composable
fun ChatScreen(
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentUserId by CacheManager.appUserIdFlow.collectAsState()
    val avatarUrl by CacheManager.avatarUrlFlow.collectAsState()

    val isMomo = currentUserId.equals("Momo", ignoreCase = true)
    val partnerDisplayName = if (isMomo) "Kanu" else "Momo"
    val myCode = if (isMomo) 2 else 1

    val partnerPresence by ChatEngine.partnerPresenceFlow.collectAsState()
    val messagesList by ChatRepository.messagesFlow.collectAsState()
    val reversedMessages = remember(messagesList) { messagesList.asReversed() }

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    var inputText by remember { mutableStateOf("") }

    var selectedMessageIds by remember { mutableStateOf(setOf<String>()) }
    val isSelectionMode = selectedMessageIds.isNotEmpty()
    var showDeleteDialog by remember { mutableStateOf(false) }

    val imeBottom = WindowInsets.ime.getBottom(density)
    val navBarsBottom = WindowInsets.navigationBars.getBottom(density)
    val bottomInsetDp = with(density) { maxOf(imeBottom, navBarsBottom).toDp() }

    val backgroundColor = MaterialTheme.colorScheme.background
    SideEffect {
        (context as? Activity)?.window?.setBackgroundDrawable(
            ColorDrawable(backgroundColor.toArgb())
        )
    }

    DisposableEffect(lifecycleOwner) {
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            ChatEngine.setChatScreenActive(context, true)
        }

        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    ChatEngine.setChatScreenActive(context, true)
                }
                Lifecycle.Event.ON_PAUSE,
                Lifecycle.Event.ON_STOP,
                Lifecycle.Event.ON_DESTROY -> {
                    ChatEngine.setChatScreenActive(context, false)
                }
                else -> {}
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            ChatEngine.setChatScreenActive(context, false)
        }
    }

    BackHandler {
        when {
            showDeleteDialog -> {
                showDeleteDialog = false
            }
            isSelectionMode -> {
                selectedMessageIds = emptySet()
            }
            else -> {
                ChatEngine.setChatScreenActive(context, false)
                onBack()
            }
        }
    }

    LaunchedEffect(messagesList.size) {
        if (messagesList.isNotEmpty() && !isSelectionMode) {
            listState.animateScrollToItem(0)
        }
    }

    if (showDeleteDialog) {
        DeleteMessagesDialog(
            selectedCount = selectedMessageIds.size,
            onDismiss = {
                showDeleteDialog = false
            },
            onConfirmDelete = {
                val targets = selectedMessageIds
                selectedMessageIds = emptySet()
                showDeleteDialog = false
                coroutineScope.launch {
                    ChatRepository.deleteMessages(context, targets)
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        if (isSelectionMode) {
            SelectionTopHeader(
                selectedCount = selectedMessageIds.size,
                onClearSelection = { selectedMessageIds = emptySet() },
                onCopy = {
                    ChatTool.copyMessagesToClipboard(context, messagesList, selectedMessageIds)
                    selectedMessageIds = emptySet()
                },
                onDelete = {
                    showDeleteDialog = true
                }
            )
        } else {
            ChatTopHeader(
                partnerName = partnerDisplayName,
                avatarUrl = avatarUrl,
                inChat = partnerPresence.inChat,
                lastSeen = partnerPresence.lastSeen,
                onBack = {
                    ChatEngine.setChatScreenActive(context, false)
                    onBack()
                }
            )
        }

        LazyColumn(
            state = listState,
            reverseLayout = true,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(backgroundColor)
                .clipToBounds(),
            contentPadding = PaddingValues(top = 6.dp, bottom = 6.dp),
            verticalArrangement = Arrangement.Bottom
        ) {
            itemsIndexed(reversedMessages, key = { _, message -> message.id }) { index, message ->
                val isOutgoing = message.senderId == myCode
                val isSelected = selectedMessageIds.contains(message.id)

                val nextChronologicalMsg = reversedMessages.getOrNull(index - 1)
                val isNextSameGroup = nextChronologicalMsg != null &&
                    nextChronologicalMsg.senderId == message.senderId &&
                    isSameMinute(message.timestamp, nextChronologicalMsg.timestamp)
                val isLastInGroup = !isNextSameGroup

                MessageBubbleRow(
                    message = message,
                    isOutgoing = isOutgoing,
                    isSelected = isSelected,
                    isSelectionMode = isSelectionMode,
                    isLastInGroup = isLastInGroup,
                    onToggleSelect = { id ->
                        selectedMessageIds = if (selectedMessageIds.contains(id)) {
                            selectedMessageIds - id
                        } else {
                            selectedMessageIds + id
                        }
                    }
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(backgroundColor)
                .padding(bottom = bottomInsetDp)
        ) {
            ChatInputBar(
                text = inputText,
                onTextChanged = { inputText = it },
                onSend = {
                    val cleanText = inputText.trim()
                    if (cleanText.isNotEmpty()) {
                        ChatEngine.sendMessage(context, cleanText)
                        inputText = ""
                        coroutineScope.launch {
                            if (reversedMessages.isNotEmpty()) {
                                listState.animateScrollToItem(0)
                            }
                        }
                    }
                }
            )
        }
    }
}

@Composable
private fun SelectionTopHeader(
    selectedCount: Int,
    onClearSelection: () -> Unit,
    onCopy: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .bounceClick(scaleDown = 0.94f) {
                        onClearSelection()
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = ArrowBackIcon,
                    contentDescription = "Cancel Selection",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = "$selectedCount",
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.weight(1f))

            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .bounceClick(scaleDown = 0.94f) {
                        onCopy()
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = CopyIcon,
                    contentDescription = "Copy Messages",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .bounceClick(scaleDown = 0.94f) {
                        onDelete()
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = DeleteIcon,
                    contentDescription = "Delete Messages",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(21.dp)
                )
            }
        }
    }
}

@Composable
private fun ChatTopHeader(
    partnerName: String,
    avatarUrl: String?,
    inChat: Boolean,
    lastSeen: Long,
    onBack: () -> Unit
) {
    val statusText = when {
        inChat -> "Online"
        lastSeen > 0L -> "Last seen ${formatLastSeenTime(lastSeen)}"
        else -> "Offline"
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
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
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
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
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = partnerName,
                    fontSize = 15.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = statusText,
                    fontSize = 11.5.sp,
                    color = if (inChat)
                        Color(0xFF25D366)
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}

private fun isSameMinute(t1: Long, t2: Long): Boolean {
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

private fun formatLastSeenTime(timestamp: Long): String {
    if (timestamp <= 0L) return ""
    return try {
        val dt = LocalDateTime.ofInstant(
            Instant.ofEpochMilli(timestamp),
            ZoneId.systemDefault()
        )
        val today = LocalDate.now()
        val targetDate = dt.toLocalDate()
        val timeStr = dt.format(DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH))

        when (targetDate) {
            today -> timeStr
            today.minusDays(1) -> "yesterday at $timeStr"
            else -> "${dt.format(DateTimeFormatter.ofPattern("dd MMM", Locale.ENGLISH))} at $timeStr"
        }
    } catch (_: Exception) {
        ""
    }
}
