package com.personal.momo.Proximity

import android.location.Location

object ProximityMath {

    /**
     * Calculates geodesic distance between two geographic coordinates using the Android OS WGS84 model.
     * Returns distance in meters with sub-meter precision.
     */
    fun calculateDistanceMeters(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double
    ): Double {
        val results = FloatArray(1)
        Location.distanceBetween(lat1, lon1, lat2, lon2, results)
        return results[0].toDouble()
    }

    /**
     * Calculates initial bearing (forward azimuth) from coordinate 1 to coordinate 2
     * using the WGS84 geodesic model.
     * Returns bearing in degrees normalized to [0, 360).
     */
    fun calculateBearing(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double
    ): Float {
        val locA = Location("source").apply {
            latitude = lat1
            longitude = lon1
        }
        val locB = Location("target").apply {
            latitude = lat2
            longitude = lon2
        }
        val initialBearing = locA.bearingTo(locB)
        return ((initialBearing % 360f) + 360f) % 360f
    }

    /**
     * Returns standard 8-point cardinal compass direction for a given bearing degree.
     */
    fun getCardinalDirection(bearing: Double): String {
        val directions = arrayOf(
            "North", "Northeast", "East", "Southeast",
            "South", "Southwest", "West", "Northwest"
        )
        val normalized = ((bearing % 360.0) + 360.0) % 360.0
        val index = (((normalized + 22.5) / 45.0).toInt()) % 8
        return directions[index]
    }

    /**
     * Ghost Exit Filter:
     * Discards inaccurate location updates (e.g. cell tower jumping indoors) where accuracy > 100 meters.
     */
    fun isValidAccuracy(accuracyMeters: Float): Boolean {
        return accuracyMeters in 0.0f..100.0f
    }
}
