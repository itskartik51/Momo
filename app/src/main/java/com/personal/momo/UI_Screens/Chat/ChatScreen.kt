package com.personal.momo.UI_Screens.Chat

import android.app.Activity
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.personal.momo.Cache.CacheManager
import com.personal.momo.UI_Screens.Chat.Preference.PrefNameManager
import com.personal.momo.UI_Screens.Chat.Preference.PrefPlayer
import com.personal.momo.UI_Screens.Chat.Preference.PrefProfileManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun ChatScreen(
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentUserId by CacheManager.appUserIdFlow.collectAsState()
    val fallbackAvatarUrl by CacheManager.avatarUrlFlow.collectAsState()

    val isMomo = currentUserId.equals("Momo", ignoreCase = true)
    val myCode = if (isMomo) 2 else 1

    val dynamicPartnerName by PrefNameManager.partnerDisplayNameFlow.collectAsState()
    val partnerDisplayName = dynamicPartnerName.ifBlank { if (isMomo) "Kanu" else "Momo" }

    val partnerChatAvatar by PrefProfileManager.partnerAvatarUrlFlow.collectAsState()
    val effectiveAvatarUrl = partnerChatAvatar.ifBlank { fallbackAvatarUrl }

    val partnerPresence by ChatEngine.partnerPresenceFlow.collectAsState()
    val messagesList by ChatRepository.messagesFlow.collectAsState()
    val reversedMessages = remember(messagesList) { messagesList.asReversed() }
    val messageMap = remember(messagesList) { messagesList.associateBy { it.id } }

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    var inputText by remember { mutableStateOf("") }
    var replyingToMessage by remember { mutableStateOf<ChatMessage?>(null) }
    var editingMessage by remember { mutableStateOf<ChatMessage?>(null) }

    var highlightedMessageId by remember { mutableStateOf<String?>(null) }
    var highlightTimerJob by remember { mutableStateOf<Job?>(null) }

    var selectedMessageIds by remember { mutableStateOf(setOf<String>()) }
    val isSelectionMode = selectedMessageIds.isNotEmpty()

    val selectedMessages = remember(selectedMessageIds, messagesList) {
        messagesList.filter { it.id in selectedMessageIds }
    }
    val canEdit = remember(selectedMessages, myCode) {
        selectedMessages.size == 1 && selectedMessages.first().senderId == myCode
    }
    val canUnsend = remember(selectedMessages, myCode) {
        selectedMessages.isNotEmpty() && selectedMessages.all { it.senderId == myCode }
    }

    var showDeleteDialog by remember { mutableStateOf(false) }
    var showFullScreenAvatar by remember { mutableStateOf(false) }

    var tempCameraUriString by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        PrefNameManager.init(context)
        PrefProfileManager.init(context)
    }

    LaunchedEffect(inputText) {
        if (inputText.trim().isNotEmpty() && editingMessage == null) {
            ChatEngine.setTyping(context, true)
            delay(2500L)
            ChatEngine.setTyping(context, false)
        } else {
            ChatEngine.setTyping(context, false)
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            val photoUri = tempCameraUriString?.let { Uri.parse(it) }
            if (photoUri != null) {
                val caption = inputText.trim()
                ChatEngine.setTyping(context, false)
                ChatEngine.sendMediaMessage(
                    context = context,
                    mediaUri = photoUri,
                    mediaType = 1,
                    caption = caption,
                    replyToId = replyingToMessage?.id,
                    replyToText = replyingToMessage?.text,
                    replyToSenderId = replyingToMessage?.senderId
                )
                inputText = ""
                replyingToMessage = null
                editingMessage = null
                coroutineScope.launch {
                    if (reversedMessages.isNotEmpty()) {
                        listState.animateScrollToItem(0)
                    }
                }
            }
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val mimeType = context.contentResolver.getType(uri) ?: ""
            val isVideo = mimeType.startsWith("video", ignoreCase = true)
            val mediaType = if (isVideo) 2 else 1
            val caption = inputText.trim()

            ChatEngine.setTyping(context, false)
            ChatEngine.sendMediaMessage(
                context = context,
                mediaUri = uri,
                mediaType = mediaType,
                caption = caption,
                replyToId = replyingToMessage?.id,
                replyToText = replyingToMessage?.text,
                replyToSenderId = replyingToMessage?.senderId
            )
            inputText = ""
            replyingToMessage = null
            editingMessage = null
            coroutineScope.launch {
                if (reversedMessages.isNotEmpty()) {
                    listState.animateScrollToItem(0)
                }
            }
        }
    }

    val imeBottom = WindowInsets.ime.getBottom(density)
    val navBarsBottom = WindowInsets.navigationBars.getBottom(density)
    val bottomInsetDp = with(density) { maxOf(imeBottom, navBarsBottom).toDp() }

    val bgColor = MaterialTheme.colorScheme.background
    SideEffect {
        val window = (context as? Activity)?.window
        if (window != null) {
            val colorInt = bgColor.toArgb()
            window.setBackgroundDrawable(ColorDrawable(colorInt))
            window.decorView.setBackgroundColor(colorInt)
        }
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
                    ChatEngine.setTyping(context, false)
                    ChatEngine.setChatScreenActive(context, false)
                }
                else -> {}
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            ChatEngine.setTyping(context, false)
            ChatEngine.setChatScreenActive(context, false)
        }
    }

    BackHandler {
        when {
            showFullScreenAvatar -> {
                showFullScreenAvatar = false
            }
            showDeleteDialog -> {
                showDeleteDialog = false
            }
            isSelectionMode -> {
                selectedMessageIds = emptySet()
            }
            editingMessage != null -> {
                editingMessage = null
                inputText = ""
            }
            replyingToMessage != null -> {
                replyingToMessage = null
            }
            else -> {
                ChatEngine.setTyping(context, false)
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
            canUnsend = canUnsend,
            onDismiss = {
                showDeleteDialog = false
            },
            onDeleteForMe = {
                val targets = selectedMessageIds
                selectedMessageIds = emptySet()
                showDeleteDialog = false
                coroutineScope.launch {
                    ChatRepository.deleteMessages(context, targets)
                }
            },
            onUnsend = {
                val targets = selectedMessageIds
                selectedMessageIds = emptySet()
                showDeleteDialog = false
                ChatEngine.unsendMessages(context, targets)
            }
        )
    }

    if (showFullScreenAvatar && !effectiveAvatarUrl.isNullOrBlank()) {
        PrefPlayer(
            mediaPath = effectiveAvatarUrl,
            isVideo = false,
            onDismiss = { showFullScreenAvatar = false }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
    ) {
        if (isSelectionMode) {
            SelectionTopHeader(
                selectedCount = selectedMessageIds.size,
                canEdit = canEdit,
                onClearSelection = { selectedMessageIds = emptySet() },
                onCopy = {
                    ChatTool.copyMessagesToClipboard(context, messagesList, selectedMessageIds)
                    selectedMessageIds = emptySet()
                },
                onEdit = {
                    val target = selectedMessages.firstOrNull()
                    if (target != null) {
                        replyingToMessage = null
                        editingMessage = target
                        inputText = target.text
                        selectedMessageIds = emptySet()
                    }
                },
                onDelete = {
                    showDeleteDialog = true
                }
            )
        } else {
            ChatTopHeader(
                partnerName = partnerDisplayName,
                avatarUrl = effectiveAvatarUrl,
                inChat = partnerPresence.inChat,
                lastSeen = partnerPresence.lastSeen,
                isTyping = partnerPresence.isTyping,
                onAvatarClick = {
                    if (!effectiveAvatarUrl.isNullOrBlank()) {
                        showFullScreenAvatar = true
                    }
                },
                onBack = {
                    ChatEngine.setTyping(context, false)
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
                .clipToBounds(),
            contentPadding = PaddingValues(top = 6.dp, bottom = 6.dp),
            verticalArrangement = Arrangement.Bottom
        ) {
            itemsIndexed(reversedMessages, key = { _, message -> message.id }) { index, message ->
                val isOutgoing = message.senderId == myCode
                val isSelected = selectedMessageIds.contains(message.id)
                val isHighlighted = message.id == highlightedMessageId
                val replyToMsg = message.replyToId?.let { messageMap[it] }

                val nextChronologicalMsg = reversedMessages.getOrNull(index - 1)
                val isNextSameSender = nextChronologicalMsg?.senderId == message.senderId
                val isNextSameGroup = isNextSameSender && (nextChronologicalMsg?.let { isSameMinute(message.timestamp, it.timestamp) } ?: false)
                val isLastInGroup = !isNextSameGroup
                val isLastInSenderGroup = !isNextSameSender

                MessageBubbleRow(
                    message = message,
                    replyToMessage = replyToMsg,
                    isOutgoing = isOutgoing,
                    isSelected = isSelected,
                    isHighlighted = isHighlighted,
                    isSelectionMode = isSelectionMode,
                    isLastInGroup = isLastInGroup,
                    isLastInSenderGroup = isLastInSenderGroup,
                    myCode = myCode,
                    partnerDisplayName = partnerDisplayName,
                    onToggleSelect = { id ->
                        selectedMessageIds = if (selectedMessageIds.contains(id)) {
                            selectedMessageIds - id
                        } else {
                            selectedMessageIds + id
                        }
                    },
                    onSwipeToReply = { targetMsg ->
                        editingMessage = null
                        replyingToMessage = targetMsg
                    },
                    onReplyClick = { targetReplyId ->
                        val targetIndex = reversedMessages.indexOfFirst { it.id == targetReplyId }
                        if (targetIndex >= 0) {
                            val isAlreadyVisible = listState.layoutInfo.visibleItemsInfo.any { it.index == targetIndex }

                            if (!isAlreadyVisible) {
                                coroutineScope.launch {
                                    val viewportHeight = listState.layoutInfo.viewportSize.height
                                    val centerOffset = -(viewportHeight / 2 - with(density) { 36.dp.roundToPx() })
                                    listState.animateScrollToItem(
                                        index = targetIndex,
                                        scrollOffset = centerOffset
                                    )
                                }
                            }

                            highlightTimerJob?.cancel()
                            highlightedMessageId = targetReplyId
                            highlightTimerJob = coroutineScope.launch {
                                delay(900L)
                                if (highlightedMessageId == targetReplyId) {
                                    highlightedMessageId = null
                                }
                            }
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
                replyingToMessage = replyingToMessage,
                onCancelReply = { replyingToMessage = null },
                editingMessage = editingMessage,
                onCancelEdit = {
                    editingMessage = null
                    inputText = ""
                },
                myCode = myCode,
                partnerDisplayName = partnerDisplayName,
                onCameraClick = {
                    val cacheFolder = File(context.cacheDir, "camera_captures").apply {
                        if (!exists()) mkdirs()
                    }
                    val photoFile = File(cacheFolder, "CAM_${System.currentTimeMillis()}.jpg")
                    val uri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.provider",
                        photoFile
                    )
                    tempCameraUriString = uri.toString()
                    cameraLauncher.launch(uri)
                },
                onGalleryClick = {
                    galleryLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                    )
                },
                onSend = {
                    val cleanText = inputText.trim()
                    if (cleanText.isNotEmpty()) {
                        val activeEdit = editingMessage
                        if (activeEdit != null) {
                            ChatEngine.editMessage(context, activeEdit.id, cleanText)
                            editingMessage = null
                            inputText = ""
                        } else {
                            ChatEngine.setTyping(context, false)
                            ChatEngine.sendMessage(
                                context = context,
                                text = cleanText,
                                replyToId = replyingToMessage?.id,
                                replyToText = null,
                                replyToSenderId = null
                            )
                            inputText = ""
                            replyingToMessage = null
                            coroutineScope.launch {
                                if (reversedMessages.isNotEmpty()) {
                                    listState.animateScrollToItem(0)
                                }
                            }
                        }
                    }
                }
            )
        }
    }
}
