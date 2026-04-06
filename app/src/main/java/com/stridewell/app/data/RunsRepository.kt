package com.stridewell.app.data

import com.stridewell.app.api.ApiResult
import com.stridewell.app.api.RunsApi
import com.stridewell.app.model.RecentRunsResponse
import com.stridewell.app.util.DateUtils
import java.io.IOException
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton
import retrofit2.Response

@Singleton
class RunsRepository @Inject constructor(
    private val runsApi: RunsApi
) {

    suspend fun recent(
        limit: Int = 20,
        offset: Int = 0,
        search: String? = null,
        date: String? = null,
        dateFrom: String? = null,
        dateTo: String? = null
    ): ApiResult<RecentRunsResponse> =
        safeCall { runsApi.recent(limit, offset, search, date, dateFrom, dateTo) }

    suspend fun runsForWeek(monday: Date, limit: Int = 20): ApiResult<RecentRunsResponse> {
        val start = DateUtils.format(monday)
        val end = DateUtils.format(DateUtils.nextMonday(monday))
        return recent(limit = limit, dateFrom = start, dateTo = end)
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
