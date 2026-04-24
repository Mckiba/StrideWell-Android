package com.stridewell.app.util

enum class UnitSystem { METRIC, IMPERIAL }

object FormatUtils {

    /**
     * Converts metres to a display string.
     * Metric:   8000 → "8.0 km",  12500 → "12.5 km"
     * Imperial: 8000 → "5.0 mi",  12500 → "7.8 mi"
     */
    fun formatDistance(meters: Double, unit: UnitSystem = UnitSystem.METRIC): String =
        when (unit) {
            UnitSystem.METRIC   -> "%.1f km".format(meters / 1000.0)
            UnitSystem.IMPERIAL -> "%.1f mi".format(meters / 1609.344)
        }

    fun distance(meters: Double, unit: UnitSystem = UnitSystem.METRIC): String =
        formatDistance(meters, unit)

    /**
     * Converts seconds-per-kilometre to a pace string.
     * Metric:   330 → "5:30 /km",  240 → "4:00 /km"
     * Imperial: 330 → "8:51 /mi",  240 → "6:26 /mi"
     */
    fun formatPace(secondsPerKm: Double, unit: UnitSystem = UnitSystem.METRIC): String =
        when (unit) {
            UnitSystem.METRIC -> {
                val total   = secondsPerKm.toInt()
                val minutes = total / 60
                val seconds = total % 60
                "%d:%02d /km".format(minutes, seconds)
            }
            UnitSystem.IMPERIAL -> {
                val total   = (secondsPerKm * 1.60934).toInt()
                val minutes = total / 60
                val seconds = total % 60
                "%d:%02d /mi".format(minutes, seconds)
            }
        }

    fun pace(secondsPerKm: Double, unit: UnitSystem = UnitSystem.METRIC): String =
        formatPace(secondsPerKm, unit)

    /**
     * Formats a min/max pace range.
     * Note: faster pace = smaller s/km. The range is displayed
     * `min–max` (keeping numerical ordering), which matches iOS.
     */
    fun formatPaceRange(
        minSecondsPerKm: Double,
        maxSecondsPerKm: Double,
        unit: UnitSystem = UnitSystem.METRIC
    ): String {
        val minStr = formatPace(minSecondsPerKm, unit)
        // Strip the unit suffix off the first value so we render "5:20–5:40 /km"
        val compactMin = minStr.substringBefore(" ")
        return "$compactMin–${formatPace(maxSecondsPerKm, unit)}"
    }

    fun paceRange(
        minSecondsPerKm: Double,
        maxSecondsPerKm: Double,
        unit: UnitSystem = UnitSystem.METRIC
    ): String = formatPaceRange(minSecondsPerKm, maxSecondsPerKm, unit)

    /** Percent formatter: 0.0–1.0 → "NN%". */
    fun formatPercent(fraction: Double): String = "${(fraction * 100).toInt()}%"

    /**
     * Converts total seconds to a duration string (unit-system independent).
     * Under 1 hour: "mm:ss" — e.g. 2700 → "45:00"
     * 1 hour or more: "h:mm" — e.g. 5400 → "1:30"
     */
    fun formatDuration(seconds: Int): String =
        if (seconds < 3600) {
            val m = seconds / 60
            val s = seconds % 60
            "%d:%02d".format(m, s)
        } else {
            val h = seconds / 3600
            val m = (seconds % 3600) / 60
            "%d:%02d".format(h, m)
        }

    fun duration(seconds: Int): String =
        formatDuration(seconds)
}
