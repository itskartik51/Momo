package com.personal.momo.UI_Screens.Finder

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.personal.momo.Cache.CacheManager
import com.personal.momo.Proximity.ProximityLocationService
import com.personal.momo.UI_Screens.bounceClick

@Composable
fun FinderScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var isLiveActive by remember { mutableStateOf(false) }
    val trackerState by CacheManager.trackerStateFlow.collectAsState()

    // Scoped Lifecycle: Auto-turn off Live mode on Back, Navigation, or App Minimize
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) {
                if (isLiveActive) {
                    isLiveActive = false
                    ProximityLocationService.stopLive(context)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            if (isLiveActive) {
                isLiveActive = false
                ProximityLocationService.stopLive(context)
            }
        }
    }

    BackHandler {
        if (isLiveActive) {
            isLiveActive = false
            ProximityLocationService.stopLive(context)
        }
        onBack()
    }

    val partnerInfo = trackerState.partnerInfo
    val selfInfo = trackerState.selfInfo
    val distanceMeters = trackerState.distanceMeters

    // Dynamic Accuracy-Aware Trigger Distance
    // Good combined satellite accuracy (<= 10m) sets tight 10m threshold
    // Weaker accuracy (> 10m) scales threshold up to 18m-20m to prevent compass spinning
    val selfAcc = selfInfo.accuracy ?: 10f
    val partnerAcc = partnerInfo.accuracy ?: 10f
    val combinedAcc = selfAcc + partnerAcc

    val dynamicThreshold = remember(combinedAcc) {
        when {
            combinedAcc <= 10f -> 10.0
            combinedAcc <= 20f -> 14.0
            combinedAcc <= 30f -> 18.0
            else -> 20.0
        }
    }

    // Hysteresis buffer to eliminate boundary flicker:
    // Enters together mode at <= dynamicThreshold
    // Exits together mode at > dynamicThreshold + 6.0m
    var isProximityTogether by remember { mutableStateOf(false) }

    LaunchedEffect(distanceMeters, dynamicThreshold) {
        val dist = distanceMeters?.toDouble()
        if (dist != null) {
            if (!isProximityTogether && dist <= dynamicThreshold) {
                isProximityTogether = true
            } else if (isProximityTogether && dist > (dynamicThreshold + 6.0)) {
                isProximityTogether = false
            }
        } else {
            isProximityTogether = false
        }
    }

    // Double-Tap Test Gesture State Override
    var isTestTogether by remember { mutableStateOf(false) }
    val isTogether = isTestTogether || isProximityTogether

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // Header Top Bar
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 3.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left Section: Back Button + "Finder" Title
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .bounceClick(scaleDown = 0.94f) {
                                    if (isLiveActive) {
                                        isLiveActive = false
                                        ProximityLocationService.stopLive(context)
                                    }
                                    onBack()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Text(
                            text = "Finder",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Right Section: Pill Buttons (Live & Sync)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Pill 1: Live Toggle
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(
                                    if (isLiveActive) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                )
                                .then(
                                    if (isLiveActive) {
                                        Modifier.border(
                                            width = 1.dp,
                                            color = MaterialTheme.colorScheme.primary,
                                            shape = RoundedCornerShape(50)
                                        )
                                    } else Modifier
                                )
                                .bounceClick(scaleDown = 0.94f) {
                                    isLiveActive = !isLiveActive
                                    if (isLiveActive) {
                                        ProximityLocationService.startLive(context)
                                        Toast.makeText(context, "10s Live Tracking Active", Toast.LENGTH_SHORT).show()
                                    } else {
                                        ProximityLocationService.stopLive(context)
                                        Toast.makeText(context, "Live Tracking Stopped", Toast.LENGTH_SHORT).show()
                                    }
                                }
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                if (isLiveActive) {
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary)
                                    )
                                }

                                Text(
                                    text = "Live",
                                    fontSize = 12.sp,
                                    fontWeight = if (isLiveActive) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isLiveActive) MaterialTheme.colorScheme.onPrimaryContainer
                                    else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        // Pill 2: Sync One-Shot Action
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .bounceClick(scaleDown = 0.94f) {
                                    ProximityLocationService.forceSync(context)
                                    Toast.makeText(context, "Syncing location...", Toast.LENGTH_SHORT).show()
                                }
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Sync",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Card 1: Modular Location Card
            FinderLocationCard(
                partnerInfo = partnerInfo,
                selfInfo = selfInfo
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Card 2: Animated Transition between Compass Radar Dial and Together Lottie Animation Card
            AnimatedContent(
                targetState = isTogether,
                transitionSpec = {
                    (fadeIn(animationSpec = tween(durationMillis = 300)) +
                            scaleIn(initialScale = 0.96f, animationSpec = tween(durationMillis = 300)))
                        .togetherWith(
                            fadeOut(animationSpec = tween(durationMillis = 200)) +
                                    scaleOut(targetScale = 0.96f, animationSpec = tween(durationMillis = 200))
                        )
                },
                label = "CompassToTogetherTransition"
            ) { togetherActive ->
                if (togetherActive) {
                    TogetherAnimationCard(
                        partnerName = partnerInfo.name,
                        onDoubleTap = {
                            isTestTogether = false
                        }
                    )
                } else {
                    CompassRadarCard(
                        partnerInfo = partnerInfo,
                        selfInfo = selfInfo,
                        distanceMeters = distanceMeters,
                        onCenterDoubleTap = {
                            isTestTogether = true
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
