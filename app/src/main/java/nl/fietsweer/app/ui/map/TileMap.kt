package nl.fietsweer.app.ui.map

import android.content.Context
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import nl.fietsweer.app.data.Net
import nl.fietsweer.app.domain.Geo
import nl.fietsweer.app.domain.LatLon
import java.io.File
import java.util.Collections
import kotlin.math.PI
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.sinh
import kotlin.math.tan
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue

// ------------------------------------------------------------------- sources

enum class TileSource(
    val id: String,
    val template: String,
    val maxZoom: Int,
    val attribution: String
) {
    /** The standard OpenStreetMap raster style. Free, no key, needs a real UA. */
    OSM(
        "osm", "https://tile.openstreetmap.org/{z}/{x}/{y}.png", 19,
        "\u00a9 OpenStreetMap contributors"
    ),

    /** Humanitarian style: softer colours, slightly less clutter. */
    OSM_HOT(
        "hot", "https://tile-a.openstreetmap.fr/hot/{z}/{x}/{y}.png", 19,
        "\u00a9 OpenStreetMap contributors \u00b7 HOT"
    );

    fun url(z: Int, x: Int, y: Int): String =
        template.replace("{z}", z.toString()).replace("{x}", x.toString()).replace("{y}", y.toString())
}

/**
 * A dark basemap without a second tile server: invert the light tiles and
 * rotate the hue back, the same trick CSS `invert() hue-rotate(180deg)` uses.
 * Slightly dimmed so the markers keep the upper hand.
 */
private val DARK_TILE_FILTER: ColorFilter = ColorFilter.colorMatrix(
    ColorMatrix(
        floatArrayOf(
            0.53f, -1.31f, -0.13f, 0f, 224f,
            -0.39f, -0.40f, -0.13f, 0f, 224f,
            -0.39f, -1.31f, 0.79f, 0f, 224f,
            0f, 0f, 0f, 1f, 0f
        )
    )
)

// -------------------------------------------------------------- tile loading

/**
 * Two-level tile cache: bitmaps in memory, PNG bytes on disk. Requests are
 * deduplicated so panning does not queue the same tile twice.
 */
object TileLoader {

    private const val MAX_MEMORY_TILES = 220
    private val memory = object : LruCache<String, ImageBitmap>(MAX_MEMORY_TILES) {}
    private val inFlight = Collections.synchronizedSet(HashSet<String>())
    private val failed = Collections.synchronizedSet(HashSet<String>())
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private fun key(source: TileSource, z: Int, x: Int, y: Int) = "${source.id}/$z/$x/$y"

    fun cached(source: TileSource, z: Int, x: Int, y: Int): ImageBitmap? =
        memory.get(key(source, z, x, y))

    fun request(
        context: Context,
        source: TileSource,
        z: Int,
        x: Int,
        y: Int,
        onLoaded: () -> Unit
    ) {
        val k = key(source, z, x, y)
        if (memory.get(k) != null || k in failed || !inFlight.add(k)) return
        val appContext = context.applicationContext
        scope.launch {
            try {
                val file = File(appContext.cacheDir, "tiles/$k.png")
                val bytes = if (file.exists() && file.length() > 0) {
                    file.readBytes()
                } else {
                    val data = Net.blockingBytes(source.url(z, x, y), 15_000)
                    runCatching {
                        file.parentFile?.mkdirs()
                        file.writeBytes(data)
                    }
                    data
                }
                val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                if (bmp != null) {
                    memory.put(k, bmp.asImageBitmap())
                    onLoaded()
                } else {
                    failed.add(k)
                }
            } catch (t: Throwable) {
                failed.add(k)
            } finally {
                inFlight.remove(k)
            }
        }
    }

    /** Lets a failed area be retried, e.g. after the network comes back. */
    fun clearFailures() = failed.clear()
}

// -------------------------------------------------------------------- camera

class MapCamera(lat: Double, lon: Double, zoom: Float) {
    var lat by mutableStateOf(lat)
    var lon by mutableStateOf(lon)
    var zoom by mutableFloatStateOf(zoom)

    val center: LatLon get() = LatLon(lat, lon)

    fun moveTo(target: LatLon, newZoom: Float? = null) {
        lat = target.lat
        lon = target.lon
        newZoom?.let { zoom = it.coerceIn(MIN_ZOOM, MAX_ZOOM) }
    }

    companion object {
        const val MIN_ZOOM = 3f
        const val MAX_ZOOM = 18.5f
    }
}

@Composable
fun rememberMapCamera(lat: Double, lon: Double, zoom: Float = 13f): MapCamera =
    remember { MapCamera(lat, lon, zoom) }

