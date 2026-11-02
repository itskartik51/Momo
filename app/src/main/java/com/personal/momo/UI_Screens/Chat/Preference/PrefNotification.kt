package com.personal.momo.UI_Screens.Chat.Preference

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.Person
import androidx.core.content.ContextCompat
import com.personal.momo.UI_Screens.Chat.ChatMessage
import com.personal.momo.UI_Screens.Chat.ChatRepository
import com.personal.momo.UI_Screens.MomoPrimaryGradient
import com.personal.momo.UI_Screens.bounceClick
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class ChatNotificationMode(
    val storageKey: String,
    val title: String,
    val appliedToastText: String
) {
    ALLOW(
        storageKey = "allow",
        title = "Allow",
        appliedToastText = "Allow mode applied"
    ),
    PRIVACY(
        storageKey = "privacy",
        title = "Privacy",
        appliedToastText = "Privacy mode applied"
    ),
    MUTE(
        storageKey = "mute",
        title = "Mute",
        appliedToastText = "Mute mode applied"
    );

    companion object {
        fun fromKey(key: String?): ChatNotificationMode {
            return entries.firstOrNull { it.storageKey.equals(key, ignoreCase = true) } ?: ALLOW
        }
    }
}

object ChatNotificationManager {
    private const val PREFS_NAME = "momo_chat_preferences"
    private const val KEY_NOTIFICATION_MODE = "chat_notification_mode"
    private const val CHANNEL_ID = "momo_chat_messages_high_v1"
    private const val CHANNEL_NAME = "MomoChat Messages"
    private const val NOTIFICATION_ID = 4091

    fun getNotificationMode(context: Context): ChatNotificationMode {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedKey = prefs.getString(KEY_NOTIFICATION_MODE, ChatNotificationMode.ALLOW.storageKey)
        return ChatNotificationMode.fromKey(savedKey)
    }

    fun setNotificationMode(context: Context, mode: ChatNotificationMode) {
        val appContext = context.applicationContext
        val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_NOTIFICATION_MODE, mode.storageKey).apply()

        if (mode == ChatNotificationMode.MUTE) {
            clearNotifications(appContext)
        }
    }

    private fun ensureChannelCreated(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
            val existing = manager.getNotificationChannel(CHANNEL_ID)
            if (existing == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "High priority notifications for incoming MomoChat messages"
                    enableVibration(true)
                    setShowBadge(true)
                }
                manager.createNotificationChannel(channel)
            }
        }
    }

    fun clearNotifications(context: Context) {
        try {
            NotificationManagerCompat.from(context.applicationContext).cancel(NOTIFICATION_ID)
        } catch (_: Exception) {}
    }

    fun onIncomingMessage(context: Context, mySenderCode: Int) {
        val appContext = context.applicationContext
        val mode = getNotificationMode(appContext)
        if (mode == ChatNotificationMode.MUTE) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPermission = ContextCompat.checkSelfPermission(
                appContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!hasPermission) return
        }

        val unreadMessages = ChatRepository.messagesFlow.value
            .filter { it.senderId != mySenderCode && it.status < 3 }
            .sortedBy { it.timestamp }

        if (unreadMessages.isEmpty()) return

        ensureChannelCreated(appContext)

        val partnerName = PrefNameManager.getPartnerDisplayName(appContext)
        val myName = if (mySenderCode == 2) "Momo" else "Kanu"

        val launchIntent = appContext.packageManager
            .getLaunchIntentForPackage(appContext.packageName)
            ?.apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
            }

        val contentPendingIntent = if (launchIntent != null) {
            PendingIntent.getActivity(
                appContext,
                NOTIFICATION_ID,
                launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        } else null

        val smallIconRes = appContext.applicationInfo.icon.takeIf { it != 0 }
            ?: android.R.drawable.sym_action_chat

        val builder = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(smallIconRes)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .setOnlyAlertOnce(false)
            .setWhen(unreadMessages.last().timestamp)
            .setShowWhen(true)

        if (contentPendingIntent != null) {
            builder.setContentIntent(contentPendingIntent)
        }

        when (mode) {
            ChatNotificationMode.PRIVACY -> {
                val count = unreadMessages.size
                val privacyTitle = if (count == 1) {
                    "1 new notification"
                } else {
                    "$count new notifications"
                }

                builder
                    .setContentTitle(privacyTitle)
                    .setContentText(null)
                    .setNumber(count)
                    .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            }

            ChatNotificationMode.ALLOW -> {
                val mePerson = Person.Builder()
                    .setName(myName)
                    .setKey("user_$mySenderCode")
                    .build()

                val partnerPerson = Person.Builder()
                    .setName(partnerName)
                    .setKey("partner_${if (mySenderCode == 1) 2 else 1}")
                    .build()

                val messagingStyle = NotificationCompat.MessagingStyle(mePerson)
                    .setGroupConversation(false)

                for (msg in unreadMessages.takeLast(15)) {
                    messagingStyle.addMessage(
                        NotificationCompat.MessagingStyle.Message(
                            formatNotificationMessageText(msg),
                            msg.timestamp,
                            partnerPerson
                        )
                    )
                }

                val latestText = formatNotificationMessageText(unreadMessages.last())

                builder
                    .setContentTitle(partnerName)
                    .setContentText(latestText)
                    .setStyle(messagingStyle)
                    .setNumber(unreadMessages.size)
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

                if (unreadMessages.size > 1) {
                    builder.setSubText("${unreadMessages.size} new messages")
                }
            }

            ChatNotificationMode.MUTE -> return
        }

        try {
            NotificationManagerCompat.from(appContext).notify(NOTIFICATION_ID, builder.build())
        } catch (_: SecurityException) {
        } catch (_: Exception) {}
    }

    private fun formatNotificationMessageText(message: ChatMessage): String {
        return when (message.mediaType) {
            1 -> if (message.text.isNotBlank()) "📷 ${message.text}" else "📷 Photo"
            2 -> if (message.text.isNotBlank()) "🎥 ${message.text}" else "🎥 Video"
            else -> message.text.ifBlank { "New message" }
        }
    }
}

