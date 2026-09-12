package nl.fietsweer.app.domain

import nl.fietsweer.app.data.Leg

/** Turns an [Advice] into the sentences shown on screen and in notifications. */
object AdviceText {

    /**
      * Rain and warmth are independent axes, so the headline names whichever
      * combination actually applies rather than picking a winner.
      */
    fun headline(a: Advice, t: Txt): String = when (a.rain) {
        Need.YES -> when (a.layer) {
            Layer.WINTER -> t.adviceRainWinter
            Layer.VEST -> t.adviceRainVest
            Layer.SHORT_SLEEVES -> t.adviceRain
        }
        Need.MAYBE -> when (a.layer) {
            Layer.WINTER -> t.adviceMaybeRainWinter
            Layer.VEST -> t.adviceMaybeRainVest
            Layer.SHORT_SLEEVES -> t.adviceMaybeRain
        }
        Need.NO -> when (a.layer) {
            Layer.WINTER -> t.adviceWinter
            Layer.VEST -> t.adviceVest
            Layer.SHORT_SLEEVES -> t.adviceNone
        }
    }

    /** Short labels for the "take with you" chips. */
    fun chips(a: Advice, t: Txt): List<Pair<String, Boolean>> {
        val out = mutableListOf<Pair<String, Boolean>>()
        if (a.rain != Need.NO) out += t.chipRainJacket to (a.rain == Need.YES)
        when (a.layer) {
            Layer.WINTER -> out += t.chipWinter to true
            Layer.VEST -> out += t.chipVest to true
            Layer.SHORT_SLEEVES -> Unit
        }
        for (e in a.extras) {
            val label = when (e) {
                Extra.GLOVES -> t.chipGloves
                Extra.HAT -> t.chipHat
                Extra.WINDY -> t.chipWindy
                Extra.FROST -> t.chipFrost
                Extra.HOT -> t.chipHot
                Extra.HEAVY_SHOWER -> t.chipHeavy
                Extra.DARK -> t.chipDark
            }
            out += label to false
        }
        return out
    }

    fun legName(leg: Leg, t: Txt): String = if (leg == Leg.OUTBOUND) t.toWork else t.toHome

    /** One line per ride, as used in the expanded notification. */
    fun legLine(r: RideAssessment, t: Txt, f: Fmt): String {
        val rain = if (r.risk < 0.10) t.notifDry else t.notifRainPct(r.riskPercent)
        val feel = if (r.hasConditions) " · ${t.feelsLike} ${f.temp(r.bikeFeelC)}" else ""
        return t.notifLegLine(legName(r.leg, t), f.time(r.departureMs), rain + feel)
    }

    fun summary(a: Advice, t: Txt, f: Fmt): String =
        a.rides.joinToString(" · ") { r ->
            val rain = if (r.risk < 0.10) t.notifDry else t.notifRainPct(r.riskPercent)
            "${f.time(r.departureMs)} $rain"
        }
}
