package com.stridewell.app.navigation

import android.net.Uri
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import androidx.navigation.compose.rememberNavController
import com.stridewell.app.model.DecisionRecord
import com.stridewell.app.ui.auth.LaunchViewModel
import com.stridewell.app.ui.auth.SignInScreen
import com.stridewell.app.ui.auth.SignUpScreen
import com.stridewell.app.ui.auth.WelcomeScreen
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessaging
import com.stridewell.app.ui.main.MainContainerScreen
import com.stridewell.app.ui.main.chat.ChatViewModel
import com.stridewell.app.ui.main.detail.RunDetailScreen
import com.stridewell.app.ui.main.notifications.NotificationsViewModel
import kotlinx.coroutines.tasks.await
import com.stridewell.app.ui.main.fitness.FitnessProfileScreen
import com.stridewell.app.ui.main.planchange.PlanChangeScreen
import com.stridewell.app.ui.main.weekly.WeeklySummaryScreen
import com.stridewell.app.ui.onboarding.IntakeInterviewScreen
import com.stridewell.app.ui.onboarding.PlanBuildingScreen
import com.stridewell.app.ui.onboarding.PlanRevealScreen
import com.stridewell.app.ui.onboarding.StravaConnectScreen
import com.stridewell.app.ui.onboarding.UnitPreferenceScreen
import com.stridewell.app.ui.onboarding.guided.GoalScreen
import com.stridewell.app.ui.onboarding.guided.HistoryConfirmScreen
import com.stridewell.app.ui.onboarding.guided.LessonsScreen
import com.stridewell.app.ui.onboarding.guided.ManualBaselineScreen
import com.stridewell.app.ui.onboarding.guided.RoutinesScreen
import com.stridewell.app.ui.onboarding.guided.SpeedworkScreen
import com.stridewell.app.ui.onboarding.guided.GoalScreen
import com.stridewell.app.ui.onboarding.guided.HistoryConfirmScreen
import com.stridewell.app.ui.onboarding.guided.LessonsScreen
import com.stridewell.app.ui.onboarding.guided.ManualBaselineScreen
import com.stridewell.app.ui.onboarding.guided.RoutinesScreen
import com.stridewell.app.ui.onboarding.guided.SpeedworkScreen
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.json.Json

