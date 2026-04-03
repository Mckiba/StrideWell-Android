package com.stridewell.app.ui.main.planchange

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stridewell.app.model.DecisionRecord
import com.stridewell.app.model.DecisionTrigger
import com.stridewell.app.model.SignalsUsed
import com.stridewell.app.ui.theme.CornerRadius
import com.stridewell.app.ui.theme.InterFamily
import com.stridewell.app.ui.theme.SofiaSansFamily
import com.stridewell.app.ui.theme.Spacing
import com.stridewell.app.util.DateUtils

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun PlanChangeScreen(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PlanChangeViewModel = hiltViewModel()
) {
    val screenState by viewModel.screenState.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Plan Update") }
            )
        }
    ) { innerPadding ->
        when (val state = screenState) {
            PlanChangeViewModel.ScreenState.Loading -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            PlanChangeViewModel.ScreenState.Empty -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text("No changes to show")
            }
            is PlanChangeViewModel.ScreenState.Error -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    Text(state.message)
                    IconButton(onClick = viewModel::load) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Retry"
                        )
                    }
                }
            }
            is PlanChangeViewModel.ScreenState.Loaded -> PlanChangeContent(
                record = state.record,
                innerPadding = innerPadding,
                onDismiss = { viewModel.markSeen(onDismiss) }
            )
        }
    }
}

@Composable
private fun PlanChangeContent(
    record: DecisionRecord,
    innerPadding: PaddingValues,
    onDismiss: () -> Unit
) {
    var rationaleExpanded by rememberSaveable { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding),
        contentPadding = PaddingValues(Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        item {
            DetailCard {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    Text(
                        text = "Your plan was updated",
                        style = sectionTitleStyle(),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = DateUtils.displayDateTime(record.created_at),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            DetailCard {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = triggerLabel(record.trigger),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        if (record.diff_summary.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    Text(
                        text = "What changed",
                        style = sectionTitleStyle(),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    DetailCard {
                        Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                            record.diff_summary.forEach { item ->
                                Text(
                                    text = "• $item",
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }

        if (record.rationale_bullets.isNotEmpty()) {
            item {
                DetailCard(
                    modifier = Modifier.clickable { rationaleExpanded = !rationaleExpanded }
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                        Text(
                            text = "Here's why",
                            style = sectionTitleStyle(),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (rationaleExpanded) {
                            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                                record.rationale_bullets.forEach { bullet ->
                                    Text(
                                        text = "• $bullet",
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        record.signals_used?.let { signals ->
            item {
                SignalsCard(signals)
            }
        }

        item {
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(CornerRadius.md)
            ) {
                Text("Got it")
            }
        }
    }
}

@Composable
private fun DetailCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(CornerRadius.md),
        tonalElevation = 2.dp,
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.md)
        ) {
            content()
        }
    }
}

@Composable
private fun SignalsCard(signals: SignalsUsed) {
    DetailCard {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Text(
                text = "Signals",
                style = sectionTitleStyle(),
                color = MaterialTheme.colorScheme.onSurface
            )
            signals.fatigue_trend?.let { fatigue ->
                Text("Fatigue trend: ${fatigue.replace('_', ' ').replaceFirstChar { it.uppercase() }}")
            }
            signals.injury_risk_level?.let { injury ->
                Text("Injury risk: ${injury.replace('_', ' ').replaceFirstChar { it.uppercase() }}")
            }
        }
    }
}

private fun triggerLabel(trigger: DecisionTrigger): String = when (trigger) {
    DecisionTrigger.new_activity -> "Run completed"
    DecisionTrigger.missed_workout -> "Missed workout"
    DecisionTrigger.reflection_submitted -> "Reflection submitted"
    DecisionTrigger.user_requested_recalc -> "Recalculation requested"
    DecisionTrigger.fatigue_flag -> "High fatigue detected"
    DecisionTrigger.injury_flag -> "Injury reported"
    DecisionTrigger.onboarding -> "Initial plan created"
}

private fun sectionTitleStyle() = TextStyle(
    fontFamily = SofiaSansFamily,
    fontWeight = FontWeight.Bold,
    fontSize = 20.sp,
    lineHeight = 24.sp
)
