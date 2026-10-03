package nl.fietsweer.app.domain

import nl.fietsweer.app.data.Leg
import nl.fietsweer.app.data.ModelSeries
import nl.fietsweer.app.data.RouteForecast
import nl.fietsweer.app.data.Settings
import java.util.Calendar
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin

enum class WindRelation { HEAD, CROSS, TAIL }

data class ModelVerdict(val id: String, val label: String, val wet: Boolean, val mm: Double)

data class RideAssessment(
    val leg: Leg,
    val departureMs: Long,
    val arrivalMs: Long,
    val durationMin: Int,
    val stillAirDurationMin: Int,
    val paceKmh: Double,
    val distanceKm: Double,

    val risk: Double,
    val modelsWetCount: Int,
    val modelCount: Int,
    val ensembleProb: Double?,
    val radarRisk: Double?,
    val radarMaxMmh: Double,
    val perModel: List<ModelVerdict>,
    val avgMm: Double,
    val maxMm: Double,
    val agreement: Double,
    val night: Boolean,

    val hasConditions: Boolean,
    val tempC: Double,
    val apparentC: Double,
    val bikeFeelC: Double,
    val minBikeFeelC: Double,
    val minTempC: Double,
    val windKmh: Double,
    val gustKmh: Double,
    val windFromDeg: Double,
    val headwindKmh: Double,
    val windRelation: WindRelation,
    val travelBearing: Double
) {
    val riskPercent: Int get() = (risk * 100).roundToInt()

    val windMinutes: Int get() = durationMin - stillAirDurationMin
}

data class DryWindow(
    val slots: List<RideAssessment>,
    val minutes: Int,
    val best: RideAssessment,
    val avgRisk: Double,
    val score: Double
) {
    val from: RideAssessment get() = slots.first()
    val to: RideAssessment get() = slots.last()
}

class Engine(private val fc: RouteForecast, private val settings: Settings) {

    companion object {
        const val GRID_MIN = 15
        const val STEP_MIN = 5
        const val DRY_RISK = 0.22

        fun rideDurationMin(distanceKm: Double, speedKmh: Double): Int {
            if (speedKmh <= 0.5) return 5
            return (distanceKm / speedKmh * 60.0).roundToInt().coerceIn(3, 360)
        }

        fun minutesToDuration(minutes: Double): Int = minutes.roundToInt().coerceIn(3, 360)

        // JAG/TI wind chill, undefined below 4.8 km/h.
        fun windChill(tempC: Double, windKmh: Double): Double {
            if (windKmh < 4.8) return tempC
            val f = windKmh.pow(0.16)
            return 13.12 + 0.6215 * tempC - 11.37 * f + 0.3965 * tempC * f
        }
    }

    val stillAirDurationMin: Int = rideDurationMin(fc.distanceKm, settings.speedKmh.toDouble())
    private val npoints = fc.points.size

    private val bearingOutbound = Geo.bearingDeg(fc.home.toLatLon(), fc.work.toLatLon())
    private val bearingReturn = (bearingOutbound + 180.0) % 360.0

    private class Sample(val minute: Int, val pointIndex: Int)

    private fun samples(leg: Leg, durationMin: Int): List<Sample> {
        val out = ArrayList<Sample>(durationMin / STEP_MIN + 2)
        var m = 0
        while (m <= durationMin) {
            val f = if (durationMin == 0) 0.0 else m.toDouble() / durationMin
            var idx = (f * (npoints - 1)).roundToInt().coerceIn(0, npoints - 1)
            if (leg == Leg.RETURN) idx = npoints - 1 - idx
            out += Sample(m, idx)
            m += STEP_MIN
        }
        if (out.isEmpty() || out.last().minute < durationMin) {
            val idx = if (leg == Leg.RETURN) 0 else npoints - 1
            out += Sample(durationMin, idx)
        }
        return out
    }

    private fun indexAt(times: LongArray, t: Long): Int {
        if (times.isEmpty() || t < times[0]) return -1
        var lo = 0
        var hi = times.size - 1
        while (lo < hi) {
            val mid = (lo + hi + 1) ushr 1
            if (times[mid] <= t) lo = mid else hi = mid - 1
        }
        return lo
    }

