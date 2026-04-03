package com.stridewell.app.api

import com.stridewell.app.model.ReflectionResponse
import com.stridewell.app.model.ReflectionSubmission
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface ReflectionApi {

    @POST("reflection")
    suspend fun submit(
        @Body submission: ReflectionSubmission
    ): Response<ReflectionResponse>
}