// ------------------------------------------------------------------- markers

data class MapMarker(
    val point: LatLon,
    val color: Color,
    val ring: Color = Color.White,
    val small: Boolean = false
)

data class MapLine(val points: List<LatLon>, val color: Color)

// ----------------------------------------------------------------- composable

/**
 * A plain raster slippy map. Rendering the tiles directly keeps the whole map
 * inside Compose: no view interop, no separate lifecycle, and markers can use
 * the same colours as the rest of the app.
 */
@Composable
fun TileMap(
    camera: MapCamera,
    source: TileSource,
    modifier: Modifier = Modifier,
    darken: Boolean = false,
    markers: List<MapMarker> = emptyList(),
    lines: List<MapLine> = emptyList(),
    interactive: Boolean = true,
    onMoved: (() -> Unit)? = null,
    onTap: ((LatLon) -> Unit)? = null
) {
    val context = LocalContext.current
    val density = LocalDensity.current.density
    var version by remember { mutableIntStateOf(0) }
    val bump = remember { { version++; Unit } }

    // Tiles are drawn at twice their pixel size on dense screens so that the
    // map does not end up microscopic; retina sources supply the extra detail.
    val tileScale = density.coerceIn(1f, 2f)
    val baseTile = 256f * tileScale

    DisposableEffect(source) {
        TileLoader.clearFailures()
        onDispose { }
    }

    Box(modifier) {
        Canvas(
            Modifier
                .fillMaxSize()
                .then(
                    if (!interactive) Modifier else Modifier.pointerInput(source) {
                        detectTransformGestures { centroid, pan, gestureZoom, _ ->
                            val world = baseTile * 2f.pow(camera.zoom)
                            // pan
                            var nx = lonToNx(camera.lon) - pan.x / world
                            var ny = latToNy(camera.lat) - pan.y / world

                            if (gestureZoom != 1f) {
                                val newZoom = (camera.zoom + ln(gestureZoom) / ln(2f))
                                    .coerceIn(MapCamera.MIN_ZOOM, MapCamera.MAX_ZOOM)
                                val worldNew = baseTile * 2f.pow(newZoom)
                                val cx = size.width / 2f
                                val cy = size.height / 2f
                                // keep the point under the fingers still
                                val pointNx = nx + (centroid.x - cx) / world
                                val pointNy = ny + (centroid.y - cy) / world
                                nx = pointNx - (centroid.x - cx) / worldNew
                                ny = pointNy - (centroid.y - cy) / worldNew
                                camera.zoom = newZoom
                            }
                            camera.lon = nxToLon(nx.coerceIn(0.0001, 0.9999))
                            camera.lat = nyToLat(ny.coerceIn(0.0001, 0.9999))
                            onMoved?.invoke()
                        }
                    }
                )
                .then(
                    if (onTap == null) Modifier else Modifier.pointerInput(source) {
                        detectTapGestures { pos ->
                            val world = baseTile * 2f.pow(camera.zoom)
                            val nx = lonToNx(camera.lon) + (pos.x - size.width / 2f) / world
                            val ny = latToNy(camera.lat) + (pos.y - size.height / 2f) / world
                            onTap(LatLon(nyToLat(ny.coerceIn(0.0001, 0.9999)), nxToLon(nx)))
                        }
                    }
                )
        ) {
            @Suppress("UNUSED_EXPRESSION") version // redraw when a tile arrives

            val z = floor(camera.zoom).toInt().coerceIn(0, source.maxZoom)
            val frac = 2f.pow(camera.zoom - z)
            val tilePx = baseTile * frac
            val n = 1 shl z

            val centerTx = lonToNx(camera.lon) * n
            val centerTy = latToNy(camera.lat) * n
            val cx = size.width / 2f
            val cy = size.height / 2f

            val firstX = floor(centerTx - cx / tilePx).toInt()
            val lastX = floor(centerTx + cx / tilePx).toInt()
            val firstY = floor(centerTy - cy / tilePx).toInt()
            val lastY = floor(centerTy + cy / tilePx).toInt()

            drawRect(if (darken) Color(0xFF15191C) else Color(0xFFE8EDF1))

            for (ty in firstY..lastY) {
                if (ty < 0 || ty >= n) continue
                for (tx in firstX..lastX) {
                    val wrapped = ((tx % n) + n) % n
                    val sx = cx + (tx - centerTx).toFloat() * tilePx
                    val sy = cy + (ty - centerTy).toFloat() * tilePx
                    val bmp = TileLoader.cached(source, z, wrapped, ty)
                    if (bmp == null) {
                        TileLoader.request(context, source, z, wrapped, ty, bump)
                        drawRect(
                            if (darken) Color(0x14FFFFFF) else Color(0x14000000),
                            topLeft = Offset(sx, sy),
                            size = Size(tilePx, tilePx)
                        )
                    } else {
                        drawImage(
                            image = bmp,
                            srcOffset = IntOffset.Zero,
                            srcSize = IntSize(bmp.width, bmp.height),
                            dstOffset = IntOffset(sx.toInt(), sy.toInt()),
                            dstSize = IntSize(tilePx.toInt() + 1, tilePx.toInt() + 1),
                            filterQuality = FilterQuality.Medium,
                            colorFilter = if (darken) DARK_TILE_FILTER else null
                        )
                    }
                }
            }

            // ---- overlays -----------------------------------------------------
            val world = baseTile * 2f.pow(camera.zoom)
            fun project(p: LatLon): Offset = Offset(
                cx + ((lonToNx(p.lon) - lonToNx(camera.lon)) * world).toFloat(),
                cy + ((latToNy(p.lat) - latToNy(camera.lat)) * world).toFloat()
            )

            for (line in lines) {
                if (line.points.size < 2) continue
                val path = Path()
                line.points.forEachIndexed { i, p ->
                    val o = project(p)
                    if (i == 0) path.moveTo(o.x, o.y) else path.lineTo(o.x, o.y)
                }
                drawPath(
                    path, line.color.copy(alpha = 0.35f),
                    style = Stroke(width = 9f * density, cap = StrokeCap.Round)
                )
                drawPath(
                    path, line.color,
                    style = Stroke(width = 4f * density, cap = StrokeCap.Round)
                )
            }

            for (m in markers) {
                drawPin(project(m.point), m.color, m.ring, density, m.small)
            }
        }
    }
}

