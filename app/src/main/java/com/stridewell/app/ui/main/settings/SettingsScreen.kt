package com.stridewell.app.ui.main.settings

import android.app.TimePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stridewell.BuildConfig
import com.stridewell.app.data.HomeCardsRepository
import com.stridewell.app.ui.main.rememberNavBarBottomInset
import com.stridewell.app.ui.theme.CornerRadius
import com.stridewell.app.ui.theme.Spacing
import com.stridewell.app.util.AppTheme
import com.stridewell.app.util.UnitSystem
import java.util.Locale

@Composable
fun SettingsScreen(
    onOpenFitnessProfile: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Dialogs rendered outside the scroll container
    DisconnectDialog(uiState, viewModel)
    DeleteDialog(uiState, viewModel)
    DeleteConfirmDialog(uiState, viewModel)

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        contentPadding = PaddingValues(
            start = Spacing.md,
            end = Spacing.md,
            top = Spacing.md,
            bottom = Spacing.md + rememberNavBarBottomInset()
        )
    ) {
        item {
            ConnectedAccountsSection(
                uiState        = uiState,
                onConnectClick = { viewModel.onConnectClicked(context) },
                viewModel      = viewModel
            )
        }
        item { TrainingPreferencesSection(uiState, viewModel, onOpenFitnessProfile) }
        item { CoachingNotificationsSection(uiState, viewModel) }
        item { AccountSection(uiState, viewModel) }
        if (BuildConfig.DEBUG) {
            item { DeveloperSection(uiState, viewModel) }
        }
    }
}

// ── DEBUG: Developer ─────────────────────────────────────────────────────────

@Composable
private fun DeveloperSection(
    uiState: SettingsViewModel.UiState,
    viewModel: SettingsViewModel
) {
    SettingsCard(title = "Developer") {
        Column(modifier = Modifier.fillMaxWidth().padding(Spacing.md)) {
            Text("Weather Cards Location", style = MaterialTheme.typography.bodyMedium)
            Text(
                text  = "Force a test location for /home/cards",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.size(Spacing.sm))
            val entries = HomeCardsRepository.DebugLocation.entries
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                entries.forEachIndexed { index, loc ->
                    SegmentedButton(
                        selected = uiState.debugWeatherLocation == loc,
                        onClick  = { viewModel.onDebugWeatherLocationChanged(loc) },
                        shape    = SegmentedButtonDefaults.itemShape(index, entries.size),
                        label    = { Text(loc.label) }
                    )
                }
            }
        }
    }
}

// ── Section 1: Connected Accounts ────────────────────────────────────────────

@Composable
private fun ConnectedAccountsSection(
    uiState: SettingsViewModel.UiState,
    onConnectClick: () -> Unit,
    viewModel: SettingsViewModel
) {
    SettingsCard(title = "Connected Accounts") {
        StravaRow(
            state        = uiState.stravaState,
            onConnect    = onConnectClick,
            onReconnect  = onConnectClick,
            onDisconnect = viewModel::onDisconnectTapped,
            onRetry      = viewModel::onRetryStravaStatus
        )
    }
}

