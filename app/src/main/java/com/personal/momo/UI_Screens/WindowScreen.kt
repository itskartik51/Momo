package com.personal.momo.UI_Screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
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

private val GlassDockBackgroundBrush = Brush.verticalGradient(
    colors = listOf(
        Color(0xEE2A2C38),
        Color(0xF5181922)
    )
)

private val GlassDockBorderBrush = Brush.verticalGradient(
    colors = listOf(
        Color.White.copy(alpha = 0.24f),
        Color.White.copy(alpha = 0.08f)
    )
)

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
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
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

                val scrollProgress = (pagerState.currentPage.toFloat() + pagerState.currentPageOffsetFraction).coerceIn(0f, 1f)
                val targetChatFraction = (1f - scrollProgress).coerceIn(0f, 1f)
                val targetCallFraction = scrollProgress.coerceIn(0f, 1f)

                val chatPopFraction by animateFloatAsState(
                    targetValue = targetChatFraction,
                    animationSpec = spring(
                        dampingRatio = 0.62f,
                        stiffness = Spring.StiffnessMedium
                    ),
                    label = "ChatPopFraction"
                )

                val callPopFraction by animateFloatAsState(
                    targetValue = targetCallFraction,
                    animationSpec = spring(
                        dampingRatio = 0.62f,
                        stiffness = Spring.StiffnessMedium
                    ),
                    label = "CallPopFraction"
                )

                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(bottom = 18.dp)
                        .width(196.dp)
                        .height(58.dp),
                    shape = RoundedCornerShape(30.dp),
                    color = Color(0xF2181920),
                    border = BorderStroke(1.2.dp, GlassDockBorderBrush),
                    shadowElevation = 10.dp
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(GlassDockBackgroundBrush)
                            .padding(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            val isChatSelected = scrollProgress < 0.5f
                            val chatContentAlpha = (0.52f + 0.48f * chatPopFraction).coerceIn(0.52f, 1f)
                            val chatIconScale = 0.88f + 0.12f * chatPopFraction

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(26.dp))
                                    .bounceClick(scaleDown = 0.94f) {
                                        coroutineScope.launch {
                                            pagerState.animateScrollToPage(0)
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (chatPopFraction > 0.01f) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .graphicsLayer {
                                                val pillScale = 0.35f + 0.65f * chatPopFraction
                                                scaleX = pillScale
                                                scaleY = pillScale
                                                alpha = chatPopFraction.coerceIn(0f, 1f)
                                            }
                                            .clip(RoundedCornerShape(26.dp))
                                            .background(MomoPrimaryGradient)
                                    )
                                }

                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                    modifier = Modifier.graphicsLayer {
                                        scaleX = chatIconScale
                                        scaleY = chatIconScale
                                    }
                                ) {
                                    Icon(
                                        imageVector = ChatTabIcon,
                                        contentDescription = "Chat",
                                        tint = Color.White.copy(alpha = chatContentAlpha),
                                        modifier = Modifier.size(19.dp)
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Chat",
                                        fontSize = 11.5.sp,
                                        fontWeight = if (isChatSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = Color.White.copy(alpha = chatContentAlpha),
                                        style = TextStyle(
                                            platformStyle = PlatformTextStyle(includeFontPadding = false)
                                        )
                                    )
                                }
                            }

                            val isCallSelected = scrollProgress >= 0.5f
                            val callContentAlpha = (0.52f + 0.48f * callPopFraction).coerceIn(0.52f, 1f)
                            val callIconScale = 0.88f + 0.12f * callPopFraction

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(26.dp))
                                    .bounceClick(scaleDown = 0.94f) {
                                        coroutineScope.launch {
                                            pagerState.animateScrollToPage(1)
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (callPopFraction > 0.01f) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .graphicsLayer {
                                                val pillScale = 0.35f + 0.65f * callPopFraction
                                                scaleX = pillScale
                                                scaleY = pillScale
                                                alpha = callPopFraction.coerceIn(0f, 1f)
                                            }
                                            .clip(RoundedCornerShape(26.dp))
                                            .background(MomoPrimaryGradient)
                                    )
                                }

                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                    modifier = Modifier.graphicsLayer {
                                        scaleX = callIconScale
                                        scaleY = callIconScale
                                    }
                                ) {
                                    Icon(
                                        imageVector = CallTabIcon,
                                        contentDescription = "Call",
                                        tint = Color.White.copy(alpha = callContentAlpha),
                                        modifier = Modifier.size(19.dp)
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Call",
                                        fontSize = 11.5.sp,
                                        fontWeight = if (isCallSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = Color.White.copy(alpha = callContentAlpha),
                                        style = TextStyle(
                                            platformStyle = PlatformTextStyle(includeFontPadding = false)
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
