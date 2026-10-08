package nl.fietsweer.app.data

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.time.Duration
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.math.pow

class ModelSeries(val precipitationPerPoint: List<DoubleArray>)

data class RadarSample(val timeMs: Long, val mmPerHour: Double)

class RouteForecast(
    val home: Place,
    val work: Place,
    val points: List<LatLon>,
    val distanceKm: Double,

    val modelTimes: LongArray,
    val models: List<ModelSeries>,

    val ensembleTimes: LongArray,
    val ensembleMembers: List<DoubleArray>,

    val quarterHourTimes: LongArray,
    val quarterHourly: Map<String, DoubleArray>,

    val hourlyTimes: LongArray,
    val hourly: Map<String, DoubleArray>,

    val dailyTimes: LongArray,
    val daily: Map<String, DoubleArray>,

    val current: Map<String, Double>,
    val radar: List<RadarSample>,

    val allSourcesFailed: Boolean,
    val fetchedAt: Long
) {
    val hasModels: Boolean get() = models.isNotEmpty() && modelTimes.isNotEmpty()
}

object WeatherApi {

    private const val SAMPLE_POINTS = 4

    private val MODEL_IDS = listOf(
        "knmi_harmonie_arome_netherlands",
        "knmi_seamless",
        "dwd_icon_d2",
        "dwd_icon_eu",
        "meteofrance_arome_france_hd",
        "meteofrance_seamless",
        "ecmwf_ifs025",
        "ecmwf_aifs025_single",
        "ukmo_uk_deterministic_2km",
        "ukmo_seamless",
        "gfs_seamless",
        "gem_seamless",
        "jma_seamless",
        "metno_seamless",
        "cma_grapes_global"
    )

    private const val MAX_RADAR_PAST_MINUTES = 180
    private val RADAR_ZONE: ZoneId = ZoneId.of("Europe/Amsterdam")
    private val RADAR_LINE = Regex("""^(\d{1,3})\|(\d{2}):(\d{2})$""")

    private val timezoneParameter: String
        get() = URLEncoder.encode(ZoneId.systemDefault().id, "UTF-8")

    suspend fun fetch(home: Place, work: Place, useRadar: Boolean, pointCount: Int = SAMPLE_POINTS): RouteForecast = coroutineScope {
        val points = Geo.samplePoints(home.toLatLon(), work.toLatLon(), pointCount)
        val midpoint = points[points.size / 2]
        val distance = Geo.routeKm(home.toLatLon(), work.toLatLon())

        val latitudes = points.joinToString(",") { it.lat.toUrlDegrees(4) }
        val longitudes = points.joinToString(",") { it.lon.toUrlDegrees(4) }

        val modelsUrl = "https://api.open-meteo.com/v1/forecast" +
            "?latitude=$latitudes&longitude=$longitudes" +
            "&minutely_15=precipitation" +
            "&models=${MODEL_IDS.joinToString(",")}" +
            "&forecast_days=4&timeformat=unixtime&timezone=$timezoneParameter"

        val ensembleUrl = "https://ensemble-api.open-meteo.com/v1/ensemble" +
            "?latitude=${midpoint.lat.toUrlDegrees(4)}&longitude=${midpoint.lon.toUrlDegrees(4)}" +
            "&hourly=precipitation" +
            "&models=icon_d2,ecmwf_ifs025" +
            "&forecast_days=4&timeformat=unixtime&timezone=$timezoneParameter"

        val radarWanted = useRadar && inBenelux(midpoint)
        val radarUrl = "https://gpsgadget.buienradar.nl/data/raintext" +
            "?lat=${midpoint.lat.toUrlDegrees(2)}&lon=${midpoint.lon.toUrlDegrees(2)}"

        val modelsRequest = async { runCatching { parseModels(Net.getText(modelsUrl)) } }
        val ensembleRequest = async { runCatching { parseEnsemble(Net.getText(ensembleUrl)) } }
        val conditionsRequest = async { runCatching { conditions(midpoint) } }
        val radarRequest = async {
            if (radarWanted) runCatching { Net.getText(radarUrl, Net.QUICK_TIMEOUT_MS) } else null
        }

        val (modelTimes, models) = modelsRequest.await().getOrNull() ?: (LongArray(0) to emptyList())
        val (ensembleTimes, ensembleMembers) = ensembleRequest.await().getOrNull() ?: (LongArray(0) to emptyList())
        val conditions = conditionsRequest.await().getOrNull()
        val radar = radarRequest.await()?.getOrNull()?.let(::parseRadar).orEmpty()

        val weather = conditions ?: Conditions.NONE
        RouteForecast(
            home = home, work = work, points = points, distanceKm = distance,
            modelTimes = modelTimes, models = models,
            ensembleTimes = ensembleTimes, ensembleMembers = ensembleMembers,
            quarterHourTimes = weather.quarterHourTimes, quarterHourly = weather.quarterHourly,
            hourlyTimes = weather.hourlyTimes, hourly = weather.hourly,
            dailyTimes = weather.dailyTimes, daily = weather.daily,
            current = weather.current, radar = radar,
            allSourcesFailed = models.isEmpty() && ensembleMembers.isEmpty() && conditions == null && radar.isEmpty(),
            fetchedAt = System.currentTimeMillis()
        )
    }

