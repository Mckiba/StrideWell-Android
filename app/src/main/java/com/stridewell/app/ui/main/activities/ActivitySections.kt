package com.stridewell.app.ui.main.activities

import com.stridewell.app.model.Run
import com.stridewell.app.util.ActivityRange
import com.stridewell.app.util.DateUtils
import java.util.Date

data class ActivitySection(val title: String, val runs: List<Run>)

/**
 * Groups a period's runs for display. The current period splits into today's
 * date and "Earlier This <Period>"; a past period is one section titled with
 * its dropdown label.
 */
fun buildActivitySections(
    range: ActivityRange,
    periodStart: Date,
    runs: List<Run>,
    today: Date = Date(),
): List<ActivitySection> {
    if (runs.isEmpty()) return emptyList()

    val isCurrent = range == ActivityRange.ALL || periodStart == DateUtils.periodStart(range, today)
    if (!isCurrent) {
        return listOf(ActivitySection(DateUtils.periodLabel(range, periodStart, today), runs))
    }

    val todayKey = DateUtils.format(today)
    val (todayRuns, earlierRuns) = runs.partition { run ->
        DateUtils.parseISO8601(run.start_time)?.let(DateUtils::format) == todayKey
    }
    return buildList {
        if (todayRuns.isNotEmpty()) add(ActivitySection(DateUtils.sectionDayLabel(today), todayRuns))
        if (earlierRuns.isNotEmpty()) add(ActivitySection(DateUtils.earlierLabel(range), earlierRuns))
    }
}
