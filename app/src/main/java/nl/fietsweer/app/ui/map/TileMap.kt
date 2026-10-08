package nl.fietsweer.app.ui.map

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import nl.fietsweer.app.domain.Geo
import nl.fietsweer.app.domain.LatLon
import nl.fietsweer.app.ui.theme.MapPalette
import kotlin.math.PI
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sinh
import kotlin.math.tan

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

data class MapMarker(val point: LatLon, val color: Color)

data class MapLine(val points: List<LatLon>, val color: Color)

@Composable
fun TileMap(
    camera: MapCamera,
    source: TileSource,
    modifier: Modifier = Modifier,
    darken: Boolean = false,
    markers: List<MapMarker> = emptyList(),
    lines: List<MapLine> = emptyList(),
    interactive: Boolean = true,
    overlay: (DrawScope.(project: (LatLon) -> Offset) -> Unit)? = null,
    onMoved: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val density = LocalDensity.current.density
    var loadedTiles by remember { mutableIntStateOf(0) }
    val onTileLoaded = remember { { loadedTiles++; Unit } }

    val palette = if (darken) MapPalette.Dark else MapPalette.Light
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
                            var worldX = lonToWorldX(camera.lon) - pan.x / world
                            var worldY = latToWorldY(camera.lat) - pan.y / world

                            if (gestureZoom != 1f) {
                                val newZoom = (camera.zoom + ln(gestureZoom) / ln(2f))
                                    .coerceIn(MapCamera.MIN_ZOOM, MapCamera.MAX_ZOOM)
                                val newWorld = baseTile * 2f.pow(newZoom)
                                val centerX = size.width / 2f
                                val centerY = size.height / 2f
                                val pinchWorldX = worldX + (centroid.x - centerX) / world
                                val pinchWorldY = worldY + (centroid.y - centerY) / world
                                worldX = pinchWorldX - (centroid.x - centerX) / newWorld
                                worldY = pinchWorldY - (centroid.y - centerY) / newWorld
                                camera.zoom = newZoom
                            }
                            camera.lon = worldXToLon(worldX.coerceIn(0.0001, 0.9999))
                            camera.lat = worldYToLat(worldY.coerceIn(0.0001, 0.9999))
                            onMoved?.invoke()
                        }
                    }
                )
        ) {
            @Suppress("UNUSED_EXPRESSION") loadedTiles // redraw when a tile arrives

            val tileZoom = camera.zoom.roundToInt().coerceIn(0, source.maxZoom)
            val tilePx = baseTile * 2f.pow(camera.zoom - tileZoom)
            val tilesPerSide = 1 shl tileZoom

            val centerTileX = lonToWorldX(camera.lon) * tilesPerSide
            val centerTileY = latToWorldY(camera.lat) * tilesPerSide
            val centerX = size.width / 2f
            val centerY = size.height / 2f

            val firstX = floor(centerTileX - centerX / tilePx).toInt()
            val lastX = floor(centerTileX + centerX / tilePx).toInt()
            val firstY = floor(centerTileY - centerY / tilePx).toInt()
            val lastY = floor(centerTileY + centerY / tilePx).toInt()

            drawRect(palette.background)

            for (tileY in firstY..lastY) {
                if (tileY < 0 || tileY >= tilesPerSide) continue
                for (tileX in firstX..lastX) {
                    val wrappedX = ((tileX % tilesPerSide) + tilesPerSide) % tilesPerSide
                    val screenX = centerX + (tileX - centerTileX).toFloat() * tilePx
                    val screenY = centerY + (tileY - centerTileY).toFloat() * tilePx
                    val tile = TileLoader.cached(source, tileZoom, wrappedX, tileY)
                    if (tile == null) {
                        TileLoader.request(context, source, tileZoom, wrappedX, tileY, onTileLoaded)
                        drawRect(
                            palette.loadingTile,
                            topLeft = Offset(screenX, screenY),
                            size = Size(tilePx, tilePx)
                        )
                    } else {
                        drawImage(
                            image = tile,
                            srcOffset = IntOffset.Zero,
                            srcSize = IntSize(tile.width, tile.height),
                            dstOffset = IntOffset(screenX.toInt(), screenY.toInt()),
                            dstSize = IntSize(tilePx.toInt() + 1, tilePx.toInt() + 1),
                            filterQuality = FilterQuality.Medium,
                            colorFilter = if (darken) DARK_TILE_FILTER else null
                        )
                    }
                }
            }

            val world = baseTile * 2f.pow(camera.zoom)
            fun project(point: LatLon): Offset = Offset(
                centerX + ((lonToWorldX(point.lon) - lonToWorldX(camera.lon)) * world).toFloat(),
                centerY + ((latToWorldY(point.lat) - latToWorldY(camera.lat)) * world).toFloat()
            )

            overlay?.invoke(this, ::project)

            for (line in lines) {
                if (line.points.size < 2) continue
                val path = Path()
                line.points.forEachIndexed { i, point ->
                    val offset = project(point)
                    if (i == 0) path.moveTo(offset.x, offset.y) else path.lineTo(offset.x, offset.y)
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

            for (marker in markers) {
                drawPin(project(marker.point), marker.color, density)
            }
        }
    }
}

private fun DrawScope.drawPin(at: Offset, color: Color, density: Float) {
    val radius = 11f * density
    val stemHeight = 16f * density
    val head = Offset(at.x, at.y - stemHeight)

    val stem = Path().apply {
        moveTo(at.x, at.y)
        lineTo(at.x - radius * 0.62f, head.y + radius * 0.45f)
        lineTo(at.x + radius * 0.62f, head.y + radius * 0.45f)
        close()
    }
    drawCircle(Color.Black.copy(alpha = 0.18f), radius = radius * 0.55f, center = Offset(at.x, at.y + 2f * density))
    drawPath(stem, color)
    drawCircle(color, radius = radius, center = head)
    drawCircle(Color.White, radius = radius, center = head, style = Stroke(width = 2.6f * density))
    drawCircle(Color.White, radius = radius * 0.36f, center = head)
}

private fun lonToWorldX(lon: Double): Double = (lon + 180.0) / 360.0

private fun latToWorldY(lat: Double): Double {
    val latRadians = Math.toRadians(lat.coerceIn(-85.05112878, 85.05112878))
    return (1.0 - ln(tan(latRadians) + 1.0 / cos(latRadians)) / PI) / 2.0
}

private fun worldXToLon(worldX: Double): Double = worldX * 360.0 - 180.0

private fun worldYToLat(worldY: Double): Double =
    Math.toDegrees(atan(sinh(PI * (1.0 - 2.0 * worldY))))

fun zoomForPair(from: LatLon, to: LatLon, widthPx: Float, densityScale: Float): Float {
    val distanceKm = Geo.haversineKm(from, to).coerceAtLeast(0.4)
    val target = Geo.zoomForSpan(from.lat, distanceKm * 1.8, widthPx.toDouble()).toFloat()
    val tileAdjust = ln(densityScale.coerceIn(1f, 2f)) / ln(2f)
    return (target - tileAdjust).coerceIn(MapCamera.MIN_ZOOM, MapCamera.MAX_ZOOM)
}
