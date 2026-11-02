package com.personal.momo.UI_Screens.Chat.Preference

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.personal.momo.UI_Screens.bounceClick

enum class ChatPreferenceOption {
    MESSAGE_COUNT,
    BACKUP_RESTORE,
    NOTIFICATION,
    MEDIA_AND_DOCS,
    EDIT_NAME,
    PROFILE
}

data class PreferenceMenuItem(
    val option: ChatPreferenceOption,
    val title: String,
    val icon: ImageVector
)

private val PreferencePopupSurfaceColor = Color(0xFF1E1F22)

private val TwoLineMenuIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "TwoLineMenuIcon",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).addPath(
        pathData = PathParser().parsePathString(
            "M4 8.5h16c.55 0 1 .45 1 1s-.45 1-1 1H4c-.55 0-1-.45-1-1s.45-1 1-1zm0 5h16c.55 0 1 .45 1 1s-.45 1-1 1H4c-.55 0-1-.45-1-1s.45-1 1-1z"
        ).toNodes(),
        fill = SolidColor(Color.White)
    ).build()
}

private val MessageCountIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "MessageCountIcon",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).addPath(
        pathData = PathParser().parsePathString(
            "M20 2H4c-1.1 0-2 .9-2 2v18l4-4h14c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2zm0 14H5.17L4 17.17V4h16v12zM7 9h2v2H7V9zm4 0h2v2h-2V9zm4 0h2v2h-2V9z"
        ).toNodes(),
        fill = SolidColor(Color.White)
    ).build()
}

private val BackupRestoreIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "BackupRestoreIcon",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).addPath(
        pathData = PathParser().parsePathString(
            "M19.35 10.04C18.67 6.59 15.64 4 12 4 9.11 4 6.6 5.64 5.35 8.04 2.34 8.36 0 10.91 0 14c0 3.31 2.69 6 6 6h13c2.76 0 5-2.24 5-5 0-2.64-2.05-4.78-4.65-4.96zM19 18H6c-2.21 0-4-1.79-4-4 0-2.05 1.53-3.76 3.56-3.97l1.07-.11.5-.95C8.08 7.14 9.94 6 12 6c2.62 0 4.88 1.86 5.39 4.43l.3 1.5 1.53.11c1.56.1 2.78 1.41 2.78 2.96 0 1.65-1.35 3-3 3zM8 13h2.55v3h2.9v-3H16l-4-4-4 4z"
        ).toNodes(),
        fill = SolidColor(Color.White)
    ).build()
}

private val NotificationIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "NotificationIcon",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).addPath(
        pathData = PathParser().parsePathString(
            "M12 22c1.1 0 2-.9 2-2h-4c0 1.1.9 2 2 2zm6-6v-5c0-3.07-1.63-5.64-4.5-6.32V4c0-.83-.67-1.5-1.5-1.5s-1.5.67-1.5 1.5v.68C7.64 5.36 6 7.92 6 11v5l-2 2v1h16v-1l-2-2zm-2 1H8v-6c0-2.48 1.51-4.5 4-4.5s4 2.02 4 4.5v6z"
        ).toNodes(),
        fill = SolidColor(Color.White)
    ).build()
}

private val MediaDocsIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "MediaDocsIcon",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).addPath(
        pathData = PathParser().parsePathString(
            "M19 5v14H5V5h14m0-2H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zm-4.86 8.86l-3 3.87L9 13.14 6 17h12l-3.86-5.14z"
        ).toNodes(),
        fill = SolidColor(Color.White)
    ).build()
}

private val EditNameIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "EditNameIcon",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).addPath(
        pathData = PathParser().parsePathString(
            "M3 17.25V21h3.75L17.81 9.94l-3.75-3.75L3 17.25zM20.71 7.04c.39-.39.39-1.02 0-1.41l-2.34-2.34c-.39-.39-1.02-.39-1.41 0l-1.83 1.83 3.75 3.75 1.83-1.83z"
        ).toNodes(),
        fill = SolidColor(Color.White)
    ).build()
}

