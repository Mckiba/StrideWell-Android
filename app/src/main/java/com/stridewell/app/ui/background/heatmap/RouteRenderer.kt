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
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.Locale

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
        // Mapbox accepts fractional zoom, so the fetched framing matches the requested
        // region exactly. The old integer-zoom + centre-crop-upscale dance is gone.
        val zoom = computeZoom(region, requestSize)
        val mapFetch = withContext(Dispatchers.IO) {
            fetchStaticMapBitmap(region, requestSize, zoom, isDark, staticMapsApiKey)
        }
        val base = mapFetch.bitmap
            ?: fallbackBasemap(
                IntSize(requestSize.width * PIXEL_SCALE, requestSize.height * PIXEL_SCALE),
                isDark
            )

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
                    "zoom=$zoom size=${base.width}x${base.height}"
            )
        }

        val mutable = base.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(mutable)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            // iOS parity: #289FFF @ 0.75 alpha, width 2.5
            color = 0xBF289FFF.toInt()
            strokeWidth = 2.5f * PIXEL_SCALE
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }

        // Project in the fetched bitmap's own pixel space: world coordinates are in
        // logical units, so scale the offsets by the @2x factor.
        val centerWorld = latLngToWorld(region.centerLat, region.centerLng, zoom)
        val halfW = mutable.width / 2.0
        val halfH = mutable.height / 2.0

        for (coords in coordinateGroups) {
            if (coords.size < 2) continue
            var prevX: Float? = null
            var prevY: Float? = null
            for (point in coords) {
                val world = latLngToWorld(point.latitude, point.longitude, zoom)
                val x = ((world.first - centerWorld.first) * PIXEL_SCALE + halfW).toFloat()
                val y = ((world.second - centerWorld.second) * PIXEL_SCALE + halfH).toFloat()
                val px = prevX
                val py = prevY
                if (px != null && py != null) {
                    canvas.drawLine(px, py, x, y, paint)
                }
                prevX = x
                prevY = y
            }
        }

        if (mutable.width == targetSize.width && mutable.height == targetSize.height) return mutable
        return Bitmap.createScaledBitmap(mutable, targetSize.width, targetSize.height, true)
    }

    private fun fallbackBasemap(size: IntSize, isDark: Boolean): Bitmap {
        val bitmap = Bitmap.createBitmap(size.width, size.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(if (isDark) Color.parseColor("#202124") else Color.parseColor("#EEF1F4"))
        return bitmap
    }

    private fun fitStaticMapSize(targetSize: IntSize): IntSize {
        val maxDimension = MAX_STATIC_DIMENSION.toFloat()
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
        zoom: Double,
        isDark: Boolean,
        key: String
    ): MapFetchResult {
        if (key.isBlank()) {
            return MapFetchResult(
                bitmap = null,
                failure = MapFailure(
                    code = "missing_api_key",
                    detail = "BuildConfig.MAPBOX_PUBLIC_TOKEN is blank"
                )
            )
        }

        // Mapbox Static Images: 1280 per dimension (Google capped at 640), @2x for
        // double the pixels, fractional zoom, and first-class light/dark styles in
        // place of the hand-rolled Google colour overrides.
        val style = if (isDark) STYLE_DARK else STYLE_LIGHT
        val centre = "%.6f,%.6f,%.4f".format(
            Locale.US, region.centerLng, region.centerLat, zoom
        )
        val url = ("https://api.mapbox.com/styles/v1/$style/static/" +
            "$centre/${size.width}x${size.height}@${PIXEL_SCALE}x")
            .toHttpUrl().newBuilder()
            .addQueryParameter("access_token", key)
            .build()

        val request = Request.Builder()
            .url(url)
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
                // Keep the @2x pixels. The previous implementation downscaled back to
                // the logical size here purely so the route maths lined up, throwing
                // away half the resolution before the final upscale to the screen.
                MapFetchResult(bitmap = bitmap, failure = null)
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

        /** Mapbox Static Images allows 1280 per dimension; Google Static Maps capped at 640. */
        private const val MAX_STATIC_DIMENSION = 1280

        /** Mapbox @2x. Doubles the fetched pixels for the same map area. */
        private const val PIXEL_SCALE = 2

        /**
         * Custom Mapbox styles, as "owner/styleId" — the Static Images path segment
         * after /styles/v1/. Edit these in Mapbox Studio; no app change is needed for
         * a restyle, but bump CACHE_VERSION or cached images keep being served.
         */
        private const val STYLE_LIGHT = "mckiba/cmtzcmfdx009301snb8xf5g0e"
        private const val STYLE_DARK = "mckiba/cmtzcs3dr00a801ssb31m95no"
    }
}
