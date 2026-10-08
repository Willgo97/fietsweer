package nl.fietsweer.app.data

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import nl.fietsweer.app.domain.Geo
import nl.fietsweer.app.domain.LatLon
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.time.Duration
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.math.pow

enum class SourceState { OK, EMPTY, FAILED, SKIPPED }

data class SourceStatus(val label: String, val state: SourceState, val note: String = "")

class ModelSeries(
    val label: String,
    val precipitationPerPoint: List<DoubleArray>
)

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

    val sources: List<SourceStatus>,
    val fetchedAt: Long
) {
    val hasModels: Boolean get() = models.isNotEmpty() && modelTimes.isNotEmpty()

    val allSourcesFailed: Boolean
        get() = sources.isNotEmpty() && sources.none { it.state == SourceState.OK }
}

object WeatherApi {

    private const val SAMPLE_POINTS = 4

    private val MODELS: List<Pair<String, String>> = listOf(
        "knmi_harmonie_arome_netherlands" to "KNMI Harmonie 2 km",
        "knmi_seamless" to "KNMI seamless",
        "dwd_icon_d2" to "DWD ICON-D2 2 km",
        "dwd_icon_eu" to "DWD ICON-EU",
        "meteofrance_arome_france_hd" to "Météo-France AROME",
        "meteofrance_seamless" to "Météo-France seamless",
        "ecmwf_ifs025" to "ECMWF IFS",
        "ecmwf_aifs025_single" to "ECMWF AIFS (AI)",
        "ukmo_uk_deterministic_2km" to "UK Met Office 2 km",
        "ukmo_seamless" to "UK Met Office seamless",
        "gfs_seamless" to "NOAA GFS",
        "gem_seamless" to "ECCC GEM",
        "jma_seamless" to "JMA",
        "metno_seamless" to "MET Norway",
        "cma_grapes_global" to "CMA GRAPES"
    )

    private val timezoneParameter: String
        get() = URLEncoder.encode(ZoneId.systemDefault().id, "UTF-8")

