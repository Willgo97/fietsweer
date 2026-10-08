package nl.fietsweer.app.domain

import nl.fietsweer.app.data.Coverage
import nl.fietsweer.app.data.RouteForecast
import nl.fietsweer.app.data.Settings
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

enum class Need { NO, MAYBE, YES }

enum class Layer { SHORT_SLEEVES, VEST, WINTER }

enum class Extra { GLOVES, HAT, WINDY, FROST, HOT, HEAVY_SHOWER, DARK }

data class Advice(
    val rain: Need,
    val layer: Layer,
    val extras: List<Extra>,
    val rides: List<RideAssessment>,
    val temperatureKnown: Boolean
) {
    val anythingNeeded: Boolean get() = rain != Need.NO || layer != Layer.SHORT_SLEEVES
    val definite: Boolean get() = rain == Need.YES || layer != Layer.SHORT_SLEEVES
}

object Jacket {

    private const val MAYBE_RAIN_FRACTION = 0.5
    private const val CERTAIN_RAIN_MM = 1.5
    private const val GLOVES_BELOW_WINTER_C = 3.0
    private const val HAT_MAX_C = 0.0
    private const val HOT_MIN_C = 24.0
    private const val FROST_MAX_C = 1.5
    private const val WINDY_GUST_KMH = 55.0
    private const val WINDY_HEADWIND_KMH = 25.0
    private const val HEAVY_SHOWER_MM = 3.0

    fun forCommute(forecast: RouteForecast, settings: Settings, coverage: Coverage): Advice {
        val engine = Engine(forecast, settings)
        return forNextDay(Commute.plannedRides(settings, coverage).map(engine::assess), settings)
    }

    fun forNextDay(rides: List<RideAssessment>, settings: Settings): Advice {
        val first = rides.minByOrNull { it.departureMs } ?: return forRides(rides, settings)
        val day = localDate(first.departureMs)
        return forRides(rides.filter { localDate(it.departureMs) == day }, settings)
    }

    private fun localDate(ms: Long): LocalDate = Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()).toLocalDate()

    private fun forRides(rides: List<RideAssessment>, settings: Settings): Advice {
        if (rides.isEmpty())
            return Advice(Need.NO, Layer.SHORT_SLEEVES, emptyList(), rides, false)

        val maxRiskPercent = rides.maxOf { it.risk } * 100.0
        val maxMm = rides.maxOf { it.maxMm }
        val threshold = settings.rainJacketPercent.toDouble()

        var rain = when {
            maxRiskPercent >= threshold -> Need.YES
            maxRiskPercent >= threshold * MAYBE_RAIN_FRACTION -> Need.MAYBE
            else -> Need.NO
        }
        if (rain == Need.MAYBE && maxMm >= CERTAIN_RAIN_MM) rain = Need.YES

        val withConditions = rides.filter { it.hasConditions }
        val coldest = withConditions.minOfOrNull { it.minBikeFeelC }
        val layer = when {
            coldest == null -> Layer.SHORT_SLEEVES
            coldest <= settings.winterCoatBelow -> Layer.WINTER
            coldest <= settings.vestBelow -> Layer.VEST
            else -> Layer.SHORT_SLEEVES
        }

        val extras = buildList {
            if (coldest != null) {
                if (coldest <= settings.winterCoatBelow - GLOVES_BELOW_WINTER_C) add(Extra.GLOVES)
                if (coldest <= HAT_MAX_C) add(Extra.HAT)
                if (coldest >= HOT_MIN_C) add(Extra.HOT)
            }
            val minTemp = withConditions.minOfOrNull { it.minTempC }
            if (minTemp != null && minTemp <= FROST_MAX_C) add(Extra.FROST)
            val gust = withConditions.maxOfOrNull { if (it.gustKmh.isNaN()) 0.0 else it.gustKmh } ?: 0.0
            val headwind = withConditions.maxOfOrNull { if (it.headwindKmh.isNaN()) 0.0 else it.headwindKmh } ?: 0.0
            if (gust >= WINDY_GUST_KMH || headwind >= WINDY_HEADWIND_KMH) add(Extra.WINDY)
            if (maxMm >= HEAVY_SHOWER_MM) add(Extra.HEAVY_SHOWER)
            if (rides.any { it.isNight }) add(Extra.DARK)
        }

        return Advice(rain, layer, extras, rides, withConditions.isNotEmpty())
    }
}
