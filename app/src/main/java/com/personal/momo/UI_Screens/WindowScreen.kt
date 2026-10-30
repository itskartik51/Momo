package com.personal.momo.UI_Screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.personal.momo.UI_Screens.Call.CallManager
import com.personal.momo.UI_Screens.Call.CallScreen
import com.personal.momo.UI_Screens.Chat.ChatEngine
import com.personal.momo.UI_Screens.Chat.ChatScreen
import com.personal.momo.UI_Screens.Chat.InboxScreen
import kotlinx.coroutines.launch

/**
 * Mutual Container for Chat & Call sections.
 * Features an edge-to-edge expandable capsule slider powered by MomoPrimaryGradient.
 * Supports fluid gesture-driven finger swiping via HorizontalPager, spring-responsive
 * real-time sliding capsule indicator, and touch bounce feedback.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WindowScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { 2 }
    )

    var isChatOpen by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        ChatEngine.startEngine(context)
    }

    // Logical multi-step Back Navigation: ChatScreen -> Inbox/Call Tab -> Chat Tab -> HomeScreen
    BackHandler(enabled = !CallManager.isIncomingCall && !CallManager.isCallActive) {
        when {
            isChatOpen -> {
                isChatOpen = false
            }
            pagerState.currentPage != 0 -> {
                coroutineScope.launch {
                    pagerState.animateScrollToPage(0)
                }
            }
            else -> {
                onBack()
            }
        }
    }

    if (CallManager.isIncomingCall || CallManager.isCallActive) {
        CallScreen(onBack = onBack)
    } else if (isChatOpen) {
        ChatScreen(
            onBack = {
                isChatOpen = false
            }
        )
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Full-Width Top Header Container
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 2.dp
            ) {
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                        .height(44.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                        .padding(4.dp)
                ) {
                    val segmentWidth = maxWidth / 2
                    val animatedPosition = (pagerState.currentPage + pagerState.currentPageOffsetFraction).coerceIn(0f,
