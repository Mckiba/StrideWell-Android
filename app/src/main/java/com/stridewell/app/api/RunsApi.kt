package com.stridewell.app.api

import com.stridewell.app.model.RecentRunsResponse
import com.stridewell.app.model.HeatmapResponse
import com.stridewell.app.model.RunDetailResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface RunsApi {

    @GET("runs/recent")
    suspend fun recent(
        @Query("limit") limit: Int = 20,
        @Query("offset") offset: Int = 0,
        @Query("search") search: String? = null,
        @Query("date") date: String? = null,
        @Query("date_from") dateFrom: String? = null,
        @Query("date_to") dateTo: String? = null
    ): Response<RecentRunsResponse>

    @GET("runs/heatmap")
    suspend fun heatmap(): Response<HeatmapResponse>

    /**
     * Full detail for a single run, including decoded route metadata, splits
     * (lap-sourced or computed-fallback), and down-sampled streams.
     *
     * Backend: `GET /runs/:id` — see Architecture-Docs/v2/Activity Detail Plan.md.
     */
    @GET("runs/{id}")
    suspend fun runDetail(@Path("id") id: String): Response<RunDetailResponse>
}
