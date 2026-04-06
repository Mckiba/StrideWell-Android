package com.stridewell.app.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object DateUtils {

    // MARK: - YYYY-MM-DD Formatter

    /** POSIX formatter for YYYY-MM-DD strings (plan dates, week keys). */
    val isoDate: SimpleDateFormat by lazy {
        SimpleDateFormat("yyyy-MM-dd", Locale.US)
    }

    // MARK: - ISO-8601 DateTime Parsing

    /** Parses an ISO-8601 string — tries fractional seconds first, then without. */
    fun parseISO8601(iso: String): Date? {
        val withFractional = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val withoutFractional = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val withOffset = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US)
        val withFractionalOffset = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.US)

        return withFractional.parseOrNull(iso)
            ?: withoutFractional.parseOrNull(iso)
            ?: withFractionalOffset.parseOrNull(iso)
            ?: withOffset.parseOrNull(iso)
    }

    private fun SimpleDateFormat.parseOrNull(s: String): Date? = try { parse(s) } catch (_: Exception) { null }

    // MARK: - Display Formatters

    /** "Mar 8, 2026 at 3:00 PM" */
    fun displayDateTime(iso: String): String {
        val date = parseISO8601(iso) ?: return iso.take(10)
        return displayDateTimeFormatter.format(date)
    }

    /** "Mar 8, 2026" */
    fun displayDate(iso: String): String {
        val date = parseISO8601(iso) ?: return iso.take(10)
        return displayDateFormatter.format(date)
    }

    private val displayDateTimeFormatter: SimpleDateFormat by lazy {
        SimpleDateFormat("MMM d, yyyy 'at' h:mm a", Locale.getDefault())
    }

    private val displayDateFormatter: SimpleDateFormat by lazy {
        SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    }

    // MARK: - Activity Card Formatters

    /** "February 18, 2025" */
    fun activityDate(iso: String): String {
        val date = parseISO8601(iso) ?: return iso.take(10)
        return activityDateFormatter.format(date)
    }

    /** "6:16 PM" */
    fun activityTime(iso: String): String {
        val date = parseISO8601(iso) ?: return ""
        return activityTimeFormatter.format(date)
    }

    private val activityDateFormatter: SimpleDateFormat by lazy {
        SimpleDateFormat("MMMM d, yyyy", Locale.getDefault())
    }

    private val activityTimeFormatter: SimpleDateFormat by lazy {
        SimpleDateFormat("h:mm a", Locale.getDefault())
    }

    // MARK: - Plan Day Date Formatter

    /** "Friday, March 20" — for YYYY-MM-DD plan day dates. */
    fun planDayDate(dateString: String): String {
        val date = parse(dateString) ?: return dateString
        return planDayDateFormatter.format(date)
    }

    private val planDayDateFormatter: SimpleDateFormat by lazy {
        SimpleDateFormat("EEEE, MMMM d", Locale.getDefault())
    }

    // MARK: - Workout Date Formatters

    /** Abbreviated day name: "Mon", "Tue", … */
    val dayAbbrevFormatter: SimpleDateFormat by lazy {
        SimpleDateFormat("EEE", Locale.getDefault())
    }

    /** Day number: "3", "14", … */
    val dayNumberFormatter: SimpleDateFormat by lazy {
        SimpleDateFormat("d", Locale.getDefault())
    }

    /** Card-style date: "Monday, Feb 23" */
    val workoutCardDateFormatter: SimpleDateFormat by lazy {
        SimpleDateFormat("EEEE, MMM d", Locale.getDefault())
    }

    // MARK: - Monday Computation

    /**
     * Returns the Monday (ISO 8601 week start) of the week containing [date].
     * Sunday is treated as the end of the prior week.
     */
    fun mondayOfWeek(containing: Date): Date {
        val cal = Calendar.getInstance()
        cal.time = containing
        val weekday = cal.get(Calendar.DAY_OF_WEEK)
        // DAY_OF_WEEK: SUNDAY=1, MONDAY=2, ..., SATURDAY=7
        val daysToSubtract = if (weekday == Calendar.SUNDAY) 6 else weekday - Calendar.MONDAY
        cal.add(Calendar.DAY_OF_YEAR, -daysToSubtract)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.time
    }

    /** ISO string (YYYY-MM-DD) for the Monday of the week containing [date]. */
    fun mondayString(containing: Date): String = format(mondayOfWeek(containing))

    // MARK: - Week Navigation

    /** Returns the Monday one week before the given Monday. */
    fun previousMonday(from: Date): Date {
        val cal = Calendar.getInstance()
        cal.time = from
        cal.add(Calendar.DAY_OF_YEAR, -7)
        return cal.time
    }

    /** Returns the Monday one week after the given Monday. */
    fun nextMonday(from: Date): Date {
        val cal = Calendar.getInstance()
        cal.time = from
        cal.add(Calendar.DAY_OF_YEAR, 7)
        return cal.time
    }

    // MARK: - Week Range Label

    /** Human-readable week range: "Mar 3 – 9" or "Feb 24 – Mar 2" (cross-month). */
    fun weekRangeLabel(monday: Date): String {
        val cal = Calendar.getInstance()
        cal.time = monday
        cal.add(Calendar.DAY_OF_YEAR, 6)
        val sunday = cal.time

        val monCal = Calendar.getInstance().apply { time = monday }
        val sunCal = Calendar.getInstance().apply { time = sunday }

        val monthDay = SimpleDateFormat("MMM d", Locale.getDefault())
        val dayOnly  = SimpleDateFormat("d",     Locale.getDefault())

        return if (monCal.get(Calendar.MONTH) == sunCal.get(Calendar.MONTH)) {
            "${monthDay.format(monday)} – ${dayOnly.format(sunday)}"
        } else {
            "${monthDay.format(monday)} – ${monthDay.format(sunday)}"
        }
    }

    // MARK: - Parse / Format (YYYY-MM-DD)

    /** Parse a YYYY-MM-DD string into a Date. */
    fun parse(dateString: String): Date? = try { isoDate.parse(dateString) } catch (_: Exception) { null }

    /** Format a Date as YYYY-MM-DD. */
    fun format(date: Date): String = isoDate.format(date)
}
