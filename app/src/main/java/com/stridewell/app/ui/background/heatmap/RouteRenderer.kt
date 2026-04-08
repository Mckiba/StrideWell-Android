package com.stridewell.app.ui.background.heatmap

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.util.Log
import androidx.compose.ui.unit.IntSize
import com.stridewell.app.ui.components.RoutePoint
import javax.inject.Inject
import javax.inject.Singleton
import java.io.IOException
import kotlin.math.PI
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request

@Singleton
class RouteRenderer @Inject constructor(
    private val httpClient: OkHttpClient
) {
    suspend fun render(
        coordinateGroups: List<List<RoutePoint>>,
        region: HeatmapRegion,
        targetSize: IntSize,
        isDark: Boolean,
        staticMapsApiKey: String
    ): Bitmap? {
        if (targetSize.width <= 0 || targetSize.height <= 0) return null
        val requestSize = fitStaticMapSize(targetSize)
        val continuousZoom = computeZoom(region, requestSize)
        val fetchZoom = staticMapZoomLevel(continuousZoom)
        val mapFetch = withContext(Dispatchers.IO) {
            fetchStaticMapBitmap(region, requestSize, fetchZoom, isDark, staticMapsApiKey)
        }
        val base = mapFetch.bitmap
            ?: fallbackBasemap(requestSize, isDark)

        if (mapFetch.bitmap == null) {
            val reason = mapFetch.failure
            Log.w(
                TAG,
                "Static map fetch failed; using fallback basemap. " +
                    "reason=${reason?.code ?: "unknown"} detail=${reason?.detail ?: "n/a"}"
            )
        } else {
            Log.i(
                TAG,
                "Static map fetch succeeded; theme=${if (isDark) "dark" else "light"} " +
                    "styleApplied=$isDark zoom=$fetchZoom"
            )
        }

        val mutable = base.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(mutable)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            // iOS parity: #289FFF @ 0.75 alpha, width 2.5
            color = 0xBF289FFF.toInt()
            strokeWidth = 2.5f
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }

        val centerWorld = latLngToWorld(region.centerLat, region.centerLng, fetchZoom.toDouble())
        val halfW = requestSize.width / 2.0
        val halfH = requestSize.height / 2.0

        for (coords in coordinateGroups) {
            if (coords.size < 2) continue
            var prevX: Float? = null
            var prevY: Float? = null
            for (point in coords) {
                val world = latLngToWorld(point.latitude, point.longitude, fetchZoom.toDouble())
                val x = (world.first - centerWorld.first + halfW).toFloat()
                val y = (world.second - centerWorld.second + halfH).toFloat()
                val px = prevX
                val py = prevY
                if (px != null && py != null) {
                    canvas.drawLine(px, py, x, y, paint)
                }
                prevX = x
                prevY = y
            }
        }

        // iOS uses continuous map zoom; Static Maps only accepts integer zoom levels.
        // Apply center-crop upscaling to emulate the missing fractional zoom component.
        val fractionalScale = 2.0.pow(continuousZoom - fetchZoom.toDouble()).toFloat()
        val zoomAligned = if (fractionalScale > 1.001f) {
            applyCenterZoom(mutable, fractionalScale)
        } else {
            mutable
        }

        if (requestSize == targetSize) return zoomAligned
        return Bitmap.createScaledBitmap(
            zoomAligned,
            targetSize.width,
            targetSize.height,
            true
        )
    }

    private fun fallbackBasemap(size: IntSize, isDark: Boolean): Bitmap {
        val bitmap = Bitmap.createBitmap(size.width, size.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(if (isDark) Color.parseColor("#202124") else Color.parseColor("#EEF1F4"))
        return bitmap
    }

    private fun fitStaticMapSize(targetSize: IntSize): IntSize {
        val maxDimension = 640f
        val scale = minOf(
            maxDimension / targetSize.width.toFloat(),
            maxDimension / targetSize.height.toFloat(),
            1f
        )
        val width = (targetSize.width * scale).roundToInt().coerceAtLeast(1)
        val height = (targetSize.height * scale).roundToInt().coerceAtLeast(1)
        return IntSize(width, height)
    }

    private fun computeZoom(region: HeatmapRegion, size: IntSize): Double {
        val latDelta = region.latDelta.coerceAtLeast(1e-6)
        val lngDelta = region.lngDelta.coerceAtLeast(1e-6)

        val zoomLon = ln((360.0 * size.width) / (256.0 * lngDelta)) / ln(2.0)

        val lat1 = (region.centerLat - latDelta / 2.0).coerceIn(-85.0, 85.0)
        val lat2 = (region.centerLat + latDelta / 2.0).coerceIn(-85.0, 85.0)
        val merc1 = mercatorY(lat1)
        val merc2 = mercatorY(lat2)
        val mercSpan = (merc1 - merc2).let { kotlin.math.abs(it) }.coerceAtLeast(1e-6)
        val zoomLat = ln((2.0 * PI * size.height) / (256.0 * mercSpan)) / ln(2.0)

        return minOf(zoomLon, zoomLat).coerceIn(2.0, 20.0)
    }

    private fun staticMapZoomLevel(continuousZoom: Double): Int {
        return floor(continuousZoom).toInt().coerceIn(2, 20)
    }

    private fun applyCenterZoom(source: Bitmap, scale: Float): Bitmap {
        if (scale <= 1f) return source
        val cropWidth = (source.width / scale).roundToInt().coerceIn(1, source.width)
        val cropHeight = (source.height / scale).roundToInt().coerceIn(1, source.height)
        val left = ((source.width - cropWidth) / 2f).roundToInt().coerceIn(0, source.width - cropWidth)
        val top = ((source.height - cropHeight) / 2f).roundToInt().coerceIn(0, source.height - cropHeight)

        val result = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val src = Rect(left, top, left + cropWidth, top + cropHeight)
        val dst = Rect(0, 0, source.width, source.height)
        canvas.drawBitmap(source, src, dst, null)
        return result
    }

    private fun mercatorY(lat: Double): Double {
        val rad = lat * PI / 180.0
        return ln(kotlin.math.tan(PI / 4.0 + rad / 2.0))
    }

    private fun latLngToWorld(lat: Double, lng: Double, zoom: Double): Pair<Double, Double> {
        val scale = 256.0 * 2.0.pow(zoom)
        val x = (lng + 180.0) / 360.0 * scale
        val sinLat = kotlin.math.sin(lat * PI / 180.0).coerceIn(-0.9999, 0.9999)
        val y = (0.5 - ln((1 + sinLat) / (1 - sinLat)) / (4.0 * PI)) * scale
        return x to y
    }

    private data class MapFailure(val code: String, val detail: String)
    private data class MapFetchResult(val bitmap: Bitmap?, val failure: MapFailure?)

    private fun fetchStaticMapBitmap(
        region: HeatmapRegion,
        size: IntSize,
        zoom: Int,
        isDark: Boolean,
        key: String
    ): MapFetchResult {
        if (key.isBlank()) {
            return MapFetchResult(
                bitmap = null,
                failure = MapFailure(
                    code = "missing_api_key",
                    detail = "BuildConfig.GOOGLE_MAPS_STATIC_API_KEY is blank"
                )
            )
        }

        val base = "https://maps.googleapis.com/maps/api/staticmap".toHttpUrl().newBuilder()
            .addQueryParameter("center", "${region.centerLat},${region.centerLng}")
            .addQueryParameter("zoom", zoom.toString())
            .addQueryParameter("size", "${size.width}x${size.height}")
            .addQueryParameter("scale", "2")
            .addQueryParameter("maptype", "roadmap")

        if (isDark) {
            // Force dark tiles for app dark mode, matching iOS trait-based map appearance.
            base.addQueryParameter("style", "feature:all|element:geometry|color:0x242f3e")
            base.addQueryParameter("style", "feature:all|element:labels.text.fill|color:0x746855")
            base.addQueryParameter("style", "feature:all|element:labels.text.stroke|color:0x242f3e")
            base.addQueryParameter("style", "feature:road|element:geometry|color:0x38414e")
            base.addQueryParameter("style", "feature:road.highway|element:geometry|color:0x746855")
            base.addQueryParameter("style", "feature:water|element:geometry|color:0x17263c")
        }

        base.addQueryParameter("key", key)

        val request = Request.Builder()
            .url(base.build())
            .get()
            .build()

        return try {
            httpClient.newCall(request).execute().use { response ->
                val bytes = response.body?.bytes()
                if (!response.isSuccessful) {
                    val raw = bytes?.toString(Charsets.UTF_8).orEmpty().take(240)
                    val lowered = raw.lowercase()
                    val failure = when {
                        response.code == 429 || lowered.contains("quota") ->
                            MapFailure("quota_exceeded", "HTTP ${response.code}: $raw")
                        lowered.contains("api key") || lowered.contains("key") || response.code == 401 || response.code == 403 ->
                            MapFailure("invalid_or_denied_api_key", "HTTP ${response.code}: $raw")
                        lowered.contains("billing") ->
                            MapFailure("billing_not_enabled", "HTTP ${response.code}: $raw")
                        else ->
                            MapFailure("http_error", "HTTP ${response.code}: $raw")
                    }
                    return@use MapFetchResult(bitmap = null, failure = failure)
                }

                if (bytes == null || bytes.isEmpty()) {
                    return@use MapFetchResult(
                        bitmap = null,
                        failure = MapFailure("empty_response_body", "Static map response body was empty")
                    )
                }

                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                if (bitmap == null) {
                    return@use MapFetchResult(
                        bitmap = null,
                        failure = MapFailure("decode_failed", "Could not decode static map image bytes")
                    )
                }
                // Static Maps returns @2x when scale=2. Draw against logical request size.
                MapFetchResult(
                    bitmap = Bitmap.createScaledBitmap(bitmap, size.width, size.height, true),
                    failure = null
                )
            }
        } catch (io: IOException) {
            MapFetchResult(
                bitmap = null,
                failure = MapFailure("network_error", io.message ?: "I/O error")
            )
        } catch (e: Exception) {
            MapFetchResult(
                bitmap = null,
                failure = MapFailure(
                    "unexpected_error",
                    "${e::class.java.simpleName}: ${e.message ?: "Unexpected static map failure"}"
                )
            )
        }
    }

    companion object {
        private const val TAG = "HeatmapRenderer"
    }
}
