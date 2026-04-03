package com.stridewell.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stridewell.app.ui.theme.CornerRadius
import com.stridewell.app.ui.theme.SofiaSansFamily
import com.stridewell.app.ui.theme.Spacing
import com.stridewell.app.ui.theme.StridewellTheme

enum class MoodOption(val score: Int, val label: String, val icon: ImageVector) {
    Low(3, "Low", Icons.Default.Close),
    Neutral(5, "Neutral", Icons.Default.Refresh),
    Good(8, "Good", Icons.Default.Check)
}

@Composable
fun MoodSelector(
    selected: MoodOption,
    onSelect: (MoodOption) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        MoodOption.entries.forEach { option ->
            val selectedState = option == selected
            Column(
                modifier = Modifier
                    .weight(1f)
                    .background(
                        color = if (selectedState) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        } else {
                            androidx.compose.ui.graphics.Color.Transparent
                        },
                        shape = RoundedCornerShape(CornerRadius.sm)
                    )
                    .clickable { onSelect(option) }
                    .padding(vertical = Spacing.sm),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Spacing.xs)
            ) {
                Icon(
                    imageVector = option.icon,
                    contentDescription = option.label,
                    tint = if (selectedState) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = option.label,
                    fontFamily = SofiaSansFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 12.sp,
                    color = if (selectedState) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun MoodSelectorPreview() {
    StridewellTheme {
        MoodSelector(
            selected = MoodOption.Neutral,
            onSelect = {},
            modifier = Modifier.padding(Spacing.md)
        )
    }
}