    // Models without data at this location are left out rather than counted as dry.
    private fun parseModels(body: String): Pair<LongArray, List<ModelSeries>> {
        val perPoint = JSONArray(body)
        val blocks = (0 until perPoint.length()).map { perPoint.getJSONObject(it).getJSONObject("minutely_15") }
        val times = blocks[0].getJSONArray("time").toMillis()
        val models = MODEL_IDS.mapNotNull { id ->
            val series = blocks.map { it.optJSONArray("precipitation_$id")?.toDoubles() }
            if (series.any { it == null || it.all(Double::isNaN) }) null else ModelSeries(series.filterNotNull())
        }
        return times to models
    }

    private fun parseEnsemble(body: String): Pair<LongArray, List<DoubleArray>> {
        val hourly = JSONObject(body).getJSONObject("hourly")
        val times = hourly.getJSONArray("time").toMillis()
        val members = hourly.keys().asSequence()
            .filter { it.contains("member") }
            .mapNotNull { hourly.optJSONArray(it)?.toDoubles() }
            .toList()
        return times to members
    }

    private class Conditions(
        val quarterHourTimes: LongArray, val quarterHourly: Map<String, DoubleArray>,
        val hourlyTimes: LongArray, val hourly: Map<String, DoubleArray>,
        val dailyTimes: LongArray, val daily: Map<String, DoubleArray>,
        val current: Map<String, Double>
    ) {
        companion object {
            val NONE = Conditions(
                LongArray(0), emptyMap(), LongArray(0), emptyMap(), LongArray(0), emptyMap(), emptyMap()
            )
        }
    }

    private const val HOURLY_VARIABLES =
        "temperature_2m,apparent_temperature,wind_speed_10m,wind_direction_10m,wind_gusts_10m"

    private const val DAILY_VARIABLES =
        "weather_code,temperature_2m_max,temperature_2m_min,precipitation_sum," +
            "precipitation_probability_max,wind_speed_10m_max,wind_direction_10m_dominant,sunrise,sunset"

    private const val CURRENT_VARIABLES =
        "temperature_2m,apparent_temperature,precipitation,wind_speed_10m,wind_direction_10m"

    private const val QUARTER_HOUR_VARIABLES =
        "temperature_2m,apparent_temperature,wind_speed_10m,wind_direction_10m,wind_gusts_10m"

    private suspend fun conditions(midpoint: LatLon): Conditions {
        val baseUrl = "https://api.open-meteo.com/v1/forecast" +
            "?latitude=${midpoint.lat.toUrlDegrees(4)}&longitude=${midpoint.lon.toUrlDegrees(4)}" +
            "&hourly=$HOURLY_VARIABLES&daily=$DAILY_VARIABLES&current=$CURRENT_VARIABLES" +
            "&forecast_days=14&timeformat=unixtime&timezone=$timezoneParameter"

        // minutely_15 makes some models fail the whole request; fall back to hourly.
        val body = runCatching { Net.getText("$baseUrl&minutely_15=$QUARTER_HOUR_VARIABLES") }
            .getOrElse { Net.getText(baseUrl) }

        val response = JSONObject(body)
        val quarterHourBlock = response.optJSONObject("minutely_15")
        val hourlyBlock = response.getJSONObject("hourly")
        val dailyBlock = response.getJSONObject("daily")
        val currentBlock = response.optJSONObject("current")

        return Conditions(
            quarterHourTimes = quarterHourBlock?.optJSONArray("time")?.toMillis() ?: LongArray(0),
            quarterHourly = quarterHourBlock?.toSeriesMap() ?: emptyMap(),
            hourlyTimes = hourlyBlock.getJSONArray("time").toMillis(),
            hourly = hourlyBlock.toSeriesMap(),
            dailyTimes = dailyBlock.getJSONArray("time").toMillis(),
            daily = dailyBlock.toSeriesMap(includeSunTimes = true),
            current = buildMap {
                currentBlock ?: return@buildMap
                for (key in currentBlock.keys()) {
                    val value = currentBlock.opt(key)
                    if (value is Number) put(key, value.toDouble())
                }
            }
        )
    }

    // Lines like `000|16:50`: 0-255 log scale, Dutch local time, rolls past midnight.
    private fun parseRadar(text: String): List<RadarSample> {
        val now = ZonedDateTime.now(RADAR_ZONE)
        val samples = mutableListOf<RadarSample>()
        var previous: ZonedDateTime? = null
        for (line in text.trim().lines()) {
            val match = RADAR_LINE.matchEntire(line.trim()) ?: continue
            var time = now.withHour(match.groupValues[2].toInt())
                .withMinute(match.groupValues[3].toInt())
                .withSecond(0).withNano(0)
            val before = previous
            if (before != null && time.isBefore(before)) time = time.plusDays(1)
            else if (before == null && Duration.between(time, now).toMinutes() > MAX_RADAR_PAST_MINUTES) time = time.plusDays(1)
            previous = time
            val level = match.groupValues[1].toInt()
            val mmPerHour = if (level <= 0) 0.0 else 10.0.pow((level - 109) / 32.0)
            samples += RadarSample(time.toInstant().toEpochMilli(), mmPerHour)
        }
        return samples
    }

    private fun inBenelux(point: LatLon): Boolean =
        point.lat in 48.5..55.5 && point.lon in 1.5..9.5

    private fun JSONObject.toSeriesMap(includeSunTimes: Boolean = false): Map<String, DoubleArray> {
        val series = LinkedHashMap<String, DoubleArray>()
        for (key in keys()) {
            if (key == "time") continue
            val values = optJSONArray(key) ?: continue
            if (!includeSunTimes && (key == "sunrise" || key == "sunset")) continue
            series[key] = values.toDoubles()
        }
        return series
    }
}