@Composable
private fun StravaRow(
    state: SettingsViewModel.StravaState,
    onConnect: () -> Unit,
    onReconnect: () -> Unit,
    onDisconnect: () -> Unit,
    onRetry: () -> Unit
) {
    when (state) {
        // Loading — single line + spinner
        SettingsViewModel.StravaState.Loading -> {
            Row(
                modifier          = Modifier.fillMaxWidth().padding(Spacing.md),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text     = "Checking connection…",
                    style    = MaterialTheme.typography.bodyMedium,
                    color    = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            }
        }

        // Connecting — single line + spinner
        SettingsViewModel.StravaState.Connecting -> {
            Row(
                modifier          = Modifier.fillMaxWidth().padding(Spacing.md),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text     = "Connecting to Strava…",
                    style    = MaterialTheme.typography.bodyMedium,
                    color    = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            }
        }

        // Disconnecting — single line + spinner
        SettingsViewModel.StravaState.Disconnecting -> {
            Row(
                modifier          = Modifier.fillMaxWidth().padding(Spacing.md),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text     = "Disconnecting…",
                    style    = MaterialTheme.typography.bodyMedium,
                    color    = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            }
        }

        // Disconnected — title + subtitle + Connect button
        SettingsViewModel.StravaState.Disconnected -> {
            Row(
                modifier          = Modifier.fillMaxWidth().padding(Spacing.md),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Strava", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        text  = "Not connected",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                TextButton(onClick = onConnect) { Text("Connect") }
            }
        }

        // Connected — title + "Connected (scope)" subtitle + Disconnect button
        is SettingsViewModel.StravaState.Connected -> {
            val subtitle = if (!state.scope.isNullOrBlank()) "Connected (${state.scope})" else "Connected"
            Row(
                modifier          = Modifier.fillMaxWidth().padding(Spacing.md),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Strava", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        text  = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                OutlinedButton(onClick = onDisconnect) {
                    Text("Disconnect", color = MaterialTheme.colorScheme.error)
                }
            }
        }

        // Expired — title + "Connection expired" (orange) + Reconnect button
        is SettingsViewModel.StravaState.Expired -> {
            Row(
                modifier          = Modifier.fillMaxWidth().padding(Spacing.md),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Strava", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        text  = "Connection expired",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFFF9800) // orange, matching iOS
                    )
                }
                TextButton(onClick = onReconnect) { Text("Reconnect") }
            }
        }

        // Error — title + retry button, error message below
        is SettingsViewModel.StravaState.Error -> {
            Column(
                modifier = Modifier.fillMaxWidth().padding(Spacing.md),
                verticalArrangement = Arrangement.spacedBy(Spacing.xs)
            ) {
                Row(
                    modifier          = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text     = "Strava",
                        style    = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = onRetry) { Text("Retry") }
                }
                Text(
                    text  = state.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

// ── Section 2: Training Preferences ──────────────────────────────────────────

@Composable
private fun TrainingPreferencesSection(
    uiState: SettingsViewModel.UiState,
    viewModel: SettingsViewModel,
    onOpenFitnessProfile: () -> Unit = {}
) {
    SettingsCard(title = "Training Preferences") {
        // Goal — read-only with description
        SettingsDescriptionRow(
            label       = "Goal",
            description = "Set via chat with your coach",
            trailing    = { ChevronRight() }
        )
        HorizontalDivider(modifier = Modifier.padding(horizontal = Spacing.md))

        // V2 Phase 2 — Fitness Profile
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onOpenFitnessProfile)
                .padding(horizontal = Spacing.md, vertical = Spacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f).padding(end = Spacing.sm),
                verticalArrangement = Arrangement.spacedBy(Spacing.xs)
            ) {
                Text("Fitness Profile", style = MaterialTheme.typography.bodyMedium)
                Text(
                    text  = "Threshold pace, pace & HR zones",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            ChevronRight()
        }
        HorizontalDivider(modifier = Modifier.padding(horizontal = Spacing.md))

        // Units
        SettingsDescriptionRow(
            label       = "Units",
            description = "Distance and pace display",
            trailing    = {
                SingleChoiceSegmentedButtonRow {
                    listOf("km", "mi").forEachIndexed { index, text ->
                        SegmentedButton(
                            selected = if (index == 0) uiState.unitSystem == UnitSystem.METRIC
                                       else uiState.unitSystem == UnitSystem.IMPERIAL,
                            onClick  = {
                                viewModel.onUnitSystemChanged(
                                    if (index == 0) UnitSystem.METRIC else UnitSystem.IMPERIAL
                                )
                            },
                            shape = SegmentedButtonDefaults.itemShape(index, 2),
                            label = { Text(text) }
                        )
                    }
                }
            }
        )
        HorizontalDivider(modifier = Modifier.padding(horizontal = Spacing.md))

        // Appearance
        SettingsDescriptionRow(
            label       = "Appearance",
            description = "App colour scheme",
            trailing    = {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth(0.6f)) {
                    listOf("Device", "Light", "Dark").forEachIndexed { index, text ->
                        SegmentedButton(
                            selected = uiState.appTheme.ordinal == index,
                            onClick  = { viewModel.onAppThemeChanged(AppTheme.entries[index]) },
                            shape    = SegmentedButtonDefaults.itemShape(index, 3),
                            label    = { Text(text) }
                        )
                    }
                }
            }
        )
        HorizontalDivider(modifier = Modifier.padding(horizontal = Spacing.md))

        // Reflection reminders — toggle with description
        SettingsToggleRow(
            label       = "Reflection reminders",
            description = "Daily check-in notifications",
            checked     = uiState.reflectionReminders,
            onToggle    = viewModel::onReflectionRemindersChanged
        )
        HorizontalDivider(modifier = Modifier.padding(horizontal = Spacing.md))

        // Plan update alerts — toggle with description
        SettingsToggleRow(
            label       = "Plan update alerts",
            description = "Notified when your plan changes",
            checked     = uiState.planUpdateAlerts,
            onToggle    = viewModel::onPlanUpdateAlertsChanged
        )
    }
}

