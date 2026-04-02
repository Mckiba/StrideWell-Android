package com.stridewell.app.api

import com.stridewell.app.model.ConfirmPlanRequest
import com.stridewell.app.model.ConfirmPlanResponse
import com.stridewell.app.model.OnboardingMessageRequest
import com.stridewell.app.model.OnboardingMessageResponse
import com.stridewell.app.model.OnboardingStartResponse
import com.stridewell.app.model.OnboardingState
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface OnboardingApi {

    @POST("onboarding/start")
    suspend fun start(): Response<OnboardingStartResponse>

    @GET("onboarding/status")
    suspend fun status(): Response<OnboardingState>

    @POST("onboarding/message")
    suspend fun message(@Body body: OnboardingMessageRequest): Response<OnboardingMessageResponse>

    @POST("onboarding/skip")
    suspend fun skip(): Response<Unit>

    @POST("onboarding/confirm-plan")
    suspend fun confirmPlan(@Body body: ConfirmPlanRequest): Response<ConfirmPlanResponse>
}
