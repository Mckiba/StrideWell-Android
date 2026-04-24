package com.stridewell.app.ui.main.weekly

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stridewell.app.model.WeeklySummary
import com.stridewell.app.ui.components.WeekNavigator
import com.stridewell.app.ui.theme.CornerRadius
import com.stridewell.app.ui.theme.Spacing
import com.stridewell.app.util.DateUtils
import com.stridewell.app.util.FormatUtils
import com.stridewell.app.util.UnitSystem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeeklySummaryScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: WeeklySummaryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Weekly Summary") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(Spacing.md),
                verticalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                item {
                    WeekNavigator(
                        label = DateUtils.weekRangeLabel(uiState.selectedMonday),
                        onPrevious = viewModel::previousWeek,
                        onNext = viewModel::nextWeek
                    )
                }

                when (val s = uiState.screenState) {
                    WeeklySummaryViewModel.ScreenState.Loading -> item { CenterBox { CircularProgressIndicator() } }
                    WeeklySummaryViewModel.ScreenState.NotReady -> item {
                        CenterBox {
                            Text(
                                "No runs logged for this week yet.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                    is WeeklySummaryViewModel.ScreenState.Error -> item {
                        CenterBox {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(s.message, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                TextButton(onClick = { viewModel.load(uiState.selectedMonday) }) { Text("Retry") }
                            }
                        }
                    }
                    is WeeklySummaryViewModel.ScreenState.Loaded -> summaryItems(s.summary, uiState.unitSystem)
                }
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.summaryItems(
    summary: WeeklySummary,
    unit: UnitSystem
) {
    item { OverviewCard(summary, unit) }
    summary.long_run?.let { item { LongRunCard(it, unit) } }
    if (summary.quality_sessions.isNotEmpty()) item { QualitySessionsCard(summary) }
    item { FatigueCard(summary) }
}

@Composable
private fun OverviewCard(s: WeeklySummary, unit: UnitSystem) {
    SectionCard("Overview") {
        DetailRow("Total distance", FormatUtils.distance(s.total_distance_m, unit))
        DetailRow("Total time", FormatUtils.duration(s.total_duration_s))
        DetailRow("Runs", "${s.run_count} / ${s.planned_run_count} planned")
        DetailRow("Compliance", "${(s.compliance_rate * 100).toInt()}%")
        s.avg_easy_pace_s_per_km?.let { DetailRow("Avg easy pace", FormatUtils.pace(it, unit)) }
        s.volume_vs_previous_week_pct?.let {
            DetailRow("Volume vs last week", "%+.0f%%".format(it))
        }
    }
}

@Composable
private fun LongRunCard(lr: com.stridewell.app.model.WeeklyLongRun, unit: UnitSystem) {
    SectionCard("Long Run") {
        DetailRow("Date", lr.date.take(10))
        DetailRow("Distance", FormatUtils.distance(lr.distance_m, unit))
        DetailRow("Pace", FormatUtils.pace(lr.pace_s_per_km, unit))
    }
}

@Composable
private fun QualitySessionsCard(s: WeeklySummary) {
    SectionCard("Quality Sessions") {
        s.quality_sessions.forEach { q ->
            DetailRow(
                "${q.date.take(10)} · ${q.type.humanize()}",
                q.execution_quality.humanize()
            )
        }
    }
}

@Composable
private fun FatigueCard(s: WeeklySummary) {
    SectionCard("Fatigue") {
        DetailRow("Trend", s.fatigue_trend.humanize())
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(CornerRadius.md),
        tonalElevation = 2.dp,
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
            content()
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        androidx.compose.foundation.layout.Spacer(modifier = Modifier.weight(1f))
        Text(value, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun CenterBox(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.xl),
        contentAlignment = Alignment.Center
    ) { content() }
}

private fun String.humanize(): String =
    replace("_", " ").replaceFirstChar { it.uppercase() }
