package com.stridewell.app.data

import com.stridewell.BuildConfig
import com.stridewell.app.api.ApiResult
import com.stridewell.app.api.HomeCardsApi
import com.stridewell.app.model.HomeCard
import com.stridewell.app.util.UnitSystem
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import retrofit2.Response

@Singleton
class HomeCardsRepository @Inject constructor(
    private val homeCardsApi: HomeCardsApi
) {
    // DEBUG-only preset locations to exercise card rendering from areas with
    // active weather. To test: set `debugLocation` (e.g. in HomeViewModel init).
    enum class DebugLocation(val label: String, val coordinate: GeoCoordinate?) {
        OFF("Off", null),
        DUBAI("Dubai", GeoCoordinate(25.2026, 55.2708)),
        DELHI("Delhi", GeoCoordinate(28.6139, 77.2090)),
        BANGKOK("Bangkok", GeoCoordinate(13.7563, 100.5018)),
        SINGAPORE("Singapore", GeoCoordinate(1.3521, 103.8198))
    }

    var debugLocation: DebugLocation = DebugLocation.OFF

    /** Fetch sorted cards for a coordinate. Empty on missing location or any failure. */
    suspend fun fetch(coordinate: GeoCoordinate?, units: UnitSystem): List<HomeCard> {
        val override = if (BuildConfig.DEBUG) debugLocation.coordinate else null
        val coord = override ?: coordinate ?: return emptyList()

        val result = safeCall {
            homeCardsApi.cards(coord.latitude, coord.longitude, units.name.lowercase())
        }
        return when (result) {
            is ApiResult.Success -> result.data.cards
            is ApiResult.Error -> emptyList() // silent — decorative feature
        }
    }

    private suspend fun <T> safeCall(call: suspend () -> Response<T>): ApiResult<T> =
        try {
            val response = call()
            val body = response.body()
            if (response.isSuccessful && body != null) {
                ApiResult.Success(body)
            } else {
                ApiResult.Error(response.code(), response.message())
            }
        } catch (e: IOException) {
            ApiResult.Error(0, "No internet connection.")
        } catch (e: Exception) {
            ApiResult.Error(-1, e.message ?: "Unknown error")
        }
}
