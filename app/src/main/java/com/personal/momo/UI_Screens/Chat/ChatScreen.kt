package com.personal.momo.UI_Screens.Chat

import android.app.Activity
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.view.ViewTreeObserver
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.personal.momo.Cache.CacheManager
import com.personal.momo.UI_Screens.Chat.Preference.ChatWallpaperBackground
import com.personal.momo.UI_Screens.Chat.Preference.PrefNameManager
import com.personal.momo.UI_Screens.Chat.Preference.PrefPlayer
import com.personal.momo.UI_Screens.Chat.Preference.PrefPlayerUpdate
import com.personal.momo.UI_Screens.Chat.Preference.PrefProfileManager
import com.personal.momo.UI_Screens.Chat.Preference.PrefWallpaperManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

private val DatePillBackgroundColor = Color(0xFF1B1C24).copy(alpha = 0.90f)

@Composable
fun ChatScreen(
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val view = LocalView.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    val currentUserId by CacheManager.appUserIdFlow.collectAsState()
    val fallbackAvatarUrl by CacheManager.avatarUrlFlow.collectAsState()

    val isMomo = currentUserId.equals("Momo", ignoreCase = true)
    val myCode = if (isMomo) 2 else 1

    val dynamicPartnerName by PrefNameManager.partnerDisplayNameFlow.collectAsState()
    val partnerDisplayName = dynamicPartnerName.ifBlank { if (isMomo) "Kanu" else "Momo" }

    val partnerChatAvatar by PrefProfileManager.partnerAvatarUrlFlow.collectAsState()
    val effectiveAvatarUrl = partnerChatAvatar.ifBlank { fallbackAvatarUrl }
    val myAvatarUrl by PrefProfileManager.myAvatarUrlFlow.collectAsState()

    val myStatuses by StatusEngine.myStatusesFlow.collectAsState()
    val partnerStatuses by StatusEngine.partnerStatusesFlow.collectAsState()
    val allActiveStatusIds = remember(myStatuses, partnerStatuses) {
        (myStatuses + partnerStatuses).flatMap { st ->
            val prefixed = if (st.id.startsWith("STATUS_")) st.id else "STATUS_${st.id}"
            listOf(st.id, prefixed)
        }.toSet()
    }

    val partnerPresence by ChatEngine.partnerPresenceFlow.collectAsState()
    val messagesList by ChatRepository.messagesFlow.collectAsState()
    val reversedMessages = remember(messagesList) { messagesList.asReversed() }
    val messageMap = remember(messagesList) { messagesList.associateBy { it.id } }

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    var inputTextFieldValue by remember { mutableStateOf(TextFieldValue("")) }
    var replyingToMessage by remember { mutableStateOf<ChatMessage?>(null) }
    var editingMessage by remember { mutableStateOf<ChatMessage?>(null) }

    var isEmojiPickerOpen by rememberSaveable { mutableStateOf(false) }

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
    var viewingSingleStatus by remember { mutableStateOf<StatusItem?>(null) }

    var tempCameraUriString by rememberSaveable { mutableStateOf<String?>(null) }

    val imeBottom = WindowInsets.ime.getBottom(density)
    val navBarsBottom = WindowInsets.navigationBars.getBottom(density)
    val imeBottomDp = with(density) { imeBottom.toDp() }
    val bottomInsetDp = with(density) { maxOf(imeBottom, navBarsBottom).toDp() }

    DisposableEffect(view) {
        val listener = ViewTreeObserver.OnGlobalLayoutListener {
            val rect = Rect()
            view.getWindowVisibleDisplayFrame(rect)
            val screenHeight = view.rootView.height
            val keypadHeight = screenHeight - rect.bottom
            if (keypadHeight > screenHeight * 0.15) {
                val keyboardDp = keypadHeight / density.density
                KeyboardHeightHelper.saveKeyboardHeight(context, keyboardDp)
                if (isEmojiPickerOpen) {
                    isEmojiPickerOpen = false
                }
            }
        }
        view.viewTreeObserver.addOnGlobalLayoutListener(listener)
        onDispose {
            view.viewTreeObserver.removeOnGlobalLayoutListener(listener)
        }
    }

    LaunchedEffect(imeBottomDp) {
        if (imeBottomDp > 120.dp) {
            KeyboardHeightHelper.saveKeyboardHeight(context, imeBottomDp.value)
            if (isEmojiPickerOpen) {
                isEmojiPickerOpen = false
            }
        }
    }

    val stickyTopDateLabel by remember(reversedMessages, density) {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val visibleItems = layoutInfo.visibleItemsInfo
            if (visibleItems.isEmpty() || reversedMessages.isEmpty()) {
                null
            } else {
                val viewportTop = layoutInfo.viewportEndOffset
                val topZonePx = with(density) { 52.dp.roundToPx() }
                val pillHeightPx = with(density) { 38.dp.roundToPx() }

                val reachesTopOfScreen = visibleItems.any { it.offset + it.size >= viewportTop - topZonePx }
                if (!reachesTopOfScreen) {
                    null
                } else {
                    val hasInlinePillAtTop = visibleItems.any { itemInfo ->
                        val idx = itemInfo.index
                        val msg = reversedMessages.getOrNull(idx)
                        val olderMsg = reversedMessages.getOrNull(idx + 1)
                        val isFirstOfDay = msg != null && (olderMsg == null || !isSameDay(msg.timestamp, olderMsg.timestamp))
                        if (isFirstOfDay) {
                            val itemTopEdge = itemInfo.offset + itemInfo.size
                            itemTopEdge in (viewportTop - topZonePx)..(viewportTop + pillHeightPx)
                        } else {
                            false
                        }
                    }

                    if (hasInlinePillAtTop) {
                        null
                    } else {
                        val topItem = visibleItems
                            .filter { it.offset < viewportTop }
                            .maxByOrNull { it.index }
                            ?: visibleItems.maxByOrNull { it.index }

                        val topMsg = topItem?.let { reversedMessages.getOrNull(it.index) }
                        topMsg?.let { formatChatDateHeader(it.timestamp) }
                    }
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        PrefNameManager.init(context)
        PrefProfileManager.init(context)
        PrefWallpaperManager.init(context)
        StatusEngine.startListening(context)
    }

    LaunchedEffect(allActiveStatusIds) {
        if (allActiveStatusIds.isNotEmpty()) {
            ChatRepository.purgeExpiredStatusQuotes(context, allActiveStatusIds)
        } else {
            delay(2000L)
            val latestStatuses = StatusEngine.myStatusesFlow.value + StatusEngine.partnerStatusesFlow.value
            if (latestStatuses.isEmpty()) {
                ChatRepository.purgeExpiredStatusQuotes(context, emptySet())
            }
        }
    }

    LaunchedEffect(inputTextFieldValue.text) {
        if (inputTextFieldValue.text.trim().isNotEmpty() && editingMessage == null) {
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
                val caption = inputTextFieldValue.text.trim()
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
                inputTextFieldValue = TextFieldValue("")
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
            val caption = inputTextFieldValue.text.trim()

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
            inputTextFieldValue = TextFieldValue("")
            replyingToMessage = null
            editingMessage = null
            coroutineScope.launch {
                if (reversedMessages.isNotEmpty()) {
                    listState.animateScrollToItem(0)
                }
            }
        }
    }

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
            isEmojiPickerOpen -> {
                isEmojiPickerOpen = false
            }
            viewingSingleStatus != null -> {
                viewingSingleStatus = null
            }
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
                inputTextFieldValue = TextFieldValue("")
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

    if (viewingSingleStatus != null) {
        val st = viewingSingleStatus!!
        val isOwn = st.senderId == myCode
        PrefPlayerUpdate(
            statuses = listOf(st),
            displayName = if (isOwn) "You" else partnerDisplayName,
            avatarUrl = if (isOwn) myAvatarUrl else effectiveAvatarUrl,
            isOwnStatus = isOwn,
            onReplySend = if (!isOwn) {
                { statusItem, replyText ->
                    val taggedStatusId = if (statusItem.id.startsWith("STATUS_")) statusItem.id else "STATUS_${statusItem.id}"
                    ChatEngine.sendMessage(
                        context = context,
                        text = replyText,
                        replyToId = taggedStatusId,
                        replyToText = statusItem.caption.ifBlank { if (statusItem.mediaType == 2) "Video" else "Photo" },
                        replyToSenderId = statusItem.senderId
                    )
                }
            } else null,
            onDismiss = { viewingSingleStatus = null }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
    ) {
        ChatWallpaperBackground(
            modifier = Modifier.fillMaxSize()
        )

        Column(
            modifier = Modifier.fillMaxSize()
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
                            inputTextFieldValue = TextFieldValue(
                                text = target.text,
                                selection = TextRange(target.text.length)
                            )
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

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clipToBounds()
            ) {
                LazyColumn(
                    state = listState,
                    reverseLayout = true,
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            if (isEmojiPickerOpen) {
                                isEmojiPickerOpen = false
                            }
                            focusManager.clearFocus()
                            keyboardController?.hide()
                        },
                    contentPadding = PaddingValues(top = 8.dp, bottom = 6.dp),
                    verticalArrangement = Arrangement.Bottom
                ) {
                    itemsIndexed(reversedMessages, key = { _, message -> message.id }) { index, message ->
                        val isOutgoing = message.senderId == myCode
                        val isSelected = selectedMessageIds.contains(message.id)
                        val isHighlighted = message.id == highlightedMessageId
                        val replyToMsg = message.replyToId?.let { messageMap[it] }

                        val replyToStatus = remember(message.replyToId, myStatuses, partnerStatuses) {
                            val rid = message.replyToId
                            if (!rid.isNullOrBlank()) {
                                val rawId = rid.removePrefix("STATUS_")
                                partnerStatuses.find { it.id == rid || it.id == rawId }
                                    ?: myStatuses.find { it.id == rid || it.id == rawId }
                            } else null
                        }

                        val isStatusActive = replyToStatus != null

                        val prevChronologicalMsg = reversedMessages.getOrNull(index + 1)
                        val isFirstInDay = prevChronologicalMsg == null || !isSameDay(message.timestamp, prevChronologicalMsg.timestamp)
                        val isFirstInSenderGroup = isFirstInDay || prevChronologicalMsg?.senderId != message.senderId

                        val nextChronologicalMsg = reversedMessages.getOrNull(index - 1)
                        val isDayChangedToNext = nextChronologicalMsg == null || !isSameDay(message.timestamp, nextChronologicalMsg.timestamp)
                        val isNextSameSender = !isDayChangedToNext && nextChronologicalMsg?.senderId == message.senderId
                        val isNextSameGroup = isNextSameSender && (nextChronologicalMsg?.let { isSameMinute(message.timestamp, it.timestamp) } ?: false)
                        val isLastInGroup = !isNextSameGroup
                        val isLastInSenderGroup = !isNextSameSender

                        Column(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (isFirstInDay) {
                                val dateHeaderText = remember(message.timestamp) {
                                    formatChatDateHeader(message.timestamp)
                                }
                                if (dateHeaderText.isNotEmpty()) {
                                    DateSeparatorPill(
                                        text = dateHeaderText,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(
                                                top = if (index == reversedMessages.lastIndex) 4.dp else 12.dp,
                                                bottom = 10.dp
                                            )
                                    )
                                }
                            }

                            MessageBubbleRow(
                                message = message,
                                replyToMessage = replyToMsg,
                                replyToStatus = replyToStatus,
                                isStatusActive = isStatusActive,
                                isOutgoing = isOutgoing,
                                isSelected = isSelected,
                                isHighlighted = isHighlighted,
                                isSelectionMode = isSelectionMode,
                                isFirstInSenderGroup = isFirstInSenderGroup,
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
                                    val rawTargetId = targetReplyId.removePrefix("STATUS_")
                                    val foundStatus = partnerStatuses.find { it.id == targetReplyId || it.id == rawTargetId }
                                        ?: myStatuses.find { it.id == targetReplyId || it.id == rawTargetId }

                                    if (foundStatus != null) {
                                        viewingSingleStatus = foundStatus
                                    } else if (!targetReplyId.startsWith("STATUS_")) {
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
                                }
                            )
                        }
                    }
                }

                StickyDateHeaderOverlay(
                    dateText = stickyTopDateLabel,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 8.dp)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = if (isEmojiPickerOpen) 0.dp else bottomInsetDp)
            ) {
                ChatInputBar(
                    textFieldValue = inputTextFieldValue,
                    onTextFieldValueChange = { inputTextFieldValue = it },
                    isEmojiPickerOpen = isEmojiPickerOpen,
                    onToggleEmojiPicker = {
                        if (isEmojiPickerOpen) {
                            isEmojiPickerOpen = false
                            keyboardController?.show()
                        } else {
                            focusManager.clearFocus()
                            keyboardController?.hide()
                            isEmojiPickerOpen = true
                        }
                    },
                    onInputClick = {
                        if (isEmojiPickerOpen) {
                            isEmojiPickerOpen = false
                        }
                    },
                    replyingToMessage = replyingToMessage,
                    onCancelReply = { replyingToMessage = null },
                    editingMessage = editingMessage,
                    onCancelEdit = {
                        editingMessage = null
                        inputTextFieldValue = TextFieldValue("")
                    },
                    myCode = myCode,
                    partnerDisplayName = partnerDisplayName,
                    onCameraClick = {
                        if (isEmojiPickerOpen) {
                            isEmojiPickerOpen = false
                        }
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
                        if (isEmojiPickerOpen) {
                            isEmojiPickerOpen = false
                        }
                        galleryLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                        )
                    },
                    onSend = {
                        val cleanText = inputTextFieldValue.text.trim()
                        if (cleanText.isNotEmpty()) {
                            val activeEdit = editingMessage
                            if (activeEdit != null) {
                                ChatEngine.editMessage(context, activeEdit.id, cleanText)
                                editingMessage = null
                                inputTextFieldValue = TextFieldValue("")
                            } else {
                                ChatEngine.setTyping(context, false)
                                ChatEngine.sendMessage(
                                    context = context,
                                    text = cleanText,
                                    replyToId = replyingToMessage?.id,
                                    replyToText = null,
                                    replyToSenderId = null
                                )
                                inputTextFieldValue = TextFieldValue("")
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

                if (isEmojiPickerOpen) {
                    ChatEmojiPicker(
                        heightDp = KeyboardHeightHelper.getCachedKeyboardHeight(context),
                        onEmojiClick = { emojiStr ->
                            val currentText = inputTextFieldValue.text
                            val selection = inputTextFieldValue.selection
                            val start = selection.min
                            val end = selection.max
                            val newText = currentText.replaceRange(start, end, emojiStr)
                            val newCursor = start + emojiStr.length
                            inputTextFieldValue = TextFieldValue(
                                text = newText,
                                selection = TextRange(newCursor)
                            )
                        },
                        onBackspaceClick = {
                            val currentText = inputTextFieldValue.text
                            val selection = inputTextFieldValue.selection
                            if (selection.min != selection.max) {
                                val newText = currentText.removeRange(selection.min, selection.max)
                                inputTextFieldValue = TextFieldValue(
                                    text = newText,
                                    selection = TextRange(selection.min)
                                )
                            } else if (selection.min > 0) {
                                val cursor = selection.min
                                val codePointBefore = Character.codePointBefore(currentText, cursor)
                                val charCount = Character.charCount(codePointBefore)
                                val newText = currentText.substring(0, cursor - charCount) + currentText.substring(cursor)
                                inputTextFieldValue = TextFieldValue(
                                    text = newText,
                                    selection = TextRange(cursor - charCount)
                                )
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun StickyDateHeaderOverlay(
    dateText: String?,
    modifier: Modifier = Modifier
) {
    var displayedText by remember { mutableStateOf("") }
    if (!dateText.isNullOrEmpty()) {
        displayedText = dateText
    }

    AnimatedVisibility(
        visible = !dateText.isNullOrEmpty(),
        enter = fadeIn(animationSpec = tween(150)),
        exit = fadeOut(animationSpec = tween(150)),
        modifier = modifier
    ) {
        DateSeparatorPill(
            text = displayedText
        )
    }
}

@Composable
private fun DateSeparatorPill(
    text: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = DatePillBackgroundColor,
            shadowElevation = 2.dp
        ) {
            Text(
                text = text,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White.copy(alpha = 0.75f),
                modifier = Modifier.padding(horizontal = 11.dp, vertical = 4.5.dp),
                style = TextStyle(
                    platformStyle = PlatformTextStyle(includeFontPadding = false),
                    lineHeight = 14.sp
                )
            )
        }
    }
}

private fun isSameDay(t1: Long, t2: Long): Boolean {
    if (t1 <= 0L || t2 <= 0L) return false
    return try {
        val d1 = Instant.ofEpochMilli(t1).atZone(ZoneId.systemDefault()).toLocalDate()
        val d2 = Instant.ofEpochMilli(t2).atZone(ZoneId.systemDefault()).toLocalDate()
        d1.isEqual(d2)
    } catch (_: Exception) {
        false
    }
}

private fun formatChatDateHeader(timestamp: Long): String {
    if (timestamp <= 0L) return ""
    return try {
        val msgDate = Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).toLocalDate()
        val today = LocalDate.now()
        val daysDiff = ChronoUnit.DAYS.between(msgDate, today)

        when {
            msgDate.isEqual(today) -> "Today"
            msgDate.isEqual(today.minusDays(1)) -> "Yesterday"
            daysDiff in 2..6 -> {
                val dayFormatter = DateTimeFormatter.ofPattern("EEEE", Locale.ENGLISH)
                msgDate.format(dayFormatter)
            }
            else -> {
                val dateFormatter = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH)
                msgDate.format(dateFormatter)
            }
        }
    } catch (_: Exception) {
        ""
    }
}