@Composable
fun StridewellNavHost(
    launchState: LaunchViewModel.LaunchState,
    unauthorizedFlow: Flow<Unit>,
    notificationDeepLinkFlow: MutableStateFlow<String?>,
    chatEntryMessageFlow: MutableStateFlow<String?>,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    // Route to the correct destination once the launch auth check completes
    LaunchedEffect(launchState) {
        when (launchState) {
            LaunchViewModel.LaunchState.Loading         -> Unit
            LaunchViewModel.LaunchState.Unauthenticated -> navController.navigate(Route.Welcome.path) {
                popUpTo(0) { inclusive = true }
            }
            is LaunchViewModel.LaunchState.NeedsOnboarding -> navController.navigate(launchState.route) {
                popUpTo(0) { inclusive = true }
            }
            LaunchViewModel.LaunchState.Authenticated   -> navController.navigate(Route.Main.path) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    // 401 on a protected endpoint → clear back stack → Welcome
    LaunchedEffect(Unit) {
        unauthorizedFlow.collect {
            navController.navigate(Route.Welcome.path) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    // Notification tap → navigate to the appropriate screen
    LaunchedEffect(Unit) {
        notificationDeepLinkFlow.collect { deepLink ->
            when (deepLink) {
                "plan_change" -> {
                    navController.navigate(Route.PlanChange.basePath)
                    notificationDeepLinkFlow.value = null
                }
                "plan_reveal" -> {
                    navController.navigate(Route.PlanReveal.path)
                    notificationDeepLinkFlow.value = null
                }
                "home", "chat" -> {
                    navController.navigate(Route.Main.path) {
                        launchSingleTop = true
                    }
                }
            }
        }
    }

    NavHost(
        navController    = navController,
        startDestination = Route.Welcome.path,
        modifier         = modifier,
        enterTransition  = { fadeIn(tween(200)) },
        exitTransition   = { fadeOut(tween(200)) }
    ) {
        // ── Auth ──────────────────────────────────────────────────────────────

        composable(Route.Welcome.path) {
            WelcomeScreen(
                onGetStarted = { navController.navigate(Route.SignUp.path) },
                onSignIn     = { navController.navigate(Route.SignIn.path) },
                onSignedIn   = { onboardingStatus ->
                    navController.navigate(Route.forOnboardingStatus(onboardingStatus)) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(Route.SignUp.path) {
            SignUpScreen(
                onSignedUp = { onboardingStatus ->
                    navController.navigate(Route.forOnboardingStatus(onboardingStatus)) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onSignIn = {
                    // Navigate to SignIn, removing SignUp from the back stack
                    navController.navigate(Route.SignIn.path) {
                        popUpTo(Route.SignUp.path) { inclusive = true }
                    }
                }
            )
        }

        composable(Route.SignIn.path) {
            SignInScreen(
                onSignedIn = { onboardingStatus ->
                    navController.navigate(Route.forOnboardingStatus(onboardingStatus)) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onBack   = { navController.popBackStack() },
                onSignUp = {
                    // Navigate to SignUp, removing SignIn from the back stack
                    navController.navigate(Route.SignUp.path) {
                        popUpTo(Route.SignIn.path) { inclusive = true }
                    }
                }
            )
        }

        // ── Onboarding ────────────────────────────────────────────────────────

        // S0. Unit preference — the fresh-onboarding entry. Begin pushes StravaConnect and
        // drops the unit step from the back stack.
        composable(Route.UnitPreference.path) {
            UnitPreferenceScreen(onNavigate = { route ->
                navController.navigate(route) {
                    popUpTo(Route.UnitPreference.path) { inclusive = true }
                }
            })
        }

        // S1. Forward moves preserve the back stack so the athlete can return; advancing into
        // plan building clears the intake stack (S1..current) so back doesn't re-enter intake.
        composable(Route.StravaConnect.path) {
            StravaConnectScreen(onNavigate = { route -> advanceOnboarding(navController, route) })
        }
        composable(Route.HistoryConfirm.path) {
            HistoryConfirmScreen(onNavigate = { route -> advanceOnboarding(navController, route) })
        }
        composable(Route.ManualBaseline.path) {
            ManualBaselineScreen(onNavigate = { route -> advanceOnboarding(navController, route) })
        }
        composable(Route.Goal.path) {
            GoalScreen(onNavigate = { route -> advanceOnboarding(navController, route) })
        }
        composable(Route.Routines.path) {
            RoutinesScreen(onNavigate = { route -> advanceOnboarding(navController, route) })
        }
        composable(Route.Speedwork.path) {
            SpeedworkScreen(onNavigate = { route -> advanceOnboarding(navController, route) })
        }
        composable(Route.Lessons.path) {
            LessonsScreen(onNavigate = { route -> advanceOnboarding(navController, route) })
        }
        // Legacy V1 single-chat interview — no longer in the guided flow, kept until removed.
        composable(Route.IntakeInterview.path) {
            IntakeInterviewScreen(
                onNavigateToPlanBuilding = {
                    navController.navigate(Route.PlanBuilding.path) {
                        popUpTo(Route.IntakeInterview.path) { inclusive = true }
                    }
                }
            )
        }
        composable(Route.PlanBuilding.path) {
            PlanBuildingScreen(
                onNavigateToPlanReveal = {
                    navController.navigate(Route.PlanReveal.path) {
                        popUpTo(Route.PlanBuilding.path) { inclusive = true }
                    }
                }
            )
        }
        composable(Route.PlanReveal.path) {
            PlanRevealScreen(
                onNavigateToMain = {
                    navController.navigate(Route.Main.path) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        // ── Main ──────────────────────────────────────────────────────────────

        composable(Route.Main.path) {
            val context = LocalContext.current
            val notificationsVm: NotificationsViewModel = hiltViewModel()
            val chatVm: ChatViewModel = hiltViewModel()
            val permissionLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { /* token is registered by onNewToken; also attempt below regardless */ }

            LaunchedEffect(Unit) {
                // On Android 13+, request POST_NOTIFICATIONS permission if not yet granted
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val granted = ContextCompat.checkSelfPermission(
                        context, Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED
                    if (!granted) permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
                // Always register the current FCM token on every launch (idempotent)
                runCatching {
                    val token = FirebaseMessaging.getInstance().token.await()
                    notificationsVm.registerToken(token)
                }
            }

            MainContainerScreen(
                onOpenPlanChange = { record ->
                    val encodedRecord = record?.let {
                        Uri.encode(Json.encodeToString(DecisionRecord.serializer(), it))
                    }
                    navController.navigate(Route.planChange(encodedRecord))
                },
                onNavigateToRunDetail = { runId ->
                    navController.navigate(Route.RunDetail.destination(runId))
                },
                onOpenFitnessProfile = { navController.navigate(Route.FitnessProfile.path) },
                onOpenWeeklySummary  = { navController.navigate(Route.WeeklySummary.path) },
                notificationDeepLinkFlow = notificationDeepLinkFlow,
                chatEntryMessageFlow = chatEntryMessageFlow,
                chatViewModel = chatVm
            )
        }
        composable(
            route = Route.PlanChange.path,
            arguments = listOf(
                navArgument(Route.PlanChange.argRecord) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            ),
            deepLinks = listOf(
                navDeepLink { uriPattern = "stridewell://plan-change" },
                navDeepLink { uriPattern = "stridewell://plan-change?record={record}" }
            )
        ) {
            PlanChangeScreen(
                onDismiss = { navController.popBackStack() }
            )
        }
        // Run Detail — replaces both the legacy ActivityDetail (basic stats)
        // and RunAnalysis (V2 analysis) screens.
        composable(
            route = Route.RunDetail.path,
            arguments = listOf(
                navArgument(Route.RunDetail.argRunId) { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val runId = backStackEntry.arguments?.getString(Route.RunDetail.argRunId).orEmpty()
            RunDetailScreen(
                runId = runId,
                onBack = { navController.popBackStack() }
            )
        }

        // ── V2 Phase 2 screens ────────────────────────────────────────────────

        composable(Route.FitnessProfile.path) {
            FitnessProfileScreen(onBack = { navController.popBackStack() })
        }

        composable(Route.WeeklySummary.path) {
            WeeklySummaryScreen(onBack = { navController.popBackStack() })
        }
    }
}

/**
 * Navigate to the next guided onboarding screen. Advancing into plan building clears the
 * intake stack (Connect Screen through the current screen); every other move is a plain push so back
 * navigation returns to earlier screens.
 */
private fun advanceOnboarding(navController: NavHostController, route: String) {
    if (route == Route.PlanBuilding.path) {
        navController.navigate(route) {
            popUpTo(Route.StravaConnect.path) { inclusive = true }
        }
    } else {
        navController.navigate(route) { launchSingleTop = true }
    }
}
