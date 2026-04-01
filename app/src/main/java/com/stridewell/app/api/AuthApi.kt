package com.stridewell.app.api

import com.stridewell.app.model.ForgotPasswordRequest
import com.stridewell.app.model.ForgotPasswordResponse
import com.stridewell.app.model.LoginRequest
import com.stridewell.app.model.LoginResponse
import com.stridewell.app.model.MeResponse
import com.stridewell.app.model.RegisterRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST

interface AuthApi {

    @POST("auth/register")
    suspend fun register(@Body body: RegisterRequest): Response<LoginResponse>

    @POST("auth/login")
    suspend fun login(@Body body: LoginRequest): Response<LoginResponse>

    @GET("auth/me")
    suspend fun me(): Response<MeResponse>

    @POST("auth/forgot-password")
    suspend fun forgotPassword(@Body body: ForgotPasswordRequest): Response<ForgotPasswordResponse>

    @DELETE("auth/account")
    suspend fun deleteAccount(): Response<Unit>
}
