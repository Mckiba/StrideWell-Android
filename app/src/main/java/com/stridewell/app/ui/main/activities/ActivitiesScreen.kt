package com.stridewell.app.ui.main.activities

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stridewell.app.model.Run
import com.stridewell.app.model.RunRoute
import com.stridewell.app.model.RunSummaryBucket
import com.stridewell.app.model.RunSummaryResponse
import com.stridewell.app.model.RunSummaryTotals
import com.stridewell.app.ui.background.heatmap.HeatmapBackgroundView
import com.stridewell.app.ui.background.heatmap.HeatmapViewModel
import com.stridewell.app.ui.background.weather.StormOverlayView
import com.stridewell.app.ui.background.weather.WeatherViewModel
import com.stridewell.app.ui.components.ActivityBucketCallout
import com.stridewell.app.ui.components.ActivityCard
import com.stridewell.app.ui.components.ActivityRangePicker
import com.stridewell.app.ui.components.ActivityStatsRow
import com.stridewell.app.ui.components.ActivityVolumeChart
import com.stridewell.app.ui.components.CenteredMessage
import com.stridewell.app.ui.components.WorkoutCard
import com.stridewell.app.ui.components.skeleton.ActivitiesOverviewSkeleton
import com.stridewell.app.ui.components.skeleton.ActivityListSkeleton
import com.stridewell.app.ui.main.rememberNavBarBottomInset
import com.stridewell.app.ui.theme.ActivityHeroUnitStyle
import com.stridewell.app.ui.theme.ActivityHeroValueStyle
import com.stridewell.app.ui.theme.ActivityPeriodLabelStyle
import com.stridewell.app.ui.theme.ActivitySectionTitleStyle
import com.stridewell.app.ui.theme.CornerRadius
import com.stridewell.app.ui.theme.Spacing
import com.stridewell.app.ui.theme.StridewellTheme
import com.stridewell.app.util.ActivityRange
import com.stridewell.app.util.DateUtils
import com.stridewell.app.util.FormatUtils
import com.stridewell.app.util.UnitSystem
import java.util.Date
import kotlin.math.roundToInt

