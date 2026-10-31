package com.personal.momo.UI_Screens.Chat

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import coil.compose.AsyncImage
import com.personal.momo.Cache.CacheManager
import com.personal.momo.UI_Screens.MomoPrimaryGradient
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
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    var inputText by remember { mutableStateOf("") }

    var selectedMessageIds by remember { mutableStateOf(setOf<String>()) }
    val isSelectionMode = selectedMessageIds.isNotEmpty()
    var showDeleteDialog by remember { mutableStateOf(false) }

    val imeBottom = WindowInsets.ime.getBottom(density)
    val navBarsBottom = WindowInsets.navigationBars.getBottom(density)
    val bottomInsetDp = with(density) { maxOf(imeBottom, navBarsBottom).toDp() }

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
            listState.animateScrollToItem(messagesList.size - 1)
        }
    }

    LaunchedEffect(imeBottom) {
        if (imeBottom > 0 && messagesList.isNotEmpty() && !isSelectionMode) {
            listState.animateScrollToItem(messagesList.size - 1)
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
            .background(MaterialTheme.colorScheme.background)
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
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clipToBounds(),
            contentPadding = PaddingValues(top = 6.dp, bottom = 6.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            itemsIndexed(messagesList, key = { _, message -> message.id }) { index, message ->
                val isOutgoing = message.senderId == myCode
                val isSelected = selectedMessageIds.contains(message.id)

                val nextMsg = messagesList.getOrNull(index + 1)
                val isNextSameGroup = nextMsg != null &&
                    nextMsg.senderId == message.senderId &&
                    isSameMinute(message.timestamp, nextMsg.timestamp)
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
                            if (messagesList.isNotEmpty()) {
                                listState.animateScrollToItem(messagesList.size - 1)
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

@Composable
private fun MessageBubbleRow(
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

    val isShortMessage = remember(message.text) {
        !message.text.contains('\n') && message.text.length <= 26
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
                if (!isLastInGroup) {
                    Column(
                        modifier = Modifier
                            .then(
                                if (isOutgoing) Modifier.background(MomoPrimaryGradient) else Modifier
                            )
                            .padding(horizontal = 11.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = message.text,
                            fontSize = 14.5.sp,
                            color = if (isOutgoing) Color.White else MaterialTheme.colorScheme.onSurface,
                            lineHeight = 19.5.sp
                        )
                    }
                } else if (isShortMessage) {
                    Row(
                        modifier = Modifier
                            .then(
                                if (isOutgoing) Modifier.background(MomoPrimaryGradient) else Modifier
                            )
                            .padding(start = 11.dp, end = 9.dp, top = 5.dp, bottom = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = message.text,
                            fontSize = 14.5.sp,
                            color = if (isOutgoing) Color.White else MaterialTheme.colorScheme.onSurface,
                            lineHeight = 19.5.sp,
                            modifier = Modifier.align(Alignment.CenterVertically)
                        )

                        Spacer(modifier = Modifier.width(6.dp))

                        Row(
                            modifier = Modifier
                                .align(Alignment.Bottom)
                                .padding(bottom = 1.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Text(
                                text = formattedTime,
                                fontSize = 10.sp,
                                color = if (isOutgoing)
                                    Color.White.copy(alpha = 0.82f)
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )

                            if (isOutgoing) {
                                BubbleStatusTicks(status = message.status)
                            }
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .then(
                                if (isOutgoing) Modifier.background(MomoPrimaryGradient) else Modifier
                            )
                            .padding(start = 11.dp, end = 10.dp, top = 5.dp, bottom = 4.dp)
                    ) {
                        Text(
                            text = message.text,
                            fontSize = 14.5.sp,
                            color = if (isOutgoing) Color.White else MaterialTheme.colorScheme.onSurface,
                            lineHeight = 19.5.sp
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        Row(
                            modifier = Modifier.align(Alignment.End),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Text(
                                text = formattedTime,
                                fontSize = 10.sp,
                                color = if (isOutgoing)
                                    Color.White.copy(alpha = 0.82f)
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
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
private fun BubbleStatusTicks(status: Int) {
    val defaultTickColor = Color.White.copy(alpha = 0.85f)
    val blueTickColor = Color(0xFF34B7F1)

    when (status) {
        0 -> {
            Icon(
                imageVector = Icons.Outlined.Schedule,
                contentDescription = "Pending",
                tint = defaultTickColor,
                modifier = Modifier.size(13.dp)
            )
        }
        1 -> {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Sent",
                tint = defaultTickColor,
                modifier = Modifier.size(13.dp)
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
                    modifier = Modifier.size(13.dp)
                )
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = defaultTickColor,
                    modifier = Modifier
                        .padding(start = 4.dp)
                        .size(13.dp)
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
    }
}

@Composable
private fun ChatInputBar(
    text: String,
    onTextChanged: (String) -> Unit,
    onSend: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 44.dp, max = 110.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
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
