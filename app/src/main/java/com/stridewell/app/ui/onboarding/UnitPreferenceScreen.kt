package com.stridewell.app.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.stridewell.app.navigation.Route
import com.stridewell.app.ui.components.PrimaryButton
import com.stridewell.app.ui.theme.Spacing
import com.stridewell.app.ui.theme.StridewellTheme
import com.stridewell.app.util.UnitSystem

/**
 * First onboarding step. The athlete picks metric or imperial before the guided intake begins,
 * so the whole flow — and the Coach — speaks in their unit. The choice is stored locally (shared
 * with Settings) and synced to the backend; "Begin" moves on to the connect screen.
 */
@Composable
fun UnitPreferenceScreen(
    onNavigate: (route: String) -> Unit,
    viewModel: UnitPreferenceViewModel = hiltViewModel(),
) {
    var selected by remember { mutableStateOf(viewModel.defaultUnit) }
    UnitPreferenceContent(
        selected = selected,
        onSelect = { selected = it },
        onBegin = {
            viewModel.commit(selected)
            onNavigate(Route.StravaConnect.path)
        }
    )
}

@Composable
private fun UnitPreferenceContent(
    selected: UnitSystem,
    onSelect: (UnitSystem) -> Unit,
    onBegin: () -> Unit,
) {
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = Spacing.lg)
                .padding(bottom = Spacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(Spacing.xxl))

            Text(
                text = "What's your unit preference",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(Spacing.xxl))

            UnitWheelPicker(selected = selected, onSelect = onSelect)

            Spacer(Modifier.height(Spacing.xl))

            UnitDisplayExample(selected = selected)

            Spacer(Modifier.height(Spacing.xl))

            Text(
                text = "This can be updated later in the app settings",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.weight(1f))

            PrimaryButton(
                text = "Begin",
                onClick = onBegin,
                backgroundColor = Color.White,
                contentColor = Color.Black
            )
        }
    }
}

// ── Wheel picker ────────────────────────────────────────────────────────────

/** Vertical two-option selector matching the design: the labels stacked with a rule between
 *  them; the selected one is accent-tinted. */
@Composable
private fun UnitWheelPicker(
    selected: UnitSystem,
    onSelect: (UnitSystem) -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        UnitOption("Imperial", selected == UnitSystem.IMPERIAL) { onSelect(UnitSystem.IMPERIAL) }
        Box(
            Modifier
                .width(143.dp)
                .height(1.dp)
                .background(Color.White.copy(alpha = 0.18f))
        )
        UnitOption("Metric", selected == UnitSystem.METRIC) { onSelect(UnitSystem.METRIC) }
    }
}

@Composable
private fun UnitOption(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Text(
        text = label,
        style = MaterialTheme.typography.bodyLarge,
        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.9f),
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = Spacing.md, vertical = Spacing.xs)
    )
}

// ── Unit example ────────────────────────────────────────────────────────────

/** Mirrors the athlete's selection: "km" over "min / km" (or the mi variants). */
@Composable
private fun UnitDisplayExample(selected: UnitSystem) {
    val unit = if (selected == UnitSystem.IMPERIAL) "mi" else "km"
    val gray = Color(0xFF4C4C4C)
    Column(
        modifier = Modifier.width(201.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        Text(text = unit, style = MaterialTheme.typography.bodyLarge, color = gray)
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            HorizontalDivider(color = gray)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("min", style = MaterialTheme.typography.bodyLarge, color = gray)
                Text("/", style = MaterialTheme.typography.bodyLarge, color = gray)
                Text(unit, style = MaterialTheme.typography.bodyLarge, color = gray)
            }
            HorizontalDivider(color = gray)
        }
    }
}

// ── Preview ─────────────────────────────────────────────────────────────────

@Preview
@Composable
private fun UnitPreferencePreview() = StridewellTheme {
    UnitPreferenceContent(selected = UnitSystem.IMPERIAL, onSelect = {}, onBegin = {})
}
