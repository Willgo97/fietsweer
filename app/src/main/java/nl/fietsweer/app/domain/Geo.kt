package nl.fietsweer.app.domain

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.sin
import kotlin.math.sinh
import kotlin.math.sqrt
import kotlin.math.tan

/** A plain WGS84 coordinate. */
data class LatLon(val lat: Double, val lon: Double)

/**
 * Great-circle helpers plus the Web-Mercator maths the tile map needs.
 * Kept dependency free so it can be unit-reasoned about easily.
 */
object Geo {

    private const val EARTH_R_KM = 6371.0088
    private const val RAD = PI / 180.0

    /** Cycling routes are longer than the crow flies; a decent Dutch average. */
    const val DETOUR_FACTOR = 1.25

    fun haversineKm(a: LatLon, b: LatLon): Double {
        val dLat = (b.lat - a.lat) * RAD
        val dLon = (b.lon - a.lon) * RAD
        val h = sin(dLat / 2) * sin(dLat / 2) +
            cos(a.lat * RAD) * cos(b.lat * RAD) * sin(dLon / 2) * sin(dLon / 2)
        return 2 * EARTH_R_KM * asin(sqrt(h.coerceIn(0.0, 1.0)))
    }

    /** Compass bearing in degrees (0 = north) when travelling from [a] to [b]. */
    fun bearingDeg(a: LatLon, b: LatLon): Double {
        val y = sin((b.lon - a.lon) * RAD) * cos(b.lat * RAD)
        val x = cos(a.lat * RAD) * sin(b.lat * RAD) -
            sin(a.lat * RAD) * cos(b.lat * RAD) * cos((b.lon - a.lon) * RAD)
        return (atan2(y, x) / RAD + 360.0) % 360.0
    }

    /** Smallest absolute difference between two bearings, 0..180. */
    fun angleDiff(a: Double, b: Double): Double {
        var d = abs(a - b) % 360.0
        if (d > 180) d = 360 - d
        return d
    }

    fun lerp(a: LatLon, b: LatLon, f: Double) =
        LatLon(a.lat + (b.lat - a.lat) * f, a.lon + (b.lon - a.lon) * f)

    /** [n] points spread evenly from [a] to [b], inclusive of both ends. */
    fun samplePoints(a: LatLon, b: LatLon, n: Int): List<LatLon> {
        if (n <= 1) return listOf(a)
        return (0 until n).map { lerp(a, b, it.toDouble() / (n - 1)) }
    }

    fun midpoint(a: LatLon, b: LatLon) = lerp(a, b, 0.5)

    // ---------------------------------------------------------------- mercator

    fun lonToTileX(lon: Double, zoom: Int): Double =
        (lon + 180.0) / 360.0 * (1 shl zoom)

    fun latToTileY(lat: Double, zoom: Int): Double {
        val l = lat.coerceIn(-85.05112878, 85.05112878) * RAD
        return (1.0 - ln(tan(l) + 1.0 / cos(l)) / PI) / 2.0 * (1 shl zoom)
    }

    fun tileXToLon(x: Double, zoom: Int): Double =
        x / (1 shl zoom) * 360.0 - 180.0

    fun tileYToLat(y: Double, zoom: Int): Double =
        atan(sinh(PI * (1.0 - 2.0 * y / (1 shl zoom)))) / RAD

    /** Zoom level at which [km] roughly fills [pixels] of screen. */
    fun zoomForSpan(lat: Double, km: Double, pixels: Double): Double {
        val target = (km * 1000.0) / pixels.coerceAtLeast(1.0)
        if (target <= 0) return 13.0
        return (ln(156543.03392 * cos(lat * RAD) / target) / ln(2.0)).coerceIn(2.0, 17.0)
    }
}
