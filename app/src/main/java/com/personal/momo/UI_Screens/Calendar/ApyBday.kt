package com.personal.momo.UI_Screens.Calendar

import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.abs
import kotlin.math.roundToLong

/**
 * Calculated cycle metrics and forward-looking predictions.
 */
data class CyclePrediction(
    val latestPeriodDate: LocalDate,
    val nextPredictedDate: LocalDate,
    val ovulationDate: LocalDate,
    val fertileWindowStart: LocalDate,
    val fertileWindowEnd: LocalDate,
    val calculatedCycleLength: Long,
    val intervalsUsed: List<Long>
)

/**
 * Projected data points for future upcoming cycles.
 */
data class ProjectedCycle(
    val periodStartDate: LocalDate,
    val ovulationDate: LocalDate,
    val fertileWindowStart: LocalDate,
    val fertileWindowEnd: LocalDate
)

/**
 * Clean data bundle containing pre-calculated date sets ready for UI rendering.
 */
data class CalendarCycleData(
    val prediction: CyclePrediction?,
    val confirmedBleedDates: Set<LocalDate> = emptySet(),
    val predictedBleedDates: Set<LocalDate> = emptySet(),
    val fertileDates: Set<LocalDate> = emptySet(),
    val ovulationDates: Set<LocalDate> = emptySet()
)

object ApyBdayCalculator {

    // Clinical Constants
    private const val CLINICAL_LUTEAL_PHASE_DAYS = 15L
    private const val PRE_OVULATION_FERTILE_DAYS = 4L
    private const val POST_OVULATION_BUFFER_DAYS = 2L
    private const val CONFIRMED_BLEED_DURATION_DAYS = 5L
    private const val PREDICTED_BLEED_DURATION_DAYS = 6L
    private const val DEFAULT_PROJECTION_CYCLES_COUNT = 12

    /**
     * Unified UI Data Provider:
     * Executes all mathematical models, outlier filtering, and projections,
     * returning complete, ready-to-draw date sets for the calendar.
     */
    fun getCalendarData(dates: List<LocalDate>): CalendarCycleData {
        if (dates.isEmpty()) return CalendarCycleData(null)

        // 1. Confirmed bleed days (5-day span from each logged date)
        val confirmedBleed = dates.flatMap { startDate ->
            (0L until CONFIRMED_BLEED_DURATION_DAYS).map { offset -> startDate.plusDays(offset) }
        }.toSet()

        val prediction = calculateCycle(dates) ?: return CalendarCycleData(
            prediction = null,
            confirmedBleedDates = confirmedBleed
        )

        val futureProjections = projectFutureCycles(prediction, count = DEFAULT_PROJECTION_CYCLES_COUNT)

        // 2. Multi-month predicted period bleed days (6-day span per future cycle)
        val predictedBleed = mutableSetOf<LocalDate>()
        futureProjections.forEach { projected ->
            for (offset in 0L until PREDICTED_BLEED_DURATION_DAYS) {
                predictedBleed.add(projected.periodStartDate.plusDays(offset))
            }
        }

        // 3. Exactly one ovulation date per cycle (Current cycle + future cycles)
        val ovulations = mutableSetOf<LocalDate>()
        ovulations.add(prediction.ovulationDate)
        futureProjections.forEach { projected ->
            ovulations.add(projected.ovulationDate)
        }

        // 4. Fertile window date sets (7-day window per cycle)
        val fertiles = mutableSetOf<LocalDate>()
        var curr = prediction.fertileWindowStart
        while (!curr.isAfter(prediction.fertileWindowEnd)) {
            fertiles.add(curr)
            curr = curr.plusDays(1)
        }
        futureProjections.forEach { projected ->
            var fCurr = projected.fertileWindowStart
            while (!fCurr.isAfter(projected.fertileWindowEnd)) {
                fertiles.add(fCurr)
                fCurr = fCurr.plusDays(1)
            }
        }

        return CalendarCycleData(
            prediction = prediction,
            confirmedBleedDates = confirmedBleed,
            predictedBleedDates = predictedBleed,
            fertileDates = fertiles,
            ovulationDates = ovulations
        )
    }

