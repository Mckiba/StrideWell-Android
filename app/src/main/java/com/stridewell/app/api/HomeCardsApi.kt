package com.stridewell.app.api

import com.stridewell.app.model.HomeCardsResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface HomeCardsApi {
    @GET("home/cards")
    suspend fun cards(
        @Query("lat") lat: Double,
        @Query("lng") lng: Double,
        @Query("units") units: String
    ): Response<HomeCardsResponse>
}