// ── Section 3: Coaching Notifications ────────────────────────────────────────

@Composable
private fun CoachingNotificationsSection(
    uiState: SettingsViewModel.UiState,
    viewModel: SettingsViewModel
) {
    val context = LocalContext.current
    val controlsEnabled = uiState.proactiveEnabled
    val quietHoursControlsEnabled = controlsEnabled && uiState.proactiveQuietHoursEnabled

    fun showTimeDialog(initial: String, onPicked: (String) -> Unit) {
        val parts = initial.split(":")
        val hour = parts.getOrNull(0)?.toIntOrNull() ?: 0
        val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0
        TimePickerDialog(
            context,
            { _, pickedHour, pickedMinute ->
                onPicked(String.format(Locale.US, "%02d:%02d", pickedHour, pickedMinute))
            },
            hour,
            minute,
            true
        ).show()
    }

    SettingsCard(title = "Coaching Notifications") {
        SettingsToggleRow(
            label = "Enable coaching notifications",
            description = "Receive proactive coach check-ins based on your training",
            checked = uiState.proactiveEnabled,
            onToggle = viewModel::onProactiveEnabledChanged
        )
        HorizontalDivider(modifier = Modifier.padding(horizontal = Spacing.md))

        SettingsToggleRow(
            label = "Training milestones",
            description = "Consistency, breakthrough workouts, fitness improvements",
            checked = uiState.proactiveTrainingMilestone,
            onToggle = viewModel::onProactiveTrainingMilestoneChanged,
            enabled = controlsEnabled
        )
        HorizontalDivider(modifier = Modifier.padding(horizontal = Spacing.md))

        SettingsToggleRow(
            label = "Training concerns",
            description = "Early flags for strain, drift, or risk patterns",
            checked = uiState.proactiveTrainingConcern,
            onToggle = viewModel::onProactiveTrainingConcernChanged,
            enabled = controlsEnabled
        )
        HorizontalDivider(modifier = Modifier.padding(horizontal = Spacing.md))

        SettingsToggleRow(
            label = "Upcoming events",
            description = "Timing alerts around race and plan events",
            checked = uiState.proactiveUpcomingEvent,
            onToggle = viewModel::onProactiveUpcomingEventChanged,
            enabled = controlsEnabled
        )
        HorizontalDivider(modifier = Modifier.padding(horizontal = Spacing.md))

        SettingsToggleRow(
            label = "Re-engagement",
            description = "Check-ins after extended inactivity",
            checked = uiState.proactiveReengagement,
            onToggle = viewModel::onProactiveReengagementChanged,
            enabled = controlsEnabled
        )
        HorizontalDivider(modifier = Modifier.padding(horizontal = Spacing.md))

        SettingsToggleRow(
            label = "Plan follow-up",
            description = "Missed-key-session and block-drift follow-ups",
            checked = uiState.proactivePlanFollowup,
            onToggle = viewModel::onProactivePlanFollowupChanged,
            enabled = controlsEnabled
        )
        HorizontalDivider(modifier = Modifier.padding(horizontal = Spacing.md))

        SettingsToggleRow(
            label = "Quiet hours",
            description = "Pause delivery during your local overnight window",
            checked = uiState.proactiveQuietHoursEnabled,
            onToggle = viewModel::onProactiveQuietHoursEnabledChanged,
            enabled = controlsEnabled
        )
        HorizontalDivider(modifier = Modifier.padding(horizontal = Spacing.md))

        SettingsTimeRow(
            label = "Quiet hours start",
            description = "Local time",
            value = uiState.proactiveQuietHoursStart,
            enabled = quietHoursControlsEnabled,
            onClick = {
                showTimeDialog(uiState.proactiveQuietHoursStart, viewModel::onProactiveQuietHoursStartChanged)
            }
        )
        HorizontalDivider(modifier = Modifier.padding(horizontal = Spacing.md))

        SettingsTimeRow(
            label = "Quiet hours end",
            description = "Local time",
            value = uiState.proactiveQuietHoursEnd,
            enabled = quietHoursControlsEnabled,
            onClick = {
                showTimeDialog(uiState.proactiveQuietHoursEnd, viewModel::onProactiveQuietHoursEndChanged)
            }
        )
        HorizontalDivider(modifier = Modifier.padding(horizontal = Spacing.md))

        SettingsDescriptionRow(
            label = "Timezone",
            description = "Used for quiet hours delivery",
            trailing = {
                Text(
                    text = uiState.proactiveTimezone,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        )

        if (!uiState.proactiveSyncError.isNullOrBlank()) {
            HorizontalDivider(modifier = Modifier.padding(horizontal = Spacing.md))
            Text(
                text = uiState.proactiveSyncError,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm)
            )
        }
    }
}

// ── Section 4: Account ────────────────────────────────────────────────────────

@Composable
private fun AccountSection(
    uiState: SettingsViewModel.UiState,
    viewModel: SettingsViewModel
) {
    SettingsCard(title = "Account") {
        // Sign out
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = viewModel::onSignOut)
                .padding(horizontal = Spacing.md, vertical = Spacing.md),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Text("Sign out", style = MaterialTheme.typography.bodyMedium)
        }
        HorizontalDivider(modifier = Modifier.padding(horizontal = Spacing.md))

        // Export data — disabled, with "Coming soon" description
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.md, vertical = Spacing.md),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text  = "Export data",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                )
                Text(
                    text  = "Coming soon",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                )
            }
        }
        HorizontalDivider(modifier = Modifier.padding(horizontal = Spacing.md))

        // Delete account — red, with inline spinner + inline error
        val isDeleting = uiState.deleteState is SettingsViewModel.DeleteState.Deleting
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = !isDeleting, onClick = viewModel::onDeleteAccountTapped)
                    .padding(horizontal = Spacing.md, vertical = Spacing.md),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text(
                    text  = "Delete account",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isDeleting) MaterialTheme.colorScheme.error.copy(alpha = 0.38f)
                            else MaterialTheme.colorScheme.error
                )
                if (isDeleting) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                }
            }
            // Inline delete error (matches iOS inline display)
            if (uiState.deleteState is SettingsViewModel.DeleteState.Error) {
                Text(
                    text     = uiState.deleteState.message,
                    style    = MaterialTheme.typography.bodySmall,
                    color    = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(start = Spacing.md, end = Spacing.md, bottom = Spacing.sm)
                )
            }
        }
    }
}

