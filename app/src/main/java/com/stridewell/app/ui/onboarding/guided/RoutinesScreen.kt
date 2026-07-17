package com.stridewell.app.ui.onboarding.guided

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stridewell.R
import com.stridewell.app.data.OnboardingSessionStore
import com.stridewell.app.model.StructuredFields
import com.stridewell.app.navigation.OnboardingFlow
import com.stridewell.app.navigation.OnboardingScreen
import com.stridewell.app.ui.components.PrimaryButton
import com.stridewell.app.ui.theme.AccentLight
import com.stridewell.app.ui.theme.Spacing
import com.stridewell.app.ui.theme.StridewellTheme

private val weekDays = listOf(
    "Mon" to "monday", "Tue" to "tuesday", "Wed" to "wednesday",
    "Thu" to "thursday", "Fri" to "friday", "Sat" to "saturday", "Sun" to "sunday"
)

private fun shortLabel(value: String): String =
    weekDays.firstOrNull { it.second == value }?.first ?: value.replaceFirstChar { it.uppercase() }

/** Captures how many days a week the athlete can run, which days, and the preferred long-run day. */
@Composable
fun RoutinesScreen(
    onNavigate: (route: String) -> Unit,
    viewModel: IntakeChatViewModel = intakeChatViewModel(OnboardingFlow.screenContext(OnboardingScreen.Routines)),
    sessionStore: OnboardingSessionStore = viewModel.session
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val partial by sessionStore.partialIntake.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.startIfNeeded() }
    GuidedAdvanceEffect(OnboardingScreen.Routines, sessionStore, viewModel.planBuilding, onNavigate)

    RoutinesContent(
        uiState = uiState,
        initialDays = partial?.available_day_names,
        initialLongRun = partial?.preferred_long_run_day,
        onCommit = { days, longRunDay ->
            val dayList = days.joinToString(", ") { shortLabel(it) }
            val plural = if (days.size == 1) "" else "s"
            var text = "I can run ${days.size} day$plural a week — $dayList"
            longRunDay?.let { text += ", with my long run on ${shortLabel(it)}" }
            text += "."
            viewModel.submit(
                text,
                StructuredFields(
                    available_days_per_week = days.size,
                    available_day_names = days,
                    preferred_long_run_day = longRunDay
                )
            )
        },
        onInputChanged = viewModel::onInputChanged,
        onSend = viewModel::onSendText,
        onRetry = viewModel::retry
    )
}

@Composable
private fun RoutinesContent(
    uiState: IntakeChatViewModel.UiState,
    initialDays: List<String>?,
    initialLongRun: String?,
    onCommit: (days: List<String>, longRunDay: String?) -> Unit,
    onInputChanged: (String) -> Unit,
    onSend: () -> Unit,
    onRetry: () -> Unit
) {
    val selectedDays = remember { mutableStateListOf<String>() }
    var longRunDay by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(initialDays, initialLongRun) {
        initialDays?.let { names ->
            selectedDays.clear()
            selectedDays.addAll(weekDays.map { it.second }.filter { names.contains(it) })
        }
        longRunDay = initialLongRun
    }

    val ordered = weekDays.map { it.second }.filter { selectedDays.contains(it) }

    GuidedScreenScaffold(
        title = "Routines",
        imageRes = R.drawable.onboarding_routines,
        autoExpand = uiState.phase == IntakeChatViewModel.Phase.Waiting,
        structuredInputs = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    weekDays.forEach { (short, value) ->
                        DayChip(
                            label = short,
                            selected = selectedDays.contains(value),
                            modifier = Modifier.weight(1f),
                            onClick = {
                                if (selectedDays.contains(value)) {
                                    selectedDays.remove(value)
                                    if (longRunDay == value) longRunDay = null
                                } else {
                                    selectedDays.add(value)
                                }
                            }
                        )
                    }
                }

                if (ordered.isNotEmpty()) {
                    LongRunPicker(options = ordered, selected = longRunDay, onSelect = { longRunDay = it })
                    PrimaryButton(text = "Set schedule", onClick = { onCommit(ordered, longRunDay) })
                }
            }
        },
        chat = { onInteract ->
            IntakeChatSurface(
                uiState = uiState,
                onInputChanged = onInputChanged,
                onSend = onSend,
                onRetry = onRetry,
                onInteract = onInteract,
                modifier = Modifier.weight(1f)
            )
        }
    )
}

@Composable
private fun DayChip(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Text(
        text = label,
        textAlign = TextAlign.Center,
        style = MaterialTheme.typography.labelMedium,
        color = if (selected) AccentLight else MaterialTheme.colorScheme.onSurface,
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) AccentLight.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
            .padding(vertical = Spacing.sm)
    )
}

@Composable
private fun LongRunPicker(options: List<String>, selected: String?, onSelect: (String?) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text("Long run", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Box {
            TextButton(onClick = { expanded = true }) {
                Text(selected?.let { shortLabel(it) } ?: "None")
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                DropdownMenuItem(text = { Text("None") }, onClick = { onSelect(null); expanded = false })
                options.forEach { value ->
                    DropdownMenuItem(text = { Text(shortLabel(value)) }, onClick = { onSelect(value); expanded = false })
                }
            }
        }
    }
}

@Preview
@Composable
private fun RoutinesPreview() = StridewellTheme {
    RoutinesContent(
        uiState = sampleChatState("Which days can you run, and when's your long run?"),
        initialDays = listOf("monday", "wednesday", "friday"),
        initialLongRun = "friday",
        onCommit = { _, _ -> },
        onInputChanged = {},
        onSend = {},
        onRetry = {}
    )
}
