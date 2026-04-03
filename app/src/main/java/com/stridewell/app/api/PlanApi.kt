package com.stridewell.app.api

import com.stridewell.app.model.PlanDay
import com.stridewell.app.model.GoalSummary
import com.stridewell.app.model.LatestDecisionResponse
import com.stridewell.app.model.PlanVersionResponse
import com.stridewell.app.model.PlanWeekResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface PlanApi {

    @GET("plan/today")
    suspend fun today(): Response<PlanDay>

    @GET("plan/week")
    suspend fun week(
        @Query("start") start: String
    ): Response<PlanWeekResponse>

    @GET("plan/goal-summary")
    suspend fun goalSummary(): Response<GoalSummary>

    @GET("plan/latest-decision")
    suspend fun latestDecision(): Response<LatestDecisionResponse>

    @GET("plan/version/{planVersionId}")
    suspend fun version(
        @Path("planVersionId") planVersionId: String,
        @Query("weeks") weeks: Int? = null
    ): Response<PlanVersionResponse>
}
