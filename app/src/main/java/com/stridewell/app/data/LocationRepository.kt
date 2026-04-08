package com.stridewell.app.data

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine

data class GeoCoordinate(
    val latitude: Double,
    val longitude: Double
)

@Singleton
class LocationRepository @Inject constructor(
    @ApplicationContext context: Context
) {
    private val fused = LocationServices.getFusedLocationProviderClient(context)
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private val _coordinate = MutableStateFlow(loadCachedCoordinate())
    val coordinate: StateFlow<GeoCoordinate?> = _coordinate.asStateFlow()

    suspend fun requestLocation(hasPermission: Boolean): GeoCoordinate? {
        if (!hasPermission) return _coordinate.value
        val location = requestLastLocation() ?: requestCurrentLocation()
        if (location != null) {
            val next = GeoCoordinate(
                latitude = location.latitude,
                longitude = location.longitude
            )
            _coordinate.value = next
            prefs.edit()
                .putFloat(KEY_LAT, next.latitude.toFloat())
                .putFloat(KEY_LNG, next.longitude.toFloat())
                .apply()
        }
        return _coordinate.value
    }

    private fun loadCachedCoordinate(): GeoCoordinate? {
        if (!prefs.contains(KEY_LAT) || !prefs.contains(KEY_LNG)) return null
        return GeoCoordinate(
            latitude = prefs.getFloat(KEY_LAT, 0f).toDouble(),
            longitude = prefs.getFloat(KEY_LNG, 0f).toDouble()
        )
    }

    @SuppressLint("MissingPermission")
    private suspend fun requestLastLocation(): Location? =
        suspendCancellableCoroutine { continuation ->
            fused.lastLocation
                .addOnSuccessListener { location -> continuation.resume(location) }
                .addOnFailureListener { continuation.resume(null) }
        }

    @SuppressLint("MissingPermission")
    private suspend fun requestCurrentLocation(): Location? =
        suspendCancellableCoroutine { continuation ->
            val tokenSource = CancellationTokenSource()
            continuation.invokeOnCancellation { tokenSource.cancel() }

            fused.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, tokenSource.token)
                .addOnSuccessListener { location -> continuation.resume(location) }
                .addOnFailureListener { continuation.resume(null) }
        }

    companion object {
        private const val PREFS = "location_prefs"
        private const val KEY_LAT = "last_lat"
        private const val KEY_LNG = "last_lng"
    }
}
