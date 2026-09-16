package com.personal.momo.UI_Screens

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.personal.momo.Cache.CacheManager
import com.personal.momo.R
import com.personal.momo.UI_Screens.Calendar.MomoCalendar
import com.personal.momo.UI_Screens.Calendar.MomoEventsCalculator
import com.personal.momo.UI_Screens.Calendar.MonthEventsAgendaCard
import com.personal.momo.UI_Screens.Notifications.MomoNotificationEngine
import com.personal.momo.UI_Screens.Notifications.NotificationAlarmReceiver
import com.personal.momo.UI_Screens.Notifications.NotificationsPreferences
import com.personal.momo.UI_Screens.Notifications.NotificationsScreen
import com.personal.momo.UI_Screens.Settings.MenuScreen
import com.personal.momo.UI_Screens.Settings.checkIsUpdateAvailable
import java.time.LocalDate
import java.time.YearMonth

private val BellIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "Bell",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).addPath(
        pathData = PathParser().parsePathString(
            "M12 22c1.1 0 2-.9 2-2h-4c0 1.1.89 2 2 2zm6-6v-5c0-3.07-1.64-5.64-4.5-6.32V4c0-.83-.67-1.5-1.5-1.5s-1.5.67-1.5 1.5v.68C7.63 5.36 6 7.92 6 11v5l-2 2v1h16v-1l-2-2z"
        ).toNodes(),
        fill = SolidColor(Color.White)
    ).build()
}

private val MomoScriptFont = FontFamily(Font(R.font.momo_script))
private val MomoBoldFont = FontFamily(Font(R.font.momo_bold))

private enum class HomeScreenDestination {
    HOME,
    MENU,
    NOTIFICATIONS
}

@Composable
fun HomeScreen() {
    val context = LocalContext.current
    var isMenuOpen by remember { mutableStateOf(false) }
    var isNotificationsOpen by remember { mutableStateOf(false) }
    var isUpdateAvailable by remember { mutableStateOf(false) }
    var currentVisibleMonth by remember { mutableStateOf(YearMonth.now()) }

    // Repeated Notification Permission Handling: Prompts every launch if denied
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            NotificationAlarmReceiver.scheduleDaily7AmAlarm(context)
        }
    }

    LaunchedEffect(Unit) {
        CacheManager.init(context)
        isUpdateAvailable = checkIsUpdateAvailable(context)

        // Schedule background 7:00 AM alarm
        NotificationAlarmReceiver.scheduleDaily7AmAlarm(context)

        // Prompt notification permission on every app open if not granted
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val isPermissionGranted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            if (!isPermissionGranted) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    // Direct navigation to NotificationsScreen if opened via system push notification
    val activity = context as? Activity
    LaunchedEffect(activity?.intent) {
        val shouldOpenNotifications = activity?.intent?.getBooleanExtra(
            NotificationAlarmReceiver.EXTRA_OPEN_NOTIFICATIONS,
            false
        ) ?: false

        if (shouldOpenNotifications) {
            isNotificationsOpen = true
            activity?.intent?.removeExtra(NotificationAlarmReceiver.EXTRA_OPEN_NOTIFICATIONS)
        }
    }

    val avatarUrl by CacheManager.avatarUrlFlow.collectAsState()
    val allEvents by CacheManager.eventsFlow.collectAsState()
    val loggedPeriodDates by CacheManager.periodDatesFlow.collectAsState()
    val today = remember { LocalDate.now() }

    val notifications = remember(allEvents, loggedPeriodDates, today) {
        MomoNotificationEngine.computeNotifications(today, allEvents, loggedPeriodDates)
    }

    val activeSignature = remember(notifications) {
        notifications.joinToString("|") { it.id }
    }

    var hasUnreadNotifications by remember(activeSignature) {
        mutableStateOf(NotificationsPreferences.isUnread(context, activeSignature))
    }

    val isMilestoneToday = remember(allEvents) {
        MomoEventsCalculator.hasMilestoneAnniversaryToday(allEvents)
    }

    val currentDestination = when {
        isNotificationsOpen -> HomeScreenDestination.NOTIFICATIONS
        isMenuOpen -> HomeScreenDestination.MENU
        else -> HomeScreenDestination.HOME
    }

    AnimatedContent(
        targetState = currentDestination,
        transitionSpec = {
            if (targetState != HomeScreenDestination.HOME) {
                (fadeIn(animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing)) +
                        scaleIn(
                            initialScale = 0.96f,
                            animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing)
                        )).togetherWith(
                    fadeOut(animationSpec = tween(durationMillis = 150))
                )
            } else {
                fadeIn(animationSpec = tween(durationMillis = 200)).togetherWith(
                    fadeOut(animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing)) +
                            scaleOut(
                                targetScale = 0.96f,
                                animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing)
                            )
                )
            }
        },
        label = "HomeScreenDestinationTransition"
    ) { destination ->
        when (destination) {
            HomeScreenDestination.NOTIFICATIONS -> {
                NotificationsScreen(
                    onBack = { isNotificationsOpen = false }
                )
            }

            HomeScreenDestination.MENU -> {
                MenuScreen(
                    onBack = { isMenuOpen = false },
                    isUpdateAvailable = isUpdateAvailable
                )
            }

            HomeScreenDestination.HOME -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        HomeHeader(
                            avatarUrl = avatarUrl,
                            isUpdateAvailable = isUpdateAvailable,
                            hasUnreadNotifications = hasUnreadNotifications,
                            onNotificationsClick = {
                                isNotificationsOpen = true
                                NotificationsPreferences.markAsSeen(context, activeSignature)
                                hasUnreadNotifications = false
                            },
                            onMenuClick = {
                                isMenuOpen = true
                            }
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .background(MaterialTheme.colorScheme.background)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState())
                                    .padding(bottom = 24.dp)
                            ) {
                                MomoCalendar(
                                    onMonthChanged = { month ->
                                        currentVisibleMonth = month
                                    }
                                )

                                Spacer(modifier = Modifier.height(14.dp))

                                MonthEventsAgendaCard(
                                    currentYearMonth = currentVisibleMonth,
                                    allEvents = allEvents
                                )
                            }
                        }
                    }

                    // Milestone Celebration Cannon Blast (Bottom-Center, upward angle 270°)
                    MomoBottomCannonCelebration(trigger = isMilestoneToday)
                }
            }
        }
    }
}

