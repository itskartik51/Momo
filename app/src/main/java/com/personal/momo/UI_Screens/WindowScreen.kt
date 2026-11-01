package com.personal.momo.UI_Screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.personal.momo.UI_Screens.Call.CallManager
import com.personal.momo.UI_Screens.Call.CallScreen
import com.personal.momo.UI_Screens.Chat.ChatEngine
import com.personal.momo.UI_Screens.Chat.ChatScreen
import com.personal.momo.UI_Screens.Chat.InboxScreen
import kotlinx.coroutines.launch

private val ChatTabIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "ChatTabIcon",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).addPath(
        pathData = PathParser().parsePathString(
            "M20 2H4c-1.1 0-2 .9-2 2v18l4-4h14c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2z"
        ).toNodes(),
        fill = SolidColor(Color.White)
    ).build()
}

private val CallTabIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "CallTabIcon",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).addPath(
        pathData = PathParser().parsePathString(
            "M6.62 10.79c1.44 2.83 3.76 5.14 6.59 6.59l2.2-2.2c.27-.27.67-.36 1.02-.24 1.12.37 2.33.57 3.57.57.55 0 1 .45 1 1V20c0 .55-.45 1-1 1-9.39 0-17-7.61-17-17 0-.55.45-1 1-1h3.5c.55 0 1 .45 1 1 0 1.25.2 2.45.57 3.57.11.35.03.74-.25 1.02l-2.2 2.2z"
        ).toNodes(),
        fill = SolidColor(Color.White)
    ).build()
}

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

    var isInChatDetail by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        ChatEngine.startEngine(context)
    }

    BackHandler(enabled = !CallManager.isIncomingCall && !CallManager.isCallActive) {
        if (isInChatDetail) {
            isInChatDetail = false
        } else if (pagerState.currentPage != 0) {
            coroutineScope.launch {
                pagerState.animateScrollToPage(0)
            }
        } else {
            onBack()
        }
    }

    if (CallManager.isIncomingCall || CallManager.isCallActive) {
        CallScreen(onBack = onBack)
    } else {
        if (isInChatDetail) {
            ChatScreen(
                onBack = { isInChatDetail = false }
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    val scrollPosition = (pagerState.currentPage + pagerState.currentPageOffsetFraction).coerceIn(0f, 1f)

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 20.dp, vertical = 14.dp)
                    ) {
                        Text(
                            text = "MomoChat",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.graphicsLayer {
                                translationX = -scrollPosition * 75.dp.toPx()
                                alpha = (1f - scrollPosition).coerceIn(0f, 1f)
                            }
                        )

                        Text(
                            text = "MomoCall",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.graphicsLayer {
                                translationX = (1f - scrollPosition) * 75.dp.toPx()
                                alpha = scrollPosition.coerceIn(0f, 1f)
                            }
                        )
                    }

                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) { page ->
                        when (page) {
                            0 -> {
                                InboxScreen(
                                    onChatClick = {
                                        isInChatDetail = true
                                    }
                                )
                            }
                            1 -> {
                                CallScreen(
                                    onBack = {
                                        coroutineScope.launch {
                                            pagerState.animateScrollToPage(0)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }

                val isChatActive = pagerState.currentPage == 0
                val isCallActive = pagerState.currentPage == 1

                val chatScale by animateFloatAsState(
                    targetValue = if (isChatActive) 1f else 0.82f,
                    animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
                    label = "ChatScale"
                )
                val chatAlpha by animateFloatAsState(
                    targetValue = if (isChatActive) 1f else 0f,
                    animationSpec = tween(durationMillis = 180),
                    label = "ChatAlpha"
                )

                val callScale by animateFloatAsState(
                    targetValue = if (isCallActive) 1f else 0.82f,
                    animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
                    label = "CallScale"
                )
                val callAlpha by animateFloatAsState(
                    targetValue = if (isCallActive) 1f else 0f,
                    animationSpec = tween(durationMillis = 180),
                    label = "CallAlpha"
                )

                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(bottom = 20.dp)
                        .width(216.dp)
                        .height(52.dp),
                    shape = RoundedCornerShape(32.dp),
                    color = Color(0xCC16171B),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.10f)),
                    shadowElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .bounceClick(scaleDown = 0.94f) {
                                    coroutineScope.launch {
                                        pagerState.animateScrollToPage(0)
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (chatAlpha > 0.01f) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .graphicsLayer {
                                            scaleX = chatScale
                                            scaleY = chatScale
                                            alpha = chatAlpha
                                        }
                                        .clip(RoundedCornerShape(26.dp))
                                        .background(MomoPrimaryGradient)
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = ChatTabIcon,
                                    contentDescription = "Chat",
                                    tint = if (isChatActive) Color.White else Color.White.copy(alpha = 0.55f),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Chat",
                                    fontSize = 13.5.sp,
                                    fontWeight = if (isChatActive) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isChatActive) Color.White else Color.White.copy(alpha = 0.55f)
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .bounceClick(scaleDown = 0.94f) {
                                    coroutineScope.launch {
                                        pagerState.animateScrollToPage(1)
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (callAlpha > 0.01f) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .graphicsLayer {
                                            scaleX = callScale
                                            scaleY = callScale
                                            alpha = callAlpha
                                        }
                                        .clip(RoundedCornerShape(26.dp))
                                        .background(MomoPrimaryGradient)
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = CallTabIcon,
                                    contentDescription = "Call",
                                    tint = if (isCallActive) Color.White else Color.White.copy(alpha = 0.55f),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Call",
                                    fontSize = 13.5.sp,
                                    fontWeight = if (isCallActive) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isCallActive) Color.White else Color.White.copy(alpha = 0.55f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
