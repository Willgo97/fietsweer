package nl.fietsweer.app.ui.map

import kotlin.math.PI
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.sinh
import kotlin.math.tan

internal const val TILE_SIZE_PX = 256f

private const val MAX_LATITUDE = 85.05112878
private const val METRES_PER_PIXEL_AT_ZOOM_0 = 156543.03392
private const val MIN_FIT_ZOOM = 2.0
private const val MAX_FIT_ZOOM = 17.0
private const val FALLBACK_ZOOM = 13.0

internal fun lonToWorldX(lon: Double): Double = (lon + 180.0) / 360.0

internal fun latToWorldY(lat: Double): Double {
    val latRadians = Math.toRadians(lat.coerceIn(-MAX_LATITUDE, MAX_LATITUDE))
    return (1.0 - ln(tan(latRadians) + 1.0 / cos(latRadians)) / PI) / 2.0
}

internal fun worldXToLon(worldX: Double): Double = worldX * 360.0 - 180.0

internal fun worldYToLat(worldY: Double): Double =
    Math.toDegrees(atan(sinh(PI * (1.0 - 2.0 * worldY))))

// Tiles are drawn up to twice as large on dense screens, so labels stay readable.
internal fun tileScaleFor(density: Float): Float = density.coerceIn(1f, 2f)

fun zoomToShow(lat: Double, spanKm: Double, widthPx: Float, density: Float): Float {
    val tileAdjust = ln(tileScaleFor(density)) / ln(2f)
    return (zoomForSpan(lat, spanKm, widthPx.toDouble()).toFloat() - tileAdjust)
        .coerceIn(MapCamera.MIN_ZOOM, MapCamera.MAX_ZOOM)
}

private fun zoomForSpan(lat: Double, km: Double, pixels: Double): Double {
    val metresPerPixel = (km * 1000.0) / pixels.coerceAtLeast(1.0)
    if (metresPerPixel <= 0) return FALLBACK_ZOOM
    return (ln(METRES_PER_PIXEL_AT_ZOOM_0 * cos(lat * PI / 180.0) / metresPerPixel) / ln(2.0))
        .coerceIn(MIN_FIT_ZOOM, MAX_FIT_ZOOM)
}
