package com.stridewell.app.ui.onboarding.guided

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.stridewell.app.ui.theme.Spacing
import com.stridewell.app.ui.theme.StridewellTheme

/** (label, training_phase enum value). */
val phaseOptions: List<Pair<String, String>> = listOf(
    "Base" to "base",
    "Building up" to "build",
    "Peaking" to "peak",
    "Recovering" to "recovery",
    "Coming back from injury" to "return_from_injury",
    "No structure" to "unstructured"
)

/** Optional training-phase picker for the manual baseline screen. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PhaseChips(
    selected: String?,
    onSelect: (value: String, label: String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        Text(
            text = "Training phase (optional)",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            phaseOptions.forEach { (label, value) ->
                OnboardingChip(
                    label = label,
                    isSelected = selected == value,
                    onClick = { onSelect(value, label) }
                )
            }
        }
    }
}

@Preview
@Composable
private fun PhaseChipsPreview() = StridewellTheme {
    PhaseChips(selected = "base", onSelect = { _, _ -> })
}
