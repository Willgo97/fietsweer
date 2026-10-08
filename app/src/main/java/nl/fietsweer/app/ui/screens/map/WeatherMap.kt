package nl.fietsweer.app.ui.screens.map

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Work
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import nl.fietsweer.app.data.AirQuality
import nl.fietsweer.app.data.LatLon
import nl.fietsweer.app.data.RadarImages
import nl.fietsweer.app.ui.components.AppSlider
import nl.fietsweer.app.ui.components.CARD_SHAPE
import nl.fietsweer.app.ui.components.SectionCard
import nl.fietsweer.app.ui.map.MapMarker
import nl.fietsweer.app.ui.map.MapTheme
import nl.fietsweer.app.ui.map.TileMap
import nl.fietsweer.app.ui.map.rememberMapCamera
import nl.fietsweer.app.ui.map.zoomToShow
import nl.fietsweer.app.ui.theme.AppTheme
import nl.fietsweer.app.ui.theme.caption
import kotlin.math.roundToInt

private const val DEFAULT_SPAN_KM = 50.0
private const val FRAME_MS = 900f
private const val FRAME_INTERVAL_MS = 33L
private const val AIR_GRID_OPACITY = 0.55f
private const val RADAR_OPACITY = 0.8f

@Composable
internal fun WeatherMap(centre: LatLon, here: LatLon?, home: LatLon, work: LatLon, mapTheme: MapTheme) {
    val format = AppTheme.format
    val strings = AppTheme.strings
    val pinColor = MaterialTheme.colorScheme.primary
    val routeColor = MaterialTheme.colorScheme.primary
    val homeColor = AppTheme.accents.dry
    val workColor = AppTheme.accents.rain
    val homeIcon = rememberVectorPainter(Icons.Rounded.Home)
    val workIcon = rememberVectorPainter(Icons.Rounded.Work)
    var layer by rememberSaveable { mutableStateOf(MapLayer.RAIN) }
    val radar by RadarAnimation.state.collectAsState()
    val radarUnavailable by RadarAnimation.unavailable.collectAsState()
    var gridFrames by remember { mutableStateOf<List<MapFrame>>(emptyList()) }
    var position by remember { mutableFloatStateOf(0f) }
    var playing by remember { mutableStateOf(true) }

    LaunchedEffect(layer, centre) {
        gridFrames = emptyList()
        position = 0f
        playing = true
        when (val air = airLayerFor(layer)) {
            null -> RadarAnimation.prepare()
            else -> gridFrames = AirQuality.around(centre)?.let { gridFrames(it, air, scaleFor(layer)) }.orEmpty()
        }
    }
    val frames = remember(layer, radar, gridFrames) {
        if (layer != MapLayer.RAIN) gridFrames
        else radar?.let { prepared ->
            prepared.times.indices.map { MapFrame(prepared.times[it], prepared.images[it], RadarImages.northWest, RadarImages.southEast) }
        }.orEmpty()
    }
    val motion = if (layer == MapLayer.RAIN) radar?.motion.orEmpty() else emptyList()

    LaunchedEffect(frames, playing) {
        if (frames.size < 2 || !playing) return@LaunchedEffect
        val count = frames.size - 1f
        var previous = withFrameNanos { it }
        while (true) {
            delay(FRAME_INTERVAL_MS)
            withFrameNanos { now ->
                position = (position + (now - previous) / 1_000_000f / FRAME_MS) % count
                previous = now
            }
        }
    }

    SectionCard(contentPadding = 0) {
        BoxWithConstraints(
            Modifier
                .fillMaxSize()
                .clip(CARD_SHAPE)
        ) {
            val density = LocalDensity.current.density
            val camera = rememberMapCamera(centre.lat, centre.lon, zoomFor(centre, constraints.maxWidth.toFloat(), density))
            LaunchedEffect(centre) { camera.moveTo(centre) }
            val showsAirGrid = layer != MapLayer.RAIN

            TileMap(
                camera = camera,
                source = mapTheme.source,
                darken = mapTheme.darken,
                markers = listOfNotNull(here?.let { MapMarker(it, pinColor) }),
                modifier = Modifier.fillMaxSize(),
                overlay = { project ->
                    if (frames.isNotEmpty()) {
                        val current = position.toInt().coerceIn(0, frames.lastIndex)
                        val next = frames.getOrNull(current + 1)
                        drawBlend(
                            frames[current], next, motion.getOrNull(current), position - current,
                            project(frames[current].northWest), project(frames[current].southEast),
                            opacity = if (showsAirGrid) AIR_GRID_OPACITY else RADAR_OPACITY
                        )
                    }
                    val from = project(home)
                    val to = project(work)
                    drawLine(routeColor.copy(alpha = 0.35f), from, to, strokeWidth = 8.dp.toPx(), cap = StrokeCap.Round)
                    drawLine(routeColor, from, to, strokeWidth = 3.5.dp.toPx(), cap = StrokeCap.Round)
                    drawPlaceBadge(from, homeColor, homeIcon)
                    drawPlaceBadge(to, workColor, workIcon)
                }
            )

            LayerPicker(layer, Modifier.align(Alignment.TopEnd).padding(10.dp)) { layer = it }

            if (showsAirGrid) Legend(scaleFor(layer), Modifier.align(Alignment.TopStart).padding(10.dp))

            ZoomPill(
                onZoomIn = { camera.zoomBy(1, snapToWhole = true) },
                onZoomOut = { camera.zoomBy(-1, snapToWhole = true) },
                modifier = Modifier.align(Alignment.BottomEnd).padding(end = 10.dp, bottom = 74.dp)
            )

            if (layer == MapLayer.RAIN && radar == null && !radarUnavailable) {
                CircularProgressIndicator(
                    strokeWidth = 2.5.dp,
                    modifier = Modifier.align(Alignment.TopStart).padding(16.dp).size(22.dp)
                )
            }

            if (frames.size > 1) {
                TimeScrubber(
                    position = { position },
                    timeAt = { at ->
                        val current = at.toInt().coerceIn(0, frames.lastIndex)
                        val next = frames.getOrNull(current + 1)
                        val timeMs = if (next == null) frames[current].timeMs
                        else frames[current].timeMs + ((next.timeMs - frames[current].timeMs) * (at - current)).toLong()
                        if (showsAirGrid && !format.isToday(timeMs)) "${format.dayShort(timeMs)} ${format.time(timeMs)}" else format.time(timeMs)
                    },
                    count = frames.size,
                    playing = playing,
                    onPlayToggle = { playing = !playing },
                    onScrub = { playing = false; position = it },
                    modifier = Modifier.align(Alignment.BottomCenter).padding(10.dp)
                )
            }
        }
    }
}

