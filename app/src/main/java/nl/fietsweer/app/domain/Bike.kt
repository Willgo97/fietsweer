package nl.fietsweer.app.domain

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tanh

/**
 * What the wind actually does to your ride time.
 *
 * The rider is modelled as a steady engine: from the pace they hold in still
 * air we derive the power they produce, then solve the power balance again for
 * the speed that same effort buys against a given wind.
 */
object Bike {

    private const val RHO = 1.225        // kg/m3, air near sea level
    private const val G = 9.81
    private const val DRIVETRAIN = 0.97

    // An upright Dutch commuter on a normal bike. Deliberately one profile and
    // not a setting: because the power is calibrated from the rider's own
    // still-air pace, a road bike and an omafiets held at the same speed come
    // out within a few percent of each other. A "bike type" knob would look
    // meaningful and change nothing.
    private const val CDA = 0.60         // m2, frontal area times drag coefficient
    private const val CRR = 0.0055       // rolling resistance
    private const val MASS = 92.0        // kg, rider plus bike plus bags

    /** Wind is quoted at 10 m; at saddle height over open ground it is weaker. */
    const val WIND_AT_BIKE = 0.65

    /**
     * Nobody rides a headwind at the same wattage they use with the wind behind
     * them: you lean in and push, and you freewheel when it is pushing you.
     * Saturates at about a quarter more effort into anything serious.
     */
    private const val EFFORT_RANGE = 0.25
    private const val EFFORT_SCALE = 3.5 // m/s of headwind for most of the effect

    /**
     * A real route is not the straight line we measure it along: the same
     * wandering that makes it a quarter longer than the crow flies keeps
     * swinging the wind angle around. Averaging over a spread of headings
     * stops a nominal "pure headwind" from being charged for the whole ride.
     */
    private val HEADING_SPREAD = listOf(0.0 to 0.40, -38.0 to 0.30, 38.0 to 0.30)

    /** Steady power that holds [stillAirKmh] with no wind at all. */
    fun powerFor(stillAirKmh: Double): Double {
        val v = stillAirKmh / 3.6
        return v * (CRR * MASS * G + 0.5 * RHO * CDA * v * v) / DRIVETRAIN
    }

    /**
     * Ground speed against [headKmh] of headwind (negative for a tailwind) and
     * [crossKmh] of crosswind. Crosswind is not free: it raises the airspeed
     * the rider has to push through.
     *
     * Power rises monotonically with ground speed, so a bisection converges
     * cleanly and cannot wander off the way Newton's method can near a tailwind.
     */
    fun speedInWind(stillAirKmh: Double, headKmh: Double, crossKmh: Double): Double {
        if (stillAirKmh <= 0) return 0.0
        val head = headKmh / 3.6
        val cross = crossKmh / 3.6
        val target = powerFor(stillAirKmh) * (1 + EFFORT_RANGE * tanh(head / EFFORT_SCALE))

        fun powerAt(v: Double): Double {
            val along = v + head
            val airspeed = sqrt(along * along + cross * cross)
            // Drag opposes travel; with a tailwind faster than the rider,
            // `along` goes negative and the same term becomes a push.
            val drag = 0.5 * RHO * CDA * airspeed * along
            return v * (CRR * MASS * G + drag) / DRIVETRAIN
        }

        var lo = 0.2
        var hi = 25.0
        repeat(45) {
            val mid = (lo + hi) / 2
            if (powerAt(mid) < target) lo = mid else hi = mid
        }
        val kmh = (lo + hi) / 2 * 3.6
        // Keep the answer somewhere a person would recognise rather than
        // trusting the model out at the ends of its range.
        return kmh.coerceIn(stillAirKmh * 0.35, stillAirKmh * 1.8)
    }

    /**
     * Minutes to cover [distanceKm], given the wind already scaled to bike
     * height. [relAngleDeg] is the angle between where the wind comes from and
     * where you are heading: 0 is straight in your face, 180 is straight behind.
     *
     * Times are averaged over the heading spread rather than speeds, because
     * time is what adds up over a route.
     */
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
            val a = Math.toRadians(relAngleDeg + deviation)
            val speed = speedInWind(
                stillAirKmh,
                windAtBikeKmh * cos(a),
                abs(windAtBikeKmh * sin(a))
            )
            minutes += weight * (distanceKm / speed * 60.0)
        }
        return minutes
    }

    /** The single speed that a ride of [minutes] over [distanceKm] works out to. */
    fun paceFor(distanceKm: Double, minutes: Double): Double =
        if (minutes <= 0) 0.0 else distanceKm / (minutes / 60.0)

    /** Combines head and cross components back into strength and relative angle. */
    fun windAngle(headKmh: Double, crossKmh: Double): Pair<Double, Double> {
        val strength = hypot(headKmh, crossKmh)
        val angle = Math.toDegrees(kotlin.math.atan2(abs(crossKmh), headKmh))
        return strength to angle
    }
}
