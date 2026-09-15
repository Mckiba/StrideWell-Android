package com.stridewell.app.util

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Date
import java.util.Locale

class ActivityPeriodsTest {

    private lateinit var originalLocale: Locale

    @Before
    fun setUp() {
        originalLocale = Locale.getDefault()
        Locale.setDefault(Locale.US)
    }

    @After
    fun tearDown() {
        Locale.setDefault(originalLocale)
    }

    private fun day(value: String): Date = DateUtils.parse(value)!!

    private fun List<Date>.formatted() = map(DateUtils::format)

    // Wednesday
    private val today = day("2026-09-16")

    @Test
    fun periodStart_alignsToMondayMonthAndYear() {
        assertEquals("2026-09-14", DateUtils.format(DateUtils.periodStart(ActivityRange.WEEK, today)))
        assertEquals("2026-09-14", DateUtils.format(DateUtils.periodStart(ActivityRange.WEEK, day("2026-09-20"))))
        assertEquals("2026-09-01", DateUtils.format(DateUtils.periodStart(ActivityRange.MONTH, today)))
        assertEquals("2026-01-01", DateUtils.format(DateUtils.periodStart(ActivityRange.YEAR, today)))
    }

    @Test
    fun periodStarts_walkBackToTheFirstRunsPeriod() {
        // A Sunday first run belongs to the week starting Monday 2026-08-24.
        val weeks = DateUtils.periodStarts(ActivityRange.WEEK, day("2026-08-30"), today)
        assertEquals(listOf("2026-09-14", "2026-09-07", "2026-08-31", "2026-08-24"), weeks.formatted())

        val months = DateUtils.periodStarts(ActivityRange.MONTH, day("2026-06-15"), today)
        assertEquals(listOf("2026-09-01", "2026-08-01", "2026-07-01", "2026-06-01"), months.formatted())
    }

    @Test
    fun periodStarts_withoutRunsOrForAll() {
        assertEquals(listOf("2026-09-14"), DateUtils.periodStarts(ActivityRange.WEEK, null, today).formatted())
        assertTrue(DateUtils.periodStarts(ActivityRange.ALL, day("2024-01-01"), today).isEmpty())
    }

    @Test
    fun periodLabel_namesCurrentAndPastPeriods() {
        assertEquals("This Week", DateUtils.periodLabel(ActivityRange.WEEK, day("2026-09-14"), today))
        assertEquals("Last Week", DateUtils.periodLabel(ActivityRange.WEEK, day("2026-09-07"), today))
        assertEquals("Aug 24 – 30", DateUtils.periodLabel(ActivityRange.WEEK, day("2026-08-24"), today))

        assertEquals("This Month", DateUtils.periodLabel(ActivityRange.MONTH, day("2026-09-01"), today))
        assertEquals("August", DateUtils.periodLabel(ActivityRange.MONTH, day("2026-08-01"), today))
        assertEquals("Aug 2025", DateUtils.periodLabel(ActivityRange.MONTH, day("2025-08-01"), today))

        assertEquals("This Year", DateUtils.periodLabel(ActivityRange.YEAR, day("2026-01-01"), today))
        assertEquals("2025", DateUtils.periodLabel(ActivityRange.YEAR, day("2025-01-01"), today))
        assertEquals("All Time", DateUtils.periodLabel(ActivityRange.ALL, day("2024-01-01"), today))
    }

    @Test
    fun bucketWindow_coversTheWholeBucket() {
        assertEquals("2026-09-14" to "2026-09-14", DateUtils.bucketWindow(ActivityRange.WEEK, "2026-09-14"))
        assertEquals("2028-02-01" to "2028-02-29", DateUtils.bucketWindow(ActivityRange.YEAR, "2028-02"))
        assertEquals("2025-01-01" to "2025-12-31", DateUtils.bucketWindow(ActivityRange.ALL, "2025"))
    }

    @Test
    fun bucketAxisLabel_isShortPerRange() {
        assertEquals("M", DateUtils.bucketAxisLabel(ActivityRange.WEEK, "2026-09-14"))
        assertEquals("8", DateUtils.bucketAxisLabel(ActivityRange.MONTH, "2026-09-08"))
        assertEquals("M", DateUtils.bucketAxisLabel(ActivityRange.YEAR, "2026-03"))
        assertEquals("2026", DateUtils.bucketAxisLabel(ActivityRange.ALL, "2026"))
    }

    @Test
    fun distanceValue_convertsToDisplayUnits() {
        assertEquals(8.0, FormatUtils.distanceValue(8_000.0, UnitSystem.METRIC), 0.0001)
        assertEquals(1.0, FormatUtils.distanceValue(1_609.344, UnitSystem.IMPERIAL), 0.0001)
        assertEquals("Miles", FormatUtils.distanceUnitName(UnitSystem.IMPERIAL))
    }
}
