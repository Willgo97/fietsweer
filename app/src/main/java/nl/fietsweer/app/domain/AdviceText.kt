package nl.fietsweer.app.domain

import nl.fietsweer.app.data.Coverage
import nl.fietsweer.app.data.Leg

object AdviceText {

    fun headline(advice: Advice, strings: Strings): String = when (advice.rain) {
        Need.YES -> when (advice.layer) {
            Layer.WINTER -> strings.adviceRainWinter
            Layer.VEST -> strings.adviceRainVest
            Layer.SHORT_SLEEVES -> strings.adviceRain
        }
        Need.MAYBE -> when (advice.layer) {
            Layer.WINTER -> strings.adviceMaybeRainWinter
            Layer.VEST -> strings.adviceMaybeRainVest
            Layer.SHORT_SLEEVES -> strings.adviceMaybeRain
        }
        Need.NO -> when (advice.layer) {
            Layer.WINTER -> strings.adviceWinter
            Layer.VEST -> strings.adviceVest
            Layer.SHORT_SLEEVES -> strings.adviceNone
        }
    }

    fun chips(advice: Advice, strings: Strings): List<Pair<String, Boolean>> {
        val chips = mutableListOf<Pair<String, Boolean>>()
        if (advice.rain != Need.NO) chips += strings.chipRainJacket to (advice.rain == Need.YES)
        when (advice.layer) {
            Layer.WINTER -> chips += strings.chipWinter to true
            Layer.VEST -> chips += strings.chipVest to true
            Layer.SHORT_SLEEVES -> Unit
        }
        for (extra in advice.extras) {
            val label = when (extra) {
                Extra.GLOVES -> strings.chipGloves
                Extra.HAT -> strings.chipHat
                Extra.WINDY -> strings.chipWindy
                Extra.FROST -> strings.chipFrost
                Extra.HOT -> strings.chipHot
                Extra.HEAVY_SHOWER -> strings.chipHeavy
                Extra.DARK -> strings.chipDark
            }
            chips += label to false
        }
        return chips
    }

    fun chipLine(advice: Advice, strings: Strings): String {
        val chips = chips(advice, strings)
        return if (chips.isEmpty()) strings.adviceNoneSub else chips.joinToString(" · ") { it.first }
    }

    fun legName(leg: Leg, strings: Strings): String = if (leg == Leg.OUTBOUND) strings.toWork else strings.toHome

    fun coverageName(coverage: Coverage, strings: Strings): String = when (coverage) {
        Coverage.OUTBOUND -> strings.coverageOutbound
        Coverage.RETURN -> strings.coverageReturn
        Coverage.BOTH -> strings.coverageBoth
    }

    fun moment(ride: RideAssessment, format: Formatter): String =
        if (format.isToday(ride.departureMs)) format.time(ride.departureMs) else format.dayTime(ride.departureMs)

    fun legLine(ride: RideAssessment, strings: Strings, format: Formatter): String {
        val rain = if (ride.risk < 0.10) strings.notifDry else strings.notifRainPct(ride.riskPercent)
        val feel = if (ride.hasConditions) " · ${strings.feelsLike} ${format.temp(ride.bikeFeelC)}" else ""
        return strings.notifLegLine(legName(ride.leg, strings), moment(ride, format), rain + feel)
    }
}