private val ProfileIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "ProfileIcon",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).addPath(
        pathData = PathParser().parsePathString(
            "M12 6c1.1 0 2 .9 2 2s-.9 2-2 2-2-.9-2-2 .9-2 2-2m0 10c2.7 0 5.8 1.29 6 2H6c.23-.72 3.31-2 6-2m0-12C9.79 4 8 5.79 8 8s1.79 4 4 4 4-1.79 4-4-1.79-4-4-4zm0 10c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z"
        ).toNodes(),
        fill = SolidColor(Color.White)
    ).build()
}

private val DefaultPreferenceItems: List<PreferenceMenuItem> by lazy {
    listOf(
        PreferenceMenuItem(
            option = ChatPreferenceOption.MESSAGE_COUNT,
            title = "Message Count",
            icon = MessageCountIcon
        ),
        PreferenceMenuItem(
            option = ChatPreferenceOption.BACKUP_RESTORE,
            title = "Backup & Restore",
            icon = BackupRestoreIcon
        ),
        PreferenceMenuItem(
            option = ChatPreferenceOption.NOTIFICATION,
            title = "Notification",
            icon = NotificationIcon
        ),
        PreferenceMenuItem(
            option = ChatPreferenceOption.MEDIA_AND_DOCS,
            title = "Media and Docs",
            icon = MediaDocsIcon
        ),
        PreferenceMenuItem(
            option = ChatPreferenceOption.EDIT_NAME,
            title = "Edit Name",
            icon = EditNameIcon
        ),
        PreferenceMenuItem(
            option = ChatPreferenceOption.PROFILE,
            title = "Profile",
            icon = ProfileIcon
        )
    )
}

@Composable
fun ChatPreferenceMenuAction(
    onOptionSelected: (ChatPreferenceOption) -> Unit = {}
) {
    val density = LocalDensity.current
    var isMenuExpanded by remember { mutableStateOf(false) }

    val transitionState = remember { MutableTransitionState(false) }
    transitionState.targetState = isMenuExpanded

    Box(
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .bounceClick(scaleDown = 0.94f) {
                    isMenuExpanded = !isMenuExpanded
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = TwoLineMenuIcon,
                contentDescription = "Preferences",
                tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f),
                modifier = Modifier.size(23.dp)
            )
        }

        if (transitionState.currentState || transitionState.targetState) {
            val popupOffsetY = with(density) { 44.dp.roundToPx() }

            Popup(
                alignment = Alignment.TopEnd,
                offset = IntOffset(x = 0, y = popupOffsetY),
                onDismissRequest = { isMenuExpanded = false },
                properties = PopupProperties(focusable = true)
            ) {
                val transition = updateTransition(
                    transitionState = transitionState,
                    label = "WhatsAppStyleMenuTransition"
                )

                val scale by transition.animateFloat(
                    transitionSpec = {
                        if (false isTransitioningTo true) {
                            tween(durationMillis = 165, easing = FastOutSlowInEasing)
                        } else {
                            tween(durationMillis = 125, easing = FastOutSlowInEasing)
                        }
                    },
                    label = "MenuScale"
                ) { expanded ->
                    if (expanded) 1f else 0.78f
                }

                val alpha by transition.animateFloat(
                    transitionSpec = {
                        if (false isTransitioningTo true) {
                            tween(durationMillis = 140)
                        } else {
                            tween(durationMillis = 115)
                        }
                    },
                    label = "MenuAlpha"
                ) { expanded ->
                    if (expanded) 1f else 0f
                }

                Surface(
                    modifier = Modifier
                        .width(190.dp)
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            this.alpha = alpha
                            transformOrigin = TransformOrigin(1f, 0f)
                        },
                    shape = RoundedCornerShape(16.dp),
                    color = PreferencePopupSurfaceColor,
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                    shadowElevation = 10.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                    ) {
                        DefaultPreferenceItems.forEach { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .bounceClick(scaleDown = 0.94f) {
                                        isMenuExpanded = false
                                        onOptionSelected(item.option)
                                    }
                                    .padding(horizontal = 15.dp, vertical = 11.5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Start
                            ) {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.title,
                                    tint = Color.White.copy(alpha = 0.90f),
                                    modifier = Modifier.size(19.5.dp)
                                )

                                Spacer(modifier = Modifier.width(13.dp))

                                Text(
                                    text = item.title,
                                    fontSize = 14.5.sp,
                                    fontWeight = FontWeight.Normal,
                                    color = Color.White.copy(alpha = 0.94f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
