package com.stridewell.app.ui.main.detail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.stridewell.app.ui.theme.Spacing
import com.stridewell.app.ui.theme.StridewellTheme

/**
 * Multi-line text that truncates at [collapsedLines] lines until the user taps
 * "Show more". Expanded state survives rotation / process death via
 * [rememberSaveable].
 *
 * Mirrors iOS `ExpandableText` (private struct in `RunDetailScreen.swift`).
 */
@Composable
fun ExpandableText(
    text: String,
    modifier: Modifier = Modifier,
    collapsedLines: Int = 3,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = if (expanded) Int.MAX_VALUE else collapsedLines,
            overflow = if (expanded) TextOverflow.Visible else TextOverflow.Ellipsis,
        )
        if (!expanded) {
            Text(
                text = "Show more",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable { expanded = true },
            )
        }
    }
}

@Preview
@Composable
private fun ExpandableTextPreview() {
    StridewellTheme {
        ExpandableText(
            text = "Long activity description that wraps over multiple lines and " +
                "should be truncated with a Show more affordance until the user " +
                "taps it. This is the third line of context that should be hidden.",
        )
    }
}
