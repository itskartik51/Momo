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

object ApyBdayCalculator {

    private const val CLINICAL_LUTEAL_PHASE_DAYS = 14L
    private const val PRE_OVULATION_FERTILE_DAYS = 4L
    private const val POST_OVULATION_BUFFER_DAYS = 2L

    /**
     * Core calculation engine:
     * Takes raw logged dates, filters biological outliers, applies Weighted Moving Average (WMA),
     * and derives ovulation and fertility windows.
     */
    fun calculateCycle(dates: List<LocalDate>): CyclePrediction? {
        if (dates.size < 2) return null

        // 1. Sort chronologically (oldest to newest)
        val sortedDates = dates.distinct().sorted()

        // 2. Compute consecutive intervals (in days)
        val rawIntervals = mutableListOf<Long>()
        for (i in 0 until sortedDates.size - 1) {
            val gap = ChronoUnit.DAYS.between(sortedDates[i], sortedDates[i + 1])
            // Medical sanity filter: exclude unnatural cycle gaps (e.g., missed logging > 60 days)
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

        // 4. Smart Outlier Filtering
        val filteredIntervals = filterOutliers(recentIntervals)

        // 5. Weighted Moving Average (WMA) Calculation
        val cycleLength = calculateWeightedAverage(filteredIntervals)

        // 6. Forward Predictions
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
     * Check if a specific target date falls within the fertile window
     */
    fun isDateInFertileWindow(targetDate: LocalDate, prediction: CyclePrediction): Boolean {
        return !targetDate.isBefore(prediction.fertileWindowStart) && !targetDate.isAfter(prediction.fertileWindowEnd)
    }

    /**
     * Check if a specific target date is the exact ovulation day
     */
    fun isOvulationDay(targetDate: LocalDate, prediction: CyclePrediction): Boolean {
        return targetDate.isEqual(prediction.ovulationDate)
    }
}
