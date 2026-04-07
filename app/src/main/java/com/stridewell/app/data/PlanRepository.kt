package com.stridewell.app.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.stridewell.app.api.ApiResult
import com.stridewell.app.api.PlanApi
import com.stridewell.app.model.GoalSummary
import com.stridewell.app.model.LatestDecisionResponse
import com.stridewell.app.model.PlanDay
import com.stridewell.app.model.PlanVersionResponse
import com.stridewell.app.model.PlanWeekResponse
import java.io.IOException
import javax.inject.Named
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import retrofit2.Response

@Singleton
class PlanRepository @Inject constructor(
    private val planApi: PlanApi,
    @Named("plan") private val dataStore: DataStore<Preferences>
) {

    @Serializable
    private data class WeekCacheEnvelope(val weeks: Map<String, PlanWeekResponse>)

    companion object {
        private val KEY_LAST_SEEN_PLAN_VERSION_ID =
            stringPreferencesKey("plan_last_seen_plan_version_id")
        private val KEY_CACHED_TODAY_JSON =
            stringPreferencesKey("plan_cached_today")
        private val KEY_CACHED_WEEKS_JSON =
            stringPreferencesKey("plan_cached_weeks")
        private val KEY_CACHED_GOAL_JSON =
            stringPreferencesKey("plan_cached_goal")
        private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _isOffline = MutableStateFlow(false)
    val isOffline: StateFlow<Boolean> = _isOffline.asStateFlow()

    private val _currentPlanVersionId = MutableStateFlow<String?>(null)
    val currentPlanVersionId: StateFlow<String?> = _currentPlanVersionId.asStateFlow()

    private val _lastSeenPlanVersionId = MutableStateFlow<String?>(null)
    val lastSeenPlanVersionId: StateFlow<String?> = _lastSeenPlanVersionId.asStateFlow()

    private val _todayPlanDay = MutableStateFlow<PlanDay?>(null)
    val todayPlanDay: StateFlow<PlanDay?> = _todayPlanDay.asStateFlow()

    private val _currentWeek = MutableStateFlow<PlanWeekResponse?>(null)
    val currentWeek: StateFlow<PlanWeekResponse?> = _currentWeek.asStateFlow()

    private val _goalSummary = MutableStateFlow<GoalSummary?>(null)
    val goalSummary: StateFlow<GoalSummary?> = _goalSummary.asStateFlow()

    private val _weekCache = MutableStateFlow<Map<String, PlanWeekResponse>>(emptyMap())
    val weekCache: StateFlow<Map<String, PlanWeekResponse>> = _weekCache.asStateFlow()

    val planUpdated: StateFlow<Boolean> = combine(
        _currentPlanVersionId,
        _lastSeenPlanVersionId
    ) { current, seen ->
        current != null && seen != null && current != seen
    }.stateIn(
        scope = scope,
        started = kotlinx.coroutines.flow.SharingStarted.Eagerly,
        initialValue = false
    )

    init {
        scope.launch {
            _lastSeenPlanVersionId.value = dataStore.data.first()[KEY_LAST_SEEN_PLAN_VERSION_ID]
        }
    }

    suspend fun today(): ApiResult<PlanDay> {
        val result = safeCall { planApi.today() }
        return when {
            result is ApiResult.Success -> {
                _isOffline.value = false
                saveTodayToCache(result.data)
                result
            }
            result is ApiResult.Error && result.status == 0 -> {
                loadTodayFromCache()?.let { cached ->
                    _isOffline.value = true
                    ApiResult.Success(cached)
                } ?: run {
                    _isOffline.value = false
                    result
                }
            }
            else -> {
                _isOffline.value = false
                result
            }
        }
    }

    suspend fun week(start: String): ApiResult<PlanWeekResponse> {
        val result = safeCall { planApi.week(start) }
        return when {
            result is ApiResult.Success -> {
                _isOffline.value = false
                saveWeekToCache(result.data)
                result
            }
            result is ApiResult.Error && result.status == 0 -> {
                loadWeekFromCache(start)?.let { cached ->
                    _isOffline.value = true
                    ApiResult.Success(cached)
                } ?: run {
                    _isOffline.value = false
                    result
                }
            }
            else -> {
                _isOffline.value = false
                result
            }
        }
    }

    suspend fun goalSummary(): ApiResult<GoalSummary> {
        val result = safeCall { planApi.goalSummary() }
        return when {
            result is ApiResult.Success -> {
                dataStore.edit { it[KEY_CACHED_GOAL_JSON] = json.encodeToString(result.data) }
                result
            }
            result is ApiResult.Error && result.status == 0 -> {
                val stored = dataStore.data.first()[KEY_CACHED_GOAL_JSON]
                val cached = stored?.let { runCatching { json.decodeFromString<GoalSummary>(it) }.getOrNull() }
                cached?.let { ApiResult.Success(it) } ?: result
            }
            else -> result
        }
    }

    suspend fun latestDecision(): ApiResult<LatestDecisionResponse> =
        safeCall { planApi.latestDecision() }

    suspend fun version(planVersionId: String, weeks: Int? = null): ApiResult<PlanVersionResponse> =
        safeCall { planApi.version(planVersionId, weeks) }

    suspend fun setTodayPlanDay(day: PlanDay) {
        _todayPlanDay.value = day
    }

    suspend fun setWeekData(week: PlanWeekResponse) {
        _currentWeek.value = week
        _currentPlanVersionId.value = week.plan_version_id
        _weekCache.value = _weekCache.value + (week.start_date to week)

        if (_lastSeenPlanVersionId.value == null) {
            markPlanChangeSeen()
        }
    }

    suspend fun setGoalSummary(summary: GoalSummary?) {
        _goalSummary.value = summary
    }

    suspend fun cacheWeek(week: PlanWeekResponse) {
        _weekCache.value = _weekCache.value + (week.start_date to week)
        _currentPlanVersionId.value = week.plan_version_id
        if (_lastSeenPlanVersionId.value == null) {
            markPlanChangeSeen()
        }
    }

    fun cachedWeek(startDate: String): PlanWeekResponse? = _weekCache.value[startDate]

    suspend fun markPlanChangeSeen(planVersionId: String? = _currentPlanVersionId.value) {
        _lastSeenPlanVersionId.value = planVersionId
        dataStore.edit { prefs ->
            if (planVersionId == null) {
                prefs.remove(KEY_LAST_SEEN_PLAN_VERSION_ID)
            } else {
                prefs[KEY_LAST_SEEN_PLAN_VERSION_ID] = planVersionId
            }
        }
    }

    fun setCurrentPlanVersionId(planVersionId: String?) {
        _currentPlanVersionId.value = planVersionId
    }

    suspend fun reset() = clearInMemoryState(clearSeenVersion = true)

    suspend fun clearInMemoryState(clearSeenVersion: Boolean = true) {
        _currentPlanVersionId.value = null
        _todayPlanDay.value = null
        _currentWeek.value = null
        _goalSummary.value = null
        _weekCache.value = emptyMap()
        _isOffline.value = false

        if (clearSeenVersion) {
            _lastSeenPlanVersionId.value = null
            dataStore.edit { prefs ->
                prefs.remove(KEY_LAST_SEEN_PLAN_VERSION_ID)
                prefs.remove(KEY_CACHED_TODAY_JSON)
                prefs.remove(KEY_CACHED_WEEKS_JSON)
                prefs.remove(KEY_CACHED_GOAL_JSON)
            }
        }
    }

    private suspend fun saveTodayToCache(day: PlanDay) {
        dataStore.edit { it[KEY_CACHED_TODAY_JSON] = json.encodeToString(day) }
    }

    private suspend fun saveWeekToCache(week: PlanWeekResponse) {
        val current = loadCachedWeeksMap().toMutableMap()
        current[week.start_date] = week
        dataStore.edit { it[KEY_CACHED_WEEKS_JSON] = json.encodeToString(WeekCacheEnvelope(current)) }
    }

    private suspend fun loadTodayFromCache(): PlanDay? {
        val stored = dataStore.data.first()[KEY_CACHED_TODAY_JSON] ?: return null
        return runCatching { json.decodeFromString<PlanDay>(stored) }.getOrNull()
    }

    private suspend fun loadWeekFromCache(start: String): PlanWeekResponse? =
        loadCachedWeeksMap()[start]

    private suspend fun loadCachedWeeksMap(): Map<String, PlanWeekResponse> {
        val stored = dataStore.data.first()[KEY_CACHED_WEEKS_JSON] ?: return emptyMap()
        return runCatching { json.decodeFromString<WeekCacheEnvelope>(stored).weeks }.getOrElse { emptyMap() }
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
