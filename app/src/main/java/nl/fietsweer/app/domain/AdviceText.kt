package nl.fietsweer.app.domain

import nl.fietsweer.app.data.Coverage
import nl.fietsweer.app.data.Leg

enum class ChipKind { RAIN_JACKET, VEST, WINTER, GLOVES, HAT, WINDY, FROST, HOT, HEAVY_SHOWER, DARK }

data class AdviceChip(val kind: ChipKind, val label: String, val strong: Boolean)

object AdviceText {

    private const val SAY_DRY_BELOW_RISK = 0.10

    fun saysDry(risk: Double): Boolean = risk < SAY_DRY_BELOW_RISK

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

    fun chips(advice: Advice, strings: Strings): List<AdviceChip> = buildList {
        if (advice.rain != Need.NO) add(AdviceChip(ChipKind.RAIN_JACKET, strings.chipRainJacket, advice.rain == Need.YES))
        when (advice.layer) {
            Layer.WINTER -> add(AdviceChip(ChipKind.WINTER, strings.chipWinter, true))
            Layer.VEST -> add(AdviceChip(ChipKind.VEST, strings.chipVest, true))
            Layer.SHORT_SLEEVES -> Unit
        }
        for (extra in advice.extras) {
            val chip = when (extra) {
                Extra.GLOVES -> AdviceChip(ChipKind.GLOVES, strings.chipGloves, false)
                Extra.HAT -> AdviceChip(ChipKind.HAT, strings.chipHat, false)
                Extra.WINDY -> AdviceChip(ChipKind.WINDY, strings.chipWindy, false)
                Extra.FROST -> AdviceChip(ChipKind.FROST, strings.chipFrost, false)
                Extra.HOT -> AdviceChip(ChipKind.HOT, strings.chipHot, false)
                Extra.HEAVY_SHOWER -> AdviceChip(ChipKind.HEAVY_SHOWER, strings.chipHeavy, false)
                Extra.DARK -> AdviceChip(ChipKind.DARK, strings.chipDark, false)
            }
            add(chip)
        }
    }

    fun chipLine(advice: Advice, strings: Strings): String {
        val chips = chips(advice, strings)
        return if (chips.isEmpty()) strings.adviceNoneSub else chips.joinToString(" · ") { it.label }
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
        val rain = if (saysDry(ride.risk)) strings.notifDry else strings.notifRainPct(ride.riskPercent)
        val feel = if (ride.hasConditions) " · ${strings.feelsLike} ${format.temp(ride.bikeFeelC)}" else ""
        return strings.notifLegLine(legName(ride.leg, strings), moment(ride, format), rain + feel)
    }
}
