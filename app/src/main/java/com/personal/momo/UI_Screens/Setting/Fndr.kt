package com.personal.momo.UI_Screens.Settings

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.personal.momo.Cache.CacheManager
import com.personal.momo.Proximity.ProximityLocationService
import com.personal.momo.UI_Screens.MomoPrimaryGradient
import com.personal.momo.UI_Screens.bounceClick
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun FinderSettingsContent(
    onShowToast: ((String) -> Unit)? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var isFinderEnabled by remember {
        mutableStateOf(CacheManager.isFinderEnabled(context))
    }

    var showConfirmDialog by remember { mutableStateOf(false) }
    var pendingTargetState by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
    ) {
        FinderCapsuleSelector(
            isEnabled = isFinderEnabled,
            onOptionSelected = { targetEnabled ->
                if (targetEnabled == isFinderEnabled) return@FinderCapsuleSelector
                pendingTargetState = targetEnabled
                showConfirmDialog = true
            }
        )
    }

    if (showConfirmDialog) {
        FinderConfirmationDialog(
            isTargetEnable = pendingTargetState,
            onConfirm = {
                showConfirmDialog = false
                val target = pendingTargetState
                isFinderEnabled = target
                CacheManager.setFinderEnabled(context, target)
                if (target) {
                    ProximityLocationService.startService(context)
                } else {
                    ProximityLocationService.stopService(context)
                }
                coroutineScope.launch {
                    delay(260)
                    val message = if (target) "Finder & Proximity enabled" else "Finder & Proximity disabled"
                    onShowToast?.invoke(message)
                }
            },
            onDismiss = {
                showConfirmDialog = false
            }
        )
    }
}

@Composable
fun FinderCapsuleSelector(
    isEnabled: Boolean,
    onOptionSelected: (Boolean) -> Unit
) {
    val options = listOf(
        false to "Disable",
        true to "Enable"
    )

    val selectedIndex = if (isEnabled) 1 else 0

    val animatedIndex by animateFloatAsState(
        targetValue = selectedIndex.toFloat(),
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "FinderCapsuleSlideSpring"
    )

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(42.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surface)
            .padding(4.dp)
    ) {
        val segmentWidth = maxWidth / 2

        Box(
            modifier = Modifier
                .offset {
                    IntOffset(
                        x = (animatedIndex * segmentWidth.toPx()).toInt(),
                        y = 0
                    )
                }
                .width(segmentWidth)
                .fillMaxHeight()
                .clip(CircleShape)
                .background(brush = MomoPrimaryGradient)
        )

        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            options.forEach { (enabledValue, label) ->
                val isSelected = isEnabled == enabledValue

                val textColor by animateColorAsState(
                    targetValue = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                    animationSpec = tween(durationMillis = 200),
                    label = "FinderTextColorFade_$label"
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            onOptionSelected(enabledValue)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = textColor
                    )
                }
            }
        }
    }
}

@Composable
private fun FinderConfirmationDialog(
    isTargetEnable: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp)
            ) {
                Text(
                    text = if (isTargetEnable) "Enable Finder & Proximity?" else "Disable Finder & Proximity?",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = if (isTargetEnable) {
                        "Enabling Finder will activate the background proximity engine to exchange relative location data with your partner. Real-time tracking and the Home Screen radar icon will be restored."
                    } else {
                        "Turning off Finder will completely stop background location tracking and sever all real-time coordinate sharing with your partner. The Finder radar icon will be hidden from your Home Screen, and no location data will be read or uploaded to the database."
                    },
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(22.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .bounceClick(scaleDown = 0.94f) { onDismiss() }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Cancel",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(brush = MomoPrimaryGradient)
                            .bounceClick(scaleDown = 0.94f) { onConfirm() }
                            .padding(horizontal = 20.dp, vertical = 9.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isTargetEnable) "Enable" else "Disable",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}
