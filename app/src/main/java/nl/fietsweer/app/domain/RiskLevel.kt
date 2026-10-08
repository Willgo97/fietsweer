package nl.fietsweer.app.domain

enum class RiskLevel {
    DRY, MOSTLY_DRY, UNCERTAIN, LIKELY_WET, WET;

    companion object {
        private const val DRY_BELOW = 0.08
        private const val MOSTLY_DRY_BELOW = 0.22
        private const val UNCERTAIN_BELOW = 0.45
        private const val LIKELY_WET_BELOW = 0.70

        fun of(risk: Double): RiskLevel = when {
            risk < DRY_BELOW -> DRY
            risk < MOSTLY_DRY_BELOW -> MOSTLY_DRY
            risk < UNCERTAIN_BELOW -> UNCERTAIN
            risk < LIKELY_WET_BELOW -> LIKELY_WET
            else -> WET
        }
    }
}
