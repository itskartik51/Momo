package com.personal.momo.UI_Screens.Chat

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.personal.momo.Cache.CacheManager
import com.personal.momo.UI_Screens.Chat.Preference.ChatPreferenceMenuAction
import com.personal.momo.UI_Screens.Chat.Preference.ChatPreferenceOption
import com.personal.momo.UI_Screens.Chat.Preference.NotificationModeAppliedToast
import com.personal.momo.UI_Screens.Chat.Preference.PrefCountDialog
import com.personal.momo.UI_Screens.Chat.Preference.PrefNameDialog
import com.personal.momo.UI_Screens.Chat.Preference.PrefNameManager
import com.personal.momo.UI_Screens.Chat.Preference.PrefNotificationDialog
import com.personal.momo.UI_Screens.Chat.Preference.PrefPlayer
import com.personal.momo.UI_Screens.Chat.Preference.PrefProfileDialog
import com.personal.momo.UI_Screens.Chat.Preference.PrefProfileManager
import com.personal.momo.UI_Screens.MomoPrimaryGradient
import com.personal.momo.UI_Screens.MomoRedLight
import com.personal.momo.UI_Screens.bounceClick
import kotlinx.coroutines.delay
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

private val PlusIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "Plus",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).addPath(
        pathData = PathParser().parsePathString(
            "M19 13h-6v6h-2v-6H5v-2h6V5h2v6h6v2z"
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
    val fallbackAvatarUrl by CacheManager.avatarUrlFlow.collectAsState()
    val messages by ChatRepository.messagesFlow.collectAsState()

    val isMomo = currentUserId.equals("Momo", ignoreCase = true)
    val mySenderCode = if (isMomo) 2 else 1

    val dynamicPartnerName by PrefNameManager.partnerDisplayNameFlow.collectAsState()
    val partnerDisplayName = dynamicPartnerName.ifBlank { if (isMomo) "Kanu" else "Momo" }

    val partnerChatAvatar by PrefProfileManager.partnerAvatarUrlFlow.collectAsState()
    val effectiveAvatarUrl = partnerChatAvatar.ifBlank { fallbackAvatarUrl }

    val myAvatarUrl by PrefProfileManager.myAvatarUrlFlow.collectAsState()

    val myStatuses by StatusEngine.myStatusesFlow.collectAsState()
    val partnerStatuses by StatusEngine.partnerStatusesFlow.collectAsState()

    var showMessageCountDialog by remember { mutableStateOf(false) }
    var showNotificationDialog by remember { mutableStateOf(false) }
    var showEditNameDialog by remember { mutableStateOf(false) }
    var showProfileDialog by remember { mutableStateOf(false) }
    var showFullScreenAvatar by remember { mutableStateOf(false) }

    var viewingMyStatus by remember { mutableStateOf(false) }
    var viewingPartnerStatus by remember { mutableStateOf(false) }
    var isUploadingStatus by remember { mutableStateOf(false) }
    var isAddRevealed by remember { mutableStateOf(false) }

    var toastMessage by remember { mutableStateOf<String?>(null) }
    var toastTriggerKey by remember { mutableIntStateOf(0) }

    val statusMediaPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val mimeType = context.contentResolver.getType(uri).orEmpty()
            val isVideo = mimeType.startsWith("video", ignoreCase = true)
            StatusEngine.uploadStatus(
                context = context,
                mediaUri = uri,
                isVideo = isVideo,
                caption = "",
                onProgress = { isUploadingStatus = it },
                onComplete = { isUploadingStatus = false }
            )
        }
    }

    LaunchedEffect(Unit) {
        ChatEngine.startEngine(context)
        StatusEngine.startListening(context)
        PrefNameManager.init(context)
        PrefProfileManager.init(context)
    }

    LaunchedEffect(toastTriggerKey) {
        if (toastMessage != null) {
            delay(2000L)
            toastMessage = null
        }
    }

    if (showMessageCountDialog) {
        PrefCountDialog(
            messages = messages,
            mySenderCode = mySenderCode,
            onDismiss = { showMessageCountDialog = false }
        )
    }

    if (showNotificationDialog) {
        PrefNotificationDialog(
            onDismiss = { showNotificationDialog = false },
            onModeApplied = { selectedMode ->
                showNotificationDialog = false
                toastMessage = selectedMode.appliedToastText
                toastTriggerKey++
            }
        )
    }

    if (showEditNameDialog) {
        PrefNameDialog(
            initialName = partnerDisplayName,
            onDismiss = { showEditNameDialog = false },
            onNameSaved = {
                showEditNameDialog = false
                toastMessage = "Name updated"
                toastTriggerKey++
            }
        )
    }

    if (showProfileDialog) {
        PrefProfileDialog(
            onDismiss = { showProfileDialog = false }
        )
    }

    if (showFullScreenAvatar && !effectiveAvatarUrl.isNullOrBlank()) {
        PrefPlayer(
            mediaPath = effectiveAvatarUrl,
            isVideo = false,
            onDismiss = { showFullScreenAvatar = false }
        )
    }

    if (viewingMyStatus && myStatuses.isNotEmpty()) {
        StatusViewerDialog(
            statuses = myStatuses,
            displayName = "You",
            avatarUrl = myAvatarUrl,
            onDismiss = { viewingMyStatus = false }
        )
    }

    if (viewingPartnerStatus && partnerStatuses.isNotEmpty()) {
        StatusViewerDialog(
            statuses = partnerStatuses,
            displayName = partnerDisplayName,
            avatarUrl = effectiveAvatarUrl,
            onDismiss = { viewingPartnerStatus = false }
        )
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "MomoChat",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1
                )

                ChatPreferenceMenuAction(
                    onOptionSelected = { option ->
                        when (option) {
                            ChatPreferenceOption.MESSAGE_COUNT -> {
                                showMessageCountDialog = true
                            }
                            ChatPreferenceOption.NOTIFICATION -> {
                                showNotificationDialog = true
                            }
                            ChatPreferenceOption.EDIT_NAME -> {
                                showEditNameDialog = true
                            }
                            ChatPreferenceOption.PROFILE -> {
                                showProfileDialog = true
                            }
                            else -> {}
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = "Updates",
                fontSize = 17.5.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
            )

            MyStatusRow(
                myAvatarUrl = myAvatarUrl,
                statuses = myStatuses,
                isUploading = isUploadingStatus,
                isAddRevealed = isAddRevealed,
                onRevealChange = { isAddRevealed = it },
                onAddClick = {
                    isAddRevealed = false
                    statusMediaPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                    )
                },
                onViewStatus = { viewingMyStatus = true }
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Chats",
                fontSize = 17.5.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
            )

            InboxChatRow(
                partnerName = partnerDisplayName,
                avatarUrl = effectiveAvatarUrl,
                partnerStatuses = partnerStatuses,
                lastMessage = lastMessageText,
                timestamp = lastTimestamp,
                isOutgoing = isOutgoing,
                deliveryStatus = deliveryStatus,
                unreadCount = unreadCount,
                onAvatarClick = {
                    if (partnerStatuses.isNotEmpty()) {
                        viewingPartnerStatus = true
                    } else if (!effectiveAvatarUrl.isNullOrBlank()) {
                        showFullScreenAvatar = true
                    }
                },
                onClick = onChatClick
            )
        }

        NotificationModeAppliedToast(
            message = toastMessage,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 88.dp)
        )
    }
}

