package nl.fietsweer.app.domain

import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tanh

object Bike {

    private const val AIR_DENSITY = 1.225 // kg/m3
    private const val GRAVITY = 9.81
    private const val DRIVETRAIN_EFFICIENCY = 0.97

    private const val DRAG_AREA = 0.60 // m2
    private const val ROLLING_RESISTANCE = 0.0055
    private const val TOTAL_MASS = 92.0 // kg, rider + bike + bags

    const val WIND_AT_BIKE = 0.65

    private const val EFFORT_RANGE = 0.25
    private const val EFFORT_SCALE = 3.5 // m/s

    private val HEADING_SPREAD = listOf(0.0 to 0.40, -38.0 to 0.30, 38.0 to 0.30)

    private fun powerFor(stillAirKmh: Double): Double {
        val speed = stillAirKmh / 3.6
        return speed * (ROLLING_RESISTANCE * TOTAL_MASS * GRAVITY + 0.5 * AIR_DENSITY * DRAG_AREA * speed * speed) /
            DRIVETRAIN_EFFICIENCY
    }

    // Bisection rather than Newton: power is monotonic in speed, Newton can diverge near a tailwind.
    private fun speedInWind(stillAirKmh: Double, headKmh: Double, crossKmh: Double): Double {
        if (stillAirKmh <= 0) return 0.0
        val head = headKmh / 3.6
        val cross = crossKmh / 3.6
        val targetPower = powerFor(stillAirKmh) * (1 + EFFORT_RANGE * tanh(head / EFFORT_SCALE))

        fun powerAt(speed: Double): Double {
            val along = speed + head
            val airspeed = sqrt(along * along + cross * cross)
            // Tailwind faster than the rider: `along` < 0 and drag pushes.
            val drag = 0.5 * AIR_DENSITY * DRAG_AREA * airspeed * along
            return speed * (ROLLING_RESISTANCE * TOTAL_MASS * GRAVITY + drag) / DRIVETRAIN_EFFICIENCY
        }

        var low = 0.2
        var high = 25.0
        repeat(45) {
            val middle = (low + high) / 2
            if (powerAt(middle) < targetPower) low = middle else high = middle
        }
        val kmh = (low + high) / 2 * 3.6
        return kmh.coerceIn(stillAirKmh * 0.35, stillAirKmh * 1.8)
    }

    // relAngleDeg: 0 = headwind, 180 = tailwind.
    fun travelMinutes(
        distanceKm: Double,
        stillAirKmh: Double,
        windAtBikeKmh: Double,
        relAngleDeg: Double
    ): Double {
        if (distanceKm <= 0 || stillAirKmh <= 0) return 0.0
        if (windAtBikeKmh <= 0.5) return distanceKm / stillAirKmh * 60.0
        var minutes = 0.0
        for ((deviation, weight) in HEADING_SPREAD) {
            val angle = Math.toRadians(relAngleDeg + deviation)
            val speed = speedInWind(
                stillAirKmh,
                windAtBikeKmh * cos(angle),
                abs(windAtBikeKmh * sin(angle))
            )
            minutes += weight * (distanceKm / speed * 60.0)
        }
        return minutes
    }

    fun paceFor(distanceKm: Double, minutes: Double): Double =
        if (minutes <= 0) 0.0 else distanceKm / (minutes / 60.0)

    fun windAngle(headKmh: Double, crossKmh: Double): Pair<Double, Double> {
        val strength = hypot(headKmh, crossKmh)
        val angle = Math.toDegrees(atan2(abs(crossKmh), headKmh))
        return strength to angle
    }
}
