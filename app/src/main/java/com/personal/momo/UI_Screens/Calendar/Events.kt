package com.personal.momo.UI_Screens.Calendar

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.personal.momo.UI_Screens.Gradient6
import com.personal.momo.UI_Screens.MomoPrimaryGradient
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

data class MomoEvent(
    val id: String,
    val title: String,
    val description: String,
    val date: LocalDate,
    val epochMillis: Long,
    val isSpecial: Boolean = false
)

data class MomoNostalgiaEvent(
    val event: MomoEvent,
    val yearsAgo: Int
)

data class MonthEventsData(
    val regularEventDates: Set<LocalDate>,
    val milestoneDates: Set<LocalDate>
)

data class MonthAgendaItem(
    val id: String,
    val dayNumber: Int,
    val title: String,
    val description: String,
    val formattedOriginalDate: String,
    val timeAgoText: String,
    val isSpecial: Boolean
)

object MomoEventsCalculator {

    /**
     * Matches events that occurred on the exact date (same day, month, and year).
     */
    fun getExactEventsForDate(selectedDate: LocalDate, allEvents: List<MomoEvent>): List<MomoEvent> {
        return allEvents.filter { it.date == selectedDate }
    }

    /**
     * "On This Day" / Nostalgia reminder:
     * Matches the same day and month from past years.
     */
    fun getNostalgiaEvents(selectedDate: LocalDate, allEvents: List<MomoEvent>): List<MomoNostalgiaEvent> {
        return allEvents.filter { event ->
            event.date.month == selectedDate.month &&
                    event.date.dayOfMonth == selectedDate.dayOfMonth &&
                    event.date.year < selectedDate.year
        }.map { event ->
            val diffYears = selectedDate.year - event.date.year
            MomoNostalgiaEvent(
                event = event,
                yearsAgo = diffYears
            )
        }
    }

    /**
     * Checks whether any exact or nostalgic event exists for the specified date.
     */
    fun hasEventOnDate(date: LocalDate, allEvents: List<MomoEvent>): Boolean {
        return allEvents.any { event ->
            event.date.month == date.month &&
                    event.date.dayOfMonth == date.dayOfMonth &&
                    event.date.year <= date.year
        }
    }

    /**
     * Checks whether any exact or nostalgic event on this day has the special milestone flag enabled.
     */
    fun hasSpecialMilestone(selectedDate: LocalDate, allEvents: List<MomoEvent>): Boolean {
        val exactMatch = getExactEventsForDate(selectedDate, allEvents).any { it.isSpecial }
        val nostalgiaMatch = getNostalgiaEvents(selectedDate, allEvents).any { it.event.isSpecial }
        return exactMatch || nostalgiaMatch
    }

    /**
     * Pre-computes regular events and milestone events for the visible month.
     * Keeps Calendar.kt completely dumb by providing ready-to-consume sets.
     */
    fun getMonthEventsData(yearMonth: YearMonth, allEvents: List<MomoEvent>): MonthEventsData {
        val daysInMonth = yearMonth.lengthOfMonth()
        val regularDates = mutableSetOf<LocalDate>()
        val milestoneDates = mutableSetOf<LocalDate>()

        for (day in 1..daysInMonth) {
            val date = yearMonth.atDay(day)
            val matchingEvents = allEvents.filter { event ->
                event.date.month == date.month &&
                        event.date.dayOfMonth == date.dayOfMonth &&
                        event.date.year <= date.year
            }

            if (matchingEvents.isNotEmpty()) {
                if (matchingEvents.any { it.isSpecial }) {
                    milestoneDates.add(date)
                } else {
                    regularDates.add(date)
                }
            }
        }

        return MonthEventsData(
            regularEventDates = regularDates,
            milestoneDates = milestoneDates
        )
    }

    /**
     * Convenience method returning all dates with any active event in the month.
     */
    fun getEventDatesInMonth(yearMonth: YearMonth, allEvents: List<MomoEvent>): Set<LocalDate> {
        val data = getMonthEventsData(yearMonth, allEvents)
        return data.regularEventDates + data.milestoneDates
    }

    /**
     * Aggregates all nostalgia memories for the visible month across all past years,
     * sorted ascending by the day of the month.
     */
    fun getMonthAgendaEvents(yearMonth: YearMonth, allEvents: List<MomoEvent>): List<MonthAgendaItem> {
        val formatter = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH)
        return allEvents.filter { event ->
            event.date.month == yearMonth.month && event.date.year < yearMonth.year
        }.sortedWith(
            compareBy<MomoEvent> { it.date.dayOfMonth }
                .thenByDescending { it.date.year }
        ).map { event ->
            val diffYears = yearMonth.year - event.date.year
            val timeAgo = if (diffYears == 1) "1 Year" else "$diffYears Years"
            MonthAgendaItem(
                id = event.id,
                dayNumber = event.date.dayOfMonth,
                title = event.title,
                description = event.description,
                formattedOriginalDate = event.date.format(formatter),
                timeAgoText = timeAgo,
                isSpecial = event.isSpecial
            )
        }
    }
}

@Composable
fun MonthEventsAgendaCard(
    currentYearMonth: YearMonth,
    allEvents: List<MomoEvent>,
    modifier: Modifier = Modifier
) {
    val agendaItems = remember(currentYearMonth, allEvents) {
        MomoEventsCalculator.getMonthAgendaEvents(currentYearMonth, allEvents)
    }

    AnimatedVisibility(
        visible = agendaItems.isNotEmpty(),
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        val cardShape = RoundedCornerShape(24.dp)
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .shadow(
                    elevation = 12.dp,
                    shape = cardShape,
                    clip = false,
                    ambientColor = MaterialTheme.colorScheme.scrim.copy(alpha = 0.08f),
                    spotColor = MaterialTheme.colorScheme.scrim.copy(alpha = 0.12f)
                ),
            shape = cardShape,
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                agendaItems.forEach { item ->
                    MonthAgendaRow(item = item)
                }
            }
        }
    }
}

@Composable
private fun MonthAgendaRow(
    item: MonthAgendaItem,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1. Date Circle Badge
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(
                    brush = if (item.isSpecial) Gradient6 else MomoPrimaryGradient
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

        // 2. Middle Details Column: Title (Small Top), Description (Large Bottom)
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = item.title,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = item.description,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // 3. Right Historic Info Column: Formatted Date (Small Top), Years Ago (Large Bottom)
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = item.formattedOriginalDate,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = item.timeAgoText,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
