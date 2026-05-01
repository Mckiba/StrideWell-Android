package com.stridewell.app.navigation

import com.stridewell.app.model.OnboardingStatus

/** Type-safe route definitions for the entire app. */
sealed class Route(val path: String) {
    // Auth
    object Welcome  : Route("welcome")
    object SignIn   : Route("sign_in")
    object SignUp   : Route("sign_up")

    // Onboarding
    object StravaConnect    : Route("onboarding/strava_connect")
    object IntakeInterview  : Route("onboarding/intake_interview")
    object PlanBuilding     : Route("onboarding/plan_building")
    object PlanReveal       : Route("onboarding/plan_reveal")

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
            OnboardingStatus.interview -> IntakeInterview.path
            OnboardingStatus.analyzing,
            OnboardingStatus.pending,
            null -> StravaConnect.path
        }

        fun planChange(encodedRecord: String? = null) = PlanChange.destination(encodedRecord)
        fun runDetail(runId: String) = RunDetail.destination(runId)
    }
}
