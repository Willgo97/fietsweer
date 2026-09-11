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

/** One weather model's precipitation series, sampled at every route point. */
class ModelSeries(
    val id: String,
    val label: String,
    val perPoint: List<DoubleArray>
)

data class RadarSample(val timeMs: Long, val mmPerHour: Double)

/**
 * Everything the analysis needs for one home/work pair. The route is
 * direction-agnostic: the same sampled points serve both legs, walked
 * forwards or backwards.
 */
class RouteForecast(
    val home: Place,
    val work: Place,
    val points: List<LatLon>,
    val distanceKm: Double,

    val modelTimes: LongArray,
    val models: List<ModelSeries>,

    val ensTimes: LongArray,
    val ensMembers: List<DoubleArray>,

    val fineTimes: LongArray,
    val fine: Map<String, DoubleArray>,

    val hourTimes: LongArray,
    val hourly: Map<String, DoubleArray>,

    val dayTimes: LongArray,
    val daily: Map<String, DoubleArray>,

    val current: Map<String, Double>,
    val radar: List<RadarSample>,

    val sources: List<SourceStatus>,
    val fetchedAt: Long
) {
    val hasModels: Boolean get() = models.isNotEmpty() && modelTimes.isNotEmpty()

    /** Every source we asked came back empty or failed — almost always offline. */
    val allSourcesFailed: Boolean
        get() = sources.isNotEmpty() && sources.none { it.state == SourceState.OK }

    /** Latest timestamp for which the deterministic models still have data. */
    val modelHorizonMs: Long get() = modelTimes.lastOrNull() ?: 0L
}

object WeatherApi {

    private const val SAMPLE_POINTS = 4

