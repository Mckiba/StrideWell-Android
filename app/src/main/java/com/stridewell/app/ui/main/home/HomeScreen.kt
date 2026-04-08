package com.stridewell.app.ui.main.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stridewell.R
import com.stridewell.app.model.DecisionRecord
import com.stridewell.app.model.PlanDay
import com.stridewell.app.model.PlanWeekResponse
import com.stridewell.app.model.Run
import com.stridewell.app.model.WorkoutType
import com.stridewell.app.ui.background.heatmap.HeatmapBackgroundView
import com.stridewell.app.ui.background.heatmap.HeatmapViewModel
import com.stridewell.app.ui.background.weather.StormOverlayView
import com.stridewell.app.ui.background.weather.WeatherViewModel
import com.stridewell.app.ui.components.ActivityCard
import com.stridewell.app.ui.components.ActivityBannerView
import com.stridewell.app.ui.components.GoalCard
import com.stridewell.app.ui.components.SnapCarousel
import com.stridewell.app.ui.components.WorkoutCard
import com.stridewell.app.ui.main.reflection.ReflectionScreen
import com.stridewell.app.ui.theme.SofiaSansFamily
import com.stridewell.app.ui.theme.Spacing
import com.stridewell.app.util.DateUtils
import com.stridewell.app.util.FormatUtils

private data class BannerCardData(
    val id: String,
    val content: @Composable () -> Unit
)

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun HomeScreen(
    onOpenPlanChange: (DecisionRecord?) -> Unit,
    onOpenChatWithMessage: (String) -> Unit,
    hasLocationPermission: Boolean,
    heatmapViewModel: HeatmapViewModel,
    weatherViewModel: WeatherViewModel,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val weatherState by weatherViewModel.uiState.collectAsStateWithLifecycle()
    val isDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f
    var selectedWorkout by remember { mutableStateOf<PlanDay?>(null) }
    var showReflection by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        HeatmapBackgroundView(
            hasLocationPermission = hasLocationPermission,
            isDarkTheme = isDarkTheme,
            heatmapViewModel = heatmapViewModel
        )
        if (!uiState.showHeatmapOnly) {
            StormOverlayView(
                condition = weatherState.activeCondition,
                modifier = Modifier.fillMaxSize()
            )
        }

        if (!uiState.showHeatmapOnly) {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                containerColor = Color.Transparent
            ) { innerPadding ->
                when (val screenState = uiState.screenState) {
                    HomeViewModel.ScreenState.Loading -> HomeLoading(innerPadding)
                    HomeViewModel.ScreenState.Empty -> HomeEmpty(innerPadding)
                    is HomeViewModel.ScreenState.Error -> HomeError(
                        message = screenState.message,
                        innerPadding = innerPadding,
                        onRetry = viewModel::refresh
                    )
                    HomeViewModel.ScreenState.Loaded -> HomeContent(
                        uiState = uiState,
                        innerPadding = innerPadding,
                        onOpenPlanChange = onOpenPlanChange,
                        onDismissPlanChangeBanner = viewModel::dismissPlanChangeBanner,
                        onOpenChatWithMessage = onOpenChatWithMessage,
                        onDismissActivityBanner = viewModel::dismissActivityBanner,
                        onOpenReflection = { showReflection = true },
                        onWorkoutClick = { selectedWorkout = it }
                    )
                }
            }
        }

        FloatingActionButton(
            onClick = {
                if (!uiState.showHeatmapOnly) {
                    selectedWorkout = null
                    showReflection = false
                }
                viewModel.toggleHeatmapOnly()
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 24.dp),
            containerColor = Color.Black.copy(alpha = 0.45f),
            contentColor = Color.White
        ) {
            Icon(
                imageVector = if (uiState.showHeatmapOnly) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                contentDescription = if (uiState.showHeatmapOnly) {
                    "Show dashboard"
                } else {
                    "Show background only"
                }
            )
        }
    }

    if (!uiState.showHeatmapOnly) selectedWorkout?.let { workout ->
        ModalBottomSheet(
            onDismissRequest = { selectedWorkout = null }
        ) {
            WorkoutDetailSheet(
                day = workout,
                unitSystem = uiState.unitSystem
            )
        }
    }

    if (!uiState.showHeatmapOnly && showReflection) {
        ModalBottomSheet(
            onDismissRequest = { showReflection = false }
        ) {
            ReflectionScreen(onDismiss = { showReflection = false })
        }
    }

    LaunchedEffect(uiState.latestSyncedRunId, uiState.showActivityBanner, isDarkTheme) {
        if (uiState.showActivityBanner && !uiState.latestSyncedRunId.isNullOrBlank()) {
            heatmapViewModel.invalidateAndRegenerate(isDark = isDarkTheme)
        }
    }

    LaunchedEffect(hasLocationPermission) {
        weatherViewModel.fetchIfNeeded(hasLocationPermission)
    }
}

