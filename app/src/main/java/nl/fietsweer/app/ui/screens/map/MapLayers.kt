package nl.fietsweer.app.ui.screens.map

import android.graphics.Bitmap
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Factory
import androidx.compose.material.icons.rounded.LocalFlorist
import androidx.compose.material.icons.rounded.Umbrella
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import nl.fietsweer.app.data.AirGrid
import nl.fietsweer.app.data.AirLayer
import nl.fietsweer.app.data.LatLon
import nl.fietsweer.app.domain.HOUR_MS
import nl.fietsweer.app.domain.Strings

private const val HOURS_SHOWN = 24

enum class MapLayer(val icon: ImageVector) {
    RAIN(Icons.Rounded.Umbrella),
    POLLEN(Icons.Rounded.LocalFlorist),
    AIR(Icons.Rounded.Factory),
    UV(Icons.Rounded.WbSunny);

    fun label(strings: Strings): String = when (this) {
        RAIN -> strings.layerRain
        POLLEN -> strings.layerPollen
        AIR -> strings.layerAir
        UV -> strings.layerUv
    }
}

internal class MapFrame(val timeMs: Long, val image: ImageBitmap, val northWest: LatLon, val southEast: LatLon)

// Colour stops from low to high, used both for the grid layers and their legend.
private val POLLEN_SCALE = listOf(0.0 to Color(0xFF4EB400), 0.3 to Color(0xFFF7E400), 0.7 to Color(0xFFF88700), 1.2 to Color(0xFFD8001D))
private val AIR_SCALE = listOf(
    0.0 to Color(0xFF50F0E6), 20.0 to Color(0xFF50CCAA), 40.0 to Color(0xFFF0E641),
    60.0 to Color(0xFFFF5050), 80.0 to Color(0xFF960032), 100.0 to Color(0xFF7D2181)
)
private val UV_SCALE = listOf(0.0 to Color(0xFF4EB400), 3.0 to Color(0xFFF7E400), 6.0 to Color(0xFFF88700), 8.0 to Color(0xFFD8001D), 11.0 to Color(0xFF998CFF))

internal fun scaleFor(layer: MapLayer) = when (layer) {
    MapLayer.POLLEN -> POLLEN_SCALE
    MapLayer.AIR -> AIR_SCALE
    MapLayer.UV -> UV_SCALE
    MapLayer.RAIN -> emptyList()
}

internal fun airLayerFor(layer: MapLayer) = when (layer) {
    MapLayer.POLLEN -> AirLayer.POLLEN
    MapLayer.AIR -> AirLayer.AIR
    MapLayer.UV -> AirLayer.UV
    MapLayer.RAIN -> null
}

internal fun gridFrames(grid: AirGrid, layer: AirLayer, scale: List<Pair<Double, Color>>): List<MapFrame> {
    val hours = grid.layers[layer] ?: return emptyList()
    val now = System.currentTimeMillis()
    return grid.times.indices
        .filter { grid.times[it] >= now - HOUR_MS }
        .take(HOURS_SHOWN)
        .map { hour ->
            val values = hours[hour]
            val pixels = IntArray(values.size) { colourAt(values[it], scale).toArgb() }
            val bitmap = Bitmap.createBitmap(pixels, grid.size, grid.size, Bitmap.Config.ARGB_8888)
            MapFrame(grid.times[hour], bitmap.asImageBitmap(), grid.northWest, grid.southEast)
        }
}

private fun colourAt(value: Double, scale: List<Pair<Double, Color>>): Color {
    if (value.isNaN()) return Color.Transparent
    val upper = scale.indexOfFirst { it.first > value }
    if (upper == -1) return scale.last().second
    if (upper == 0) return scale.first().second
    val (lowValue, lowColor) = scale[upper - 1]
    val (highValue, highColor) = scale[upper]
    val fraction = ((value - lowValue) / (highValue - lowValue)).toFloat()
    return Color(
        lowColor.red + (highColor.red - lowColor.red) * fraction,
        lowColor.green + (highColor.green - lowColor.green) * fraction,
        lowColor.blue + (highColor.blue - lowColor.blue) * fraction
    )
}
