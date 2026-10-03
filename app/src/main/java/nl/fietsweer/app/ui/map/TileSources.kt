package nl.fietsweer.app.ui.map

import android.content.Context
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import nl.fietsweer.app.data.Net
import java.io.File
import java.util.Collections

enum class TileSource(
    val id: String,
    val template: String,
    val maxZoom: Int,
    val attribution: String
) {
    OSM(
        "osm", "https://tile.openstreetmap.org/{z}/{x}/{y}.png", 19,
        "\u00a9 OpenStreetMap contributors"
    ),

    OSM_HOT(
        "hot", "https://tile-a.openstreetmap.fr/hot/{z}/{x}/{y}.png", 19,
        "\u00a9 OpenStreetMap contributors \u00b7 HOT"
    );

    fun url(zoom: Int, x: Int, y: Int): String =
        template.replace("{z}", zoom.toString()).replace("{x}", x.toString()).replace("{y}", y.toString())
}

data class MapTheme(val source: TileSource, val darken: Boolean) {
    val attribution: String get() = source.attribution
}

fun mapThemeFor(style: nl.fietsweer.app.data.MapStyle, dark: Boolean): MapTheme =
    when (style) {
        nl.fietsweer.app.data.MapStyle.SOFT -> MapTheme(TileSource.OSM_HOT, false)
        nl.fietsweer.app.data.MapStyle.LIGHT -> MapTheme(TileSource.OSM, false)
        nl.fietsweer.app.data.MapStyle.DARK -> MapTheme(TileSource.OSM, true)
        nl.fietsweer.app.data.MapStyle.AUTO -> MapTheme(TileSource.OSM, dark)
    }

object TileLoader {

    private const val MAX_MEMORY_TILES = 220
    private val memory = object : LruCache<String, ImageBitmap>(MAX_MEMORY_TILES) {}
    private val inFlight = Collections.synchronizedSet(HashSet<String>())
    private val failed = Collections.synchronizedSet(HashSet<String>())
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private fun cacheKey(source: TileSource, zoom: Int, x: Int, y: Int) = "${source.id}/$zoom/$x/$y"

    fun cached(source: TileSource, zoom: Int, x: Int, y: Int): ImageBitmap? =
        memory.get(cacheKey(source, zoom, x, y))

    fun request(
        context: Context,
        source: TileSource,
        zoom: Int,
        x: Int,
        y: Int,
        onLoaded: () -> Unit
    ) {
        val key = cacheKey(source, zoom, x, y)
        if (memory.get(key) != null || key in failed || !inFlight.add(key)) return
        val appContext = context.applicationContext
        scope.launch {
            try {
                val file = File(appContext.cacheDir, "tiles/$key.png")
                val bytes = if (file.exists() && file.length() > 0) {
                    file.readBytes()
                } else {
                    val downloaded = Net.blockingBytes(source.url(zoom, x, y), 15_000)
                    runCatching {
                        file.parentFile?.mkdirs()
                        file.writeBytes(downloaded)
                    }
                    downloaded
                }
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                if (bitmap != null) {
                    memory.put(key, bitmap.asImageBitmap())
                    onLoaded()
                } else {
                    failed.add(key)
                }
            } catch (e: Throwable) {
                failed.add(key)
            } finally {
                inFlight.remove(key)
            }
        }
    }

    fun clearFailures() = failed.clear()
}
