package com.stridewell.app.api

import com.stridewell.app.model.FitnessProfile
import com.stridewell.app.model.RunAnalysisResponse
import com.stridewell.app.model.WeeklySummary
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * V2 Phase 2 endpoints:
 *   • GET /runs/:runId/analysis — stored per-run analysis
 *   • GET /profile/fitness      — user's fitness profile (threshold pace, zones)
 *   • GET /analysis/weekly      — per-week summary
 *
 * 404 on /runs/:id/analysis can mean analysis not ready OR run not found; the
 * caller decides UX (loading vs empty). 404 on /profile/fitness means we don't
 * yet have enough data to estimate.
 */
interface AnalysisApi {

    @GET("runs/{runId}/analysis")
    suspend fun runAnalysis(@Path("runId") runId: String): Response<RunAnalysisResponse>

    @GET("profile/fitness")
    suspend fun fitnessProfile(): Response<FitnessProfile>

    @GET("analysis/weekly")
    suspend fun weeklySummary(@Query("start") weekStart: String): Response<WeeklySummary>
}
