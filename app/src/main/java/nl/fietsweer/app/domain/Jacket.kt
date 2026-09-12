package nl.fietsweer.app.domain

import nl.fietsweer.app.data.Settings

enum class Need { NO, MAYBE, YES }

/**
 * How much you need to wear, as a ladder rather than a yes/no. Anything warmer
 * than the vest threshold is short-sleeve weather — provided it stays dry,
 * which is a separate question entirely.
 */
enum class Layer { SHORT_SLEEVES, VEST, WINTER }

/** Small extras worth a line in the notification when conditions are unusual. */
enum class Extra { GLOVES, HAT, WINDY, FROST, HOT, HEAVY_SHOWER, DARK }

data class Advice(
    val rain: Need,
    val layer: Layer,
    val extras: List<Extra>,
    val rides: List<RideAssessment>,
    /** False when no ride had usable temperature data to judge. */
    val temperatureKnown: Boolean = true
) {
    val anythingNeeded: Boolean get() = rain != Need.NO || layer != Layer.SHORT_SLEEVES
    val definite: Boolean get() = rain == Need.YES || layer != Layer.SHORT_SLEEVES

    /** 0 = nothing to worry about, 3 = take everything. */
    val severity: Int
        get() {
            val r = when (rain) { Need.NO -> 0; Need.MAYBE -> 1; Need.YES -> 2 }
            val w = when (layer) { Layer.SHORT_SLEEVES -> 0; Layer.VEST -> 1; Layer.WINTER -> 2 }
            return maxOf(r, w) + if (r == 2 && w == 2) 1 else 0
        }

    val worstRide: RideAssessment? get() = rides.maxByOrNull { it.risk }
}

object Jacket {

    fun forRide(a: RideAssessment, s: Settings): Advice = forRides(listOf(a), s)

    fun forRides(rides: List<RideAssessment>, s: Settings): Advice {
        if (rides.isEmpty())
            return Advice(Need.NO, Layer.SHORT_SLEEVES, emptyList(), rides, false)

        val maxRiskPct = rides.maxOf { it.risk } * 100.0
        val maxMm = rides.maxOf { it.maxMm }
        val threshold = s.rainJacketPercent.toDouble()

        var rain = when {
            maxRiskPct >= threshold -> Need.YES
            maxRiskPct >= threshold * 0.5 -> Need.MAYBE
            else -> Need.NO
        }
        // A small chance of a proper downpour still means taking the jacket.
        if (rain == Need.MAYBE && maxMm >= 1.5) rain = Need.YES

        val withConditions = rides.filter { it.hasConditions }
        // The coldest moment of the ride decides, not the average: arriving
        // frozen is what you remember.
        val coldest = withConditions.minOfOrNull { it.minBikeFeelC }
        val layer = when {
            coldest == null -> Layer.SHORT_SLEEVES
            coldest <= s.winterCoatBelow -> Layer.WINTER
            coldest <= s.vestBelow -> Layer.VEST
            else -> Layer.SHORT_SLEEVES
        }

        val extras = buildList {
            if (coldest != null) {
                if (coldest <= s.winterCoatBelow - 3.0) add(Extra.GLOVES)
                if (coldest <= 0.0) add(Extra.HAT)
                if (coldest >= 24.0) add(Extra.HOT)
            }
            val minTemp = withConditions.minOfOrNull { it.minTempC }
            if (minTemp != null && minTemp <= 1.5) add(Extra.FROST)
            val gust = withConditions.maxOfOrNull { if (it.gustKmh.isNaN()) 0.0 else it.gustKmh } ?: 0.0
            val head = withConditions.maxOfOrNull { if (it.headwindKmh.isNaN()) 0.0 else it.headwindKmh } ?: 0.0
            if (gust >= 55.0 || head >= 25.0) add(Extra.WINDY)
            if (maxMm >= 3.0) add(Extra.HEAVY_SHOWER)
            if (rides.any { it.night }) add(Extra.DARK)
        }

        return Advice(rain, layer, extras, rides, withConditions.isNotEmpty())
    }
}
