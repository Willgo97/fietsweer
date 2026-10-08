package nl.fietsweer.app.data

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONObject
import java.time.LocalDateTime
import java.time.ZoneOffset

class RadarFrame(val timeMs: Long, val bitmap: Bitmap)

// Buienradar's web-mercator radar of the Netherlands: past 50 minutes and two hours ahead,
// every 10 minutes, as images that line up with OpenStreetMap tiles.
object RadarImages {

    val northWest = LatLon(54.8, 0.0)
    val southEast = LatLon(49.5, 10.0)

    private const val FRESH_MS = 5 * 60_000L
    private const val METADATA_URL = "https://image.buienradar.nl/2.0/metadata/sprite/RadarMapRainWebmercatorNL" +
        "?width=700&height=606&extension=png&renderBackground=false&renderText=false&renderBranding=false" +
        "&history=6&forecast=12&skip=1"

    private val lock = Mutex()
    private var cached: List<RadarFrame> = emptyList()
    private var fetchedAt = 0L

    suspend fun frames(): List<RadarFrame> = lock.withLock {
        if (cached.isNotEmpty() && System.currentTimeMillis() - fetchedAt < FRESH_MS) return cached
        val fresh = runCatching { download() }.getOrDefault(emptyList())
        if (fresh.isNotEmpty()) {
            cached = fresh
            fetchedAt = System.currentTimeMillis()
        }
        cached
    }

    private suspend fun download(): List<RadarFrame> = coroutineScope {
        val times = JSONObject(Net.getText(METADATA_URL)).getJSONArray("times")
        (0 until times.length()).map { i ->
            val entry = times.getJSONObject(i)
            async(Dispatchers.IO) {
                runCatching {
                    val bytes = Net.blockingBytes(entry.getString("url"), Net.IMAGE_TIMEOUT_MS)
                    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return@runCatching null
                    RadarFrame(utcMillis(entry.getString("timestamp")), bitmap)
                }.getOrNull()
            }
        }.awaitAll().filterNotNull().sortedBy { it.timeMs }
    }

    private fun utcMillis(timestamp: String): Long =
        LocalDateTime.parse(timestamp).toInstant(ZoneOffset.UTC).toEpochMilli()
}
