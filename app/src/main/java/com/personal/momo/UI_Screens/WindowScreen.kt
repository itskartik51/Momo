package com.personal.momo.UI_Screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.personal.momo.UI_Screens.Call.CallManager
import com.personal.momo.UI_Screens.Call.CallScreen

enum class WindowTab {
    CHAT,
    CALL
}

/**
 * Mutual Container for Chat & Call features.
 * Features a clean, centered segmented pill toggle with zero back arrow.
 * Logical back navigation is controlled smoothly via system BackHandler.
 */
@Composable
fun WindowScreen(
    onBack: () -> Unit
) {
    var selectedTab by rememberSaveable { mutableStateOf(WindowTab.CHAT) }

    BackHandler(enabled = !CallManager.isIncomingCall && !CallManager.isCallActive) {
        onBack()
    }

    if (CallManager.isIncomingCall || CallManager.isCallActive) {
        CallScreen(onBack = onBack)
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Top Bar: Clean Centered Dual-Tab Capsule (Chat | Call)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 2.dp
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        modifier = Modifier
                            .width(220.dp)
                            .height(42.dp),
                        shape = RoundedCornerShape(21.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Left Tab: Chat (Default Active)
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(
                                        if (selectedTab == WindowTab.CHAT)
                                            MaterialTheme.colorScheme.surface
                                        else
                                            Color.Transparent
                                    )
                                    .bounceClick(scaleDown = 0.94f) {
                                        selectedTab = WindowTab.CHAT
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Chat",
                                    fontSize = 14.sp,
                                    fontWeight = if (selectedTab == WindowTab.CHAT) FontWeight.Bold else FontWeight.Medium,
                                    color = if (selectedTab == WindowTab.CHAT)
                                        MaterialTheme.colorScheme.onSurface
                                    else
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // Right Tab: Call
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(
                                        if (selectedTab == WindowTab.CALL)
                                            MaterialTheme.colorScheme.surface
                                        else
                                            Color.Transparent
                                    )
                                    .bounceClick(scaleDown = 0.94f) {
                                        selectedTab = WindowTab.CALL
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Call",
                                    fontSize = 14.sp,
                                    fontWeight = if (selectedTab == WindowTab.CALL) FontWeight.Bold else FontWeight.Medium,
                                    color = if (selectedTab == WindowTab.CALL)
                                        MaterialTheme.colorScheme.onSurface
                                    else
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Tab Content Switcher
            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = {
                    fadeIn(animationSpec = tween(150)) togetherWith fadeOut(animationSpec = tween(150))
                },
                label = "WindowTabContentTransition",
                modifier = Modifier.weight(1f)
            ) { tab ->
                when (tab) {
                    WindowTab.CHAT -> {
                        ChatPlaceholderSection()
                    }
                    WindowTab.CALL -> {
                        CallScreen(onBack = onBack)
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatPlaceholderSection() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "No messages yet",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
    }
}