/**
 * Activities overview: range picker, period dropdown, period totals, a volume
 * chart, and the period's activities grouped by date. The full searchable list
 * is [AllActivitiesScreen], opened from the nav bar's Search button.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivitiesScreen(
    onNavigateToDetail: (String) -> Unit,
    hasLocationPermission: Boolean,
    heatmapViewModel: HeatmapViewModel,
    weatherViewModel: WeatherViewModel,
    modifier: Modifier = Modifier,
    viewModel: ActivitiesOverviewViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val weatherState by weatherViewModel.uiState.collectAsStateWithLifecycle()
    val isDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f

    Box(modifier = modifier.fillMaxSize()) {
        HeatmapBackgroundView(
            hasLocationPermission = hasLocationPermission,
            isDarkTheme = isDarkTheme,
            heatmapViewModel = heatmapViewModel
        )
        StormOverlayView(
            condition = weatherState.activeCondition,
            modifier = Modifier.fillMaxSize()
        )

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (val state = uiState.screenState) {
                    ActivitiesOverviewViewModel.ScreenState.Loading ->
                        ActivitiesOverviewSkeleton(modifier = Modifier.fillMaxSize())

                    ActivitiesOverviewViewModel.ScreenState.Empty -> EmptyActivities()

                    is ActivitiesOverviewViewModel.ScreenState.Error ->
                        CenteredMessage(message = state.message, onRetry = viewModel::refresh)

                    ActivitiesOverviewViewModel.ScreenState.Loaded -> OverviewContent(
                        uiState = uiState,
                        onSelectRange = viewModel::selectRange,
                        onSelectPeriod = viewModel::selectPeriod,
                        onRefresh = viewModel::refresh,
                        onLoadMore = viewModel::loadMore,
                        loadRun = viewModel::runForBucket,
                        onOpenRun = onNavigateToDetail
                    )
                }
            }
        }
    }

    LaunchedEffect(hasLocationPermission) {
        weatherViewModel.fetchIfNeeded(hasLocationPermission)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OverviewContent(
    uiState: ActivitiesOverviewViewModel.UiState,
    onSelectRange: (ActivityRange) -> Unit,
    onSelectPeriod: (Date) -> Unit,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    loadRun: suspend (RunSummaryBucket) -> Run?,
    onOpenRun: (String) -> Unit,
) {
    val listState = rememberLazyListState()
    val shouldLoadMore by remember {
        derivedStateOf {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
            val total = listState.layoutInfo.totalItemsCount
            total > 0 && lastVisible >= total - 3
        }
    }
    LaunchedEffect(shouldLoadMore, uiState.hasMore) {
        if (shouldLoadMore && uiState.hasMore) onLoadMore()
    }

    val sections = remember(uiState.range, uiState.periodStart, uiState.runs) { uiState.sections() }

    PullToRefreshBox(
        isRefreshing = uiState.isRefreshing,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(
                start = Spacing.md,
                end = Spacing.md,
                top = Spacing.sm,
                bottom = Spacing.md + rememberNavBarBottomInset()
            ),
            verticalArrangement = Arrangement.spacedBy(Spacing.xl2)
        ) {
            item(key = "range") {
                ActivityRangePicker(selected = uiState.range, onSelect = onSelectRange)
            }

            uiState.summary?.let { summary ->
                item(key = "summary") {
                    SummarySection(
                        uiState = uiState,
                        summary = summary,
                        onSelectPeriod = onSelectPeriod,
                        loadRun = loadRun,
                        onOpenRun = onOpenRun,
                        modifier = Modifier.alpha(if (uiState.isLoadingSummary) 0.5f else 1f)
                    )
                }
            }

            if (sections.isEmpty()) {
                item(key = "no-runs") {
                    if (uiState.isLoadingRuns) {
                        ActivityListSkeleton(cardCount = 3)
                    } else {
                        Text(
                            text = "No runs in this period",
                            style = ActivitySectionTitleStyle,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                sections.forEach { section ->
                    item(key = "section-${section.title}") {
                        Text(
                            text = section.title,
                            style = ActivitySectionTitleStyle,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    items(section.runs, key = { it.id }) { run ->
                        val planDay = uiState.planDaysByRunId[run.id]
                        if (planDay != null) {
                            WorkoutCard(
                                day = planDay,
                                unitSystem = uiState.unitSystem,
                                onClick = { onOpenRun(run.id) }
                            )
                        } else {
                            ActivityCard(
                                run = run,
                                unitSystem = uiState.unitSystem,
                                onClick = { onOpenRun(run.id) }
                            )
                        }
                    }
                }
            }

            if (uiState.isLoadingMore) {
                item(key = "loading-more") {
                    ActivityListSkeleton(cardCount = 1, showsHeader = false)
                }
            }
        }
    }
}

/**
 * Period dropdown, distance, stats row and chart. A selected bar's callout is
 * placed just above the chart, overlapping the stats, without shifting layout.
 */
@Composable
private fun SummarySection(
    uiState: ActivitiesOverviewViewModel.UiState,
    summary: RunSummaryResponse,
    onSelectPeriod: (Date) -> Unit,
    loadRun: suspend (RunSummaryBucket) -> Run?,
    onOpenRun: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val chartRange = ActivityRange.fromKey(summary.range) ?: uiState.range
    var selectedKey by remember(summary) { mutableStateOf<String?>(null) }
    val selectedBucket = summary.buckets.firstOrNull { it.key == selectedKey }
    val selectedRun by produceState<Run?>(initialValue = null, selectedBucket) {
        value = selectedBucket?.takeIf { it.run_count == 1 }?.let { loadRun(it) }
    }
    var chartTop by remember { mutableIntStateOf(0) }
    val calloutGap = with(LocalDensity.current) { Spacing.sm.roundToPx() }

    Layout(
        modifier = modifier,
        content = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                PeriodMenu(
                    range = uiState.range,
                    periodStart = uiState.periodStart,
                    periodStarts = uiState.periodStarts,
                    onSelect = onSelectPeriod
                )
                HeroDistance(metres = summary.totals.distance_m, unitSystem = uiState.unitSystem)
                ActivityStatsRow(totals = summary.totals, unitSystem = uiState.unitSystem)
                Spacer(modifier = Modifier.height(Spacing.xl2 - Spacing.sm))
                ActivityVolumeChart(
                    range = chartRange,
                    buckets = summary.buckets,
                    unitSystem = uiState.unitSystem,
                    selectedKey = selectedKey,
                    onSelectedKeyChange = { selectedKey = it },
                    modifier = Modifier.onGloballyPositioned {
                        chartTop = it.positionInParent().y.roundToInt()
                    }
                )
            }
            if (selectedBucket != null) {
                ActivityBucketCallout(
                    range = chartRange,
                    bucket = selectedBucket,
                    run = selectedRun,
                    unitSystem = uiState.unitSystem,
                    onOpenRun = { onOpenRun(it.id) }
                )
            }
        }
    ) { measurables, constraints ->
        val content = measurables[0].measure(constraints)
        val callout = measurables.getOrNull(1)?.measure(constraints.copy(minHeight = 0))
        layout(content.width, content.height) {
            content.place(0, 0)
            callout?.place(0, (chartTop - callout.height - calloutGap).coerceAtLeast(0))
        }
    }
}