@Composable
private fun HomeLoading(innerPadding: PaddingValues) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun HomeEmpty(innerPadding: PaddingValues) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            Text(
                text = "No plan yet",
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Complete onboarding to get your training plan.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun HomeError(
    message: String,
    innerPadding: PaddingValues,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            Text(
                text = message,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Tap to retry",
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable(onClick = onRetry)
            )
        }
    }
}

@Composable
private fun HomeContent(
    uiState: HomeViewModel.UiState,
    innerPadding: PaddingValues,
    onOpenPlanChange: (DecisionRecord?) -> Unit,
    onDismissPlanChangeBanner: () -> Unit,
    onOpenChatWithMessage: (String) -> Unit,
    onDismissActivityBanner: () -> Unit,
    onOpenReflection: () -> Unit,
    onWorkoutClick: (PlanDay) -> Unit
) {
    val cards = remember(
        uiState.hasPlanChanged,
        uiState.latestDecision,
        uiState.showActivityBanner,
        uiState.latestSyncedRunId
    ) {
        buildList {
            if (uiState.hasPlanChanged) {
                add(
                    BannerCardData("plan-change") {
                        ActivityBannerView(
                            title1 = "Let's Review the Plan",
                            subtitle = "Your plan has been updated based on your recent runs and reflections",
                            imageRes = R.drawable.onboarding_background,
                            onTap = { onOpenPlanChange(uiState.latestDecision) },
                            onDismiss = onDismissPlanChangeBanner
                        )
                    }
                )
            }

            if (uiState.showActivityBanner) {
                add(
                    BannerCardData("activity-${uiState.latestSyncedRunId ?: "pending"}") {
                        ActivityBannerView(
                            title1 = "Great work out there!",
                            subtitle = "Let's talk about that last run",
                            imageRes = R.drawable.onboarding_background,
                            onTap = {
                                onDismissActivityBanner()
                                onOpenChatWithMessage("Let's talk about my last run.")
                            },
                            onDismiss = onDismissActivityBanner
                        )
                    }
                )
            }

            add(
                BannerCardData("reflection") {
                    ActivityBannerView(
                        title1 = "Time to check In!",
                        subtitle = "Lets check in to see how you're doing",
                        imageRes = R.drawable.onboarding_background,
                        onTap = onOpenReflection
                    )
                }
            )
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding),
        state = rememberLazyListState(),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
        contentPadding = PaddingValues(Spacing.md)
    ) {
        if (uiState.isOffline) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = "Offline \u2014 viewing cached data",
                        modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.xs),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        uiState.goalSummary?.let { goal ->
            item {
                GoalCard(
                    summary = goal,
                    unitSystem = uiState.unitSystem
                )
            }
        }

        item {
            if (cards.isNotEmpty()) {
                BannerCarousel(cards)
            }
        }

        item {
            FeaturedWorkoutSection(
                todayPlanDay = uiState.todayPlanDay,
                currentWeek = uiState.currentWeek,
                nextWeek = uiState.nextWeek,
                unitSystem = uiState.unitSystem,
                onWorkoutClick = onWorkoutClick
            )
        }

        item {
            RecentActivitiesSection(
                runs = uiState.recentRuns,
                unitSystem = uiState.unitSystem
            )
        }
    }
}

