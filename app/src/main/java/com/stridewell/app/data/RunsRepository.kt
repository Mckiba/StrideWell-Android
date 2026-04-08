package com.stridewell.app.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.stridewell.app.api.ApiResult
import com.stridewell.app.api.RunsApi
import com.stridewell.app.model.HeatmapResponse
import com.stridewell.app.model.RecentRunsResponse
import com.stridewell.app.util.DateUtils
import java.io.IOException
import java.util.Date
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import retrofit2.Response

@Singleton
class RunsRepository @Inject constructor(
    private val runsApi: RunsApi,
    @Named("runs") private val dataStore: DataStore<Preferences>
) {

    @Serializable
    private data class WeekRunsEnvelope(val weeks: Map<String, RecentRunsResponse>)

    companion object {
        private val KEY_CACHED_RECENT = stringPreferencesKey("runs_cached_recent")
        private val KEY_CACHED_WEEKS  = stringPreferencesKey("runs_cached_weeks")
        private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }
    }

    suspend fun recent(
        limit: Int = 20,
        offset: Int = 0,
        search: String? = null,
        date: String? = null,
        dateFrom: String? = null,
        dateTo: String? = null
    ): ApiResult<RecentRunsResponse> {
        val result = safeCall { runsApi.recent(limit, offset, search, date, dateFrom, dateTo) }
        val isHomeCall = offset == 0 && search == null && date == null && dateFrom == null && dateTo == null
        return when {
            result is ApiResult.Success && isHomeCall -> {
                dataStore.edit { it[KEY_CACHED_RECENT] = json.encodeToString(result.data) }
                result
            }
            result is ApiResult.Error && result.status == 0 && isHomeCall -> {
                val stored = dataStore.data.first()[KEY_CACHED_RECENT]
                val cached = stored?.let { runCatching { json.decodeFromString<RecentRunsResponse>(it) }.getOrNull() }
                cached?.let { ApiResult.Success(it) } ?: result
            }
            else -> result
        }
    }

    suspend fun runsForWeek(monday: Date, limit: Int = 20): ApiResult<RecentRunsResponse> {
        val start = DateUtils.format(monday)
        val end = DateUtils.format(DateUtils.nextMonday(monday))
        val result = safeCall { runsApi.recent(limit, 0, null, null, start, end) }
        return when {
            result is ApiResult.Success -> {
                saveWeekRunsToCache(start, result.data)
                result
            }
            result is ApiResult.Error && result.status == 0 -> {
                loadWeekRunsFromCache(start)?.let { ApiResult.Success(it) } ?: result
            }
            else -> result
        }
    }

    suspend fun heatmap(): ApiResult<HeatmapResponse> =
        safeCall { runsApi.heatmap() }

    suspend fun reset() {
        dataStore.edit { prefs ->
            prefs.remove(KEY_CACHED_RECENT)
            prefs.remove(KEY_CACHED_WEEKS)
        }
    }

    private suspend fun saveWeekRunsToCache(start: String, data: RecentRunsResponse) {
        val current = loadWeekRunsMap().toMutableMap()
        current[start] = data
        dataStore.edit { it[KEY_CACHED_WEEKS] = json.encodeToString(WeekRunsEnvelope(current)) }
    }

    private suspend fun loadWeekRunsFromCache(start: String): RecentRunsResponse? =
        loadWeekRunsMap()[start]

    private suspend fun loadWeekRunsMap(): Map<String, RecentRunsResponse> {
        val stored = dataStore.data.first()[KEY_CACHED_WEEKS] ?: return emptyMap()
        return runCatching { json.decodeFromString<WeekRunsEnvelope>(stored).weeks }.getOrElse { emptyMap() }
    }

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

    private fun String.extractMessage(): String {
        val match = Regex(""""(?:message|err|error)"\s*:\s*"([^"]+)"""").find(this)
        return match?.groupValues?.get(1) ?: this
    }
}