private fun DrawScope.drawPlaceBadge(at: Offset, color: Color, icon: Painter) {
    val radius = 13.dp.toPx()
    val iconSize = 16.dp.toPx()
    drawCircle(Color.Black.copy(alpha = 0.25f), radius + 1.dp.toPx(), at + Offset(0f, 1.dp.toPx()))
    drawCircle(color, radius, at)
    drawCircle(Color.White, radius, at, style = Stroke(width = 2.dp.toPx()))
    translate(at.x - iconSize / 2, at.y - iconSize / 2) {
        with(icon) { draw(Size(iconSize, iconSize), colorFilter = ColorFilter.tint(Color.White)) }
    }
}

@Composable
private fun ZoomPill(onZoomIn: () -> Unit, onZoomOut: () -> Unit, modifier: Modifier) {
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 4.dp,
        modifier = modifier.width(52.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            IconButton(onClick = onZoomIn, modifier = Modifier.size(52.dp)) {
                Icon(Icons.Rounded.Add, AppTheme.strings.zoomIn, tint = MaterialTheme.colorScheme.onSurface)
            }
            HorizontalDivider(Modifier.width(28.dp), color = MaterialTheme.colorScheme.outlineVariant)
            IconButton(onClick = onZoomOut, modifier = Modifier.size(52.dp)) {
                Icon(Icons.Rounded.Remove, AppTheme.strings.zoomOut, tint = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

@Composable
private fun LayerPicker(selected: MapLayer, modifier: Modifier, onSelect: (MapLayer) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 4.dp,
        modifier = modifier.width(52.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            IconButton(onClick = { open = !open }, modifier = Modifier.size(52.dp)) {
                Icon(Icons.Rounded.Layers, AppTheme.strings.layers, tint = MaterialTheme.colorScheme.onSurface)
            }
            AnimatedVisibility(visible = open, enter = expandVertically(), exit = shrinkVertically()) {
                Column(
                    Modifier.padding(bottom = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    for (option in MapLayer.entries) {
                        val active = option == selected
                        IconButton(
                            onClick = { onSelect(option); open = false },
                            modifier = Modifier
                                .size(42.dp)
                                .background(
                                    if (active) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
                                    CircleShape
                                )
                        ) {
                            Icon(
                                option.icon, option.label(AppTheme.strings),
                                tint = if (active) MaterialTheme.colorScheme.onSecondaryContainer
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TimeScrubber(
    position: () -> Float,
    timeAt: (Float) -> String,
    count: Int,
    playing: Boolean,
    onPlayToggle: () -> Unit,
    onScrub: (Float) -> Unit,
    modifier: Modifier
) {
    val strings = AppTheme.strings
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
        shadowElevation = 4.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(start = 4.dp, end = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onPlayToggle) {
                Icon(
                    if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    if (playing) strings.pause else strings.play
                )
            }
            val at = position()
            AppSlider(
                value = at,
                onValueChange = onScrub,
                valueRange = 0f..(count - 1).toFloat(),
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(12.dp))
            Text(
                timeAt(at),
                style = MaterialTheme.typography.caption.copy(fontFeatureSettings = "tnum"),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun Legend(scale: List<Pair<Double, Color>>, modifier: Modifier) {
    val strings = AppTheme.strings
    Row(
        modifier
            .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.85f), RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(strings.legendLow, style = MaterialTheme.typography.caption, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(6.dp))
        Box(
            Modifier
                .width(72.dp)
                .height(6.dp)
                .background(Brush.horizontalGradient(scale.map { it.second }), RoundedCornerShape(50))
        )
        Spacer(Modifier.width(6.dp))
        Text(strings.legendHigh, style = MaterialTheme.typography.caption, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun zoomFor(centre: LatLon, widthPx: Float, density: Float): Float =
    zoomToShow(centre.lat, DEFAULT_SPAN_KM, widthPx, density).roundToInt().toFloat()