    /** Every deterministic model Open-Meteo serves; unavailable ones drop out. */
    val MODELS: List<Pair<String, String>> = listOf(
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

    private val zoneParam: String
        get() = URLEncoder.encode(ZoneId.systemDefault().id, "UTF-8")

    // ------------------------------------------------------------------ fetch

    suspend fun fetch(home: Place, work: Place, useRadar: Boolean): RouteForecast = coroutineScope {
        val points = Geo.samplePoints(home.toLatLon(), work.toLatLon(), SAMPLE_POINTS)
        val mid = points[points.size / 2]
        val distance = Geo.haversineKm(home.toLatLon(), work.toLatLon()) * Geo.DETOUR_FACTOR

        val lats = points.joinToString(",") { fmt(it.lat) }
        val lons = points.joinToString(",") { fmt(it.lon) }
        val ids = MODELS.joinToString(",") { it.first }

        val urlModels = "https://api.open-meteo.com/v1/forecast" +
            "?latitude=$lats&longitude=$lons" +
            "&minutely_15=precipitation" +
            "&models=$ids" +
            "&forecast_days=2&timeformat=unixtime&timezone=$zoneParam"

        val urlEnsemble = "https://ensemble-api.open-meteo.com/v1/ensemble" +
            "?latitude=${fmt(mid.lat)}&longitude=${fmt(mid.lon)}" +
            "&hourly=precipitation" +
            "&models=icon_d2,ecmwf_ifs025" +
            "&forecast_days=2&timeformat=unixtime&timezone=$zoneParam"

        val radarWanted = useRadar && inBenelux(mid)
        val urlRadar = "https://gpsgadget.buienradar.nl/data/raintext" +
            "?lat=${"%.2f".format(java.util.Locale.US, mid.lat)}" +
            "&lon=${"%.2f".format(java.util.Locale.US, mid.lon)}"

        val dModels = async { runCatching { Net.getText(urlModels) } }
        val dEns = async { runCatching { Net.getText(urlEnsemble) } }
        val dCond = async { runCatching { conditions(mid) } }
        val dRadar = async {
            if (radarWanted) runCatching { Net.getText(urlRadar, 12_000) } else null
        }

        val sources = mutableListOf<SourceStatus>()

        // --- deterministic models, one series per route point -----------------
        var modelTimes = LongArray(0)
        val models = mutableListOf<ModelSeries>()
        dModels.await().onSuccess { body ->
            runCatching {
                val arr = JSONArray(body)
                val blocks = (0 until arr.length()).map { arr.getJSONObject(it).getJSONObject("minutely_15") }
                modelTimes = blocks[0].getJSONArray("time").toMillis()
                var missing = 0
                for ((id, label) in MODELS) {
                    val key = "precipitation_$id"
                    val perPoint = blocks.map { it.optJSONArray(key)?.toDoubles() }
                    if (perPoint.any { it == null } || perPoint.any { arr2 -> arr2!!.none { !it.isNaN() } }) {
                        missing++
                        continue
                    }
                    @Suppress("UNCHECKED_CAST")
                    models += ModelSeries(id, label, perPoint as List<DoubleArray>)
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

        // --- ensemble members give a genuine probability ----------------------
        var ensTimes = LongArray(0)
        val ensMembers = mutableListOf<DoubleArray>()
        dEns.await().onSuccess { body ->
            runCatching {
                val o = JSONObject(body)
                val h = o.getJSONObject("hourly")
                ensTimes = h.getJSONArray("time").toMillis()
                val keys = h.keys().asSequence().filter { it.contains("member") }.toList()
                for (k in keys) h.optJSONArray(k)?.toDoubles()?.let { ensMembers += it }
                sources += SourceStatus(
                    "Ensembles ICON-D2 + ECMWF · ${ensMembers.size} members",
                    if (ensMembers.isEmpty()) SourceState.EMPTY else SourceState.OK
                )
            }.onFailure {
                sources += SourceStatus("Ensembles ICON-D2 + ECMWF", SourceState.FAILED, it.shortMessage())
            }
        }.onFailure {
            sources += SourceStatus("Ensembles ICON-D2 + ECMWF", SourceState.FAILED, it.shortMessage())
        }

        // --- temperature, wind, daily outlook ---------------------------------
        var fineTimes = LongArray(0)
        var fine: Map<String, DoubleArray> = emptyMap()
        var hourTimes = LongArray(0)
        var hourly: Map<String, DoubleArray> = emptyMap()
        var dayTimes = LongArray(0)
        var daily: Map<String, DoubleArray> = emptyMap()
        var current: Map<String, Double> = emptyMap()
        dCond.await().onSuccess { c ->
            fineTimes = c.fineTimes; fine = c.fine
            hourTimes = c.hourTimes; hourly = c.hourly
            dayTimes = c.dayTimes; daily = c.daily
            current = c.current
            sources += SourceStatus("Open-Meteo · temperature & wind", SourceState.OK)
        }.onFailure {
            sources += SourceStatus("Open-Meteo · temperature & wind", SourceState.FAILED, it.shortMessage())
        }

        // --- Buienradar rain radar nowcast (0-2 h) ----------------------------
        val radar = mutableListOf<RadarSample>()
        val radarResult = dRadar.await()
        if (radarResult == null) {
            sources += SourceStatus(
                "Buienradar rain radar",
                SourceState.SKIPPED,
                if (!useRadar) "switched off" else "outside coverage"
            )
        } else {
            radarResult.onSuccess { txt ->
                radar += parseRadar(txt)
                sources += SourceStatus(
                    "Buienradar rain radar",
                    if (radar.isEmpty()) SourceState.EMPTY else SourceState.OK
                )
            }.onFailure {
                sources += SourceStatus("Buienradar rain radar", SourceState.FAILED, it.shortMessage())
            }
        }

        RouteForecast(
            home = home, work = work, points = points, distanceKm = distance,
            modelTimes = modelTimes, models = models,
            ensTimes = ensTimes, ensMembers = ensMembers,
            fineTimes = fineTimes, fine = fine,
            hourTimes = hourTimes, hourly = hourly,
            dayTimes = dayTimes, daily = daily,
            current = current, radar = radar,
            sources = sources, fetchedAt = System.currentTimeMillis()
        )
    }

    // ------------------------------------------------------------- conditions

    private class Conditions(
        val fineTimes: LongArray, val fine: Map<String, DoubleArray>,
        val hourTimes: LongArray, val hourly: Map<String, DoubleArray>,
        val dayTimes: LongArray, val daily: Map<String, DoubleArray>,
        val current: Map<String, Double>
    )

    private const val HOURLY_VARS =
        "temperature_2m,apparent_temperature,precipitation,precipitation_probability," +
            "weather_code,wind_speed_10m,wind_direction_10m,wind_gusts_10m," +
            "cloud_cover,relative_humidity_2m,is_day"

    private const val DAILY_VARS =
        "weather_code,temperature_2m_max,temperature_2m_min,apparent_temperature_min," +
            "apparent_temperature_max,precipitation_sum,precipitation_hours," +
            "precipitation_probability_max,wind_speed_10m_max,sunrise,sunset"

    private const val CURRENT_VARS =
        "temperature_2m,apparent_temperature,precipitation,weather_code," +
            "wind_speed_10m,wind_direction_10m,wind_gusts_10m,relative_humidity_2m,is_day"

    private const val FINE_VARS =
        "temperature_2m,apparent_temperature,precipitation,wind_speed_10m," +
            "wind_direction_10m,wind_gusts_10m"

    private suspend fun conditions(mid: LatLon): Conditions {
        val base = "https://api.open-meteo.com/v1/forecast" +
            "?latitude=${fmt(mid.lat)}&longitude=${fmt(mid.lon)}" +
            "&hourly=$HOURLY_VARS&daily=$DAILY_VARS&current=$CURRENT_VARS" +
            "&forecast_days=4&timeformat=unixtime&timezone=$zoneParam"

        // The 15-minute block is a bonus: if a model does not serve it the whole
        // request would fail, so fall back to hourly-only rather than lose it all.
        val body = runCatching { Net.getText("$base&minutely_15=$FINE_VARS") }
            .getOrElse { Net.getText(base) }

        val o = JSONObject(body)
        val fineObj = o.optJSONObject("minutely_15")
        val hourObj = o.getJSONObject("hourly")
        val dayObj = o.getJSONObject("daily")
        val curObj = o.optJSONObject("current")

        return Conditions(
            fineTimes = fineObj?.optJSONArray("time")?.toMillis() ?: LongArray(0),
            fine = fineObj?.toSeriesMap() ?: emptyMap(),
            hourTimes = hourObj.getJSONArray("time").toMillis(),
            hourly = hourObj.toSeriesMap(),
            dayTimes = dayObj.getJSONArray("time").toMillis(),
            daily = dayObj.toSeriesMap(includeTimeLike = true),
            current = buildMap {
                curObj ?: return@buildMap
                for (k in curObj.keys()) {
                    val v = curObj.opt(k)
                    if (v is Number) put(k, v.toDouble())
                }
            }
        )
    }

    // ------------------------------------------------------------------ radar

    /**
     * Buienradar serves lines like `000|16:50`: a 0-255 value on a logarithmic
     * scale, timestamped in Dutch local time. Values roll past midnight.
     */
    fun parseRadar(text: String): List<RadarSample> {
        val zone = ZoneId.of("Europe/Amsterdam")
        val now = ZonedDateTime.now(zone)
        val line = Regex("""^(\d{1,3})\|(\d{2}):(\d{2})$""")
        val out = mutableListOf<RadarSample>()
        var prev: ZonedDateTime? = null
        for (raw in text.trim().lines()) {
            val m = line.matchEntire(raw.trim()) ?: continue
            var t = now.withHour(m.groupValues[2].toInt())
                .withMinute(m.groupValues[3].toInt())
                .withSecond(0).withNano(0)
            val p = prev
            if (p != null && t.isBefore(p)) t = t.plusDays(1)
            else if (p == null && Duration.between(t, now).toMinutes() > 180) t = t.plusDays(1)
            prev = t
            val v = m.groupValues[1].toInt()
            val mmh = if (v <= 0) 0.0 else 10.0.pow((v - 109) / 32.0)
            out += RadarSample(t.toInstant().toEpochMilli(), mmh)
        }
        return out
    }

    /** Buienradar only covers the Netherlands, Belgium and the German border. */
    fun inBenelux(p: LatLon): Boolean =
        p.lat in 48.5..55.5 && p.lon in 1.5..9.5

    // ----------------------------------------------------------------- helpers

    private fun fmt(v: Double) = String.format(java.util.Locale.US, "%.4f", v)

    private fun Throwable.shortMessage(): String =
        (message ?: this::class.java.simpleName).take(60)

    private fun JSONArray.toMillis(): LongArray =
        LongArray(length()) { optLong(it) * 1000L }

    private fun JSONArray.toDoubles(): DoubleArray =
        DoubleArray(length()) { if (isNull(it)) Double.NaN else optDouble(it, Double.NaN) }

    /**
     * Turns an Open-Meteo block into name -> values. `time` is skipped; day
     * blocks additionally carry sunrise/sunset, which are unix seconds too.
     */
    private fun JSONObject.toSeriesMap(includeTimeLike: Boolean = false): Map<String, DoubleArray> {
        val out = LinkedHashMap<String, DoubleArray>()
        for (k in keys()) {
            if (k == "time") continue
            val a = optJSONArray(k) ?: continue
            if (!includeTimeLike && (k == "sunrise" || k == "sunset")) continue
            out[k] = a.toDoubles()
        }
        return out
    }
}
