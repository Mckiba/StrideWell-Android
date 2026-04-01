package com.stridewell.app.navigation

/** Type-safe route definitions for the entire app. */
sealed class Route(val path: String) {
    // Auth
    object Welcome     : Route("welcome")
    object SignIn      : Route("sign_in")

    // Onboarding
    object StravaConnect    : Route("onboarding/strava_connect")
    object IntakeInterview  : Route("onboarding/intake_interview")
    object PlanBuilding     : Route("onboarding/plan_building")
    object PlanReveal       : Route("onboarding/plan_reveal")

    // Main
    object Main : Route("main")

    // Shared modal screens
    object Reflection : Route("reflection")
    data class PlanChange(val planVersionId: String) : Route("plan_change/{planVersionId}") {
        fun destination() = "plan_change/$planVersionId"
    }
    data class ActivityDetail(val runId: String) : Route("activity_detail/{runId}") {
        fun destination() = "activity_detail/$runId"
    }

    companion object {
        /** Returns a Route.PlanChange route string with the ID filled in. */
        fun planChange(planVersionId: String) = "plan_change/$planVersionId"
        fun activityDetail(runId: String) = "activity_detail/$runId"
    }
}
