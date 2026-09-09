package com.personal.momo.UI_Screens.Calendar

import java.time.LocalDate
import java.time.YearMonth

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
}