    private fun value(series: Map<String, DoubleArray>, times: LongArray, key: String, t: Long): Double {
        val a = series[key] ?: return Double.NaN
        val i = indexAt(times, t)
        if (i < 0 || i >= a.size) return Double.NaN
        return a[i]
    }

    private fun windOver(departureMs: Long, minutes: Int, bearing: Double): Pair<Double, Double>? {
        var headSum = 0.0
        var crossSum = 0.0
        var count = 0
        var m = 0
        while (m <= minutes) {
            val t = departureMs + m * 60_000L
            val speed = condition("wind_speed_10m", t)
            val from = condition("wind_direction_10m", t)
            if (!speed.isNaN() && !from.isNaN()) {
                val atBike = speed * Bike.WIND_AT_BIKE
                val rel = Math.toRadians(Geo.angleDiff(from, bearing))
                headSum += atBike * cos(rel)
                crossSum += atBike * sin(rel)
                count++
            }
            m += STEP_MIN
        }
        return if (count == 0) null else (headSum / count) to (crossSum / count)
    }

    private fun durationFor(departureMs: Long, bearing: Double): Int {
        if (!settings.windAdjustSpeed) return stillAirDurationMin
        var minutes = stillAirDurationMin
        repeat(2) {
            val wind = windOver(departureMs, minutes, bearing) ?: return stillAirDurationMin
            val (strength, angle) = Bike.windAngle(wind.first, wind.second)
            minutes = minutesToDuration(
                Bike.travelMinutes(fc.distanceKm, settings.speedKmh.toDouble(), strength, angle)
            )
        }
        return minutes
    }

