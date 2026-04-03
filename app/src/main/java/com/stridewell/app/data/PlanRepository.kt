package com.stridewell.app.data

import com.stridewell.app.api.ApiResult
import com.stridewell.app.api.PlanApi
import com.stridewell.app.model.PlanDay
import com.stridewell.app.model.PlanVersionResponse
import com.stridewell.app.model.PlanWeekResponse
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import retrofit2.Response

@Singleton
class PlanRepository @Inject constructor(
    private val planApi: PlanApi
) {

    suspend fun today(): ApiResult<PlanDay> =
        safeCall { planApi.today() }

    suspend fun week(start: String): ApiResult<PlanWeekResponse> =
        safeCall { planApi.week(start) }

    suspend fun version(planVersionId: String, weeks: Int? = null): ApiResult<PlanVersionResponse> =
        safeCall { planApi.version(planVersionId, weeks) }

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