@Composable
private fun PeriodMenu(
    range: ActivityRange,
    periodStart: Date,
    periodStarts: List<Date>,
    onSelect: (Date) -> Unit,
) {
    val label = DateUtils.periodLabel(range, periodStart)
    val textColor = MaterialTheme.colorScheme.onSurface

    if (range == ActivityRange.ALL) {
        Text(text = label, style = ActivityPeriodLabelStyle, color = textColor)
        return
    }

    var expanded by remember { mutableStateOf(false) }
    Box {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(CornerRadius.sm))
                .clickable { expanded = true }
                .padding(vertical = Spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
        ) {
            Text(text = label, style = ActivityPeriodLabelStyle, color = textColor)
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = "Choose period",
                tint = textColor,
                modifier = Modifier.size(20.dp)
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.heightIn(max = 360.dp)
        ) {
            periodStarts.forEach { start ->
                DropdownMenuItem(
                    text = { Text(DateUtils.periodLabel(range, start)) },
                    onClick = {
                        expanded = false
                        onSelect(start)
                    },
                    trailingIcon = if (start == periodStart) {
                        { Icon(imageVector = Icons.Default.Check, contentDescription = "Selected") }
                    } else {
                        null
                    }
                )
            }
        }
    }
}

@Composable
private fun HeroDistance(metres: Double, unitSystem: UnitSystem) {
    Column {
        Text(
            text = "%.1f".format(FormatUtils.distanceValue(metres, unitSystem)),
            style = ActivityHeroValueStyle,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = FormatUtils.distanceUnitName(unitSystem),
            style = ActivityHeroUnitStyle,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun EmptyActivities() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(Spacing.lg),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.xs)
        ) {
            Text(
                text = "No activities yet",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Connect Strava to see your activities",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Preview(showBackground = true, heightDp = 1000)
@Composable
private fun ActivitiesOverviewPreview() {
    StridewellTheme {
        val days = listOf("2026-09-14", "2026-09-15", "2026-09-16", "2026-09-17", "2026-09-18", "2026-09-19", "2026-09-20")
        val distances = listOf(8_046.0, 0.0, 12_874.0, 4_828.0, 0.0, 19_312.0, 6_437.0)
        val summary = RunSummaryResponse(
            range = "week",
            start = "2026-09-14",
            end = "2026-09-21",
            totals = RunSummaryTotals(
                distance_m = distances.sum(),
                run_count = 5,
                duration_s = 16_200,
                avg_pace_s_per_km = 314.6
            ),
            buckets = days.zip(distances) { day, distance ->
                RunSummaryBucket(key = day, distance_m = distance, run_count = if (distance > 0) 1 else 0)
            },
            first_run_date = "2024-03-02"
        )
        val polyline = "o}mlHlariVoBiC{A{B_BaCuBeDyF_Jw@yAeAuBmAaCkBuDeBkDoAsB}@cBqAeCqBiEiBkD{BkEaC_FuAgC"
        val runs = listOf(
            Run(
                id = "1", provider = "strava", sport_type = "Run", title = "Long Run",
                start_time = "2026-09-19T15:00:00Z", distance_m = 19_312.0, duration_s = 6_300,
                avg_pace_s_per_km = 326.2, elevation_gain_m = 120.0, route = RunRoute(polyline)
            ),
            Run(
                id = "2", provider = "strava", sport_type = "Run", title = "Easy Run",
                start_time = "2026-09-16T13:30:00Z", distance_m = 12_874.0, duration_s = 4_200,
                avg_pace_s_per_km = 326.2, elevation_gain_m = 40.0, route = RunRoute(polyline)
            )
        )
        OverviewContent(
            uiState = ActivitiesOverviewViewModel.UiState(
                screenState = ActivitiesOverviewViewModel.ScreenState.Loaded,
                unitSystem = UnitSystem.IMPERIAL,
                periodStart = DateUtils.parse("2026-09-14")!!,
                summary = summary,
                runs = runs
            ),
            onSelectRange = {},
            onSelectPeriod = {},
            onRefresh = {},
            onLoadMore = {},
            loadRun = { null },
            onOpenRun = {}
        )
    }
}
