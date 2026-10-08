package nl.fietsweer.app.data

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import kotlin.math.cos

enum class AirLayer { POLLEN, AIR, UV }

// Values on a regular grid around a centre, one array of grid values per hour.
class AirGrid(
    val centre: LatLon,
    val northWest: LatLon,
    val southEast: LatLon,
    val size: Int,
    val times: LongArray,
    val layers: Map<AirLayer, List<DoubleArray>>
)

object AirQuality {

    private const val GRID = 9
    private const val SPAN_KM = 160.0
    private const val FRESH_MS = 30 * 60_000L
    private const val MOVED_KM = 20.0

    // Grains per m³ at which each pollen type counts as "high"; the layer shows the worst ratio.
    private val POLLEN_HIGH = mapOf(
        "alder_pollen" to 100.0, "birch_pollen" to 100.0, "grass_pollen" to 50.0,
        "mugwort_pollen" to 50.0, "ragweed_pollen" to 20.0, "olive_pollen" to 100.0
    )

    private val lock = Mutex()
    private var cached: AirGrid? = null
    private var fetchedAt = 0L

    suspend fun around(centre: LatLon): AirGrid? = lock.withLock {
        val previous = cached
        if (previous != null && System.currentTimeMillis() - fetchedAt < FRESH_MS &&
            Geo.haversineKm(previous.centre, centre) < MOVED_KM
        ) return previous
        val fresh = runCatching { download(centre) }.getOrNull()
        if (fresh != null) {
            cached = fresh
            fetchedAt = System.currentTimeMillis()
        }
        fresh ?: previous
    }

    private suspend fun download(centre: LatLon): AirGrid {
        val latStep = SPAN_KM / 111.0 / (GRID - 1)
        val lonStep = SPAN_KM / (111.0 * cos(Math.toRadians(centre.lat))) / (GRID - 1)
        val north = centre.lat + latStep * (GRID - 1) / 2
        val west = centre.lon - lonStep * (GRID - 1) / 2
        val points = (0 until GRID).flatMap { row ->
            (0 until GRID).map { column -> LatLon(north - row * latStep, west + column * lonStep) }
        }
        val variables = (listOf("european_aqi", "uv_index") + POLLEN_HIGH.keys).joinToString(",")
        val url = "https://air-quality-api.open-meteo.com/v1/air-quality" +
            "?latitude=${points.joinToString(",") { "%.3f".format(java.util.Locale.US, it.lat) }}" +
            "&longitude=${points.joinToString(",") { "%.3f".format(java.util.Locale.US, it.lon) }}" +
            "&hourly=$variables&forecast_days=2&timeformat=unixtime"
        val locations = JSONArray(Net.getText(url))
        val first = locations.getJSONObject(0).getJSONObject("hourly")
        val times = first.getJSONArray("time").let { array -> LongArray(array.length()) { array.getLong(it) * 1000 } }

        fun series(index: Int, key: String): JSONArray? = locations.getJSONObject(index).getJSONObject("hourly").optJSONArray(key)
        fun valueOf(array: JSONArray?, hour: Int): Double =
            if (array == null || array.isNull(hour)) Double.NaN else array.optDouble(hour, Double.NaN)

        val air = ArrayList<DoubleArray>(times.size)
        val uv = ArrayList<DoubleArray>(times.size)
        val pollen = ArrayList<DoubleArray>(times.size)
        val aqiSeries = points.indices.map { series(it, "european_aqi") }
        val uvSeries = points.indices.map { series(it, "uv_index") }
        val pollenSeries = points.indices.map { i -> POLLEN_HIGH.map { (key, high) -> series(i, key) to high } }
        for (hour in times.indices) {
            air += DoubleArray(points.size) { valueOf(aqiSeries[it], hour) }
            uv += DoubleArray(points.size) { valueOf(uvSeries[it], hour) }
            pollen += DoubleArray(points.size) { i ->
                pollenSeries[i].maxOf { (array, high) -> valueOf(array, hour).takeIf { !it.isNaN() }?.div(high) ?: 0.0 }
            }
        }
        return AirGrid(
            centre = centre,
            northWest = LatLon(north + latStep / 2, west - lonStep / 2),
            southEast = LatLon(north - latStep * (GRID - 0.5), west + lonStep * (GRID - 0.5)),
            size = GRID,
            times = times,
            layers = mapOf(AirLayer.AIR to air, AirLayer.UV to uv, AirLayer.POLLEN to pollen)
        )
    }
}