    fun assess(departureMs: Long, leg: Leg): RideAssessment {
        val bearing = if (leg == Leg.OUTBOUND) bearingOutbound else bearingReturn
        val durationMin = durationFor(departureMs, bearing)
        val samples = samples(leg, durationMin)
        val arrival = departureMs + durationMin * 60_000L

        val verdicts = ArrayList<ModelVerdict>(fc.models.size)
        var wetCount = 0
        var mmSum = 0.0
        var mmMax = 0.0
        for (m: ModelSeries in fc.models) {
            var peak = 0.0
            var any = false
            for (s in samples) {
                val t = departureMs + s.minute * 60_000L
                val i = indexAt(fc.modelTimes, t)
                if (i < 0) continue
                val arr = m.perPoint[s.pointIndex]
                if (i >= arr.size) continue
                val v = arr[i]
                if (!v.isNaN()) { any = true; if (v > peak) peak = v }
            }
            // Short-range models run out after a day or two; no data is not a dry vote.
            if (!any) continue
            val wet = peak >= settings.wetThreshold
            verdicts += ModelVerdict(m.id, m.label, wet, peak)
            if (wet) wetCount++
            mmSum += peak
            if (peak > mmMax) mmMax = peak
        }
        val n = verdicts.size
        val modelsWet = if (n == 0) 0.0 else wetCount.toDouble() / n

        var ensembleProb: Double? = null
        if (fc.ensMembers.isNotEmpty() && fc.ensTimes.isNotEmpty()) {
            val idx = ArrayList<Int>()
            for (i in fc.ensTimes.indices) {
                val t = fc.ensTimes[i]
                if (t + 3_600_000L > departureMs && t < arrival) idx += i
            }
            if (idx.isNotEmpty()) {
                var hit = 0
                var total = 0
                for (member in fc.ensMembers) {
                    var peak = 0.0
                    var any = false
                    for (i in idx) {
                        if (i >= member.size) continue
                        val v = member[i]
                        if (!v.isNaN()) { any = true; if (v > peak) peak = v }
                    }
                    if (!any) continue
                    total++
                    if (peak >= settings.wetThreshold * 2) hit++ // hourly peak vs per-quarter threshold
                }
                if (total > 0) ensembleProb = hit.toDouble() / total
            }
        }

        var radarRisk: Double? = null
        var radarMax = 0.0
        if (fc.radar.isNotEmpty()) {
            var hits = 0
            var covered = 0
            for (s in samples) {
                val t = departureMs + s.minute * 60_000L
                var best: Double? = null
                var bestDelta = Long.MAX_VALUE
                for (r in fc.radar) {
                    val d = abs(r.timeMs - t)
                    if (d < bestDelta) { bestDelta = d; best = r.mmPerHour }
                }
                if (best != null && bestDelta <= 600_000L) {
                    covered++
                    if (best > radarMax) radarMax = best
                    if (best / 4.0 >= settings.wetThreshold) hits++ // mm/h vs per-quarter threshold
                }
            }
            if (covered >= Math.ceil(samples.size * 0.6).toInt()) {
                radarRisk = if (hits > 0) minOf(1.0, 0.6 + 0.4 * (hits.toDouble() / covered)) else 0.0
            }
        }

        val leadMin = (departureMs - System.currentTimeMillis()) / 60_000.0
        var weighted = 0.0
        var weightSum = 0.0
        radarRisk?.let { weighted += it * (if (leadMin <= 60) 1.4 else 0.9); weightSum += if (leadMin <= 60) 1.4 else 0.9 }
        ensembleProb?.let { weighted += it * 0.8; weightSum += 0.8 }
        if (n > 0) { weighted += modelsWet * 1.0; weightSum += 1.0 }
        val risk = if (weightSum == 0.0) modelsWet else weighted / weightSum

        var temp = Double.NaN
        var apparent = Double.NaN
        var wind = Double.NaN
        var gust = Double.NaN
        var windDir = Double.NaN
        var minFeel = Double.MAX_VALUE
        var minTemp = Double.MAX_VALUE
        var feelSum = 0.0
        var feelCount = 0
        var headSum = 0.0

        for (s in samples) {
            val t = departureMs + s.minute * 60_000L
            val tt = condition("temperature_2m", t)
            val ap = condition("apparent_temperature", t)
            val ws = condition("wind_speed_10m", t)
            val wd = condition("wind_direction_10m", t)
            val wg = condition("wind_gusts_10m", t)
            if (tt.isNaN() || ap.isNaN() || ws.isNaN() || wd.isNaN()) continue
            val feel = bikeFeel(tt, ap, ws, wd, bearing, Bike.paceFor(fc.distanceKm, durationMin.toDouble()))
            if (feel < minFeel) minFeel = feel
            if (tt < minTemp) minTemp = tt
            feelSum += feel
            feelCount++
            headSum += headwindComponent(ws, wd, bearing)
            if (temp.isNaN()) { temp = tt; apparent = ap; wind = ws; windDir = wd; gust = wg }
        }

        val hasConditions = feelCount > 0
        val bikeFeel = if (hasConditions) feelSum / feelCount else Double.NaN
        val headwind = if (hasConditions) headSum / feelCount else Double.NaN
        val relation = when {
            !hasConditions -> WindRelation.CROSS
            headwind > 3 -> WindRelation.HEAD
            headwind < -3 -> WindRelation.TAIL
            else -> WindRelation.CROSS
        }

        val cal = Calendar.getInstance().apply { timeInMillis = departureMs }
        val hour = cal.get(Calendar.HOUR_OF_DAY)

        return RideAssessment(
            leg = leg,
            departureMs = departureMs,
            arrivalMs = arrival,
            durationMin = durationMin,
            stillAirDurationMin = stillAirDurationMin,
            paceKmh = Bike.paceFor(fc.distanceKm, durationMin.toDouble()),
            distanceKm = fc.distanceKm,
            risk = risk,
            modelsWetCount = wetCount,
            modelCount = n,
            ensembleProb = ensembleProb,
            radarRisk = radarRisk,
            radarMaxMmh = radarMax,
            perModel = verdicts,
            avgMm = if (n == 0) 0.0 else mmSum / n,
            maxMm = mmMax,
            agreement = abs(2 * modelsWet - 1),
            night = hour < 6 || hour >= 23,
            hasConditions = hasConditions,
            tempC = temp,
            apparentC = apparent,
            bikeFeelC = bikeFeel,
            minBikeFeelC = if (hasConditions) minFeel else Double.NaN,
            minTempC = if (hasConditions) minTemp else Double.NaN,
            windKmh = wind,
            gustKmh = gust,
            windFromDeg = windDir,
            headwindKmh = headwind,
            windRelation = relation,
            travelBearing = bearing
        )
    }

    private fun condition(key: String, t: Long): Double {
        val fine = value(fc.fine, fc.fineTimes, key, t)
        if (!fine.isNaN()) return fine
        return value(fc.hourly, fc.hourTimes, key, t)
    }

