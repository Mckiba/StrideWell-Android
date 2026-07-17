package com.stridewell.app.ui.onboarding.guided

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stridewell.R
import com.stridewell.app.data.OnboardingSessionStore
import com.stridewell.app.model.StructuredFields
import com.stridewell.app.navigation.OnboardingFlow
import com.stridewell.app.navigation.OnboardingScreen
import com.stridewell.app.ui.components.PrimaryButton
import com.stridewell.app.ui.theme.Spacing
import com.stridewell.app.ui.theme.StridewellTheme
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private data class Goal(val label: String, val value: String, val phrase: String)
private data class RaceDistance(val label: String, val meters: Double)

private val goals = listOf(
    Goal("A race", "race", "to run a race"),
    Goal("General fitness", "fitness", "general fitness"),
    Goal("Build a base", "base_building", "to build a base"),
    Goal("Return to running", "return_to_running", "to return to running")
)
private val raceDistances = listOf(
    RaceDistance("5K", 5000.0),
    RaceDistance("10K", 10000.0),
    RaceDistance("Half", 21097.0),
    RaceDistance("Marathon", 42195.0)
)
private val isoDate: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneOffset.UTC)

/** Captures the training goal. When the goal is a race, an optional distance-and-date row is revealed. */
@Composable
fun GoalScreen(
    onNavigate: (route: String) -> Unit,
    viewModel: IntakeChatViewModel = intakeChatViewModel(OnboardingFlow.screenContext(OnboardingScreen.Goal)),
    sessionStore: OnboardingSessionStore = viewModel.session
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val partial by sessionStore.partialIntake.collectAsStateWithLifecycle()

    var selectedGoal by remember { mutableStateOf<String?>(null) }
    var raceDistanceM by remember { mutableStateOf(21097.0) }
    var raceDateMillis by remember { mutableStateOf(Instant.now().plusSeconds(60L * 60 * 24 * 84).toEpochMilli()) }
    var raceHandled by remember { mutableStateOf(false) }

    LaunchedEffect(partial) {
        partial?.goal_type?.let { selectedGoal = it }
        partial?.goal_race_distance_m?.let { raceDistanceM = it }
    }
    LaunchedEffect(Unit) { viewModel.startIfNeeded() }

    GuidedAdvanceEffect(
        screen = OnboardingScreen.Goal,
        sessionStore = sessionStore,
        planBuilding = viewModel.planBuilding,
        onNavigate = onNavigate,
        extraReady = raceHandled || selectedGoal != "race"
    )

    GoalContent(
        uiState = uiState,
        selectedGoal = selectedGoal,
        raceDistanceM = raceDistanceM,
        raceDateMillis = raceDateMillis,
        onRaceDistanceChange = { raceDistanceM = it },
        onRaceDateChange = { raceDateMillis = it },
        onSelectGoal = { goal ->
            selectedGoal = goal.value
            if (goal.value == "race") {
                raceHandled = false
            } else {
                raceHandled = true
                viewModel.submit("My goal is ${goal.phrase}.", StructuredFields(goal_type = goal.value))
            }
        },
        onSetRace = {
            val label = raceDistances.firstOrNull { it.meters == raceDistanceM }?.label ?: "race"
            val dateString = isoDate.format(Instant.ofEpochMilli(raceDateMillis))
            raceHandled = true
            viewModel.submit(
                "My goal is a $label on $dateString.",
                StructuredFields(goal_type = "race", goal_race_date = dateString, goal_race_distance_m = raceDistanceM)
            )
        },
        onSkipDetails = {
            raceHandled = true
            viewModel.submit("My goal is to run a race.", StructuredFields(goal_type = "race"))
        },
        onInputChanged = viewModel::onInputChanged,
        onSend = viewModel::onSendText,
        onRetry = viewModel::retry
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GoalContent(
    uiState: IntakeChatViewModel.UiState,
    selectedGoal: String?,
    raceDistanceM: Double,
    raceDateMillis: Long,
    onRaceDistanceChange: (Double) -> Unit,
    onRaceDateChange: (Long) -> Unit,
    onSelectGoal: (Goal) -> Unit,
    onSetRace: () -> Unit,
    onSkipDetails: () -> Unit,
    onInputChanged: (String) -> Unit,
    onSend: () -> Unit,
    onRetry: () -> Unit
) {
    GuidedScreenScaffold(
        title = "Your Goal. Our Goal",
        imageRes = R.drawable.onboarding_goal,
        autoExpand = uiState.phase == IntakeChatViewModel.Phase.Waiting,
        structuredInputs = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    goals.forEach { goal ->
                        OnboardingChip(
                            label = goal.label,
                            isSelected = selectedGoal == goal.value,
                            onClick = { onSelectGoal(goal) }
                        )
                    }
                }
                if (selectedGoal == "race") {
                    RaceDetailRow(
                        distanceM = raceDistanceM,
                        onDistanceChange = onRaceDistanceChange,
                        dateMillis = raceDateMillis,
                        onDateChange = onRaceDateChange,
                        onSetRace = onSetRace,
                        onSkipDetails = onSkipDetails
                    )
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RaceDetailRow(
    distanceM: Double,
    onDistanceChange: (Double) -> Unit,
    dateMillis: Long,
    onDateChange: (Long) -> Unit,
    onSetRace: () -> Unit,
    onSkipDetails: () -> Unit
) {
    var showDatePicker by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            raceDistances.forEachIndexed { index, d ->
                SegmentedButton(
                    selected = distanceM == d.meters,
                    onClick = { onDistanceChange(d.meters) },
                    shape = SegmentedButtonDefaults.itemShape(index, raceDistances.size)
                ) { Text(d.label) }
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Race date", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            TextButton(onClick = { showDatePicker = true }) {
                Text(isoDate.format(Instant.ofEpochMilli(dateMillis)))
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            PrimaryButton(text = "Set race", onClick = onSetRace, modifier = Modifier.weight(1f))
            TextButton(onClick = onSkipDetails) { Text("Skip details") }
        }
    }

    if (showDatePicker) {
        val dateState = androidx.compose.material3.rememberDatePickerState(initialSelectedDateMillis = dateMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    dateState.selectedDateMillis?.let(onDateChange)
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } }
        ) {
            DatePicker(state = dateState)
        }
    }
}

@Preview
@Composable
private fun GoalPreview() = StridewellTheme {
    GoalContent(
        uiState = sampleChatState("What are we training for?"),
        selectedGoal = "race",
        raceDistanceM = 21097.0,
        raceDateMillis = Instant.now().toEpochMilli(),
        onRaceDistanceChange = {},
        onRaceDateChange = {},
        onSelectGoal = {},
        onSetRace = {},
        onSkipDetails = {},
        onInputChanged = {},
        onSend = {},
        onRetry = {}
    )
}