// ── Dialogs ───────────────────────────────────────────────────────────────────

@Composable
private fun DisconnectDialog(
    uiState: SettingsViewModel.UiState,
    viewModel: SettingsViewModel
) {
    if (!uiState.showDisconnectDialog) return
    AlertDialog(
        onDismissRequest = viewModel::onDisconnectDismissed,
        title            = { Text("Disconnect Strava?") },
        text             = { Text("Your run data will remain, but new activities won't sync.") },
        confirmButton    = {
            TextButton(onClick = viewModel::onDisconnectConfirmed) {
                Text("Disconnect", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton    = {
            TextButton(onClick = viewModel::onDisconnectDismissed) { Text("Cancel") }
        }
    )
}

@Composable
private fun DeleteDialog(
    uiState: SettingsViewModel.UiState,
    viewModel: SettingsViewModel
) {
    if (!uiState.showDeleteDialog) return
    AlertDialog(
        onDismissRequest = viewModel::onDeleteDialogDismissed,
        title            = { Text("Delete Account?") },
        text             = { Text("This will permanently delete your account, training plan, all run data, and conversation history.") },
        confirmButton    = {
            TextButton(onClick = viewModel::onDeleteFirstConfirmed) {
                Text("Delete My Account", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton    = {
            TextButton(onClick = viewModel::onDeleteDialogDismissed) { Text("Cancel") }
        }
    )
}

@Composable
private fun DeleteConfirmDialog(
    uiState: SettingsViewModel.UiState,
    viewModel: SettingsViewModel
) {
    if (!uiState.showDeleteConfirmDialog) return
    val isDeleting = uiState.deleteState is SettingsViewModel.DeleteState.Deleting
    AlertDialog(
        onDismissRequest = viewModel::onDeleteConfirmDismissed,
        title            = { Text("This cannot be undone") },
        text             = { Text("All data will be permanently removed from our servers.") },
        confirmButton    = {
            TextButton(
                onClick = viewModel::onDeleteConfirmed,
                enabled = !isDeleting
            ) {
                if (isDeleting) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                } else {
                    Text("Permanently Delete", color = MaterialTheme.colorScheme.error)
                }
            }
        },
        dismissButton    = {
            TextButton(
                onClick = viewModel::onDeleteConfirmDismissed,
                enabled = !isDeleting
            ) { Text("Cancel") }
        }
    )
}

// ── Helper composables ────────────────────────────────────────────────────────

@Composable
private fun SettingsCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Text(
            text     = title,
            style    = MaterialTheme.typography.titleSmall,
            color    = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(horizontal = Spacing.xs)
        )
        Card(
            shape  = RoundedCornerShape(CornerRadius.md),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.fillMaxWidth(), content = content)
        }
    }
}

/** Row: [VStack(title, description) weight=1] [trailing control]. Text never wraps. */
@Composable
private fun SettingsDescriptionRow(
    label: String,
    description: String,
    trailing: @Composable () -> Unit = {}
) {
    Row(
        modifier          = Modifier.fillMaxWidth().padding(horizontal = Spacing.md, vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier            = Modifier.weight(1f).padding(end = Spacing.sm),
            verticalArrangement = Arrangement.spacedBy(Spacing.xs)
        ) {
            Text(label, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                text     = description,
                style    = MaterialTheme.typography.bodySmall,
                color    = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        trailing()
    }
}

@Composable
private fun SettingsToggleRow(
    label: String,
    description: String,
    checked: Boolean,
    onToggle: (Boolean) -> Unit,
    enabled: Boolean = true
) {
    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else 0.5f)
            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier            = Modifier.weight(1f).padding(end = Spacing.sm),
            verticalArrangement = Arrangement.spacedBy(Spacing.xs)
        ) {
            Text(label, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                text     = description,
                style    = MaterialTheme.typography.bodySmall,
                color    = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = if (enabled) onToggle else null,
            enabled = enabled
        )
    }
}

@Composable
private fun SettingsTimeRow(
    label: String,
    description: String,
    value: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else 0.5f)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = Spacing.sm),
            verticalArrangement = Arrangement.spacedBy(Spacing.xs)
        ) {
            Text(label, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ChevronRight() {
    Text(
        text  = "›",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}
