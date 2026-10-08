package nl.fietsweer.app.domain

import nl.fietsweer.app.data.Geo
import nl.fietsweer.app.data.Leg
import nl.fietsweer.app.data.RouteForecast
import nl.fietsweer.app.data.Settings
import java.time.Instant
import java.time.ZoneId
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin

enum class WindRelation { HEAD, CROSS, TAIL }

data class RideAssessment(
    val leg: Leg,
    val departureMs: Long,
    val arrivalMs: Long,
    val durationMinutes: Int,
    val stillAirDurationMinutes: Int,

    val risk: Double,
    val modelCount: Int,
    val averageMm: Double,
    val maxMm: Double,
    val isNight: Boolean,

    val hasConditions: Boolean,
    val bikeFeelC: Double,
    val minBikeFeelC: Double,
    val minTempC: Double,
    val windKmh: Double,
    val gustKmh: Double,
    val headwindKmh: Double,
    val windRelation: WindRelation
) {
    val riskPercent: Int get() = (risk * 100).roundToInt()

    val windDelayMinutes: Int get() = durationMinutes - stillAirDurationMinutes

    val slotCentreMs: Long get() = departureMs + Engine.GRID_MS / 2
}

class Engine(private val forecast: RouteForecast, private val settings: Settings) {

    companion object {
        const val GRID_MINUTES = 15
        const val GRID_MS = GRID_MINUTES * MINUTE_MS
        private const val SAMPLE_STEP_MINUTES = 5
        private const val MIN_HOUR_OVERLAP_MS = 10 * MINUTE_MS

        private const val MIN_RIDE_MINUTES = 3
        private const val MAX_RIDE_MINUTES = 360
        private const val STANDSTILL_KMH = 0.5
        private const val STANDSTILL_RIDE_MINUTES = 5

        // The wet threshold is per quarter hour; ensembles are hourly peaks and radar is mm/h.
        private const val ENSEMBLE_THRESHOLD_FACTOR = 2.0
        private const val QUARTERS_PER_HOUR = 4.0

        private const val RADAR_MAX_GAP_MS = 10 * MINUTE_MS
        private const val RADAR_MIN_COVERAGE = 0.6
        private const val RADAR_WET_FLOOR = 0.6

        private const val RADAR_NEAR_LEAD_MINUTES = 60
        private const val RADAR_WEIGHT_NEAR = 1.4
        private const val RADAR_WEIGHT_FAR = 0.9
        private const val ENSEMBLE_WEIGHT = 0.8
        private const val MODEL_WEIGHT = 1.0

        private const val HEADWIND_FROM_KMH = 3
        private const val NIGHT_ENDS_HOUR = 6
        private const val NIGHT_STARTS_HOUR = 23

        fun rideDurationMinutes(distanceKm: Double, speedKmh: Double): Int {
            if (speedKmh <= STANDSTILL_KMH) return STANDSTILL_RIDE_MINUTES
            return clampDuration(distanceKm / speedKmh * 60.0)
        }

        private fun clampDuration(minutes: Double): Int =
            minutes.roundToInt().coerceIn(MIN_RIDE_MINUTES, MAX_RIDE_MINUTES)

        // JAG/TI wind chill, undefined below 4.8 km/h.
        private fun windChill(tempC: Double, windKmh: Double): Double {
            if (windKmh < 4.8) return tempC
            val windFactor = windKmh.pow(0.16)
            return 13.12 + 0.6215 * tempC - 11.37 * windFactor + 0.3965 * tempC * windFactor
        }

        // Head (positive) and cross component of a wind relative to the direction of travel.
        private fun windAlongRoute(windKmh: Double, windFromDeg: Double, travelBearing: Double): Pair<Double, Double> {
            val relative = Math.toRadians(Geo.angleDiff(windFromDeg, travelBearing))
            return windKmh * cos(relative) to windKmh * sin(relative)
        }
    }

    private val stillAirDurationMinutes: Int = rideDurationMinutes(forecast.distanceKm, settings.speedKmh.toDouble())
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

    // Precipitation is a sum over the interval ending at its timestamp.
    private fun intervalEndingAfter(times: LongArray, timeMs: Long): Int {
        if (times.size < 2) return indexAt(times, timeMs)
        return indexAt(times, timeMs + (times[1] - times[0]) - 1)
    }

    private fun value(series: Map<String, DoubleArray>, times: LongArray, key: String, timeMs: Long): Double {
        val values = series[key] ?: return Double.NaN
        val i = indexAt(times, timeMs)
        if (i < 0 || i >= values.size) return Double.NaN
        return values[i]
    }

