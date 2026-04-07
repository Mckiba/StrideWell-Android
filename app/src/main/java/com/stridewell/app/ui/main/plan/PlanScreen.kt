package com.stridewell.app.ui.main.plan

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stridewell.app.ui.components.WeekNavigator
import com.stridewell.app.ui.components.WeekOverviewCard
import com.stridewell.app.ui.components.WorkoutCard
import com.stridewell.app.ui.components.WorkoutDetailSheet
import com.stridewell.app.ui.theme.SofiaSansFamily
import com.stridewell.app.ui.theme.Spacing
import com.stridewell.app.util.DateUtils

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun PlanScreen(
    onOpenPlanChange: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PlanViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    when (uiState.screenState) {
        PlanViewModel.ScreenState.Loading -> Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) { androidx.compose.material3.CircularProgressIndicator() }
        is PlanViewModel.ScreenState.Error -> Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                Text((uiState.screenState as PlanViewModel.ScreenState.Error).message)
                Text(
                    "Tap to retry",
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable(onClick = viewModel::retry)
                )
            }
        }
        PlanViewModel.ScreenState.Empty,
        PlanViewModel.ScreenState.Loaded -> {
            PullToRefreshBox(
                isRefreshing = uiState.isRefreshing,
                onRefresh = viewModel::pullToRefresh,
                modifier = modifier.fillMaxSize()
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(Spacing.md),
                    contentPadding = PaddingValues(Spacing.md)
                ) {
                    if (uiState.isOffline) {
                        item {
                            androidx.compose.material3.Surface(
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

                    if (uiState.hasPlanChanged) {
                        item {
                            androidx.compose.material3.Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(onClick = onOpenPlanChange),
                                tonalElevation = 2.dp,
                                shadowElevation = 2.dp
                            ) {
                                Column(modifier = Modifier.padding(Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                                    Text("Your plan was updated", style = sectionTitleStyle(), color = MaterialTheme.colorScheme.onSurface)
                                    Text("Tap to see what changed", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }

                    item {
                        WeekNavigator(
                            label = DateUtils.weekRangeLabel(uiState.selectedMonday),
                            onPrevious = viewModel::previousWeek,
                            onNext = viewModel::nextWeek
                        )
                    }

                    item {
                        WeekOverviewCard(
                            days = uiState.displayedWeek?.days.orEmpty(),
                            weekRuns = uiState.weekRuns,
                            monday = uiState.selectedMonday,
                            unitSystem = uiState.unitSystem
                        )
                    }

                    if (uiState.screenState == PlanViewModel.ScreenState.Empty) {
                        item {
                            Text(
                                text = "No workouts this week",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = Spacing.lg)
                            )
                        }
                    } else {
                        items(uiState.displayedWeek?.days.orEmpty().size) { index ->
                            val day = uiState.displayedWeek!!.days[index]
                            WorkoutCard(
                                day = day,
                                unitSystem = uiState.unitSystem,
                                onClick = { viewModel.selectDay(day) }
                            )
                        }
                    }

                    val week = uiState.displayedWeek
                    if (week != null && (week.phase_label != null || week.coaching_notes != null)) {
                        item {
                            androidx.compose.material3.Surface(
                                modifier = Modifier.fillMaxWidth(),
                                tonalElevation = 2.dp,
                                shadowElevation = 2.dp
                            ) {
                                Column(modifier = Modifier.padding(Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                                    week.phase_label?.let {
                                        Text(it.replace("_", " ").replaceFirstChar { c -> c.uppercase() }, color = MaterialTheme.colorScheme.primary)
                                    }
                                    week.coaching_notes?.let {
                                        Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    uiState.selectedDay?.let { day ->
        ModalBottomSheet(onDismissRequest = viewModel::dismissSelectedDay) {
            WorkoutDetailSheet(day = day, unitSystem = uiState.unitSystem)
        }
    }
}

private fun sectionTitleStyle() = TextStyle(
    fontFamily = SofiaSansFamily,
    fontWeight = FontWeight.Bold,
    fontSize = 16.sp
)
