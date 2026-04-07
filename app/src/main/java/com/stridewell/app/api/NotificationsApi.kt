package com.stridewell.app.api

import com.stridewell.app.model.RegisterTokenRequest
import com.stridewell.app.model.RegisterTokenResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface NotificationsApi {

    @POST("notifications/register")
    suspend fun registerToken(@Body body: RegisterTokenRequest): Response<RegisterTokenResponse>
}
