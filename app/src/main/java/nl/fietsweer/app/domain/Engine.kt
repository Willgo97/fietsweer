package nl.fietsweer.app.domain

import nl.fietsweer.app.data.Leg
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

data class ModelVerdict(val label: String, val wet: Boolean)

data class RideAssessment(
    val leg: Leg,
    val departureMs: Long,
    val arrivalMs: Long,
    val durationMinutes: Int,
    val stillAirDurationMinutes: Int,
    val paceKmh: Double,
    val distanceKm: Double,

    val risk: Double,
    val modelsWetCount: Int,
    val modelCount: Int,
    val ensembleProbability: Double?,
    val radarRisk: Double?,
    val radarMaxMmh: Double,
    val modelVerdicts: List<ModelVerdict>,
    val averageMm: Double,
    val maxMm: Double,
    val agreement: Double,
    val isNight: Boolean,

    val hasConditions: Boolean,
    val bikeFeelC: Double,
    val minBikeFeelC: Double,
    val minTempC: Double,
    val windKmh: Double,
    val gustKmh: Double,
    val windFromDeg: Double,
    val headwindKmh: Double,
    val windRelation: WindRelation
) {
    val riskPercent: Int get() = (risk * 100).roundToInt()

    val windDelayMinutes: Int get() = durationMinutes - stillAirDurationMinutes
}

data class DryWindow(
    val slots: List<RideAssessment>,
    val minutes: Int,
    val best: RideAssessment,
    val averageRisk: Double,
    val score: Double
) {
    val from: RideAssessment get() = slots.first()
    val to: RideAssessment get() = slots.last()
}

class Engine(private val forecast: RouteForecast, private val settings: Settings) {

    companion object {
        const val GRID_MINUTES = 15
        const val SAMPLE_STEP_MINUTES = 5
        const val DRY_RISK = 0.22

        fun rideDurationMinutes(distanceKm: Double, speedKmh: Double): Int {
            if (speedKmh <= 0.5) return 5
            return (distanceKm / speedKmh * 60.0).roundToInt().coerceIn(3, 360)
        }

        private fun clampDuration(minutes: Double): Int = minutes.roundToInt().coerceIn(3, 360)

        // JAG/TI wind chill, undefined below 4.8 km/h.
        private fun windChill(tempC: Double, windKmh: Double): Double {
            if (windKmh < 4.8) return tempC
            val windFactor = windKmh.pow(0.16)
            return 13.12 + 0.6215 * tempC - 11.37 * windFactor + 0.3965 * tempC * windFactor
        }
    }

    val stillAirDurationMinutes: Int = rideDurationMinutes(forecast.distanceKm, settings.speedKmh.toDouble())
    private val pointCount = forecast.points.size

    private val bearingOutbound = Geo.bearingDeg(forecast.home.toLatLon(), forecast.work.toLatLon())
    private val bearingReturn = (bearingOutbound + 180.0) % 360.0

    private class Sample(val minute: Int, val pointIndex: Int)

    private fun samples(leg: Leg, durationMinutes: Int): List<Sample> {
        val samples = ArrayList<Sample>(durationMinutes / SAMPLE_STEP_MINUTES + 2)
        var minute = 0
        while (minute <= durationMinutes) {
            val fraction = if (durationMinutes == 0) 0.0 else minute.toDouble() / durationMinutes
            var pointIndex = (fraction * (pointCount - 1)).roundToInt().coerceIn(0, pointCount - 1)
            if (leg == Leg.RETURN) pointIndex = pointCount - 1 - pointIndex
            samples += Sample(minute, pointIndex)
            minute += SAMPLE_STEP_MINUTES
        }
        if (samples.isEmpty() || samples.last().minute < durationMinutes) {
            val destinationIndex = if (leg == Leg.RETURN) 0 else pointCount - 1
            samples += Sample(durationMinutes, destinationIndex)
        }
        return samples
    }

    private fun indexAt(times: LongArray, timeMs: Long): Int {
        if (times.isEmpty() || timeMs < times[0]) return -1
        var low = 0
        var high = times.size - 1
        while (low < high) {
            val middle = (low + high + 1) ushr 1
            if (times[middle] <= timeMs) low = middle else high = middle - 1
        }
        return low
    }

    private fun value(series: Map<String, DoubleArray>, times: LongArray, key: String, timeMs: Long): Double {
        val values = series[key] ?: return Double.NaN
        val i = indexAt(times, timeMs)
        if (i < 0 || i >= values.size) return Double.NaN
        return values[i]
    }

