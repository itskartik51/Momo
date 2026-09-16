package com.personal.momo.UI_Screens.Notifications

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.NotificationsNone
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.personal.momo.Cache.CacheManager
import com.personal.momo.UI_Screens.Calendar.ApyBdayCalculator
import com.personal.momo.UI_Screens.Calendar.MomoEvent
import com.personal.momo.UI_Screens.Gradient6
import com.personal.momo.UI_Screens.MomoPrimaryGradient
import com.personal.momo.UI_Screens.bounceClick
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

sealed interface MomoNotificationItem {
    val id: String
    val dayNumber: Int
    val formattedDate: String

    data class EventNotification(
        override val id: String,
        override val dayNumber: Int,
        override val formattedDate: String,
        val event: MomoEvent,
        val headline: String,
        val countdownTag: String,
        val isMilestone: Boolean
    ) : MomoNotificationItem

    data class PeriodNotification(
        override val id: String,
        override val dayNumber: Int,
        override val formattedDate: String,
        val alertMessage: String
    ) : MomoNotificationItem
}

object NotificationsPreferences {
    private const val PREFS_NAME = "momo_notifications_prefs"
    private const val KEY_SEEN_SIGNATURE = "seen_notifications_signature"

    fun isUnread(context: Context, signature: String): Boolean {
        if (signature.isEmpty()) return false
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastSeen = prefs.getString(KEY_SEEN_SIGNATURE, "") ?: ""
        return lastSeen != signature
    }

    fun markAsSeen(context: Context, signature: String) {
        if (signature.isEmpty()) return
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_SEEN_SIGNATURE, signature).apply()
    }
}

object MomoNotificationEngine {

    fun computeNotifications(
        today: LocalDate,
        allEvents: List<MomoEvent>,
        loggedPeriodDates: List<LocalDate>
    ): List<MomoNotificationItem> {
        val list = mutableListOf<MomoNotificationItem>()
        val dateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH)

        // 1. Event Notifications (Milestone & Regular)
        for (event in allEvents) {
            var targetAnniversary = event.date.withYear(today.year)
            if (targetAnniversary.isBefore(today)) {
                targetAnniversary = targetAnniversary.plusYears(1)
            }

            val daysRemaining = ChronoUnit.DAYS.between(today, targetAnniversary)
            val yearsDiff = targetAnniversary.year - event.date.year
            val yearsAgoPrefix = when {
                yearsDiff <= 0 -> ""
                yearsDiff == 1 -> "1 Year ago"
                else -> "$yearsDiff Years ago"
            }

            if (event.isSpecial) {
                // Milestone discrete schedule: 7, 5, 2, 0 days
                if (daysRemaining in listOf(7L, 5L, 2L, 0L)) {
                    val (tag, headline) = if (daysRemaining == 0L) {
                        "Today" to if (yearsAgoPrefix.isNotEmpty()) "$yearsAgoPrefix Today" else "Today"
                    } else {
                        "$daysRemaining Days" to if (yearsAgoPrefix.isNotEmpty()) "$yearsAgoPrefix that Day" else "That Day"
                    }

                    list.add(
                        MomoNotificationItem.EventNotification(
                            id = "milestone_${event.id}_d$daysRemaining",
                            dayNumber = targetAnniversary.dayOfMonth,
                            formattedDate = event.date.format(dateFormatter),
                            event = event,
                            headline = headline,
                            countdownTag = tag,
                            isMilestone = true
                        )
                    )
                }
            } else {
                // Non-milestone regular schedule: strictly same day (0 days)
                if (daysRemaining == 0L) {
                    val headline = if (yearsAgoPrefix.isNotEmpty()) "$yearsAgoPrefix Today" else "Today"
                    list.add(
                        MomoNotificationItem.EventNotification(
                            id = "regular_${event.id}_d0",
                            dayNumber = targetAnniversary.dayOfMonth,
                            formattedDate = event.date.format(dateFormatter),
                            event = event,
                            headline = headline,
                            countdownTag = "Today",
                            isMilestone = false
                        )
                    )
                }
            }
        }

