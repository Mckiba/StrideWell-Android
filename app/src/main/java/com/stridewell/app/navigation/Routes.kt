package com.stridewell.app.navigation

import com.stridewell.app.model.OnboardingStatus

/** Type-safe route definitions for the entire app. */
sealed class Route(val path: String) {
    // Auth
    object Welcome  : Route("welcome")
    object SignIn   : Route("sign_in")
    object SignUp   : Route("sign_up")

    // Onboarding
    object UnitPreference   : Route("onboarding/unit_preference")   // S0 unit choice (fresh entry)
    object StravaConnect    : Route("onboarding/strava_connect")   // S1 IntegrationsScreen
    object IntakeInterview  : Route("onboarding/intake_interview") // V1 legacy; kept until fully removed
    object PlanBuilding     : Route("onboarding/plan_building")    // S7
    object PlanReveal       : Route("onboarding/plan_reveal")      // S8

    // Onboarding V2 — guided field-owning screens
    object HistoryConfirm   : Route("onboarding/history_confirm")  // S2a (Strava branch)
    object ManualBaseline   : Route("onboarding/manual_baseline")  // S2b (no-Strava branch)
    object Goal             : Route("onboarding/goal")             // S3
    object Routines         : Route("onboarding/routines")         // S4
    object Speedwork        : Route("onboarding/speedwork")        // S5
    object Lessons          : Route("onboarding/lessons")          // S6

    // Main
    object Main : Route("main")

    // Shared modal screens
    object Reflection : Route("reflection")
    object PlanChange : Route("plan_change?record={record}") {
        const val basePath = "plan_change"
        const val argRecord = "record"
        fun destination(encodedRecord: String? = null): String =
            if (encodedRecord.isNullOrBlank()) basePath else "$basePath?record=$encodedRecord"
    }
    /**
     * Run Detail screen — interactive Mapbox map + three-detent sheet hosting
     * full stats, splits, V2 analysis, and elevation/HR/cadence charts. Replaces
     * the legacy `ActivityDetail` (basic info) and `RunAnalysis` (analysis only)
     * screens.
     */
    object RunDetail : Route("run_detail/{runId}") {
        const val argRunId = "runId"
        fun destination(runId: String): String = "run_detail/$runId"
    }

    // V2 Phase 2 screens
    object FitnessProfile : Route("fitness_profile")
    object WeeklySummary  : Route("weekly_summary")

    companion object {
        fun forOnboardingStatus(status: OnboardingStatus?): String = when (status) {
            OnboardingStatus.complete,
            OnboardingStatus.skipped -> Main.path
            // Fresh onboarding (no session yet) opens on the unit-preference step, which
            // then pushes StravaConnect.
            null -> UnitPreference.path
            // V2 guided flow: (StravaConnect) is the resume entry point for an in-progress
            // interview. It performs the one-shot resume-advance to the first unsatisfied
            // screen (or rests on S1 while the data-connection decision is pending). The unit
            // step is skipped on resume — it was chosen when the session started.
            OnboardingStatus.interview,
            OnboardingStatus.analyzing,
            OnboardingStatus.pending -> StravaConnect.path
        }

        fun planChange(encodedRecord: String? = null) = PlanChange.destination(encodedRecord)
        fun runDetail(runId: String) = RunDetail.destination(runId)
    }
}