    private fun condition(key: String, timeMs: Long): Double {
        val quarterHourValue = value(forecast.quarterHourly, forecast.quarterHourTimes, key, timeMs)
        if (!quarterHourValue.isNaN()) return quarterHourValue
        return value(forecast.hourly, forecast.hourlyTimes, key, timeMs)
    }

    private fun windOver(departureMs: Long, minutes: Int, bearing: Double): Pair<Double, Double>? {
        var headSum = 0.0
        var crossSum = 0.0
        var count = 0
        var minute = 0
        while (minute <= minutes) {
            val timeMs = departureMs + minute * MINUTE_MS
            val speed = condition("wind_speed_10m", timeMs)
            val from = condition("wind_direction_10m", timeMs)
            if (!speed.isNaN() && !from.isNaN()) {
                val (head, cross) = windAlongRoute(speed * Bike.WIND_AT_BIKE, from, bearing)
                headSum += head
                crossSum += cross
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
        val arrivalMs = departureMs + durationMinutes * MINUTE_MS

        val models = modelVote(samples, departureMs)
        val risk = blendedRisk(
            departureMs = departureMs,
            modelVote = models,
            ensembleProbability = ensembleProbability(departureMs, arrivalMs),
            radarRisk = radarRisk(samples, departureMs)
        )
        val paceKmh = Bike.paceFor(forecast.distanceKm, durationMinutes.toDouble())
        val conditions = conditions(samples, departureMs, bearing, paceKmh)
        val departureHour = Instant.ofEpochMilli(departureMs).atZone(ZoneId.systemDefault()).hour

        return RideAssessment(
            leg = leg,
            departureMs = departureMs,
            arrivalMs = arrivalMs,
            durationMinutes = durationMinutes,
            stillAirDurationMinutes = stillAirDurationMinutes,
            risk = risk,
            modelCount = models.modelCount,
            averageMm = models.averageMm,
            maxMm = models.maxMm,
            isNight = departureHour < NIGHT_ENDS_HOUR || departureHour >= NIGHT_STARTS_HOUR,
            hasConditions = conditions != null,
            bikeFeelC = conditions?.bikeFeelC ?: Double.NaN,
            minBikeFeelC = conditions?.minBikeFeelC ?: Double.NaN,
            minTempC = conditions?.minTempC ?: Double.NaN,
            windKmh = conditions?.windKmh ?: Double.NaN,
            gustKmh = conditions?.gustKmh ?: Double.NaN,
            headwindKmh = conditions?.headwindKmh ?: Double.NaN,
            windRelation = when {
                conditions == null -> WindRelation.CROSS
                conditions.headwindKmh > HEADWIND_FROM_KMH -> WindRelation.HEAD
                conditions.headwindKmh < -HEADWIND_FROM_KMH -> WindRelation.TAIL
                else -> WindRelation.CROSS
            }
        )
    }

    private class ModelVote(val modelCount: Int, val wetCount: Int, val averageMm: Double, val maxMm: Double) {
        val wetFraction: Double get() = if (modelCount == 0) 0.0 else wetCount.toDouble() / modelCount
    }

    private fun modelVote(samples: List<Sample>, departureMs: Long): ModelVote {
        var modelCount = 0
        var wetCount = 0
        var mmSum = 0.0
        var mmMax = 0.0
        for (model in forecast.models) {
            var peak = 0.0
            var anyValue = false
            for (sample in samples) {
                val timeMs = departureMs + sample.minute * MINUTE_MS
                val i = intervalEndingAfter(forecast.modelTimes, timeMs)
                if (i < 0) continue
                val values = model.precipitationPerPoint[sample.pointIndex]
                if (i >= values.size) continue
                val value = values[i]
                if (!value.isNaN()) { anyValue = true; if (value > peak) peak = value }
            }
            // Short-range models run out after a day or two; no data is not a dry vote.
            if (!anyValue) continue
            modelCount++
            if (peak >= settings.wetThreshold) wetCount++
            mmSum += peak
            if (peak > mmMax) mmMax = peak
        }
        return ModelVote(modelCount, wetCount, if (modelCount == 0) 0.0 else mmSum / modelCount, mmMax)
    }

    private fun ensembleProbability(departureMs: Long, arrivalMs: Long): Double? {
        if (forecast.ensembleMembers.isEmpty() || forecast.ensembleTimes.isEmpty()) return null
        val hoursDuringRide = forecast.ensembleTimes.indices.filter { i ->
            val hourEnd = forecast.ensembleTimes[i]
            minOf(hourEnd, arrivalMs) - maxOf(hourEnd - HOUR_MS, departureMs) >= MIN_HOUR_OVERLAP_MS
        }
        if (hoursDuringRide.isEmpty()) return null

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
            if (peak >= settings.wetThreshold * ENSEMBLE_THRESHOLD_FACTOR) wetMembers++
        }
        return if (membersWithData > 0) wetMembers.toDouble() / membersWithData else null
    }

    private fun radarRisk(samples: List<Sample>, departureMs: Long): Double? {
        if (forecast.radar.isEmpty()) return null
        var wetSamples = 0
        var coveredSamples = 0
        for (sample in samples) {
            val timeMs = departureMs + sample.minute * MINUTE_MS
            val nearest = forecast.radar.minByOrNull { abs(it.timeMs - timeMs) } ?: continue
            if (abs(nearest.timeMs - timeMs) > RADAR_MAX_GAP_MS) continue
            coveredSamples++
            if (nearest.mmPerHour / QUARTERS_PER_HOUR >= settings.wetThreshold) wetSamples++
        }
        if (coveredSamples < ceil(samples.size * RADAR_MIN_COVERAGE).toInt()) return null
        if (wetSamples == 0) return 0.0
        return minOf(1.0, RADAR_WET_FLOOR + (1 - RADAR_WET_FLOOR) * (wetSamples.toDouble() / coveredSamples))
    }

    private fun blendedRisk(departureMs: Long, modelVote: ModelVote, ensembleProbability: Double?, radarRisk: Double?): Double {
        val leadMinutes = (departureMs - System.currentTimeMillis()) / 60_000.0
        var weighted = 0.0
        var weightSum = 0.0
        radarRisk?.let {
            val radarWeight = if (leadMinutes <= RADAR_NEAR_LEAD_MINUTES) RADAR_WEIGHT_NEAR else RADAR_WEIGHT_FAR
            weighted += it * radarWeight
            weightSum += radarWeight
        }
        ensembleProbability?.let { weighted += it * ENSEMBLE_WEIGHT; weightSum += ENSEMBLE_WEIGHT }
        if (modelVote.modelCount > 0) { weighted += modelVote.wetFraction * MODEL_WEIGHT; weightSum += MODEL_WEIGHT }
        return if (weightSum == 0.0) modelVote.wetFraction else weighted / weightSum
    }

    private class Conditions(
        val bikeFeelC: Double,
        val minBikeFeelC: Double,
        val minTempC: Double,
        val windKmh: Double,
        val gustKmh: Double,
        val headwindKmh: Double
    )

    private fun conditions(samples: List<Sample>, departureMs: Long, bearing: Double, paceKmh: Double): Conditions? {
        var firstWind = Double.NaN
        var firstGust = Double.NaN
        var minFeel = Double.MAX_VALUE
        var minTemp = Double.MAX_VALUE
        var feelSum = 0.0
        var headSum = 0.0
        var count = 0

        for (sample in samples) {
            val timeMs = departureMs + sample.minute * MINUTE_MS
            val temperature = condition("temperature_2m", timeMs)
            val apparent = condition("apparent_temperature", timeMs)
            val wind = condition("wind_speed_10m", timeMs)
            val windFrom = condition("wind_direction_10m", timeMs)
            val gust = condition("wind_gusts_10m", timeMs)
            if (temperature.isNaN() || apparent.isNaN() || wind.isNaN() || windFrom.isNaN()) continue
            val feel = bikeFeel(temperature, apparent, wind, windFrom, bearing, paceKmh)
            if (feel < minFeel) minFeel = feel
            if (temperature < minTemp) minTemp = temperature
            feelSum += feel
            headSum += windAlongRoute(wind, windFrom, bearing).first
            if (count == 0) {
                firstWind = wind
                firstGust = gust
            }
            count++
        }
        if (count == 0) return null
        return Conditions(
            bikeFeelC = feelSum / count,
            minBikeFeelC = minFeel,
            minTempC = minTemp,
            windKmh = firstWind,
            gustKmh = firstGust,
            headwindKmh = headSum / count
        )
    }

    private fun bikeFeel(
        tempC: Double,
        apparentC: Double,
        wind10Kmh: Double,
        windFromDeg: Double,
        travelBearing: Double,
        bikeKmh: Double
    ): Double {
        val (head, cross) = windAlongRoute(wind10Kmh * Bike.WIND_AT_BIKE, windFromDeg, travelBearing)
        val relativeWind = hypot(bikeKmh + head, cross)
        val extraChill = (windChill(tempC, wind10Kmh) - windChill(tempC, relativeWind)).coerceAtLeast(0.0)
        return apparentC - extraChill
    }

    fun scanWindow(leg: Leg, fromMs: Long, untilMs: Long): List<RideAssessment> {
        if (!forecast.hasModels || untilMs < fromMs) return emptyList()
        val gridStepsNeeded = ceil(stillAirDurationMinutes * 2.0 / GRID_MINUTES).toInt() + 1
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
}