private val DialogSurfaceColor = Color(0xFF1E1F22)
private val CapsuleTrackColor = Color(0xFF151619)

private val BellHeaderIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "BellHeaderIcon",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).addPath(
        pathData = PathParser().parsePathString(
            "M12 22c1.1 0 2-.9 2-2h-4c0 1.1.9 2 2 2zm6-6v-5c0-3.07-1.63-5.64-4.5-6.32V4c0-.83-.67-1.5-1.5-1.5s-1.5.67-1.5 1.5v.68C7.64 5.36 6 7.92 6 11v5l-2 2v1h16v-1l-2-2z"
        ).toNodes(),
        fill = SolidColor(Color.White)
    ).build()
}

private val ClosePopupIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "ClosePopupIcon",
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
fun PrefNotificationDialog(
    onDismiss: () -> Unit,
    onModeApplied: (ChatNotificationMode) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedMode by remember {
        mutableStateOf(ChatNotificationManager.getNotificationMode(context))
    }
    var isClosing by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            shape = RoundedCornerShape(22.dp),
            color = DialogSurfaceColor,
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.09f)),
            shadowElevation = 14.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(MomoPrimaryGradient),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = BellHeaderIcon,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(11.dp))

                        Text(
                            text = "Notification",
                            fontSize = 17.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.07f))
                            .bounceClick(scaleDown = 0.94f) {
                                onDismiss()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = ClosePopupIcon,
                            contentDescription = "Close",
                            tint = Color.White.copy(alpha = 0.75f),
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(CircleShape)
                        .background(CapsuleTrackColor)
                        .padding(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ChatNotificationMode.entries.forEach { mode ->
                            val isSelected = selectedMode == mode

                            val pillScale by animateFloatAsState(
                                targetValue = if (isSelected) 1f else 0.85f,
                                animationSpec = tween(durationMillis = 150, easing = FastOutSlowInEasing),
                                label = "ModePillScale_${mode.name}"
                            )

                            val pillAlpha by animateFloatAsState(
                                targetValue = if (isSelected) 1f else 0f,
                                animationSpec = tween(durationMillis = 140),
                                label = "ModePillAlpha_${mode.name}"
                            )

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clip(CircleShape)
                                    .bounceClick(scaleDown = 0.94f) {
                                        if (!isClosing) {
                                            isClosing = true
                                            selectedMode = mode
                                            ChatNotificationManager.setNotificationMode(context, mode)
                                            scope.launch {
                                                delay(140L)
                                                onModeApplied(mode)
                                            }
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (pillAlpha > 0.01f) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .graphicsLayer {
                                                scaleX = pillScale
                                                scaleY = pillScale
                                                alpha = pillAlpha
                                            }
                                            .clip(CircleShape)
                                            .background(MomoPrimaryGradient)
                                    )
                                }

                                Text(
                                    text = mode.title,
                                    fontSize = 13.5.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color.White else Color.White.copy(alpha = 0.60f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationModeAppliedToast(
    message: String?,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = !message.isNullOrBlank(),
        enter = fadeIn(animationSpec = tween(180)) + slideInVertically(
            initialOffsetY = { it / 2 },
            animationSpec = tween(200, easing = FastOutSlowInEasing)
        ),
        exit = fadeOut(animationSpec = tween(180)) + slideOutVertically(
            targetOffsetY = { it / 2 },
            animationSpec = tween(180)
        ),
        modifier = modifier
    ) {
        Surface(
            shape = CircleShape,
            color = DialogSurfaceColor,
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.10f)),
            shadowElevation = 10.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(MomoPrimaryGradient),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(13.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Text(
                    text = message ?: "",
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White.copy(alpha = 0.95f)
                )
            }
        }
    }
}