@Composable
private fun HomeHeader(
    avatarUrl: String?,
    isUpdateAvailable: Boolean,
    hasUnreadNotifications: Boolean,
    onNotificationsClick: () -> Unit,
    onMenuClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 3.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Profile Avatar & Typography Branding
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface)
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (!avatarUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = avatarUrl,
                            contentDescription = "Profile Avatar",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "hello ",
                        fontFamily = MomoScriptFont,
                        fontSize = 24.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Spacer(modifier = Modifier.width(1.dp))

                    Text(
                        text = "MOMO",
                        fontFamily = MomoBoldFont,
                        fontSize = 20.sp,
                        letterSpacing = 0.5.sp,
                        style = TextStyle(brush = MomoPrimaryGradient)
                    )
                }
            }

            // Right Action Buttons
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Bell Notification Icon with unread indicator dot
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .bounceClick(scaleDown = 0.88f) {
                            onNotificationsClick()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = BellIcon,
                        contentDescription = "Notifications",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp)
                    )

                    if (hasUnreadNotifications) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .align(Alignment.TopEnd)
                                .offset(x = (-4).dp, y = 4.dp)
                                .clip(CircleShape)
                                .background(brush = MomoPrimaryGradient)
                        )
                    }
                }

                // Veggie Burger Icon (2 Parallel Rounded Bars with Update Dot)
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .bounceClick(scaleDown = 0.88f) {
                            onMenuClick()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.5.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .width(18.dp)
                                .height(2.5.dp)
                                .clip(RoundedCornerShape(1.5.dp))
                                .background(MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                        Box(
                            modifier = Modifier
                                .width(18.dp)
                                .height(2.5.dp)
                                .clip(RoundedCornerShape(1.5.dp))
                                .background(MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                    }

                    if (isUpdateAvailable) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .align(Alignment.TopEnd)
                                .offset(x = (-4).dp, y = 4.dp)
                                .clip(CircleShape)
                                .background(brush = MomoPrimaryGradient)
                        )
                    }
                }
            }
        }
    }
}