    fun bikeFeel(
        tempC: Double,
        apparentC: Double,
        wind10Kmh: Double,
        windFromDeg: Double,
        travelBearing: Double,
        bikeKmh: Double
    ): Double {
        val atBike = wind10Kmh * Bike.WIND_AT_BIKE
        val relDeg = Geo.angleDiff(windFromDeg, travelBearing)
        val rel = Math.toRadians(relDeg)
        val head = atBike * cos(rel)
        val cross = atBike * sin(rel)
        val vRel = hypot(bikeKmh + head, cross)
        val extra = (windChill(tempC, wind10Kmh) - windChill(tempC, vRel)).coerceAtLeast(0.0)
        return apparentC - extra
    }

    // Positive = headwind km/h, negative = tailwind.
    fun headwindComponent(wind10Kmh: Double, windFromDeg: Double, travelBearing: Double): Double {
        val rel = Math.toRadians(Geo.angleDiff(windFromDeg, travelBearing))
        return wind10Kmh * cos(rel)
    }

    fun scan(leg: Leg, horizonMin: Int = 24 * 60, fromMs: Long = System.currentTimeMillis()): List<RideAssessment> {
        if (!fc.hasModels) return emptyList()
        val out = ArrayList<RideAssessment>()
        val need = Math.ceil(stillAirDurationMin * 2.0 / GRID_MIN).toInt() + 1
        val last = fc.modelTimes.size - need
        for (i in 0 until last) {
            val t = fc.modelTimes[i]
            if (t + GRID_MIN * 60_000L <= fromMs) continue
            if (t - fromMs > horizonMin * 60_000L) break
            out += assess(t, leg)
        }
        return out
    }

    fun scanWindow(leg: Leg, fromMs: Long, untilMs: Long): List<RideAssessment> {
        if (!fc.hasModels || untilMs < fromMs) return emptyList()
        val need = Math.ceil(stillAirDurationMin * 2.0 / GRID_MIN).toInt() + 1
        val last = fc.modelTimes.size - need
        val out = ArrayList<RideAssessment>()
        for (i in 0 until last) {
            val t = fc.modelTimes[i]
            if (t < fromMs) continue
            if (t > untilMs) break
            out += assess(t, leg)
        }
        return out
    }

    fun ridePrecipProfile(departureMs: Long, leg: Leg, durationMin: Int): List<Double> {
        if (fc.models.isEmpty()) return emptyList()
        val steps = (durationMin / GRID_MIN) + 1
        return (0..steps).map { k ->
            val minute = k * GRID_MIN
            val t = departureMs + minute * 60_000L
            val f = if (durationMin == 0) 0.0 else (minute.toDouble() / durationMin).coerceIn(0.0, 1.0)
            var idx = (f * (npoints - 1)).roundToInt().coerceIn(0, npoints - 1)
            if (leg == Leg.RETURN) idx = npoints - 1 - idx
            val i = indexAt(fc.modelTimes, t)
            if (i < 0) 0.0 else {
                var sum = 0.0
                var count = 0
                for (m in fc.models) {
                    val arr = m.perPoint[idx]
                    if (i < arr.size && !arr[i].isNaN()) { sum += arr[i]; count++ }
                }
                if (count == 0) 0.0 else sum / count
            }
        }
    }

    fun windows(slots: List<RideAssessment>, dayOnly: Boolean = true): List<DryWindow> {
        val raw = ArrayList<MutableList<RideAssessment>>()
        var cur: MutableList<RideAssessment>? = null
        for (s in slots) {
            val usable = s.risk < DRY_RISK && !(dayOnly && s.night)
            if (usable) {
                if (cur == null) { cur = mutableListOf(s); raw += cur }
                else cur.add(s)
            } else cur = null
        }
        return raw.map { group ->
            val minutes = group.size * GRID_MIN
            val best = group.reduce { a, b -> if (b.risk < a.risk - 0.05) b else a }
            val avg = group.sumOf { it.risk } / group.size
            val leadHours = (group.first().departureMs - System.currentTimeMillis()) / 3_600_000.0
            DryWindow(
                slots = group,
                minutes = minutes,
                best = best,
                avgRisk = avg,
                score = (1 - avg) * 3 + minOf(minutes, 180) / 180.0 - leadHours * 0.08
            )
        }.sortedByDescending { it.score }
    }
}
