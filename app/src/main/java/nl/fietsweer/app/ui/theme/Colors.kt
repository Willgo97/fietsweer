package nl.fietsweer.app.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import nl.fietsweer.app.data.Leg
import nl.fietsweer.app.domain.Advice
import nl.fietsweer.app.domain.Layer
import nl.fietsweer.app.domain.Need
import nl.fietsweer.app.domain.RiskLevel

internal val BrandLight = lightColorScheme(
    primary = Color(0xFF006684),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFBEE8FF),
    onPrimaryContainer = Color(0xFF001F2A),
    secondary = Color(0xFF00696E),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFF9CF1F6),
    onSecondaryContainer = Color(0xFF002022),
    tertiary = Color(0xFF5B5B7E),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFE1DFFF),
    onTertiaryContainer = Color(0xFF181837),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFF6F9FB),
    onBackground = Color(0xFF171C1F),
    surface = Color(0xFFF6F9FB),
    onSurface = Color(0xFF171C1F),
    surfaceVariant = Color(0xFFDCE4E9),
    onSurfaceVariant = Color(0xFF40484C),
    outline = Color(0xFF70787D),
    outlineVariant = Color(0xFFC0C8CD),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFDDE5EA),
    surfaceContainer = Color(0xFFD4DDE3),
    surfaceContainerHigh = Color(0xFFCAD4DB),
    surfaceContainerHighest = Color(0xFFC0CBD3),
    inverseSurface = Color(0xFF2C3134),
    inverseOnSurface = Color(0xFFEDF1F4)
)

internal val BrandDark = darkColorScheme(
    primary = Color(0xFF6DD3FF),
    onPrimary = Color(0xFF003546),
    primaryContainer = Color(0xFF004D64),
    onPrimaryContainer = Color(0xFFBEE8FF),
    secondary = Color(0xFF80D4DA),
    onSecondary = Color(0xFF00373A),
    secondaryContainer = Color(0xFF004F53),
    onSecondaryContainer = Color(0xFF9CF1F6),
    tertiary = Color(0xFFC4C2EA),
    onTertiary = Color(0xFF2D2D4D),
    tertiaryContainer = Color(0xFF434465),
    onTertiaryContainer = Color(0xFFE1DFFF),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF0E1417),
    onBackground = Color(0xFFDEE3E7),
    surface = Color(0xFF0E1417),
    onSurface = Color(0xFFDEE3E7),
    surfaceVariant = Color(0xFF40484C),
    onSurfaceVariant = Color(0xFFC0C8CD),
    outline = Color(0xFF8A9297),
    outlineVariant = Color(0xFF40484C),
    surfaceContainerLowest = Color(0xFF090E11),
    surfaceContainerLow = Color(0xFF161C1F),
    surfaceContainer = Color(0xFF1A2023),
    surfaceContainerHigh = Color(0xFF242B2E),
    surfaceContainerHighest = Color(0xFF2F3639),
    inverseSurface = Color(0xFFDEE3E7),
    inverseOnSurface = Color(0xFF2C3134)
)

data class Accents(
    val dry: Color,
    val mostlyDry: Color,
    val uncertain: Color,
    val likelyWet: Color,
    val wet: Color,
    val rain: Color,
    val warm: Color,
    val cold: Color,
    val heat: Color,
    val freezing: Color,
    val cool: Color,
    val balmy: Color,
    val heroInk: Color
) {
    fun forRisk(risk: Double): Color = when (RiskLevel.of(risk)) {
        RiskLevel.DRY -> dry
        RiskLevel.MOSTLY_DRY -> mostlyDry
        RiskLevel.UNCERTAIN -> uncertain
        RiskLevel.LIKELY_WET -> likelyWet
        RiskLevel.WET -> wet
    }

    fun forTemperature(celsius: Double): Color = when {
        celsius.isNaN() -> uncertain
        celsius <= 0 -> freezing
        celsius <= 6 -> cold
        celsius <= 12 -> cool
        celsius <= 18 -> dry
        celsius <= 24 -> balmy
        else -> heat
    }

    fun forLeg(leg: Leg): Color = if (leg == Leg.OUTBOUND) rain else warm
}

internal val LightAccents = Accents(
    dry = Color(0xFF1F9D55),
    mostlyDry = Color(0xFF6DAF3C),
    uncertain = Color(0xFFDD9A26),
    likelyWet = Color(0xFFDD6234),
    wet = Color(0xFFC22C3A),
    rain = Color(0xFF2D7FF0),
    warm = Color(0xFFE07B32),
    cold = Color(0xFF3D7BD6),
    heat = Color(0xFFE2562F),
    freezing = Color(0xFF7EC8FF),
    cool = Color(0xFF4FB3C9),
    balmy = Color(0xFFE0A93B),
    heroInk = Color(0xFF10171C)
)

internal val DarkAccents = Accents(
    dry = Color(0xFF3ECB78),
    mostlyDry = Color(0xFF95D95C),
    uncertain = Color(0xFFF2B944),
    likelyWet = Color(0xFFF4854F),
    wet = Color(0xFFE8505F),
    rain = Color(0xFF5FA8FF),
    warm = Color(0xFFF59A55),
    cold = Color(0xFF6EA8F5),
    heat = Color(0xFFF4785A),
    freezing = Color(0xFF7EC8FF),
    cool = Color(0xFF4FB3C9),
    balmy = Color(0xFFE0A93B),
    heroInk = Color(0xFF10171C)
)

data class MapPalette(val background: Color, val loadingTile: Color) {
    companion object {
        val Light = MapPalette(background = Color(0xFFE8EDF1), loadingTile = Color(0x14000000))
        val Dark = MapPalette(background = Color(0xFF15191C), loadingTile = Color(0x14FFFFFF))
    }
}

object SystemColors {
    val notificationLight: Int = Color(0xFF17A2A8).toArgb()
    val widgetIdle: Int = Color(0xFF7D8B95).toArgb()

    fun adviceAccent(advice: Advice): Int = when {
        advice.rain == Need.YES && advice.layer == Layer.WINTER -> Color(0xFF8A5BD6)
        advice.rain == Need.YES -> LightAccents.rain
        advice.layer == Layer.WINTER -> Color(0xFFC85A2B)
        advice.layer == Layer.VEST -> LightAccents.warm
        advice.rain == Need.MAYBE -> LightAccents.uncertain
        else -> LightAccents.dry
    }.toArgb()
}
