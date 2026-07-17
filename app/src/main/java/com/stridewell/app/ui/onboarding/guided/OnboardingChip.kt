package com.stridewell.app.ui.onboarding.guided

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.tooling.preview.Preview
import com.stridewell.app.ui.theme.AccentLight
import com.stridewell.app.ui.theme.Spacing
import com.stridewell.app.ui.theme.StridewellTheme

/** Selectable capsule chip for the guided structured-input rows (goal, days, speedwork, phase). */
@Composable
fun OnboardingChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelLarge,
        textAlign = TextAlign.Center,
        color = if (isSelected) AccentLight else MaterialTheme.colorScheme.onSurface,
        modifier = modifier
            .clip(CircleShape)
            .background(
                if (isSelected) AccentLight.copy(alpha = 0.18f)
                else MaterialTheme.colorScheme.surfaceVariant
            )
            .clickable(onClick = onClick)
            .padding(horizontal = Spacing.md, vertical = Spacing.sm)
    )
}

@Preview
@Composable
private fun OnboardingChipPreview() = StridewellTheme {
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        OnboardingChip(label = "Selected", isSelected = true, onClick = {})
        OnboardingChip(label = "Unselected", isSelected = false, onClick = {})
    }
}
