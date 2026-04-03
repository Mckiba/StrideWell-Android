package com.stridewell.app.ui.components

data class RoutePoint(
    val latitude: Double,
    val longitude: Double
)

object PolylineDecoder {

    fun decode(encoded: String): List<RoutePoint> {
        if (encoded.isEmpty()) return emptyList()

        val coordinates = mutableListOf<RoutePoint>()
        var index = 0
        var lat = 0
        var lng = 0

        while (index < encoded.length) {
            var result = 0
            var shift = 0
            var b: Int

            do {
                if (index >= encoded.length) return coordinates
                b = encoded[index++].code - 63
                result = result or ((b and 0x1F) shl shift)
                shift += 5
            } while (b >= 0x20)

            lat += if ((result and 1) != 0) (result shr 1).inv() else result shr 1

            result = 0
            shift = 0

            do {
                if (index >= encoded.length) return coordinates
                b = encoded[index++].code - 63
                result = result or ((b and 0x1F) shl shift)
                shift += 5
            } while (b >= 0x20)

            lng += if ((result and 1) != 0) (result shr 1).inv() else result shr 1

            coordinates += RoutePoint(
                latitude = lat / 1e5,
                longitude = lng / 1e5
            )
        }

        return coordinates
    }
}
