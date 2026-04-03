package com.stridewell.app.api

import com.stridewell.app.model.ConfirmPlanRequest
import com.stridewell.app.model.ConfirmPlanResponse
import com.stridewell.app.model.OnboardingMessageRequest
import com.stridewell.app.model.OnboardingMessageResponse
import com.stridewell.app.model.OnboardingStartResponse
import com.stridewell.app.model.OnboardingState
import com.stridewell.app.model.OnboardingHistoryResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface OnboardingApi {

    @POST("onboarding/start")
    suspend fun start(): Response<OnboardingStartResponse>

    @GET("onboarding/status")
    suspend fun status(): Response<OnboardingState>

    @GET("onboarding/history")
    suspend fun history(
        @Query("conversation_id") conversationId: String,
        @Query("limit") limit: Int = 50,
        @Query("before") before: String? = null
    ): Response<OnboardingHistoryResponse>

    @POST("onboarding/message")
    suspend fun message(@Body body: OnboardingMessageRequest): Response<OnboardingMessageResponse>

    @POST("onboarding/skip")
    suspend fun skip(): Response<Unit>

    @POST("onboarding/confirm-plan")
    suspend fun confirmPlan(@Body body: ConfirmPlanRequest): Response<ConfirmPlanResponse>
}