        // 2. Period Notifications ("Hpy Bday")
        val cyclePrediction = ApyBdayCalculator.calculateCycle(loggedPeriodDates)
        if (cyclePrediction != null) {
            val nextPredictedDate = cyclePrediction.nextPredictedDate
            val daysRemaining = ChronoUnit.DAYS.between(today, nextPredictedDate)
            val hasUserConfirmedPeriod = loggedPeriodDates.any { !it.isBefore(nextPredictedDate) }

            if (!hasUserConfirmedPeriod) {
                if (daysRemaining <= 0L) {
                    // Day 0 or overdue persistence: holds daily until logged by user
                    list.add(
                        MomoNotificationItem.PeriodNotification(
                            id = "period_${nextPredictedDate}_today",
                            dayNumber = nextPredictedDate.dayOfMonth,
                            formattedDate = nextPredictedDate.format(dateFormatter),
                            alertMessage = "Periods may start today"
                        )
                    )
                } else if (daysRemaining in listOf(5L, 3L, 2L, 1L)) {
                    val message = if (daysRemaining == 1L) {
                        "Periods may start within 1 day"
                    } else {
                        "Periods may start within $daysRemaining days"
                    }
                    list.add(
                        MomoNotificationItem.PeriodNotification(
                            id = "period_${nextPredictedDate}_d$daysRemaining",
                            dayNumber = nextPredictedDate.dayOfMonth,
                            formattedDate = nextPredictedDate.format(dateFormatter),
                            alertMessage = message
                        )
                    )
                }
            }
        }

        return list
    }
}

@Composable
fun NotificationsScreen(
    onBack: () -> Unit
) {
    BackHandler {
        onBack()
    }

    val context = LocalContext.current
    val allEvents by CacheManager.eventsFlow.collectAsState()
    val loggedPeriodDates by CacheManager.periodDatesFlow.collectAsState()
    val today = remember { LocalDate.now() }

    val notifications = remember(allEvents, loggedPeriodDates, today) {
        MomoNotificationEngine.computeNotifications(today, allEvents, loggedPeriodDates)
    }

    val activeSignature = remember(notifications) {
        notifications.joinToString("|") { it.id }
    }

    // Automatically mark notifications as read when screen is viewed
    LaunchedEffect(activeSignature) {
        if (activeSignature.isNotEmpty()) {
            NotificationsPreferences.markAsSeen(context, activeSignature)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header Top Bar
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

                    Text(
                        text = "Notifications",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }

            // Notification List or Clean Empty State
            if (notifications.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 60.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.NotificationsNone,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        Text(
                            text = "All caught up",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Text(
                            text = "No notifications for today",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    notifications.forEach { item ->
                        when (item) {
                            is MomoNotificationItem.EventNotification -> {
                                EventNotificationCard(item = item)
                            }
                            is MomoNotificationItem.PeriodNotification -> {
                                PeriodNotificationCard(item = item)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EventNotificationCard(item: MomoNotificationItem.EventNotification) {
    var isExpanded by remember { mutableStateOf(false) }
    val cardShape = RoundedCornerShape(20.dp)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 10.dp,
                shape = cardShape,
                clip = false,
                ambientColor = MaterialTheme.colorScheme.scrim.copy(alpha = 0.06f),
                spotColor = MaterialTheme.colorScheme.scrim.copy(alpha = 0.10f)
            ),
        shape = cardShape,
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    if (item.event.description.isNotBlank()) {
                        isExpanded = !isExpanded
                    }
                }
                .padding(horizontal = 18.dp, vertical = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Circular Date Badge
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(
                            brush = if (item.isMilestone) Gradient6 else MomoPrimaryGradient
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${item.dayNumber}",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                // Content Column
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    // Line 1: Title & Original Event Date
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = item.event.title,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = item.formattedDate,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Line 2: Headline and Countdown Tag strictly on a single line
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = item.headline,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = item.countdownTag,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            style = TextStyle(brush = MomoPrimaryGradient)
                        )
                    }
                }
            }

            // Expandable Description
            AnimatedVisibility(
                visible = isExpanded && item.event.description.isNotBlank(),
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = item.event.description,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Normal,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun PeriodNotificationCard(item: MomoNotificationItem.PeriodNotification) {
    val cardShape = RoundedCornerShape(20.dp)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 10.dp,
                shape = cardShape,
                clip = false,
                ambientColor = MaterialTheme.colorScheme.scrim.copy(alpha = 0.06f),
                spotColor = MaterialTheme.colorScheme.scrim.copy(alpha = 0.10f)
            ),
        shape = cardShape,
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Circular Date Badge
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(brush = MomoPrimaryGradient),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${item.dayNumber}",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Non-expandable Column
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                // Line 1: Fixed Title "Hpy Bday" & Predicted Date
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Hpy Bday",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = item.formattedDate,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Line 2: Periods status message (Right side strictly blank)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.alertMessage,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
