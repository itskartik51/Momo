package com.personal.momo.UI_Screens.Settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.personal.momo.UI_Screens.MomoPrimaryGradient
import com.personal.momo.UI_Screens.WelcomeLayoutMode
import com.personal.momo.UI_Screens.bounceClick
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun MomoMenuPopup(
    isOpen: Boolean,
    onDismissRequest: () -> Unit,
    isUpdateAvailable: Boolean = false,
    onModeChanged: ((WelcomeLayoutMode) -> Unit)? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var currentMode by remember {
        mutableStateOf(WelcomeSettingsPrefs.getSavedLayoutMode(context))
    }
    var isWelcomeExpanded by remember { mutableStateOf(false) }
    var showUpdateDialog by remember { mutableStateOf(false) }
    var toastMessage by remember { mutableStateOf<String?>(null) }

    val arrowRotation by animateFloatAsState(
        targetValue = if (isWelcomeExpanded) 180f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "WelcomeArrowRotation"
    )

    if (isOpen) {
        Popup(
            alignment = Alignment.TopEnd,
            offset = IntOffset(x = -20, y = 140),
            onDismissRequest = onDismissRequest,
            properties = PopupProperties(focusable = true)
        ) {
            Surface(
                modifier = Modifier
                    .width(285.dp)
                    .shadow(elevation = 18.dp, shape = RoundedCornerShape(22.dp))
                    .clip(RoundedCornerShape(22.dp))
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                        shape = RoundedCornerShape(22.dp)
                    )
                    .animateContentSize(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioLowBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    ),
                color = MaterialTheme.colorScheme.background,
                shape = RoundedCornerShape(22.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    // 1. Welcome Note Menu Item
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .bounceClick(scaleDown = 0.98f) {
                                isWelcomeExpanded = !isWelcomeExpanded
                            }
                            .padding(vertical = 6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = "Welcome Note",
                                    tint = MaterialTheme.colorScheme.onBackground,
                                    modifier = Modifier.size(22.dp)
                                )

                                Text(
                                    text = "Welcome Note",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                            }

                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = "Toggle Section",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .size(20.dp)
                                    .rotate(arrowRotation)
                            )
                        }
                    }

                    // Smooth Synchronized Expansion for Capsule Selector
                    AnimatedVisibility(
                        visible = isWelcomeExpanded,
                        enter = expandVertically(
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioLowBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            )
                        ) + fadeIn(animationSpec = tween(200)),
                        exit = shrinkVertically(
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioNoBouncy,
                                stiffness = Spring.StiffnessMedium
                            )
                        ) + fadeOut(animationSpec = tween(150))
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Spacer(modifier = Modifier.height(10.dp))

                            WelcomeModeCapsuleSelector(
                                currentMode = currentMode,
                                onModeSelected = { selectedMode, label ->
                                    if (currentMode != selectedMode) {
                                        currentMode = selectedMode
                                        WelcomeSettingsPrefs.saveLayoutMode(context, selectedMode)
                                        coroutineScope.launch {
                                            delay(260)
                                            toastMessage = "$label mode applied"
                                            onModeChanged?.invoke(selectedMode)
                                        }
                                    }
                                }
                            )

                            Spacer(modifier = Modifier.height(4.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Minimalist Divider
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // 2. App Update Menu Item
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .bounceClick(scaleDown = 0.98f) {
                                showUpdateDialog = true
                                onDismissRequest()
                            }
                            .padding(vertical = 6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Download,
                                    contentDescription = "App Update",
                                    tint = MaterialTheme.colorScheme.onBackground,
                                    modifier = Modifier.size(22.dp)
                                )

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "App Update",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )

                                    if (isUpdateAvailable) {
                                        Box(
                                            modifier = Modifier
                                                .size(7.dp)
                                                .clip(CircleShape)
                                                .background(brush = MomoPrimaryGradient)
                                        )
                                    }
                                }
                            }

                            if (isUpdateAvailable) {
                                Text(
                                    text = "New",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    style = TextStyle(brush = MomoPrimaryGradient)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Floating Confirmation Toast
    WelcomeAppliedToast(
        message = toastMessage,
        onDismiss = { toastMessage = null }
    )

    // Momo Update Dialog
    MomoUpdateDialog(
        isOpen = showUpdateDialog,
        onDismissRequest = { showUpdateDialog = false }
    )
}
