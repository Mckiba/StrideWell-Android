package com.stridewell.app.ui.main.activities

import com.stridewell.app.model.Run
import com.stridewell.app.util.ActivityRange
import com.stridewell.app.util.DateUtils
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class ActivitySectionsTest {

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

    private val utc = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    /** Local noon on [date], so the run's local day is [date] in any timezone. */
    private fun localNoon(date: String): Date = Date(DateUtils.parse(date)!!.time + 12 * 60 * 60 * 1000)

    private fun run(id: String, date: String) = Run(
        id = id,
        provider = "strava",
        sport_type = "Run",
        start_time = utc.format(localNoon(date)),
        distance_m = 5_000.0,
        duration_s = 1_500,
        elevation_gain_m = 10.0,
    )

    private val today = localNoon("2026-09-16")
    private val thisWeek = DateUtils.parse("2026-09-14")!!

    @Test
    fun currentPeriod_splitsTodayFromEarlier() {
        val runs = listOf(run("a", "2026-09-16"), run("b", "2026-09-15"), run("c", "2026-09-14"))
        val sections = buildActivitySections(ActivityRange.WEEK, thisWeek, runs, today)

        assertEquals(listOf("September 16", "Earlier This Week"), sections.map { it.title })
        assertEquals(listOf("a"), sections[0].runs.map { it.id })
        assertEquals(listOf("b", "c"), sections[1].runs.map { it.id })
    }

    @Test
    fun currentPeriod_withoutTodayRuns_hasOnlyEarlier() {
        val sections = buildActivitySections(ActivityRange.MONTH, DateUtils.parse("2026-09-01")!!, listOf(run("a", "2026-09-03")), today)
        assertEquals(listOf("Earlier This Month"), sections.map { it.title })
    }

    @Test
    fun pastPeriod_isOneSectionTitledWithItsLabel() {
        val lastWeek = DateUtils.parse("2026-09-07")!!
        val sections = buildActivitySections(ActivityRange.WEEK, lastWeek, listOf(run("a", "2026-09-08")), today)
        assertEquals(listOf("Last Week"), sections.map { it.title })
    }

    @Test
    fun noRuns_hasNoSections() {
        assertTrue(buildActivitySections(ActivityRange.WEEK, thisWeek, emptyList(), today).isEmpty())
    }
}
