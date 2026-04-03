package com.stridewell.app.ui.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stridewell.app.model.PlanDay
import com.stridewell.app.model.PlanVersionResponse
import com.stridewell.app.model.PlanVersionWeek
import com.stridewell.app.model.PlanSource
import com.stridewell.app.model.Workout
import com.stridewell.app.model.WorkoutIntensity
import com.stridewell.app.model.WorkoutType
import com.stridewell.app.ui.components.WorkoutCard
import com.stridewell.app.ui.theme.CornerRadius
import com.stridewell.app.ui.theme.OnWarningContainerDark
import com.stridewell.app.ui.theme.OnWarningContainerLight
import com.stridewell.app.ui.theme.Spacing
import com.stridewell.app.ui.theme.StridewellTheme
import com.stridewell.app.ui.theme.WarningBadgeDark
import com.stridewell.app.ui.theme.WarningBadgeLight
import com.stridewell.app.ui.theme.WarningContainerDark
import com.stridewell.app.ui.theme.WarningContainerLight
import com.stridewell.app.util.UnitSystem

@Composable
fun PlanRevealScreen(
    onNavigateToMain: () -> Unit,
    viewModel: PlanRevealViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Spec: no back navigation on plan reveal — empty handler consumes the event.
    BackHandler(enabled = true) {}

    LaunchedEffect(Unit) {
        viewModel.navigateToMain.collect {
            onNavigateToMain()
        }
    }

    when (val screenState = uiState.screenState) {
        PlanRevealViewModel.ScreenState.Loading -> PlanRevealLoading()
        is PlanRevealViewModel.ScreenState.Error -> PlanRevealError(
            message = screenState.message,
            onRetry = viewModel::retry
        )
        is PlanRevealViewModel.ScreenState.Loaded -> PlanRevealContent(
            plan = screenState.plan,
            unitSystem = uiState.unitSystem,
            confirmError = uiState.confirmError,
            isConfirming = false,
            onConfirm = viewModel::confirmPlan
        )
        is PlanRevealViewModel.ScreenState.Confirming -> PlanRevealContent(
            plan = screenState.plan,
            unitSystem = uiState.unitSystem,
            confirmError = uiState.confirmError,
            isConfirming = true,
            onConfirm = {}
        )
    }
}

@Composable
private fun PlanRevealLoading() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            CircularProgressIndicator()
            Text(
                text = "Building preview…",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PlanRevealError(
    message: String,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            androidx.compose.material3.Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(40.dp)
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Button(onClick = onRetry) {
                Text("Try again")
            }
        }
    }
}

@Composable
private fun PlanRevealContent(
    plan: PlanVersionResponse,
    unitSystem: UnitSystem,
    confirmError: String?,
    isConfirming: Boolean,
    onConfirm: () -> Unit
) {
    val days = plan.weeks.firstOrNull()?.days.orEmpty()
    var rationaleExpanded by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            Surface(
                tonalElevation = 8.dp,
                shadowElevation = 12.dp,
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = Spacing.md, vertical = Spacing.sm),
                    verticalArrangement = Arrangement.spacedBy(Spacing.xs)
                ) {
                    if (confirmError != null) {
                        Text(
                            text = confirmError,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Button(
                        onClick = onConfirm,
                        enabled = !isConfirming,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isConfirming) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Start training")
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = Spacing.md,
                top = Spacing.md,
                end = Spacing.md,
                bottom = 96.dp
            )
        ) {
            item { PlanRevealHeader() }

            if (plan.coaching_notes != null || plan.phase_label != null) {
                item {
                    CoachCard(
                        notes = plan.coaching_notes,
                        phaseLabel = plan.phase_label
                    )
                }
            }

            items(days, key = { it.date }) { day ->
                WorkoutCard(
                    day = day,
                    unitSystem = unitSystem
                )
            }

            if (!plan.rationale_bullets.isNullOrEmpty()) {
                item {
                    RationaleSection(
                        bullets = plan.rationale_bullets.orEmpty(),
                        expanded = rationaleExpanded,
                        onToggle = { rationaleExpanded = !rationaleExpanded }
                    )
                }
            }

            if (!plan.warning_flags.isNullOrEmpty()) {
                item {
                    WarningSection(plan.warning_flags.orEmpty())
                }
            }
        }
    }
}

@Composable
private fun PlanRevealHeader() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(top = Spacing.xs, bottom = Spacing.xs),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Your plan",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun CoachCard(
    notes: String?,
    phaseLabel: String?
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(CornerRadius.md)
            )
            .padding(Spacing.md),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (phaseLabel != null) {
            PhaseBadge(phaseLabel)
        }
        if (notes != null) {
            Text(
                text = notes,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun PhaseBadge(label: String) {
    Text(
        text = label.replace('_', ' ').replaceFirstChar { it.uppercase() },
        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .background(
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                shape = RoundedCornerShape(999.dp)
            )
            .padding(horizontal = 10.dp, vertical = 4.dp)
    )
}

@Composable
private fun RationaleSection(
    bullets: List<String>,
    expanded: Boolean,
    onToggle: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(vertical = Spacing.xs),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Why this plan?",
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(modifier = Modifier.weight(1f))
            androidx.compose.material3.Icon(
                imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        AnimatedVisibility(visible = expanded) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                bullets.forEach { bullet ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = "\u2022",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = bullet,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WarningSection(flags: List<String>) {
    val dark = isSystemInDarkTheme()
    val containerColor = if (dark) WarningContainerDark else WarningContainerLight
    val onContainerColor = if (dark) OnWarningContainerDark else OnWarningContainerLight
    val badgeColor = if (dark) WarningBadgeDark else WarningBadgeLight

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(color = containerColor, shape = RoundedCornerShape(CornerRadius.sm))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(Spacing.xs)
    ) {
        flags.forEach { flag ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = "!",
                    style = MaterialTheme.typography.labelSmall,
                    color = onContainerColor,
                    modifier = Modifier
                        .background(badgeColor, CircleShape)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
                Text(
                    text = flag,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PlanRevealContentPreview() {
    StridewellTheme {
        PlanRevealContent(
            plan = previewPlan,
            unitSystem = UnitSystem.METRIC,
            confirmError = null,
            isConfirming = false,
            onConfirm = {}
        )
    }
}

private val previewPlan = PlanVersionResponse(
    plan_version_id = "pv_preview",
    source = PlanSource.architect,
    start_date = "2026-04-06",
    horizon_days = 7,
    phase_label = "base_building",
    coaching_notes = "We're starting with a gentle base-building phase to establish your aerobic foundation before adding intensity.",
    rationale_bullets = listOf(
        "Your recent training suggests a stable aerobic base.",
        "Mileage climbs gradually before harder sessions arrive.",
        "Recovery spacing is built around your available running days."
    ),
    warning_flags = listOf("Avoid consecutive hard efforts if shin pain returns."),
    weeks = listOf(
        PlanVersionWeek(
            week_number = 1,
            start_date = "2026-04-06",
            days = listOf(
                PlanDay(
                    date = "2026-04-06",
                    workout = Workout(
                        type = WorkoutType.easy,
                        label = "Easy Run",
                        target_distance_m = 8000.0,
                        target_pace_s_per_km = 360.0,
                        intensity = WorkoutIntensity.easy,
                        description = "Relaxed aerobic running."
                    )
                ),
                PlanDay(
                    date = "2026-04-07",
                    workout = Workout(
                        type = WorkoutType.rest,
                        label = "Rest Day",
                        notes = "Mobility or walking only if it feels good."
                    )
                )
            )
        )
    )
)
