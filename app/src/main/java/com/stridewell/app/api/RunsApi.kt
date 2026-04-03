package com.stridewell.app.api

import com.stridewell.app.model.RecentRunsResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface RunsApi {

    @GET("runs/recent")
    suspend fun recent(
        @Query("limit") limit: Int = 3,
        @Query("date_from") dateFrom: String? = null,
        @Query("date_to") dateTo: String? = null
    ): Response<RecentRunsResponse>
}
