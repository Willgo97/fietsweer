package nl.fietsweer.app.domain

enum class RiskLevel {
    DRY, MOSTLY_DRY, UNCERTAIN, LIKELY_WET, WET;

    companion object {
        fun of(risk: Double): RiskLevel = when {
            risk < 0.08 -> DRY
            risk < Engine.DRY_RISK -> MOSTLY_DRY
            risk < 0.45 -> UNCERTAIN
            risk < 0.70 -> LIKELY_WET
            else -> WET
        }
    }
}
