package com.personal.momo.UI_Screens.Calendar

import java.time.LocalDate

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
     * Checks whether any exact or nostalgic event on this day has the special milestone flag enabled.
     */
    fun hasSpecialMilestone(selectedDate: LocalDate, allEvents: List<MomoEvent>): Boolean {
        val exactMatch = getExactEventsForDate(selectedDate, allEvents).any { it.isSpecial }
        val nostalgiaMatch = getNostalgiaEvents(selectedDate, allEvents).any { it.event.isSpecial }
        return exactMatch || nostalgiaMatch
    }
}