private fun DrawScope.drawPin(
    at: Offset,
    color: Color,
    ring: Color,
    density: Float,
    small: Boolean
) {
    val r = (if (small) 7f else 11f) * density
    val stemH = (if (small) 10f else 16f) * density
    val head = Offset(at.x, at.y - stemH)

    val path = Path().apply {
        moveTo(at.x, at.y)
        lineTo(at.x - r * 0.62f, head.y + r * 0.45f)
        lineTo(at.x + r * 0.62f, head.y + r * 0.45f)
        close()
    }
    drawCircle(Color.Black.copy(alpha = 0.18f), radius = r * 0.55f, center = Offset(at.x, at.y + 2f * density))
    drawPath(path, color)
    drawCircle(color, radius = r, center = head)
    drawCircle(ring, radius = r, center = head, style = Stroke(width = 2.6f * density))
    drawCircle(ring, radius = r * 0.36f, center = head)
}

// ------------------------------------------------------------------- helpers

internal fun lonToNx(lon: Double): Double = (lon + 180.0) / 360.0

internal fun latToNy(lat: Double): Double {
    val l = Math.toRadians(lat.coerceIn(-85.05112878, 85.05112878))
    return (1.0 - ln(tan(l) + 1.0 / cos(l)) / PI) / 2.0
}

internal fun nxToLon(nx: Double): Double = nx * 360.0 - 180.0

internal fun nyToLat(ny: Double): Double =
    Math.toDegrees(atan(sinh(PI * (1.0 - 2.0 * ny))))

/** Picks a zoom that comfortably frames two points on a map of [heightPx]. */
fun zoomForPair(a: LatLon, b: LatLon, widthPx: Float, densityScale: Float): Float {
    val km = Geo.haversineKm(a, b).coerceAtLeast(0.4)
    val target = Geo.zoomForSpan(a.lat, km * 1.8, widthPx.toDouble()).toFloat()
    val tileAdjust = ln(densityScale.coerceIn(1f, 2f)) / ln(2f)
    return (target - tileAdjust).coerceIn(MapCamera.MIN_ZOOM, MapCamera.MAX_ZOOM)
}

/** Which tiles to draw, and whether to run them through the dark filter. */
data class MapTheme(val source: TileSource, val darken: Boolean) {
    val attribution: String get() = source.attribution
}

/** Maps the user's preference onto a concrete basemap. */
fun mapThemeFor(style: nl.fietsweer.app.data.MapStyle, dark: Boolean): MapTheme =
    when (style) {
        nl.fietsweer.app.data.MapStyle.SOFT -> MapTheme(TileSource.OSM_HOT, false)
        nl.fietsweer.app.data.MapStyle.LIGHT -> MapTheme(TileSource.OSM, false)
        nl.fietsweer.app.data.MapStyle.DARK -> MapTheme(TileSource.OSM, true)
        nl.fietsweer.app.data.MapStyle.AUTO -> MapTheme(TileSource.OSM, dark)
    }
