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

object ApyBdayCalculator {

    // Clinical standard offset: Period start minus 15 days produces the exact 1-day weekday shift
    private const val CLINICAL_LUTEAL_PHASE_DAYS = 15L
    private const val PRE_OVULATION_FERTILE_DAYS = 4L
    private const val POST_OVULATION_BUFFER_DAYS = 2L

    /**
     * Core calculation engine (Option B - Medically Pure):
     * Takes raw logged dates, filters single biological anomalies (spread > 4),
     * derives dynamic cycle length via Weighted Moving Average (WMA),
     * and maps clinical ovulation and fertility windows.
     */
    fun calculateCycle(dates: List<LocalDate>): CyclePrediction? {
        if (dates.size < 2) return null

        // 1. Sort chronologically (oldest to newest)
        val sortedDates = dates.distinct().sorted()

        // 2. Compute consecutive intervals (in days)
        val rawIntervals = mutableListOf<Long>()
        for (i in 0 until sortedDates.size - 1) {
            val gap = ChronoUnit.DAYS.between(sortedDates[i], sortedDates[i + 1])
            // Medical sanity filter: exclude unnatural gaps (missed logging > 60 days)
            if (gap in 18..60) {
                rawIntervals.add(gap)
            }
        }

        if (rawIntervals.isEmpty()) return null

        // 3. Take up to the 6 most recent intervals
        val recentIntervals = if (rawIntervals.size > 6) {
            rawIntervals.takeLast(6)
        } else {
            rawIntervals
        }

        // 4. Biological Outlier Filtering (Preserves natural rhythm)
        val filteredIntervals = filterOutliers(recentIntervals)

        // 5. Weighted Moving Average (WMA) Calculation (Dynamic, non-hardcoded)
        val cycleLength = calculateWeightedAverage(filteredIntervals)

        // 6. Forward Calculations
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
     * - If max - min <= 4 days: The cycle is biologically consistent. Keep all intervals.
     * - If max - min > 4 days: Identify the single most extreme outlier relative to the median
     *   and remove only ONE instance of it.
     */
    private fun filterOutliers(intervals: List<Long>): List<Long> {
        if (intervals.size < 4) return intervals

        val minVal = intervals.minOrNull() ?: return intervals
        val maxVal = intervals.maxOrNull() ?: return intervals

        // Normal biological variation: no outlier
        if (maxVal - minVal <= 4) {
            return intervals
        }

        // Calculate median
        val sorted = intervals.sorted()
        val median = if (sorted.size % 2 == 0) {
            (sorted[sorted.size / 2 - 1] + sorted[sorted.size / 2]) / 2.0
        } else {
            sorted[sorted.size / 2].toDouble()
        }

        // Check which extreme deviated further from the median
        val minDiff = abs(minVal - median)
        val maxDiff = abs(maxVal - median)

        val outlierToRemove = if (maxDiff >= minDiff) maxVal else minVal

        // Drop ONLY one instance of the outlier
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
     * Formula: Sum(interval_i * weight_i) / Sum(weight_i)
     */
    private fun calculateWeightedAverage(intervals: List<Long>): Long {
        if (intervals.isEmpty()) return 28L

        var weightedSum = 0.0
        var totalWeights = 0

        for (i in intervals.indices) {
            val weight = i + 1 // Older = 1, Most recent = intervals.size
            weightedSum += intervals[i] * weight
            totalWeights += weight
        }

        val average = (weightedSum / totalWeights).roundToLong()
        return average.coerceIn(21L, 40L)
    }

    /**
     * Generates a chain of forward cycle projections dynamically based on the calculated rhythm.
     */
    fun projectFutureCycles(prediction: CyclePrediction, count: Int = 12): List<ProjectedCycle> {
        val projections = mutableListOf<ProjectedCycle>()
        var currentStart = prediction.nextPredictedDate

        for (i in 0 until count) {
            val nextCycleStart = currentStart.plusDays(prediction.calculatedCycleLength)
            val ovulation = nextCycleStart.minusDays(CLINICAL_LUTEAL_PHASE_DAYS)
            val fertileStart = ovulation.minusDays(PRE_OVULATION_FERTILE_DAYS)
            val fertileEnd = ovulation.plusDays(POST_OVULATION_BUFFER_DAYS)

            projections.add(
                ProjectedCycle(
                    periodStartDate = currentStart,
                    ovulationDate = ovulation,
                    fertileWindowStart = fertileStart,
                    fertileWindowEnd = fertileEnd
                )
            )
            currentStart = nextCycleStart
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
