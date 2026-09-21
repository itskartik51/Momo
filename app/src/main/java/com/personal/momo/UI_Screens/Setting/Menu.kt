package com.personal.momo.UI_Screens.Settings

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.personal.momo.Cache.CacheManager
import com.personal.momo.Proximity.ProximityLocationService
import com.personal.momo.UI_Screens.MomoPrimaryGradient
import com.personal.momo.UI_Screens.WelcomeLayoutMode
import com.personal.momo.UI_Screens.bounceClick
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.Period
import java.util.Locale

@Composable
fun ExpandableMenuTile(
    icon: ImageVector,
    title: String,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    badge: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .bounceClick(scaleDown = 0.98f) { onToggle() }
                .padding(vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(22.dp)
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    badge?.invoke()
                }
            }
        }

        AnimatedVisibility(
            visible = isExpanded,
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
            Box(modifier = Modifier.fillMaxWidth()) {
                content()
            }
        }
    }
}

@Composable
fun MenuHeaderCounter(
    startDate: LocalDateTime,
    modifier: Modifier = Modifier
) {
    var years by remember { mutableStateOf(0) }
    var months by remember { mutableStateOf(0) }
    var days by remember { mutableStateOf(0) }
    var hours by remember { mutableStateOf("00") }
    var minutes by remember { mutableStateOf("00") }
    var seconds by remember { mutableStateOf("00") }

    LaunchedEffect(startDate) {
        while (true) {
            val now = LocalDateTime.now()
            if (!now.isBefore(startDate)) {
                val period = Period.between(startDate.toLocalDate(), now.toLocalDate())
                years = period.years
                months = period.months
                days = period.days
                hours = String.format(Locale.ENGLISH, "%02d", now.hour)
                minutes = String.format(Locale.ENGLISH, "%02d", now.minute)
                seconds = String.format(Locale.ENGLISH, "%02d", now.second)
            }
            delay(1000L)
        }
    }

    val baseStyle = TextStyle(
        fontSize = 17.sp,
        fontWeight = FontWeight.Bold,
        fontFeatureSettings = "tnum"
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.40f))
            .padding(horizontal = 14.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${years}y ${months}m ${days}d ${hours}h ${minutes}m ",
                style = baseStyle.copy(
                    color = MaterialTheme.colorScheme.onSurface
                )
            )

            Text(
                text = "${seconds}s",
                style = baseStyle.copy(
                    brush = MomoPrimaryGradient
                )
            )
        }
    }
}

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
                            .bounceClick(scaleDown = 0.92f) { onDismiss() }
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
                            .bounceClick(scaleDown = 0.92f) { onConfirm() }
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

@Composable
fun MenuScreen(
    onBack: () -> Unit,
    isUpdateAvailable: Boolean = false,
    onModeChanged: ((WelcomeLayoutMode) -> Unit)? = null
) {
    BackHandler {
        onBack()
    }

    val scrollState = rememberScrollState()
    var isWelcomeExpanded by remember { mutableStateOf(false) }
    var isThemeExpanded by remember { mutableStateOf(false) }
    var isUpdateExpanded by remember { mutableStateOf(false) }
    var isFinderExpanded by remember { mutableStateOf(false) }
    var isSecurityExpanded by remember { mutableStateOf(false) }
    var isAppIdExpanded by remember { mutableStateOf(false) }
    var toastMessage by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 3.dp
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .align(Alignment.CenterStart)
                            .clip(CircleShape)
                            .bounceClick(scaleDown = 0.88f) { onBack() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    MenuHeaderCounter(
                        startDate = LocalDateTime.of(2022, 11, 23, 0, 0, 0),
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                // 1. Welcome Note Setting Row
                ExpandableMenuTile(
                    icon = Icons.Default.AutoAwesome,
                    title = "Welcome Note",
                    isExpanded = isWelcomeExpanded,
                    onToggle = { isWelcomeExpanded = !isWelcomeExpanded }
                ) {
                    WelcomeSettingsContent(
                        onModeChanged = onModeChanged,
                        onShowToast = { toastMessage = it }
                    )
                }

                HorizontalDivider(
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                )

                // 2. Theme Setting Row
                ExpandableMenuTile(
                    icon = Icons.Default.Palette,
                    title = "Theme",
                    isExpanded = isThemeExpanded,
                    onToggle = { isThemeExpanded = !isThemeExpanded }
                ) {
                    ThemeSettingsContent(
                        onShowToast = { toastMessage = it }
                    )
                }

                HorizontalDivider(
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                )

                // 3. RupeeFlow-Inspired App Update Row
                ExpandableMenuTile(
                    icon = Icons.Default.Download,
                    title = "App Update",
                    isExpanded = isUpdateExpanded,
                    onToggle = { isUpdateExpanded = !isUpdateExpanded },
                    badge = {
                        if (isUpdateAvailable) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(brush = MomoPrimaryGradient)
                            )
                        }
                    }
                ) {
                    AppUpdateContent(isExpanded = isUpdateExpanded)
                }

                HorizontalDivider(
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                )

                // 4. Finder Proximity Setting Row
                ExpandableMenuTile(
                    icon = Icons.Outlined.MyLocation,
                    title = "Finder",
                    isExpanded = isFinderExpanded,
                    onToggle = { isFinderExpanded = !isFinderExpanded }
                ) {
                    FinderSettingsContent(
                        onShowToast = { toastMessage = it }
                    )
                }

                HorizontalDivider(
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                )

                // 5. Security Lock Row
                ExpandableMenuTile(
                    icon = Icons.Default.Fingerprint,
                    title = "Security Lock",
                    isExpanded = isSecurityExpanded,
                    onToggle = { isSecurityExpanded = !isSecurityExpanded }
                ) {
                    SecurityLockSettingsContent(
                        onShowToast = { toastMessage = it }
                    )
                }

                HorizontalDivider(
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                )

                // 6. App ID Row
                ExpandableMenuTile(
                    icon = Icons.Default.Person,
                    title = "App ID",
                    isExpanded = isAppIdExpanded,
                    onToggle = { isAppIdExpanded = !isAppIdExpanded }
                ) {
                    AppIdSettingsContent(
                        onShowToast = { toastMessage = it }
                    )
                }

                HorizontalDivider(
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                )
            }
        }

        WelcomeAppliedToast(
            message = toastMessage,
            onDismiss = { toastMessage = null },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 36.dp)
        )
    }
}