    /**
     * Core calculation engine (Option B - Medically Pure):
     * Takes raw logged dates, filters biological anomalies (spread > 4),
     * derives dynamic cycle length via Weighted Moving Average (WMA),
     * and maps clinical ovulation and fertility windows.
     */
    fun calculateCycle(dates: List<LocalDate>): CyclePrediction? {
        if (dates.size < 2) return null

        val sortedDates = dates.distinct().sorted()

        val rawIntervals = mutableListOf<Long>()
        for (i in 0 until sortedDates.size - 1) {
            val gap = ChronoUnit.DAYS.between(sortedDates[i], sortedDates[i + 1])
            if (gap in 18..60) {
                rawIntervals.add(gap)
            }
        }

        if (rawIntervals.isEmpty()) return null

        val recentIntervals = if (rawIntervals.size > 6) {
            rawIntervals.takeLast(6)
        } else {
            rawIntervals
        }

        val filteredIntervals = filterOutliers(recentIntervals)
        val cycleLength = calculateWeightedAverage(filteredIntervals)

        val latestDate = sortedDates.last()
        val nextPeriod = latestDate.plusDays(cycleLength)
        val ovulationDay = nextPeriod.minusDays(CLINICAL_LUTEAL_PHASE_DAYS)
        val fertileStart = ovulationDay.minusDays(PRE_OVULATION_FERTILE_DAYS)
        val fertileEnd = ovulationDay.plusDays(POST_OVULATION_BUFFER_DAYS)

        return CyclePrediction(
            latestPeriodDate = latestDate,
            nextPredictedDate = nextPeriod,
            ovulationDate = ovulationDay,
            fertileWindowStart = fertileStart,
            fertileWindowEnd = fertileEnd,
            calculatedCycleLength = cycleLength,
            intervalsUsed = filteredIntervals
        )
    }

    /**
     * Outlier Handling:
     * - If max - min <= 4 days: biological consistency preserved.
     * - If max - min > 4 days: filters single extreme outlier relative to median.
     */
    private fun filterOutliers(intervals: List<Long>): List<Long> {
        if (intervals.size < 4) return intervals

        val minVal = intervals.minOrNull() ?: return intervals
        val maxVal = intervals.maxOrNull() ?: return intervals

        if (maxVal - minVal <= 4) {
            return intervals
        }

        val sorted = intervals.sorted()
        val median = if (sorted.size % 2 == 0) {
            (sorted[sorted.size / 2 - 1] + sorted[sorted.size / 2]) / 2.0
        } else {
            sorted[sorted.size / 2].toDouble()
        }

        val minDiff = abs(minVal - median)
        val maxDiff = abs(maxVal - median)

        val outlierToRemove = if (maxDiff >= minDiff) maxVal else minVal

        val mutableList = intervals.toMutableList()
        val removeIndex = mutableList.indexOf(outlierToRemove)
        if (removeIndex != -1) {
            mutableList.removeAt(removeIndex)
        }

        return mutableList
    }

    /**
     * Weighted Moving Average (WMA):
     * Newer intervals receive linearly increasing weights.
     */
    private fun calculateWeightedAverage(intervals: List<Long>): Long {
        if (intervals.isEmpty()) return 28L

        var weightedSum = 0.0
        var totalWeights = 0

        for (i in intervals.indices) {
            val weight = i + 1
            weightedSum += intervals[i] * weight
            totalWeights += weight
        }

        val average = (weightedSum / totalWeights).roundToLong()
        return average.coerceIn(21L, 40L)
    }

    /**
     * Generates a chain of forward cycle projections dynamically based on the calculated rhythm.
     */
    fun projectFutureCycles(prediction: CyclePrediction, count: Int = DEFAULT_PROJECTION_CYCLES_COUNT): List<ProjectedCycle> {
        val projections = mutableListOf<ProjectedCycle>()
        var currentPeriodStart = prediction.nextPredictedDate

        for (i in 0 until count) {
            val nextPeriodStart = currentPeriodStart.plusDays(prediction.calculatedCycleLength)
            val ovulation = nextPeriodStart.minusDays(CLINICAL_LUTEAL_PHASE_DAYS)
            val fertileStart = ovulation.minusDays(PRE_OVULATION_FERTILE_DAYS)
            val fertileEnd = ovulation.plusDays(POST_OVULATION_BUFFER_DAYS)

            projections.add(
                ProjectedCycle(
                    periodStartDate = currentPeriodStart,
                    ovulationDate = ovulation,
                    fertileWindowStart = fertileStart,
                    fertileWindowEnd = fertileEnd
                )
            )
            currentPeriodStart = nextPeriodStart
        }
        return projections
    }

    /**
     * Check if a target date falls inside the fertile window.
     */
    fun isDateInFertileWindow(targetDate: LocalDate, prediction: CyclePrediction): Boolean {
        return !targetDate.isBefore(prediction.fertileWindowStart) && !targetDate.isAfter(prediction.fertileWindowEnd)
    }

    /**
     * Check if a target date is the peak ovulation day.
     */
    fun isOvulationDay(targetDate: LocalDate, prediction: CyclePrediction): Boolean {
        return targetDate.isEqual(prediction.ovulationDate)
    }
}
