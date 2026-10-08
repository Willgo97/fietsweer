package nl.fietsweer.app.domain

import nl.fietsweer.app.data.Coverage
import nl.fietsweer.app.data.RouteForecast
import nl.fietsweer.app.data.Settings
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

enum class Need { NO, MAYBE, YES }

enum class Layer { SHORT_SLEEVES, VEST, WINTER }

enum class Extra { GLOVES, SCARF, HAT }

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

        // Hands and head, each only for people who wear it and below their own limit.
        val extras = buildList {
            if (coldest != null) {
                if (settings.wearsGloves && coldest <= settings.glovesBelow) add(Extra.GLOVES)
                if (settings.wearsScarf && coldest <= settings.scarfBelow) add(Extra.SCARF)
                if (settings.wearsHat && coldest <= settings.hatBelow) add(Extra.HAT)
            }
        }

        return Advice(rain, layer, extras, rides, withConditions.isNotEmpty())
    }
}
