package com.stridewell.app.navigation

import com.stridewell.app.model.StravaHistorySummary

/**
 * Screens in the guided onboarding flow, in order. [HistoryConfirm] and [ManualBaseline] are
 * mutually exclusive — the baseline screen shown depends on whether usable Strava history
 * exists. The next screen is chosen by which intake fields are confirmed, never by turn count.
 */
enum class OnboardingScreen {
    Integrations,     // connect data sources
    HistoryConfirm,   // confirm the baseline read from Strava
    ManualBaseline,   // enter the baseline by hand
    Goal,
    Routines,
    Speedwork,
    Lessons,
    PlanBuilding,     // generating the first plan
    PlanReveal        // review and confirm the plan
}

object OnboardingFlow {

    /**
     * The `screen_context` sent with each message on this screen. null for screens that
     * have no conversation (connect, plan building, plan reveal).
     */
    fun screenContext(screen: OnboardingScreen): String? = when (screen) {
        OnboardingScreen.HistoryConfirm -> "history_confirm"
        OnboardingScreen.ManualBaseline -> "manual_baseline"
        OnboardingScreen.Goal -> "goal"
        OnboardingScreen.Routines -> "routines"
        OnboardingScreen.Speedwork -> "speedwork"
        OnboardingScreen.Lessons -> "lessons"
        OnboardingScreen.Integrations,
        OnboardingScreen.PlanBuilding,
        OnboardingScreen.PlanReveal -> null
    }

    /**
     * Intake fields that must be confirmed before leaving this screen. The screen is
     * satisfied once all of them appear in `confirmed_fields`.
     */
    fun requiredFields(screen: OnboardingScreen): List<String> = when (screen) {
        OnboardingScreen.HistoryConfirm,
        OnboardingScreen.ManualBaseline ->
            listOf("current_weekly_volume_km", "training_phase", "active_injury")
        OnboardingScreen.Goal -> listOf("goal_type")
        OnboardingScreen.Routines -> listOf("available_days_per_week")
        OnboardingScreen.Speedwork -> listOf("has_done_speedwork")
        OnboardingScreen.Lessons -> listOf("what_hasnt_worked")
        OnboardingScreen.Integrations,
        OnboardingScreen.PlanBuilding,
        OnboardingScreen.PlanReveal -> emptyList()
    }

    /**
     * Chooses the baseline screen: confirm-from-Strava when connected history is usable,
     * otherwise manual entry. Recomputed on each call rather than stored.
     */
    fun baselineBranch(
        stravaConnected: Boolean,
        historySummary: StravaHistorySummary?
    ): OnboardingScreen {
        if (!stravaConnected || historySummary == null) return OnboardingScreen.ManualBaseline
        if (historySummary.inferred_training_phase == "insufficient_data") {
            return OnboardingScreen.ManualBaseline
        }
        return OnboardingScreen.HistoryConfirm
    }

    /**
     * The intake screens in order, starting from the chosen baseline screen. Connect, plan
     * building, and plan reveal sit outside this sequence.
     */
    fun intakeSequence(branch: OnboardingScreen): List<OnboardingScreen> =
        listOf(branch, OnboardingScreen.Goal, OnboardingScreen.Routines, OnboardingScreen.Speedwork, OnboardingScreen.Lessons)

    /**
     * The first intake screen whose required fields aren't all confirmed. Screens whose
     * fields were already answered are skipped. Returns null once every intake screen is
     * satisfied.
     */
    fun firstUnsatisfied(branch: OnboardingScreen, confirmed: List<String>): OnboardingScreen? {
        val confirmedSet = confirmed.toSet()
        for (screen in intakeSequence(branch)) {
            val satisfied = requiredFields(screen).all { confirmedSet.contains(it) }
            if (!satisfied) return screen
        }
        return null
    }

    /** Whether the screen's required fields are all present in the confirmed set. */
    fun isSatisfied(screen: OnboardingScreen, confirmed: List<String>): Boolean {
        val confirmedSet = confirmed.toSet()
        return requiredFields(screen).all { confirmedSet.contains(it) }
    }

    /** The navigation route for a guided screen. */
    fun route(screen: OnboardingScreen): String = when (screen) {
        OnboardingScreen.Integrations -> Route.StravaConnect.path
        OnboardingScreen.HistoryConfirm -> Route.HistoryConfirm.path
        OnboardingScreen.ManualBaseline -> Route.ManualBaseline.path
        OnboardingScreen.Goal -> Route.Goal.path
        OnboardingScreen.Routines -> Route.Routines.path
        OnboardingScreen.Speedwork -> Route.Speedwork.path
        OnboardingScreen.Lessons -> Route.Lessons.path
        OnboardingScreen.PlanBuilding -> Route.PlanBuilding.path
        OnboardingScreen.PlanReveal -> Route.PlanReveal.path
    }
}