@Composable
private fun FeaturedWorkoutSection(
    todayPlanDay: PlanDay?,
    currentWeek: PlanWeekResponse?,
    nextWeek: PlanWeekResponse?,
    unitSystem: com.stridewell.app.util.UnitSystem,
    onWorkoutClick: (PlanDay) -> Unit
) {
    val featured = nextDisplayedWorkout(todayPlanDay, currentWeek, nextWeek)

    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        Text(
            text = featured?.first ?: "Today's workout",
            style = TextStyle(
                fontFamily = SofiaSansFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                lineHeight = 26.sp
            ),
            color = MaterialTheme.colorScheme.onSurface
        )

        featured?.second?.let { day ->
            WorkoutCard(
                day = day,
                unitSystem = unitSystem,
                onClick = { onWorkoutClick(day) }
            )
        } ?: HomeInfoBanner(
            title = "No workout queued",
            subtitle = "Your next training day will appear here once the week is available."
        )
    }
}

private fun nextDisplayedWorkout(
    todayPlanDay: PlanDay?,
    currentWeek: PlanWeekResponse?,
    nextWeek: PlanWeekResponse?
): Pair<String, PlanDay>? {
    val todayString = DateUtils.format(java.util.Date())

    if (todayPlanDay != null &&
        todayPlanDay.workout.type != WorkoutType.rest &&
        todayPlanDay.workout.type != WorkoutType.recovery
    ) {
        return "Today" to todayPlanDay
    }

    val next = currentWeek?.days?.firstOrNull { day ->
        day.date > todayString &&
            day.workout.type != WorkoutType.rest &&
            day.workout.type != WorkoutType.recovery
    }

    if (next != null) {
        return "Next Workout" to next
    }

    val firstNextWeekWorkout = nextWeek
        ?.days
        ?.firstOrNull { day ->
            day.workout.type != WorkoutType.rest &&
                day.workout.type != WorkoutType.recovery
        }

    return firstNextWeekWorkout?.let { "Next Workout" to it }
}

@Composable
private fun BannerCarousel(cards: List<BannerCardData>) {
    SnapCarousel(
        items = cards,
        cardWidth = 300.dp,
        cardHeight = 99.dp,
        key = { it.id }
    ) { card ->
        card.content()
    }
}

@Composable
private fun HomeInfoBanner(
    title: String,
    subtitle: String
) {
    ActivityBannerView(
        title1 = title,
        subtitle = subtitle,
        imageRes = R.drawable.onboarding_background
    )
}

@Composable
private fun RecentActivitiesSection(
    runs: List<Run>,
    unitSystem: com.stridewell.app.util.UnitSystem
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        Text(
            text = "Recent activities",
            style = TextStyle(
                fontFamily = SofiaSansFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                lineHeight = 26.sp
            ),
            color = MaterialTheme.colorScheme.onSurface
        )

        if (runs.isEmpty()) {
            HomeInfoBanner(
                title = "No recent activities",
                subtitle = "Connect Strava and your latest runs will show up here."
            )
        } else {
            runs.forEach { run ->
                ActivityCard(run = run, unitSystem = unitSystem)
            }
        }
    }
}

@Composable
private fun WorkoutDetailSheet(
    day: PlanDay,
    unitSystem: com.stridewell.app.util.UnitSystem
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        Text(
            text = day.workout.label,
            style = TextStyle(
                fontFamily = SofiaSansFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                lineHeight = 28.sp
            ),
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = DateUtils.planDayDate(day.date),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        day.workout.description?.let { description ->
            Text(
                text = description,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        val details = buildList {
            day.workout.target_distance_m?.let { add(FormatUtils.distance(it, unitSystem)) }
            day.workout.target_duration_s?.let { add(FormatUtils.duration(it)) }
            day.workout.target_pace_s_per_km?.let { add(FormatUtils.pace(it, unitSystem)) }
            day.workout.notes?.let { add(it) }
            day.notes?.let { add(it) }
        }
        details.forEach { detail ->
            Text(
                text = "• $detail",
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Surface(modifier = Modifier.height(24.dp), color = Color.Transparent) {}
    }
}