    suspend fun fetch(home: Place, work: Place, useRadar: Boolean, pointCount: Int = SAMPLE_POINTS): RouteForecast = coroutineScope {
        val points = Geo.samplePoints(home.toLatLon(), work.toLatLon(), pointCount)
        val midpoint = points[points.size / 2]
        val distance = Geo.routeKm(home.toLatLon(), work.toLatLon())

        val latitudes = points.joinToString(",") { coordinate(it.lat) }
        val longitudes = points.joinToString(",") { coordinate(it.lon) }
        val modelIds = MODELS.joinToString(",") { it.first }

        val modelsUrl = "https://api.open-meteo.com/v1/forecast" +
            "?latitude=$latitudes&longitude=$longitudes" +
            "&minutely_15=precipitation" +
            "&models=$modelIds" +
            "&forecast_days=4&timeformat=unixtime&timezone=$timezoneParameter"

        val ensembleUrl = "https://ensemble-api.open-meteo.com/v1/ensemble" +
            "?latitude=${coordinate(midpoint.lat)}&longitude=${coordinate(midpoint.lon)}" +
            "&hourly=precipitation" +
            "&models=icon_d2,ecmwf_ifs025" +
            "&forecast_days=4&timeformat=unixtime&timezone=$timezoneParameter"

        val radarWanted = useRadar && inBenelux(midpoint)
        val radarUrl = "https://gpsgadget.buienradar.nl/data/raintext" +
            "?lat=${"%.2f".format(java.util.Locale.US, midpoint.lat)}" +
            "&lon=${"%.2f".format(java.util.Locale.US, midpoint.lon)}"

        val modelsRequest = async { runCatching { Net.getText(modelsUrl) } }
        val ensembleRequest = async { runCatching { Net.getText(ensembleUrl) } }
        val conditionsRequest = async { runCatching { conditions(midpoint) } }
        val radarRequest = async {
            if (radarWanted) runCatching { Net.getText(radarUrl, 12_000) } else null
        }

        val sources = mutableListOf<SourceStatus>()

        var modelTimes = LongArray(0)
        val models = mutableListOf<ModelSeries>()
        modelsRequest.await().onSuccess { body ->
            runCatching {
                val perPoint = JSONArray(body)
                val blocks = (0 until perPoint.length()).map { perPoint.getJSONObject(it).getJSONObject("minutely_15") }
                modelTimes = blocks[0].getJSONArray("time").toMillis()
                var missing = 0
                for ((id, label) in MODELS) {
                    val key = "precipitation_$id"
                    val series = blocks.map { it.optJSONArray(key)?.toDoubles() }
                    if (series.any { it == null } || series.any { values -> values!!.none { !it.isNaN() } }) {
                        missing++
                        continue
                    }
                    @Suppress("UNCHECKED_CAST")
                    models += ModelSeries(label, series as List<DoubleArray>)
                }
                sources += SourceStatus(
                    "Open-Meteo · ${models.size} weather models",
                    if (models.isEmpty()) SourceState.EMPTY else SourceState.OK,
                    if (missing > 0) "$missing not available here" else ""
                )
            }.onFailure {
                sources += SourceStatus("Open-Meteo · weather models", SourceState.FAILED, it.shortMessage())
            }
        }.onFailure {
            sources += SourceStatus("Open-Meteo · weather models", SourceState.FAILED, it.shortMessage())
        }

        var ensembleTimes = LongArray(0)
        val ensembleMembers = mutableListOf<DoubleArray>()
        ensembleRequest.await().onSuccess { body ->
            runCatching {
                val hourly = JSONObject(body).getJSONObject("hourly")
                ensembleTimes = hourly.getJSONArray("time").toMillis()
                val memberKeys = hourly.keys().asSequence().filter { it.contains("member") }.toList()
                for (key in memberKeys) hourly.optJSONArray(key)?.toDoubles()?.let { ensembleMembers += it }
                sources += SourceStatus(
                    "Ensembles ICON-D2 + ECMWF · ${ensembleMembers.size} members",
                    if (ensembleMembers.isEmpty()) SourceState.EMPTY else SourceState.OK
                )
            }.onFailure {
                sources += SourceStatus("Ensembles ICON-D2 + ECMWF", SourceState.FAILED, it.shortMessage())
            }
        }.onFailure {
            sources += SourceStatus("Ensembles ICON-D2 + ECMWF", SourceState.FAILED, it.shortMessage())
        }

        var conditions: Conditions? = null
        conditionsRequest.await().onSuccess {
            conditions = it
            sources += SourceStatus("Open-Meteo · temperature & wind", SourceState.OK)
        }.onFailure {
            sources += SourceStatus("Open-Meteo · temperature & wind", SourceState.FAILED, it.shortMessage())
        }

        val radar = mutableListOf<RadarSample>()
        val radarResult = radarRequest.await()
        if (radarResult == null) {
            sources += SourceStatus(
                "Buienradar rain radar",
                SourceState.SKIPPED,
                if (!useRadar) "switched off" else "outside coverage"
            )
        } else {
            radarResult.onSuccess { text ->
                radar += parseRadar(text)
                sources += SourceStatus(
                    "Buienradar rain radar",
                    if (radar.isEmpty()) SourceState.EMPTY else SourceState.OK
                )
            }.onFailure {
                sources += SourceStatus("Buienradar rain radar", SourceState.FAILED, it.shortMessage())
            }
        }

        val weather = conditions ?: Conditions.NONE
        RouteForecast(
            home = home, work = work, points = points, distanceKm = distance,
            modelTimes = modelTimes, models = models,
            ensembleTimes = ensembleTimes, ensembleMembers = ensembleMembers,
            quarterHourTimes = weather.quarterHourTimes, quarterHourly = weather.quarterHourly,
            hourlyTimes = weather.hourlyTimes, hourly = weather.hourly,
            dailyTimes = weather.dailyTimes, daily = weather.daily,
            current = weather.current, radar = radar,
            sources = sources, fetchedAt = System.currentTimeMillis()
        )
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
        "temperature_2m,apparent_temperature,precipitation,precipitation_probability," +
            "weather_code,wind_speed_10m,wind_direction_10m,wind_gusts_10m," +
            "cloud_cover,relative_humidity_2m,is_day"

    private const val DAILY_VARIABLES =
        "weather_code,temperature_2m_max,temperature_2m_min,apparent_temperature_min," +
            "apparent_temperature_max,precipitation_sum,precipitation_hours," +
            "precipitation_probability_max,wind_speed_10m_max,wind_gusts_10m_max," +
            "wind_direction_10m_dominant,sunrise,sunset"

    private const val CURRENT_VARIABLES =
        "temperature_2m,apparent_temperature,precipitation,weather_code," +
            "wind_speed_10m,wind_direction_10m,wind_gusts_10m,relative_humidity_2m,is_day"

    private const val QUARTER_HOUR_VARIABLES =
        "temperature_2m,apparent_temperature,precipitation,wind_speed_10m," +
            "wind_direction_10m,wind_gusts_10m"

    private suspend fun conditions(midpoint: LatLon): Conditions {
        val baseUrl = "https://api.open-meteo.com/v1/forecast" +
            "?latitude=${coordinate(midpoint.lat)}&longitude=${coordinate(midpoint.lon)}" +
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
        val zone = ZoneId.of("Europe/Amsterdam")
        val now = ZonedDateTime.now(zone)
        val linePattern = Regex("""^(\d{1,3})\|(\d{2}):(\d{2})$""")
        val samples = mutableListOf<RadarSample>()
        var previous: ZonedDateTime? = null
        for (line in text.trim().lines()) {
            val match = linePattern.matchEntire(line.trim()) ?: continue
            var time = now.withHour(match.groupValues[2].toInt())
                .withMinute(match.groupValues[3].toInt())
                .withSecond(0).withNano(0)
            val before = previous
            if (before != null && time.isBefore(before)) time = time.plusDays(1)
            else if (before == null && Duration.between(time, now).toMinutes() > 180) time = time.plusDays(1)
            previous = time
            val level = match.groupValues[1].toInt()
            val mmPerHour = if (level <= 0) 0.0 else 10.0.pow((level - 109) / 32.0)
            samples += RadarSample(time.toInstant().toEpochMilli(), mmPerHour)
        }
        return samples
    }

    private fun inBenelux(point: LatLon): Boolean =
        point.lat in 48.5..55.5 && point.lon in 1.5..9.5

    private fun coordinate(degrees: Double) = String.format(java.util.Locale.US, "%.4f", degrees)

    private fun Throwable.shortMessage(): String =
        (message ?: this::class.java.simpleName).take(60)

    private fun JSONArray.toMillis(): LongArray =
        LongArray(length()) { optLong(it) * 1000L }

    private fun JSONArray.toDoubles(): DoubleArray =
        DoubleArray(length()) { if (isNull(it)) Double.NaN else optDouble(it, Double.NaN) }

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
