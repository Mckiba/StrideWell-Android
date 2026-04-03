package com.stridewell.app.ui.main.reflection

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stridewell.app.ui.components.FatigueSlider
import com.stridewell.app.ui.components.MoodSelector
import com.stridewell.app.ui.components.SorenessEntryRow
import com.stridewell.app.ui.theme.CornerRadius
import com.stridewell.app.ui.theme.SofiaSansFamily
import com.stridewell.app.ui.theme.Spacing

@Composable
fun ReflectionScreen(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ReflectionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    if (uiState.showSuccess) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(Spacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Text("Check-in recorded", style = reflectionSectionTitleStyle(), color = MaterialTheme.colorScheme.onSurface)
            Text("Thanks! This helps your plan stay on track.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.md),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
                Text("Daily Check-in", style = reflectionTitleStyle(), color = MaterialTheme.colorScheme.onSurface)
            }
        }

        item {
            ReflectionCard(title = "Fatigue", trailing = uiState.fatigue.toInt().toString()) {
                FatigueSlider(
                    labelStart = "Fresh",
                    labelEnd = "Exhausted",
                    value = uiState.fatigue,
                    onValueChange = viewModel::setFatigue
                )
            }
        }

        item {
            ReflectionCard(title = "Sleep Quality", trailing = uiState.sleepQuality.toInt().toString()) {
                FatigueSlider(
                    labelStart = "Poor",
                    labelEnd = "Great",
                    value = uiState.sleepQuality,
                    onValueChange = viewModel::setSleepQuality
                )
            }
        }

        item {
            ReflectionCard(title = "Mood") {
                MoodSelector(
                    selected = uiState.mood,
                    onSelect = viewModel::setMood
                )
            }
        }

        item {
            ReflectionCard(title = "Soreness") {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    uiState.sorenessEntries.forEach { entry ->
                        SorenessEntryRow(
                            location = entry.location,
                            score = entry.score,
                            onLocationChange = { viewModel.updateSorenessLocation(entry.id, it) },
                            onScoreChange = { viewModel.updateSorenessScore(entry.id, it) },
                            onRemove = { viewModel.removeSorenessEntry(entry.id) }
                        )
                    }
                    Button(onClick = viewModel::addSorenessEntry) {
                        Icon(Icons.Default.AddCircle, contentDescription = null)
                        Text("Add body part", modifier = Modifier.padding(start = Spacing.xs))
                    }
                }
            }
        }

        item {
            ReflectionCard(title = "How are you feeling? Any pain or tightness?") {
                OutlinedTextField(
                    value = uiState.freeText,
                    onValueChange = viewModel::setFreeText,
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 4
                )
            }
        }

        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.md),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                uiState.errorMessage?.let {
                    Text(it, color = MaterialTheme.colorScheme.error)
                }
                Button(
                    onClick = { viewModel.submit(onDismiss) },
                    enabled = !uiState.isSubmitting,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (uiState.isSubmitting) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary)
                    } else {
                        Text("Submit check-in")
                    }
                }
            }
        }
    }
}

@Composable
private fun ReflectionCard(
    title: String,
    trailing: String? = null,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.md),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(CornerRadius.md),
        tonalElevation = 2.dp,
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            androidx.compose.foundation.layout.Row {
                Text(title, style = reflectionSectionTitleStyle(), color = MaterialTheme.colorScheme.onSurface)
                androidx.compose.foundation.layout.Spacer(modifier = Modifier.weight(1f))
                trailing?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            content()
        }
    }
}

private fun reflectionTitleStyle() = TextStyle(
    fontFamily = SofiaSansFamily,
    fontWeight = FontWeight.Bold,
    fontSize = 24.sp
)

private fun reflectionSectionTitleStyle() = TextStyle(
    fontFamily = SofiaSansFamily,
    fontWeight = FontWeight.Bold,
    fontSize = 16.sp
)
