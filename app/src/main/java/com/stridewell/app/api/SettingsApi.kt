package com.stridewell.app.api

import com.stridewell.app.model.ProactivePreferencesRequest
import com.stridewell.app.model.ProactivePreferencesStoredResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.PUT

interface SettingsApi {

    @PUT("profile/proactive-preferences")
    suspend fun putProactivePreferences(
        @Body body: ProactivePreferencesRequest,
    ): Response<ProactivePreferencesStoredResponse>
}
