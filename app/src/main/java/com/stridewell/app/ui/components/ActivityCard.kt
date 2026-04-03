package com.stridewell.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.stridewell.app.model.Run
import com.stridewell.app.model.RunRoute
import com.stridewell.app.ui.theme.ActivityNameStyle
import com.stridewell.app.ui.theme.ActivityStatLabelStyle
import com.stridewell.app.ui.theme.ActivityStatValueStyle
import com.stridewell.app.ui.theme.ActivityTimestampStyle
import com.stridewell.app.ui.theme.AccentDark
import com.stridewell.app.ui.theme.AccentLight
import com.stridewell.app.ui.theme.CardSurfaceDark
import com.stridewell.app.ui.theme.CardSurfaceLight
import com.stridewell.app.ui.theme.CornerRadius
import com.stridewell.app.ui.theme.Spacing
import com.stridewell.app.ui.theme.StridewellTheme
import com.stridewell.app.util.DateUtils
import com.stridewell.app.util.FormatUtils
import com.stridewell.app.util.UnitSystem

@Composable
fun ActivityCard(
    run: Run,
    unitSystem: UnitSystem,
    modifier: Modifier = Modifier
) {
    val coordinates = remember(run.route?.summary_polyline) {
        run.route?.summary_polyline
            ?.takeIf { it.isNotEmpty() }
            ?.let(PolylineDecoder::decode)
            .orEmpty()
    }
    val cardColor = if (isSystemInDarkTheme()) CardSurfaceDark else CardSurfaceLight
    val routeColor = if (isSystemInDarkTheme()) AccentDark else AccentLight

    Row(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(CornerRadius.md),
                ambientColor = Color.Black.copy(alpha = 0.08f),
                spotColor = Color.Black.copy(alpha = 0.10f)
            )
            .background(
                color = cardColor,
                shape = RoundedCornerShape(CornerRadius.md)
            )
            .padding(horizontal = Spacing.md, vertical = Spacing.xs),
        horizontalArrangement = Arrangement.spacedBy(Spacing.xl2),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RouteThumbnail(
            coordinates = coordinates,
            strokeColor = routeColor,
            placeholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(Spacing.xs)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = DateUtils.activityDate(run.start_time),
                    style = ActivityTimestampStyle,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = DateUtils.activityTime(run.start_time),
                    style = ActivityTimestampStyle,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Text(
                text = run.title ?: run.sport_type.replace("_", " ").replaceFirstChar { it.uppercase() },
                style = ActivityNameStyle,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.lg)) {
                ActivityStat(
                    label = "DISTANCE",
                    value = FormatUtils.distance(run.distance_m, unitSystem)
                )
                Spacer(modifier = Modifier.weight(1f))
                ActivityStat(
                    label = "TIME",
                    value = FormatUtils.duration(run.duration_s)
                )
                Spacer(modifier = Modifier.weight(1f))
                ActivityStat(
                    label = "AVG PACE",
                    value = run.avg_pace_s_per_km?.let { FormatUtils.pace(it, unitSystem) } ?: "—"
                )
            }
        }
    }
}

@Composable
private fun ActivityStat(
    label: String,
    value: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Text(
            text = label,
            style = ActivityStatLabelStyle,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
        Text(
            text = value,
            style = ActivityStatValueStyle,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ActivityCardPreview() {
    StridewellTheme {
        ActivityCard(
            run = Run(
                id = "1",
                provider = "strava",
                sport_type = "Run",
                title = "Seattle Running",
                start_time = "2025-02-18T18:16:00Z",
                distance_m = 4850.0,
                duration_s = 1568,
                avg_pace_s_per_km = 323.0,
                elevation_gain_m = 42.0,
                route = RunRoute(summary_polyline = "o}mlHlariVoBiC{A{B_BaCuBeDyF_Jw@yAeAuBmAaCkBuDeBkDoAsB}@cBqAeCqBiEiBkD{BkEaC_FuAgC")
            ),
            unitSystem = UnitSystem.METRIC,
            modifier = Modifier.padding(Spacing.md)
        )
    }
}
