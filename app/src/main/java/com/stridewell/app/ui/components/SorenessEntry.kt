package com.stridewell.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.stridewell.app.ui.theme.Spacing
import com.stridewell.app.ui.theme.StridewellTheme

@Composable
fun SorenessEntryRow(
    location: String,
    score: Float,
    onLocationChange: (String) -> Unit,
    onScoreChange: (Float) -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.xs)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            OutlinedTextField(
                value = location,
                onValueChange = onLocationChange,
                modifier = Modifier.weight(1f),
                singleLine = true,
                label = { Text("Body part") }
            )
            IconButton(onClick = onRemove) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Remove soreness entry",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            FatigueSlider(
                labelStart = "1",
                labelEnd = "10",
                value = score,
                onValueChange = onScoreChange,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = score.toInt().toString(),
                modifier = Modifier.width(24.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SorenessEntryPreview() {
    StridewellTheme {
        SorenessEntryRow(
            location = "Left knee",
            score = 5f,
            onLocationChange = {},
            onScoreChange = {},
            onRemove = {},
            modifier = Modifier
        )
    }
}
