package nl.fietsweer.app.domain

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.sin
import kotlin.math.sqrt

data class LatLon(val lat: Double, val lon: Double)

object Geo {

    private const val EARTH_R_KM = 6371.0088
    private const val RAD = PI / 180.0

    // Cycling routes are longer than the crow flies; a decent Dutch average.
    const val DETOUR_FACTOR = 1.25

    fun haversineKm(a: LatLon, b: LatLon): Double {
        val dLat = (b.lat - a.lat) * RAD
        val dLon = (b.lon - a.lon) * RAD
        val h = sin(dLat / 2) * sin(dLat / 2) +
            cos(a.lat * RAD) * cos(b.lat * RAD) * sin(dLon / 2) * sin(dLon / 2)
        return 2 * EARTH_R_KM * asin(sqrt(h.coerceIn(0.0, 1.0)))
    }

    /** 0 = north. */
    fun bearingDeg(a: LatLon, b: LatLon): Double {
        val y = sin((b.lon - a.lon) * RAD) * cos(b.lat * RAD)
        val x = cos(a.lat * RAD) * sin(b.lat * RAD) -
            sin(a.lat * RAD) * cos(b.lat * RAD) * cos((b.lon - a.lon) * RAD)
        return (atan2(y, x) / RAD + 360.0) % 360.0
    }

    /** 0..180. */
    fun angleDiff(a: Double, b: Double): Double {
        var d = abs(a - b) % 360.0
        if (d > 180) d = 360 - d
        return d
    }

    fun lerp(a: LatLon, b: LatLon, f: Double) =
        LatLon(a.lat + (b.lat - a.lat) * f, a.lon + (b.lon - a.lon) * f)

    fun samplePoints(a: LatLon, b: LatLon, n: Int): List<LatLon> {
        if (n <= 1) return listOf(a)
        return (0 until n).map { lerp(a, b, it.toDouble() / (n - 1)) }
    }

    fun midpoint(a: LatLon, b: LatLon) = lerp(a, b, 0.5)

    fun zoomForSpan(lat: Double, km: Double, pixels: Double): Double {
        val target = (km * 1000.0) / pixels.coerceAtLeast(1.0)
        if (target <= 0) return 13.0
        return (ln(156543.03392 * cos(lat * RAD) / target) / ln(2.0)).coerceIn(2.0, 17.0)
    }
}
