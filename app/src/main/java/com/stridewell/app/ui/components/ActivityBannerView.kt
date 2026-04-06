package com.stridewell.app.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.stridewell.R
import com.stridewell.app.model.Workout
import com.stridewell.app.ui.theme.CornerRadius
import com.stridewell.app.ui.theme.SofiaSansFamily
import com.stridewell.app.ui.theme.Spacing
import com.stridewell.app.ui.theme.StridewellTheme
import com.stridewell.app.util.FormatUtils
import com.stridewell.app.util.UnitSystem

@Composable
fun ActivityBannerView(
    title1: String,
    subtitle: String?,
    modifier: Modifier = Modifier,
    detail: String? = null,
    title2: String? = null,
    workout: Workout? = null,
    unitSystem: UnitSystem = UnitSystem.METRIC,
    @DrawableRes imageRes: Int? = null,
    onTap: (() -> Unit)? = null,
    onDismiss: (() -> Unit)? = null
) {
    val cardColor = MaterialTheme.colorScheme.surface

    Box(modifier = modifier) {
        Row(
            modifier = Modifier
                .width(300.dp)
                .fillMaxWidth()
                .height(99.dp)
                .shadow(
                    elevation = 8.dp,
                    shape = RoundedCornerShape(CornerRadius.sm),
                    ambientColor = Color.Black.copy(alpha = 0.08f),
                    spotColor = Color.Black.copy(alpha = 0.12f)
                )
                .background(
                    color = cardColor,
                    shape = RoundedCornerShape(CornerRadius.sm)
                )
                .clickable(enabled = onTap != null) { onTap?.invoke() }
                .padding(Spacing.md),
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            verticalAlignment = Alignment.Top
        ) {
            imageRes?.let { resId ->
                AsyncImage(
                    model = resId,
                    contentDescription = null,
                    modifier = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(CornerRadius.sm)),
                    contentScale = ContentScale.Crop
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title1,
                        style = bannerTextStyle(FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    detail?.let {
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = it,
                            style = bannerTextStyle(FontWeight.Normal),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                val secondLine = title2 ?: workoutMetricLine(workout, unitSystem)
                secondLine?.let {
                    Text(
                        text = it,
                        style = bannerTextStyle(FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                subtitle?.let {
                    Text(
                        text = it,
                        style = bannerTextStyle(FontWeight.Normal),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        onDismiss?.let {
            IconButton(
                onClick = it,
                modifier = Modifier.align(Alignment.TopEnd)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Dismiss",
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}

private fun workoutMetricLine(
    workout: Workout?,
    unitSystem: UnitSystem
): String? {
    if (workout == null) return null
    val parts = buildList {
        workout.target_distance_m?.let { add(FormatUtils.distance(it, unitSystem)) }
        when {
            workout.target_pace_s_per_km != null -> add(
                FormatUtils.pace(workout.target_pace_s_per_km, unitSystem)
            )
            workout.target_duration_s != null -> add(
                FormatUtils.duration(workout.target_duration_s)
            )
        }
    }
    return parts.takeIf { it.isNotEmpty() }?.joinToString("  ·  ")
}

private fun bannerTextStyle(weight: FontWeight) = TextStyle(
    fontFamily = SofiaSansFamily,
    fontWeight = weight,
    fontSize = 12.sp,
    lineHeight = 16.sp
)

@Preview(showBackground = true)
@Composable
private fun ActivityBannerViewPreview() {
    StridewellTheme {
        Column(
            modifier = Modifier.padding(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            ActivityBannerView(
                title1 = "Time to check In!",
                subtitle = "Lets check in to see how you're doing",
                imageRes = R.drawable.onboarding_background
            )
            ActivityBannerView(
                title1 = "Let's Review the Plan",
                subtitle = "Your plan has been updated based on your recent runs and reflections",
                imageRes = R.drawable.onboarding_background,
                onTap = {},
                onDismiss = {}
            )
        }
    }
}