    private fun windOver(departureMs: Long, minutes: Int, bearing: Double): Pair<Double, Double>? {
        var headSum = 0.0
        var crossSum = 0.0
        var count = 0
        var minute = 0
        while (minute <= minutes) {
            val timeMs = departureMs + minute * 60_000L
            val speed = condition("wind_speed_10m", timeMs)
            val from = condition("wind_direction_10m", timeMs)
            if (!speed.isNaN() && !from.isNaN()) {
                val atBike = speed * Bike.WIND_AT_BIKE
                val relative = Math.toRadians(Geo.angleDiff(from, bearing))
                headSum += atBike * cos(relative)
                crossSum += atBike * sin(relative)
                count++
            }
            minute += SAMPLE_STEP_MINUTES
        }
        return if (count == 0) null else (headSum / count) to (crossSum / count)
    }

    private fun durationFor(departureMs: Long, bearing: Double): Int {
        if (!settings.windAdjustSpeed) return stillAirDurationMinutes
        var minutes = stillAirDurationMinutes
        repeat(2) {
            val wind = windOver(departureMs, minutes, bearing) ?: return stillAirDurationMinutes
            val (strength, angle) = Bike.windAngle(wind.first, wind.second)
            minutes = clampDuration(
                Bike.travelMinutes(forecast.distanceKm, settings.speedKmh.toDouble(), strength, angle)
            )
        }
        return minutes
    }

    fun assess(ride: PlannedRide): RideAssessment = assess(ride.departureMs, ride.leg)

