package com.personal.momo.UI_Screens.Notifications

import android.content.Context
import com.personal.momo.UI_Screens.Calendar.ApyBdayCalculator
import com.personal.momo.UI_Screens.Calendar.MomoEvent
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
            val yearsSuffix = when {
                yearsDiff <= 0 -> ""
                yearsDiff == 1 -> "1 Year"
                else -> "$yearsDiff Years"
            }
            val yearsAgoPrefix = when {
                yearsDiff <= 0 -> ""
                yearsDiff == 1 -> "1 Year ago"
                else -> "$yearsDiff Years ago"
            }

            if (event.isSpecial) {
                // Milestone schedule: 7, 5, 2, 0 days
                if (daysRemaining in listOf(7L, 5L, 2L, 0L)) {
                    val headline = if (daysRemaining == 0L) "Today" else "$daysRemaining Days to go"
                    val tag = yearsSuffix

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
