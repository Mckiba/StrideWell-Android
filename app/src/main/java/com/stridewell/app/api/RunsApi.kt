package com.stridewell.app.api

import com.stridewell.app.model.RecentRunsResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface RunsApi {

    @GET("runs/recent")
    suspend fun recent(
        @Query("limit") limit: Int = 3
    ): Response<RecentRunsResponse>
}