    fun assess(departureMs: Long, leg: Leg): RideAssessment {
        val bearing = if (leg == Leg.OUTBOUND) bearingOutbound else bearingReturn
        val durationMinutes = durationFor(departureMs, bearing)
        val samples = samples(leg, durationMinutes)
        val arrival = departureMs + durationMinutes * 60_000L

        val verdicts = ArrayList<ModelVerdict>(forecast.models.size)
        var wetCount = 0
        var mmSum = 0.0
        var mmMax = 0.0
        for (model in forecast.models) {
            var peak = 0.0
            var anyValue = false
            for (sample in samples) {
                val timeMs = departureMs + sample.minute * 60_000L
                val i = indexAt(forecast.modelTimes, timeMs)
                if (i < 0) continue
                val values = model.precipitationPerPoint[sample.pointIndex]
                if (i >= values.size) continue
                val value = values[i]
                if (!value.isNaN()) { anyValue = true; if (value > peak) peak = value }
            }
            // Short-range models run out after a day or two; no data is not a dry vote.
            if (!anyValue) continue
            val wet = peak >= settings.wetThreshold
            verdicts += ModelVerdict(model.label, wet)
            if (wet) wetCount++
            mmSum += peak
            if (peak > mmMax) mmMax = peak
        }
        val modelCount = verdicts.size
        val modelsWet = if (modelCount == 0) 0.0 else wetCount.toDouble() / modelCount

        var ensembleProbability: Double? = null
        if (forecast.ensembleMembers.isNotEmpty() && forecast.ensembleTimes.isNotEmpty()) {
            val hoursDuringRide = ArrayList<Int>()
            for (i in forecast.ensembleTimes.indices) {
                val hourStart = forecast.ensembleTimes[i]
                if (hourStart + 3_600_000L > departureMs && hourStart < arrival) hoursDuringRide += i
            }
            if (hoursDuringRide.isNotEmpty()) {
                var wetMembers = 0
                var membersWithData = 0
                for (member in forecast.ensembleMembers) {
                    var peak = 0.0
                    var anyValue = false
                    for (i in hoursDuringRide) {
                        if (i >= member.size) continue
                        val value = member[i]
                        if (!value.isNaN()) { anyValue = true; if (value > peak) peak = value }
                    }
                    if (!anyValue) continue
                    membersWithData++
                    if (peak >= settings.wetThreshold * 2) wetMembers++ // hourly peak vs per-quarter threshold
                }
                if (membersWithData > 0) ensembleProbability = wetMembers.toDouble() / membersWithData
            }
        }

        var radarRisk: Double? = null
        var radarMax = 0.0
        if (forecast.radar.isNotEmpty()) {
            var wetSamples = 0
            var coveredSamples = 0
            for (sample in samples) {
                val timeMs = departureMs + sample.minute * 60_000L
                var nearest: Double? = null
                var nearestDelta = Long.MAX_VALUE
                for (radarSample in forecast.radar) {
                    val delta = abs(radarSample.timeMs - timeMs)
                    if (delta < nearestDelta) { nearestDelta = delta; nearest = radarSample.mmPerHour }
                }
                if (nearest != null && nearestDelta <= 600_000L) {
                    coveredSamples++
                    if (nearest > radarMax) radarMax = nearest
                    if (nearest / 4.0 >= settings.wetThreshold) wetSamples++ // mm/h vs per-quarter threshold
                }
            }
            if (coveredSamples >= Math.ceil(samples.size * 0.6).toInt()) {
                radarRisk = if (wetSamples > 0) {
                    minOf(1.0, 0.6 + 0.4 * (wetSamples.toDouble() / coveredSamples))
                } else 0.0
            }
        }

        val leadMinutes = (departureMs - System.currentTimeMillis()) / 60_000.0
        var weighted = 0.0
        var weightSum = 0.0
        radarRisk?.let {
            val radarWeight = if (leadMinutes <= 60) 1.4 else 0.9
            weighted += it * radarWeight
            weightSum += radarWeight
        }
        ensembleProbability?.let { weighted += it * 0.8; weightSum += 0.8 }
        if (modelCount > 0) { weighted += modelsWet * 1.0; weightSum += 1.0 }
        val risk = if (weightSum == 0.0) modelsWet else weighted / weightSum

        var wind = Double.NaN
        var gust = Double.NaN
        var windDirection = Double.NaN
        var minFeel = Double.MAX_VALUE
        var minTemp = Double.MAX_VALUE
        var feelSum = 0.0
        var feelCount = 0
        var headSum = 0.0

        for (sample in samples) {
            val timeMs = departureMs + sample.minute * 60_000L
            val sampleTemp = condition("temperature_2m", timeMs)
            val sampleApparent = condition("apparent_temperature", timeMs)
            val sampleWind = condition("wind_speed_10m", timeMs)
            val sampleDirection = condition("wind_direction_10m", timeMs)
            val sampleGust = condition("wind_gusts_10m", timeMs)
            if (sampleTemp.isNaN() || sampleApparent.isNaN() || sampleWind.isNaN() || sampleDirection.isNaN()) continue
            val feel = bikeFeel(
                sampleTemp, sampleApparent, sampleWind, sampleDirection, bearing,
                Bike.paceFor(forecast.distanceKm, durationMinutes.toDouble())
            )
            if (feel < minFeel) minFeel = feel
            if (sampleTemp < minTemp) minTemp = sampleTemp
            feelSum += feel
            feelCount++
            headSum += headwindComponent(sampleWind, sampleDirection, bearing)
            if (wind.isNaN()) {
                wind = sampleWind
                windDirection = sampleDirection
                gust = sampleGust
            }
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

        val departureHour = Calendar.getInstance().apply { timeInMillis = departureMs }.get(Calendar.HOUR_OF_DAY)

        return RideAssessment(
            leg = leg,
            departureMs = departureMs,
            arrivalMs = arrival,
            durationMinutes = durationMinutes,
            stillAirDurationMinutes = stillAirDurationMinutes,
            paceKmh = Bike.paceFor(forecast.distanceKm, durationMinutes.toDouble()),
            distanceKm = forecast.distanceKm,
            risk = risk,
            modelsWetCount = wetCount,
            modelCount = modelCount,
            ensembleProbability = ensembleProbability,
            radarRisk = radarRisk,
            radarMaxMmh = radarMax,
            modelVerdicts = verdicts,
            averageMm = if (modelCount == 0) 0.0 else mmSum / modelCount,
            maxMm = mmMax,
            agreement = abs(2 * modelsWet - 1),
            isNight = departureHour < 6 || departureHour >= 23,
            hasConditions = hasConditions,
            bikeFeelC = bikeFeel,
            minBikeFeelC = if (hasConditions) minFeel else Double.NaN,
            minTempC = if (hasConditions) minTemp else Double.NaN,
            windKmh = wind,
            gustKmh = gust,
            windFromDeg = windDirection,
            headwindKmh = headwind,
            windRelation = relation
        )
    }

    private fun condition(key: String, timeMs: Long): Double {
        val quarterHourValue = value(forecast.quarterHourly, forecast.quarterHourTimes, key, timeMs)
        if (!quarterHourValue.isNaN()) return quarterHourValue
        return value(forecast.hourly, forecast.hourlyTimes, key, timeMs)
    }

    private fun bikeFeel(
        tempC: Double,
        apparentC: Double,
        wind10Kmh: Double,
        windFromDeg: Double,
        travelBearing: Double,
        bikeKmh: Double
    ): Double {
        val atBike = wind10Kmh * Bike.WIND_AT_BIKE
        val relative = Math.toRadians(Geo.angleDiff(windFromDeg, travelBearing))
        val head = atBike * cos(relative)
        val cross = atBike * sin(relative)
        val relativeWind = hypot(bikeKmh + head, cross)
        val extraChill = (windChill(tempC, wind10Kmh) - windChill(tempC, relativeWind)).coerceAtLeast(0.0)
        return apparentC - extraChill
    }

    // Positive = headwind km/h, negative = tailwind.
    private fun headwindComponent(wind10Kmh: Double, windFromDeg: Double, travelBearing: Double): Double {
        val relative = Math.toRadians(Geo.angleDiff(windFromDeg, travelBearing))
        return wind10Kmh * cos(relative)
    }

    fun scan(leg: Leg, horizonMinutes: Int): List<RideAssessment> {
        val fromMs = System.currentTimeMillis()
        if (!forecast.hasModels) return emptyList()
        val slots = ArrayList<RideAssessment>()
        val gridStepsNeeded = Math.ceil(stillAirDurationMinutes * 2.0 / GRID_MINUTES).toInt() + 1
        val lastStart = forecast.modelTimes.size - gridStepsNeeded
        for (i in 0 until lastStart) {
            val departureMs = forecast.modelTimes[i]
            if (departureMs + GRID_MINUTES * 60_000L <= fromMs) continue
            if (departureMs - fromMs > horizonMinutes * 60_000L) break
            slots += assess(departureMs, leg)
        }
        return slots
    }

    fun scanWindow(leg: Leg, fromMs: Long, untilMs: Long): List<RideAssessment> {
        if (!forecast.hasModels || untilMs < fromMs) return emptyList()
        val gridStepsNeeded = Math.ceil(stillAirDurationMinutes * 2.0 / GRID_MINUTES).toInt() + 1
        val lastStart = forecast.modelTimes.size - gridStepsNeeded
        val slots = ArrayList<RideAssessment>()
        for (i in 0 until lastStart) {
            val departureMs = forecast.modelTimes[i]
            if (departureMs < fromMs) continue
            if (departureMs > untilMs) break
            slots += assess(departureMs, leg)
        }
        return slots
    }

    fun ridePrecipitationProfile(departureMs: Long, leg: Leg, durationMinutes: Int): List<Double> {
        if (forecast.models.isEmpty()) return emptyList()
        val steps = (durationMinutes / GRID_MINUTES) + 1
        return (0..steps).map { step ->
            val minute = step * GRID_MINUTES
            val timeMs = departureMs + minute * 60_000L
            val fraction = if (durationMinutes == 0) 0.0 else (minute.toDouble() / durationMinutes).coerceIn(0.0, 1.0)
            var pointIndex = (fraction * (pointCount - 1)).roundToInt().coerceIn(0, pointCount - 1)
            if (leg == Leg.RETURN) pointIndex = pointCount - 1 - pointIndex
            val i = indexAt(forecast.modelTimes, timeMs)
            if (i < 0) 0.0 else {
                var sum = 0.0
                var count = 0
                for (model in forecast.models) {
                    val values = model.precipitationPerPoint[pointIndex]
                    if (i < values.size && !values[i].isNaN()) { sum += values[i]; count++ }
                }
                if (count == 0) 0.0 else sum / count
            }
        }
    }

    fun dryWindows(slots: List<RideAssessment>): List<DryWindow> {
        val groups = ArrayList<MutableList<RideAssessment>>()
        var currentGroup: MutableList<RideAssessment>? = null
        for (slot in slots) {
            val usable = slot.risk < DRY_RISK && !slot.isNight
            if (usable) {
                if (currentGroup == null) { currentGroup = mutableListOf(slot); groups += currentGroup }
                else currentGroup.add(slot)
            } else currentGroup = null
        }
        return groups.map { group ->
            val minutes = group.size * GRID_MINUTES
            val best = group.reduce { best, candidate -> if (candidate.risk < best.risk - 0.05) candidate else best }
            val averageRisk = group.sumOf { it.risk } / group.size
            val leadHours = (group.first().departureMs - System.currentTimeMillis()) / 3_600_000.0
            DryWindow(
                slots = group,
                minutes = minutes,
                best = best,
                averageRisk = averageRisk,
                score = (1 - averageRisk) * 3 + minOf(minutes, 180) / 180.0 - leadHours * 0.08
            )
        }.sortedByDescending { it.score }
    }
}