@Composable
private fun MyStatusRow(
    myAvatarUrl: String?,
    statuses: List<StatusItem>,
    isUploading: Boolean,
    isAddRevealed: Boolean,
    onRevealChange: (Boolean) -> Unit,
    onAddClick: () -> Unit,
    onViewStatus: () -> Unit
) {
    val hasStatus = statuses.isNotEmpty()
    val latestStatus = statuses.lastOrNull()

    val draggableState = rememberDraggableState { delta ->
        if (hasStatus) {
            if (delta < -14f) {
                onRevealChange(true)
            } else if (delta > 14f) {
                onRevealChange(false)
            }
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .draggable(
                state = draggableState,
                orientation = Orientation.Horizontal,
                enabled = hasStatus
            )
            .bounceClick(scaleDown = 0.96f) {
                if (!hasStatus) {
                    onAddClick()
                } else {
                    if (isAddRevealed) {
                        onRevealChange(false)
                    } else {
                        onViewStatus()
                    }
                }
            },
        color = MaterialTheme.colorScheme.background
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(58.dp),
                contentAlignment = Alignment.Center
            ) {
                SegmentedStatusRing(
                    statusCount = statuses.size,
                    ringGap = 3.dp,
                    strokeWidth = 2.5.dp
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!myAvatarUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = myAvatarUrl,
                                contentDescription = "My Avatar",
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
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        if (isUploading) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.55f)),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = Color.White,
                                    strokeWidth = 2.5.dp
                                )
                            }
                        }
                    }
                }

                if (!hasStatus && !isUploading) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(21.dp)
                            .clip(CircleShape)
                            .background(MomoPrimaryGradient)
                            .border(2.dp, MaterialTheme.colorScheme.background, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = PlusIcon,
                            contentDescription = "Add Status",
                            tint = Color.White,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = if (hasStatus) "You" else "Add update",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = TextStyle(
                        platformStyle = PlatformTextStyle(includeFontPadding = false),
                        lineHeight = 19.sp
                    )
                )

                Spacer(modifier = Modifier.height(1.5.dp))

                Text(
                    text = if (hasStatus && latestStatus != null) {
                        formatStatusTime(latestStatus.timestamp)
                    } else {
                        "Disappears after 24 hours"
                    },
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Normal,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = TextStyle(
                        platformStyle = PlatformTextStyle(includeFontPadding = false),
                        lineHeight = 15.sp
                    )
                )
            }

            AnimatedVisibility(
                visible = isAddRevealed && hasStatus,
                enter = slideInHorizontally(
                    initialOffsetX = { it },
                    animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing)
                ) + fadeIn(animationSpec = tween(200)),
                exit = slideOutHorizontally(
                    targetOffsetX = { it },
                    animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing)
                ) + fadeOut(animationSpec = tween(180))
            ) {
                Box(
                    modifier = Modifier
                        .padding(start = 10.dp)
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MomoPrimaryGradient)
                        .bounceClick(scaleDown = 0.92f) {
                            onAddClick()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = PlusIcon,
                        contentDescription = "Add More Updates",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun InboxChatRow(
    partnerName: String,
    avatarUrl: String?,
    partnerStatuses: List<StatusItem>,
    lastMessage: String,
    timestamp: Long,
    isOutgoing: Boolean,
    deliveryStatus: Int,
    unreadCount: Int,
    onAvatarClick: () -> Unit,
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
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(58.dp),
                contentAlignment = Alignment.Center
            ) {
                SegmentedStatusRing(
                    statusCount = partnerStatuses.size,
                    ringGap = 3.dp,
                    strokeWidth = 2.5.dp
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                                shape = CircleShape
                            )
                            .bounceClick(scaleDown = 0.94f) {
                                onAvatarClick()
                            },
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
