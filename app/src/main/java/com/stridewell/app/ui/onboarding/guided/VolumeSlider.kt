package com.stridewell.app.ui.onboarding.guided

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.stridewell.app.ui.components.PrimaryButton
import com.stridewell.app.ui.theme.Spacing
import com.stridewell.app.ui.theme.StridewellTheme
import kotlin.math.roundToInt

/**
 * Weekly-volume picker for the manual baseline screen. [displayValue] is in the athlete's
 * unit; 0 means "not currently running". The parent converts to km on commit.
 */
@Composable
fun VolumeSlider(
    displayValue: Double,
    onValueChange: (Double) -> Unit,
    onCommit: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Weekly volume", style = MaterialTheme.typography.titleSmall)
            Text(
                text = if (displayValue < 1) "Not currently running"
                else "${displayValue.roundToInt()} ${OnboardingUnits.unitLabel}/wk",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Slider(
            value = displayValue.toFloat(),
            onValueChange = { onValueChange(it.toDouble()) },
            valueRange = 0f..OnboardingUnits.weeklyVolumeMax.toFloat(),
            steps = (OnboardingUnits.weeklyVolumeMax.toInt() - 1).coerceAtLeast(0)
        )

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                "Not running",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "${OnboardingUnits.weeklyVolumeMax.toInt()} ${OnboardingUnits.unitLabel}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        PrimaryButton(text = "Set weekly volume", onClick = onCommit)
    }
}

@Preview
@Composable
private fun VolumeSliderPreview() = StridewellTheme {
    var value by remember { mutableStateOf(30.0) }
    VolumeSlider(displayValue = value, onValueChange = { value = it }, onCommit = {})
}
