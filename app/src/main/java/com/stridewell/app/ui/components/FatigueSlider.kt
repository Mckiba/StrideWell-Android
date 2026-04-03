package com.stridewell.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.stridewell.app.ui.theme.ProgressTrackEmptyDark
import com.stridewell.app.ui.theme.ProgressTrackEmptyLight
import com.stridewell.app.ui.theme.Spacing
import com.stridewell.app.ui.theme.StridewellTheme
import kotlin.math.roundToInt

@Composable
fun FatigueSlider(
    labelStart: String,
    labelEnd: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val inactiveTrack = if (androidx.compose.foundation.isSystemInDarkTheme()) {
        ProgressTrackEmptyDark
    } else {
        ProgressTrackEmptyLight
    }

    androidx.compose.foundation.layout.Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = 1f..10f,
            steps = 8,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = inactiveTrack
            )
        )
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(labelStart, color = MaterialTheme.colorScheme.onSurfaceVariant)
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.weight(1f))
            Text(labelEnd, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun FatigueSliderPreview() {
    StridewellTheme {
        FatigueSlider(
            labelStart = "Fresh",
            labelEnd = "Exhausted",
            value = 5f,
            onValueChange = {},
            modifier = Modifier.padding(Spacing.md)
        )
    }
}
