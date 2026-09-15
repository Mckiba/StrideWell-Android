package com.stridewell.app.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.stridewell.app.ui.theme.ActivityRangeLabelStyle
import com.stridewell.app.ui.theme.Spacing
import com.stridewell.app.ui.theme.StridewellTheme
import com.stridewell.app.util.ActivityRange

private val PickerShape = RoundedCornerShape(50)
private val SegmentWidth = 50.dp

/**
 * Capsule W / M / Y / All selector for the Activities overview. The selected
 * segment gets an accent pill that slides between options.
 */
@Composable
fun ActivityRangePicker(
    selected: ActivityRange,
    onSelect: (ActivityRange) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = LocalHapticFeedback.current
    val ranges = ActivityRange.entries

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(30.dp)
            .clip(PickerShape)
            .background(MaterialTheme.colorScheme.surface)
            .border(0.5.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), PickerShape)
            .padding(0.5.dp)
    ) {
        val gap = (maxWidth - SegmentWidth * ranges.size) / (ranges.size - 1)
        val pillOffset by animateDpAsState(
            targetValue = (SegmentWidth + gap) * selected.ordinal,
            label = "rangePillOffset"
        )

        Box(
            modifier = Modifier
                .offset(x = pillOffset)
                .width(SegmentWidth)
                .fillMaxHeight()
                .clip(PickerShape)
                .background(MaterialTheme.colorScheme.primary)
        )

        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            ranges.forEach { range ->
                Box(
                    modifier = Modifier
                        .width(SegmentWidth)
                        .fillMaxHeight()
                        .clip(PickerShape)
                        .selectable(
                            selected = range == selected,
                            role = Role.Tab,
                            onClick = {
                                if (range != selected) {
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onSelect(range)
                                }
                            }
                        )
                        .semantics { contentDescription = range.title },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = range.shortLabel,
                        style = ActivityRangeLabelStyle,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ActivityRangePickerPreview() {
    StridewellTheme {
        var range by remember { mutableStateOf(ActivityRange.WEEK) }
        ActivityRangePicker(
            selected = range,
            onSelect = { range = it },
            modifier = Modifier.padding(Spacing.md)
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF121212)
@Composable
private fun ActivityRangePickerDarkPreview() {
    StridewellTheme(darkTheme = true) {
        ActivityRangePicker(
            selected = ActivityRange.YEAR,
            onSelect = {},
            modifier = Modifier.padding(Spacing.md)
        )
    }
}
