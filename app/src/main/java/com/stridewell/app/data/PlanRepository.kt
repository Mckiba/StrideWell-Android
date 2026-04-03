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
import retrofit2.Response

@Singleton
class PlanRepository @Inject constructor(
    private val planApi: PlanApi,
    @Named("plan") private val dataStore: DataStore<Preferences>
) {

    companion object {
        private val KEY_LAST_SEEN_PLAN_VERSION_ID =
            stringPreferencesKey("plan_last_seen_plan_version_id")
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

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

    suspend fun today(): ApiResult<PlanDay> =
        safeCall { planApi.today() }

    suspend fun week(start: String): ApiResult<PlanWeekResponse> =
        safeCall { planApi.week(start) }

    suspend fun goalSummary(): ApiResult<GoalSummary> =
        safeCall { planApi.goalSummary() }

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
        _currentWeek.value = week
        if (_lastSeenPlanVersionId.value == null) {
            markPlanChangeSeen()
        }
    }

    fun cachedWeek(startDate: String): PlanWeekResponse? = _weekCache.value[startDate]

    suspend fun markPlanChangeSeen() {
        val current = _currentPlanVersionId.value
        _lastSeenPlanVersionId.value = current
        dataStore.edit { prefs ->
            if (current == null) {
                prefs.remove(KEY_LAST_SEEN_PLAN_VERSION_ID)
            } else {
                prefs[KEY_LAST_SEEN_PLAN_VERSION_ID] = current
            }
        }
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
