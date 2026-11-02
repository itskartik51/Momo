package com.personal.momo.UI_Screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
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
    val density = LocalDensity.current
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
                    BoxWithConstraints(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 20.dp, vertical = 14.dp)
                            .clipToBounds()
                    ) {
                        val fullWidthPx = with(density) { maxWidth.toPx() }
                        val scrollProgress = (pagerState.currentPage.toFloat() + pagerState.currentPageOffsetFraction).coerceIn(0f, 1f)

                        Text(
                            text = "MomoChat",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.graphicsLayer {
                                translationX = -scrollProgress * fullWidthPx
                                alpha = (1f - scrollProgress * 1.8f).coerceIn(0f, 1f)
                            }
                        )

                        Text(
                            text = "MomoCall",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.graphicsLayer {
                                translationX = (1f - scrollProgress) * fullWidthPx
                                alpha = ((scrollProgress - 0.45f) * 1.8f).coerceIn(0f, 1f)
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

                val dockWidth: Dp = 216.dp
                val dockHeight: Dp = 54.dp
                val innerPadding: Dp = 4.dp
                val tabWidth: Dp = (dockWidth - (innerPadding * 2)) / 2
                val tabWidthPx: Float = with(density) { tabWidth.toPx() }

                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(bottom = 20.dp)
                        .width(dockWidth)
                        .height(dockHeight),
                    shape = RoundedCornerShape(27.dp),
                    color = Color(0xD918191E),
                    border = BorderStroke(1.2.dp, Color.White.copy(alpha = 0.16f)),
                    shadowElevation = 14.dp
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        val scrollProgress = (pagerState.currentPage.toFloat() + pagerState.currentPageOffsetFraction).coerceIn(0f, 1f)
                        val midDiff = (2f * scrollProgress - 1f)
                        val stretchFactor = 1f + 0.10f * (1f - midDiff * midDiff)

                        Box(
                            modifier = Modifier
                                .width(tabWidth)
                                .fillMaxHeight()
                                .graphicsLayer {
                                    translationX = scrollProgress * tabWidthPx
                                    scaleX = stretchFactor
                                }
                                .clip(RoundedCornerShape(23.dp))
                                .background(MomoPrimaryGradient)
                        )

                        Row(
                            modifier = Modifier.fillMaxSize(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val chatAlpha = (1f - scrollProgress * 0.7f).coerceIn(0.50f, 1f)
                            val isChatActive = scrollProgress < 0.5f

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(23.dp))
                                    .bounceClick(scaleDown = 0.94f) {
                                        coroutineScope.launch {
                                            pagerState.animateScrollToPage(0)
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = ChatTabIcon,
                                        contentDescription = "Chat",
                                        tint = Color.White.copy(alpha = chatAlpha),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Chat",
                                        fontSize = 13.5.sp,
                                        fontWeight = if (isChatActive) FontWeight.Bold else FontWeight.Medium,
                                        color = Color.White.copy(alpha = chatAlpha)
                                    )
                                }
                            }

                            val callAlpha = (0.30f + scrollProgress * 0.7f).coerceIn(0.50f, 1f)
                            val isCallActive = scrollProgress >= 0.5f

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(23.dp))
                                    .bounceClick(scaleDown = 0.94f) {
                                        coroutineScope.launch {
                                            pagerState.animateScrollToPage(1)
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = CallTabIcon,
                                        contentDescription = "Call",
                                        tint = Color.White.copy(alpha = callAlpha),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Call",
                                        fontSize = 13.5.sp,
                                        fontWeight = if (isCallActive) FontWeight.Bold else FontWeight.Medium,
                                        color = Color.White.copy(alpha = callAlpha)
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
