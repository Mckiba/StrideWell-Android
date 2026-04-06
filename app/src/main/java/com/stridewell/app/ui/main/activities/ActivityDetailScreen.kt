package com.stridewell.app.ui.main.activities

import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.stridewell.app.model.Run
import com.stridewell.app.ui.theme.ActivityStatLabelStyle
import com.stridewell.app.ui.theme.ActivityStatValueStyle
import com.stridewell.app.ui.theme.ActivityTimestampStyle
import com.stridewell.app.ui.theme.CardSurfaceDark
import com.stridewell.app.ui.theme.CardSurfaceLight
import com.stridewell.app.ui.theme.CornerRadius
import com.stridewell.app.ui.theme.Spacing
import com.stridewell.app.util.DateUtils
import com.stridewell.app.util.FormatUtils
import com.stridewell.app.util.UnitSystem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityDetailScreen(
    run: Run?,
    unitSystem: UnitSystem,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val title = run?.title
        ?: run?.sport_type?.replace("_", " ")?.replaceFirstChar { it.uppercase() }
        ?: "Activity"

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        if (run == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Activity not found",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(Spacing.md),
                verticalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                // Header card — date and time
                item {
                    DetailCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = DateUtils.activityDate(run.start_time),
                                style = ActivityTimestampStyle,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = DateUtils.activityTime(run.start_time),
                                style = ActivityTimestampStyle,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                // Stats card — distance, time, pace
                item {
                    DetailCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            DetailStat(
                                label = "DISTANCE",
                                value = FormatUtils.distance(run.distance_m, unitSystem)
                            )
                            DetailStat(
                                label = "TIME",
                                value = FormatUtils.duration(run.duration_s)
                            )
                            DetailStat(
                                label = "AVG PACE",
                                value = run.avg_pace_s_per_km
                                    ?.let { FormatUtils.pace(it, unitSystem) }
                                    ?: "—"
                            )
                        }
                    }
                }

                // Elevation card — only when data is present (0.0 = backfill row)
                if (run.elevation_gain_m > 0.0) {
                    item {
                        DetailCard {
                            Row(modifier = Modifier.fillMaxWidth()) {
                                DetailStat(
                                    label = "ELEVATION",
                                    value = when (unitSystem) {
                                        UnitSystem.METRIC   -> "%.0f m".format(run.elevation_gain_m)
                                        UnitSystem.IMPERIAL -> "%.0f ft".format(run.elevation_gain_m * 3.28084)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailCard(
    content: @Composable () -> Unit
) {
    val cardColor = if (isSystemInDarkTheme()) CardSurfaceDark else CardSurfaceLight
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(CornerRadius.md),
        color = cardColor,
        shadowElevation = Spacing.xs
    ) {
        Box(modifier = Modifier.padding(Spacing.md)) {
            content()
        }
    }
}

@Composable
private fun DetailStat(
    label: String,
    value: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.xs)
    ) {
        Text(
            text = label,
            style = ActivityStatLabelStyle,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = ActivityStatValueStyle,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
