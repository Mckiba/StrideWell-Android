package com.stridewell.app.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.stridewell.app.api.ApiResult
import com.stridewell.app.api.OnboardingApi
import com.stridewell.app.api.StravaApi
import com.stridewell.app.model.ConfirmPlanRequest
import com.stridewell.app.model.ConfirmPlanResponse
import com.stridewell.app.model.OnboardingHistoryResponse
import com.stridewell.app.model.OnboardingMessageRequest
import com.stridewell.app.model.OnboardingMessageResponse
import com.stridewell.app.model.OnboardingStartResponse
import com.stridewell.app.model.OnboardingState
import com.stridewell.app.model.StravaConnectRequest
import com.stridewell.app.model.StravaConnectResponse
import kotlinx.coroutines.flow.first
import retrofit2.Response
import java.io.IOException
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class OnboardingRepository @Inject constructor(
    private val onboardingApi: OnboardingApi,
    private val stravaApi: StravaApi,
    @Named("onboarding") private val dataStore: DataStore<Preferences>
) {

    // ── Keys ──────────────────────────────────────────────────────────────────

    companion object {
        private val KEY_COMPLETE = booleanPreferencesKey("onboarding_complete")
        private val KEY_CONVERSATION_ID = stringPreferencesKey("onboarding_conversation_id")
    }

    // ── Onboarding ────────────────────────────────────────────────────────────

    suspend fun start(): ApiResult<OnboardingStartResponse> =
        safeCall { onboardingApi.start() }

    suspend fun status(): ApiResult<OnboardingState> =
        safeCall { onboardingApi.status() }

    suspend fun message(body: OnboardingMessageRequest): ApiResult<OnboardingMessageResponse> =
        safeCall { onboardingApi.message(body) }

    suspend fun history(
        conversationId: String,
        limit: Int = 50,
        before: String? = null
    ): ApiResult<OnboardingHistoryResponse> =
        safeCall { onboardingApi.history(conversationId, limit, before) }

    suspend fun skip(): ApiResult<Unit> =
        safeCallUnit { onboardingApi.skip() }

    suspend fun confirmPlan(planVersionId: String): ApiResult<ConfirmPlanResponse> =
        safeCall { onboardingApi.confirmPlan(ConfirmPlanRequest(planVersionId)) }

    // ── Strava ────────────────────────────────────────────────────────────────

    suspend fun stravaConnect(code: String): ApiResult<StravaConnectResponse> =
        safeCall { stravaApi.connect(StravaConnectRequest(code)) }

    // ── Persistence ───────────────────────────────────────────────────────────

    /**
     * Returns true if onboarding was previously completed.
     * Used by LaunchViewModel for offline cold starts — avoids a network
     * call when the device has no connectivity.
     */
    suspend fun isOnboardingComplete(): Boolean =
        dataStore.data.first()[KEY_COMPLETE] ?: false

    suspend fun getConversationId(): String? =
        dataStore.data.first()[KEY_CONVERSATION_ID]

    suspend fun saveConversationId(conversationId: String) {
        dataStore.edit { prefs -> prefs[KEY_CONVERSATION_ID] = conversationId }
    }

    suspend fun clearConversationId() {
        dataStore.edit { prefs -> prefs.remove(KEY_CONVERSATION_ID) }
    }

    suspend fun markComplete() {
        dataStore.edit { prefs ->
            prefs[KEY_COMPLETE] = true
            prefs.remove(KEY_CONVERSATION_ID)
        }
    }

    suspend fun reset() {
        dataStore.edit { prefs ->
            prefs.remove(KEY_COMPLETE)
            prefs.remove(KEY_CONVERSATION_ID)
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private suspend fun <T> safeCall(call: suspend () -> Response<T>): ApiResult<T> {
        return try {
            val response = call()
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    ApiResult.Success(body)
                } else {
                    ApiResult.Error(response.code(), "Empty response body")
                }
            } else {
                val errorMessage = response.errorBody()?.string()?.extractMessage()
                    ?: response.message()
                ApiResult.Error(response.code(), errorMessage)
            }
        } catch (e: IOException) {
            ApiResult.Error(0, "No internet connection. Please check your network.")
        } catch (e: Exception) {
            ApiResult.Error(-1, e.message ?: "Unknown error")
        }
    }

    private suspend fun safeCallUnit(call: suspend () -> Response<Unit>): ApiResult<Unit> {
        return try {
            val response = call()
            if (response.isSuccessful) ApiResult.Success(Unit)
            else {
                val msg = response.errorBody()?.string()?.extractMessage() ?: response.message()
                ApiResult.Error(response.code(), msg)
            }
        } catch (e: IOException) {
            ApiResult.Error(0, "No internet connection. Please check your network.")
        } catch (e: Exception) {
            ApiResult.Error(-1, e.message ?: "Unknown error")
        }
    }

    private fun String.extractMessage(): String {
        val match = Regex(""""(?:message|err|error)"\s*:\s*"([^"]+)"""").find(this)
        return match?.groupValues?.get(1) ?: this
    }
}
