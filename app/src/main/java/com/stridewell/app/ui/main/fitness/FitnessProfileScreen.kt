package com.stridewell.app.ui.main.fitness

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
import com.stridewell.app.model.FitnessProfile
import com.stridewell.app.model.HRZones
import com.stridewell.app.model.PaceZones
import com.stridewell.app.ui.theme.CornerRadius
import com.stridewell.app.ui.theme.Spacing
import com.stridewell.app.util.FormatUtils
import com.stridewell.app.util.UnitSystem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FitnessProfileScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FitnessProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Fitness Profile") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            when (val s = uiState.screenState) {
                FitnessProfileViewModel.ScreenState.Loading -> Box(
                    modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center
                ) { CircularProgressIndicator() }
                FitnessProfileViewModel.ScreenState.NotReady -> EmptyState()
                is FitnessProfileViewModel.ScreenState.Error -> Box(
                    modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(s.message, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        TextButton(onClick = viewModel::load) { Text("Retry") }
                    }
                }
                is FitnessProfileViewModel.ScreenState.Loaded -> ProfileContent(s.profile, uiState.unitSystem)
            }
        }
    }
}

@Composable
private fun EmptyState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = "Not enough data yet. Once you log a race or accumulate 12+ runs, " +
                   "your training paces will personalize to you.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(Spacing.lg),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
private fun ProfileContent(profile: FitnessProfile, unit: UnitSystem) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        item { ThresholdCard(profile, unit) }
        profile.pace_zones?.let { item { PaceZonesCard(it, unit) } }
        profile.hr_zones?.let { item { HRZonesCard(it) } }
        if (profile.history.isNotEmpty()) item { HistoryCard(profile, unit) }
    }
}

@Composable
private fun ThresholdCard(profile: FitnessProfile, unit: UnitSystem) {
    SectionCard("Lactate Threshold") {
        profile.estimated_threshold_pace_s_per_km?.let {
            Text(
                text = FormatUtils.pace(it, unit),
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        profile.confidence?.let {
            DetailRow("Confidence", it.humanize())
        }
        profile.estimation_method?.let {
            DetailRow("Method", it.humanize())
        }
        profile.estimation_source?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        profile.estimation_date?.let {
            Text(
                "Updated ${it.take(10)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PaceZonesCard(z: PaceZones, unit: UnitSystem) {
    SectionCard("Pace Zones") {
        listOf(
            "Recovery" to z.recovery,
            "Easy" to z.easy,
            "Moderate" to z.moderate,
            "Tempo" to z.tempo,
            "Threshold" to z.threshold,
            "Interval" to z.interval,
            "Repetition" to z.repetition
        ).forEach { (label, range) ->
            DetailRow(label, FormatUtils.formatPaceRange(range.min_s_per_km, range.max_s_per_km, unit))
        }
    }
}

@Composable
private fun HRZonesCard(h: HRZones) {
    SectionCard("Heart Rate Zones") {
        DetailRow("Max HR", "${h.max_hr_bpm} bpm")
        DetailRow("Source", h.max_hr_source.humanize())
        DetailRow("Z1", "${h.zone_1.min_bpm}–${h.zone_1.max_bpm} bpm")
        DetailRow("Z2", "${h.zone_2.min_bpm}–${h.zone_2.max_bpm} bpm")
        DetailRow("Z3", "${h.zone_3.min_bpm}–${h.zone_3.max_bpm} bpm")
        DetailRow("Z4", "${h.zone_4.min_bpm}–${h.zone_4.max_bpm} bpm")
        DetailRow("Z5", "${h.zone_5.min_bpm}–${h.zone_5.max_bpm} bpm")
    }
}

@Composable
private fun HistoryCard(profile: FitnessProfile, unit: UnitSystem) {
    SectionCard("History") {
        profile.history.take(10).forEach { entry ->
            DetailRow(
                "${entry.date.take(10)} · ${entry.method.humanize()}",
                FormatUtils.pace(entry.threshold_pace_s_per_km, unit)
            )
        }
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

private fun String.humanize(): String =
    replace("_", " ").replaceFirstChar { it.uppercase() }
