package com.stridewell.app.data

import com.stridewell.app.model.StormCondition
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.HttpUrl.Companion.toHttpUrl

@Singleton
class WeatherRepository @Inject constructor(
    private val httpClient: OkHttpClient
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun fetchCondition(coordinate: GeoCoordinate): StormCondition? =
        withContext(Dispatchers.IO) {
            val url = BASE_URL.toHttpUrl().newBuilder()
                .addQueryParameter("latitude", coordinate.latitude.toString())
                .addQueryParameter("longitude", coordinate.longitude.toString())
                .addQueryParameter("current", "weather_code")
                .addQueryParameter("timezone", "auto")
                .build()

            val request = Request.Builder()
                .url(url)
                .get()
                .build()

            runCatching {
                httpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@use null
                    val payload = response.body?.string().orEmpty()
                    if (payload.isBlank()) return@use null
                    val decoded = json.decodeFromString(OpenMeteoResponse.serializer(), payload)
                    decoded.current?.weatherCode?.toStormCondition()
                }
            }.getOrNull()
        }

    private fun Int.toStormCondition(): StormCondition {
        return when (this) {
            in setOf(51, 53, 55, 56, 57, 61, 63, 65, 66, 67, 80, 81, 82, 95, 96, 99) ->
                StormCondition.RAIN
            in setOf(71, 73, 75, 77, 85, 86) ->
                StormCondition.SNOW
            else -> StormCondition.CLEAR
        }
    }

    @Serializable
    private data class OpenMeteoResponse(
        val current: CurrentPayload? = null
    )

    @Serializable
    private data class CurrentPayload(
        @SerialName("weather_code") val weatherCode: Int? = null
    )

    companion object {
        private const val BASE_URL = "https://api.open-meteo.com/v1/forecast"
    }
}
