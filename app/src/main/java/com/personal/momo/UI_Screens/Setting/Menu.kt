package com.personal.momo.UI_Screens.Settings

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Palette
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.personal.momo.UI_Screens.MomoPrimaryDark
import com.personal.momo.UI_Screens.MomoPrimaryGradient
import com.personal.momo.UI_Screens.WelcomeLayoutMode
import com.personal.momo.UI_Screens.bounceClick
import kotlinx.coroutines.delay
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
    var timeString by remember { mutableStateOf("00:00:00") }

    // Lifecycle-aware: Stops immediately when leaving Menu screen or app
    LaunchedEffect(startDate) {
        while (true) {
            val now = LocalDateTime.now()
            if (!now.isBefore(startDate)) {
                val period = Period.between(startDate.toLocalDate(), now.toLocalDate())
                years = period.years
                months = period.months
                days = period.days
                timeString = String.format(
                    Locale.ENGLISH,
                    "%02d:%02d:%02d",
                    now.hour,
                    now.minute,
                    now.second
                )
            }
            delay(1000L)
        }
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "${years}y ${months}m ${days}d",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Text(
            text = "•",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )

        Text(
            text = timeString,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = MomoPrimaryDark
        )
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
    var toastMessage by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header Top Bar with Live Clean Ticker
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 3.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
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

                    Spacer(modifier = Modifier.width(12.dp))

                    // Option A Inline Counter replacing standard "Menu" text
                    MenuHeaderCounter(
                        startDate = LocalDateTime.of(2022, 11, 23, 0, 0, 0)
                    )
                }
            }

            // Scrollable Settings Container
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
            }
        }

        // Screen-Level Floating Confirmation Toast (Anchored to Bottom Center)
        WelcomeAppliedToast(
            message = toastMessage,
            onDismiss = { toastMessage = null },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 36.dp)
        )
    }
}
