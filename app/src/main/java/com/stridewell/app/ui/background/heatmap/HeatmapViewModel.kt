package com.stridewell.app.ui.background.heatmap

import androidx.compose.ui.unit.IntSize
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stridewell.BuildConfig
import com.stridewell.app.api.ApiResult
import com.stridewell.app.data.LocationRepository
import com.stridewell.app.data.RunsRepository
import com.stridewell.app.data.TokenStore
import com.stridewell.app.ui.components.PolylineDecoder
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface HeatmapState {
    data object Idle : HeatmapState
    data object Loading : HeatmapState
    data class Ready(val imageBytes: ByteArray) : HeatmapState
    data object Insufficient : HeatmapState
    data class Error(val message: String) : HeatmapState
}

@HiltViewModel
class HeatmapViewModel @Inject constructor(
    private val runsRepository: RunsRepository,
    private val tokenStore: TokenStore,
    private val locationRepository: LocationRepository,
    private val heatmapCache: HeatmapCache,
    private val routeRenderer: RouteRenderer
) : ViewModel() {

    private val _state = MutableStateFlow<HeatmapState>(HeatmapState.Idle)
    val state: StateFlow<HeatmapState> = _state.asStateFlow()

    private var targetSize: IntSize = IntSize.Zero
    private var locationPermissionGranted = false
    private var lastThemeIsDark: Boolean? = null
    private var generationJob: Job? = null

    fun setRenderSize(size: IntSize) {
        if (size.width > 0 && size.height > 0) {
            targetSize = size
        }
    }

    fun setLocationPermissionGranted(granted: Boolean) {
        locationPermissionGranted = granted
        if (granted) {
            viewModelScope.launch {
                locationRepository.requestLocation(hasPermission = true)
            }
        }
    }

    fun loadIfNeeded(isDark: Boolean) {
        val current = _state.value
        if (current !is HeatmapState.Idle &&
            current !is HeatmapState.Error &&
            lastThemeIsDark == isDark) {
            return
        }
        Log.i(TAG, "loadIfNeeded theme=${if (isDark) "dark" else "light"} current=$current lastTheme=$lastThemeIsDark")
        generate(force = false, isDark = isDark)
    }

    fun invalidateAndRegenerate(isDark: Boolean) {
        Log.i(TAG, "invalidateAndRegenerate theme=${if (isDark) "dark" else "light"}")
        generate(force = true, isDark = isDark)
    }

    private fun generate(force: Boolean, isDark: Boolean) {
        generationJob?.cancel()
        generationJob = viewModelScope.launch {
            val wasReady = _state.value is HeatmapState.Ready
            if (!wasReady) {
                _state.value = HeatmapState.Loading
            }

            val userId = tokenStore.getUserId() ?: "anonymous"
            val location = locationRepository.requestLocation(locationPermissionGranted)

            when (val response = runsRepository.heatmap()) {
                is ApiResult.Error -> {
                    Log.w(TAG, "Heatmap API error status=${response.status} message=${response.message}")
                    if (!wasReady || force) {
                        _state.value = HeatmapState.Error(response.message)
                    }
                }
                is ApiResult.Success -> {
                    val data = response.data
                    if (data.run_count < 3) {
                        Log.i(TAG, "Heatmap insufficient data run_count=${data.run_count}")
                        _state.value = HeatmapState.Insufficient
                        lastThemeIsDark = isDark
                        return@launch
                    }

                    val hasLocation = location != null
                    val cached = heatmapCache.load(
                        userId = userId,
                        runCount = data.run_count,
                        hasLocation = hasLocation,
                        isDark = isDark
                    )
                    if (cached != null) {
                        Log.i(
                            TAG,
                            "Heatmap cache hit runCount=${data.run_count} theme=${if (isDark) "dark" else "light"} hasLocation=$hasLocation"
                        )
                        _state.value = HeatmapState.Ready(cached.toJpegBytes())
                        lastThemeIsDark = isDark
                        return@launch
                    }
                    Log.i(
                        TAG,
                        "Heatmap cache miss runCount=${data.run_count} theme=${if (isDark) "dark" else "light"} hasLocation=$hasLocation"
                    )

                    if (targetSize == IntSize.Zero) {
                        Log.w(TAG, "Heatmap target size unavailable")
                        _state.value = HeatmapState.Error("Heatmap target size unavailable")
                        return@launch
                    }

                    val coordinateGroups = withContext(Dispatchers.Default) {
                        data.polylines.mapNotNull { encoded ->
                            PolylineDecoder.decode(encoded).takeIf { it.size > 1 }
                        }
                    }

                    val region = RegionCalculator.region(
                        coordinateGroups = coordinateGroups,
                        userLocation = location,
                        boundingBox = data.bounding_box
                    )
                    if (region == null) {
                        Log.w(TAG, "Heatmap region unavailable; falling back to insufficient")
                        _state.value = HeatmapState.Insufficient
                        lastThemeIsDark = isDark
                        return@launch
                    }

                    val image = routeRenderer.render(
                        coordinateGroups = coordinateGroups,
                        region = region,
                        targetSize = targetSize,
                        isDark = isDark,
                        staticMapsApiKey = BuildConfig.GOOGLE_MAPS_STATIC_API_KEY
                    )

                    if (image == null) {
                        Log.e(TAG, "Heatmap render returned null image")
                        _state.value = HeatmapState.Error("Failed to render heatmap")
                        return@launch
                    }

                    heatmapCache.save(
                        image = image,
                        userId = userId,
                        runCount = data.run_count,
                        hasLocation = hasLocation,
                        isDark = isDark
                    )
                    _state.value = HeatmapState.Ready(image.toJpegBytes())
                    lastThemeIsDark = isDark
                }
            }
        }
    }

    private fun android.graphics.Bitmap.toJpegBytes(): ByteArray {
        val stream = java.io.ByteArrayOutputStream()
        compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, stream)
        return stream.toByteArray()
    }

    companion object {
        private const val TAG = "HeatmapViewModel"
    }
}
