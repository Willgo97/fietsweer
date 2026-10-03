package nl.fietsweer.app.domain

import nl.fietsweer.app.data.Settings

enum class Need { NO, MAYBE, YES }

enum class Layer { SHORT_SLEEVES, VEST, WINTER }

enum class Extra { GLOVES, HAT, WINDY, FROST, HOT, HEAVY_SHOWER, DARK }

data class Advice(
    val rain: Need,
    val layer: Layer,
    val extras: List<Extra>,
    val rides: List<RideAssessment>,
    val temperatureKnown: Boolean = true
) {
    val anythingNeeded: Boolean get() = rain != Need.NO || layer != Layer.SHORT_SLEEVES
    val definite: Boolean get() = rain == Need.YES || layer != Layer.SHORT_SLEEVES
}

object Jacket {

    fun forRides(rides: List<RideAssessment>, settings: Settings): Advice {
        if (rides.isEmpty())
            return Advice(Need.NO, Layer.SHORT_SLEEVES, emptyList(), rides, false)

        val maxRiskPercent = rides.maxOf { it.risk } * 100.0
        val maxMm = rides.maxOf { it.maxMm }
        val threshold = settings.rainJacketPercent.toDouble()

        var rain = when {
            maxRiskPercent >= threshold -> Need.YES
            maxRiskPercent >= threshold * 0.5 -> Need.MAYBE
            else -> Need.NO
        }
        if (rain == Need.MAYBE && maxMm >= 1.5) rain = Need.YES

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
                if (coldest <= settings.winterCoatBelow - 3.0) add(Extra.GLOVES)
                if (coldest <= 0.0) add(Extra.HAT)
                if (coldest >= 24.0) add(Extra.HOT)
            }
            val minTemp = withConditions.minOfOrNull { it.minTempC }
            if (minTemp != null && minTemp <= 1.5) add(Extra.FROST)
            val gust = withConditions.maxOfOrNull { if (it.gustKmh.isNaN()) 0.0 else it.gustKmh } ?: 0.0
            val headwind = withConditions.maxOfOrNull { if (it.headwindKmh.isNaN()) 0.0 else it.headwindKmh } ?: 0.0
            if (gust >= 55.0 || headwind >= 25.0) add(Extra.WINDY)
            if (maxMm >= 3.0) add(Extra.HEAVY_SHOWER)
            if (rides.any { it.isNight }) add(Extra.DARK)
        }

        return Advice(rain, layer, extras, rides, withConditions.isNotEmpty())
    }
}
