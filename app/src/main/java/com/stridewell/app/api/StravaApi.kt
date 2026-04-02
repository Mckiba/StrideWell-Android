package com.stridewell.app.api

import com.stridewell.app.model.StravaConnectRequest
import com.stridewell.app.model.StravaConnectResponse
import com.stridewell.app.model.StravaDisconnectResponse
import com.stridewell.app.model.StravaStatusResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface StravaApi {

    /** Exchange the OAuth authorization code for a connected Strava account. */
    @POST("oauth/strava/connect")
    suspend fun connect(@Body body: StravaConnectRequest): Response<StravaConnectResponse>

    /** Revoke the user's Strava connection. Used by SettingsScreen (M12). */
    @POST("oauth/strava/disconnect")
    suspend fun disconnect(): Response<StravaDisconnectResponse>

    /** Poll current Strava connection status. Used by SettingsScreen (M12). */
    @GET("auth/strava-status")
    suspend fun stravaStatus(): Response<StravaStatusResponse>
}
