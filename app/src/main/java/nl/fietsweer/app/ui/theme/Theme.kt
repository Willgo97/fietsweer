package nl.fietsweer.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import nl.fietsweer.app.data.ThemeMode
import nl.fietsweer.app.domain.Fmt
import nl.fietsweer.app.domain.Txt

// -------------------------------------------------------------------- palette

private val BrandLight = lightColorScheme(
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
    surfaceContainerLow = Color(0xFFF0F4F7),
    surfaceContainer = Color(0xFFEAEEF2),
    surfaceContainerHigh = Color(0xFFE4E9EC),
    surfaceContainerHighest = Color(0xFFDEE3E7),
    inverseSurface = Color(0xFF2C3134),
    inverseOnSurface = Color(0xFFEDF1F4)
)

private val BrandDark = darkColorScheme(
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

/** Colours that carry meaning rather than brand: risk levels and garments. */
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
    val onAccent: Color
) {
    fun forRisk(risk: Double): Color = when {
        risk < 0.08 -> dry
        risk < 0.22 -> mostlyDry
        risk < 0.45 -> uncertain
        risk < 0.70 -> likelyWet
        else -> wet
    }

    /** Blue when cold, teal when pleasant, orange when hot. */
    fun forTemperature(c: Double): Color = when {
        c.isNaN() -> uncertain
        c <= 0 -> Color(0xFF7EC8FF)
        c <= 6 -> cold
        c <= 12 -> Color(0xFF4FB3C9)
        c <= 18 -> dry
        c <= 24 -> Color(0xFFE0A93B)
        else -> heat
    }
}

private val LightAccents = Accents(
    dry = Color(0xFF1F9D55),
    mostlyDry = Color(0xFF6DAF3C),
    uncertain = Color(0xFFDD9A26),
    likelyWet = Color(0xFFDD6234),
    wet = Color(0xFFC22C3A),
    rain = Color(0xFF2D7FF0),
    warm = Color(0xFFE07B32),
    cold = Color(0xFF3D7BD6),
    heat = Color(0xFFE2562F),
    onAccent = Color(0xFFFFFFFF)
)

private val DarkAccents = Accents(
    dry = Color(0xFF3ECB78),
    mostlyDry = Color(0xFF95D95C),
    uncertain = Color(0xFFF2B944),
    likelyWet = Color(0xFFF4854F),
    wet = Color(0xFFE8505F),
    rain = Color(0xFF5FA8FF),
    warm = Color(0xFFF59A55),
    cold = Color(0xFF6EA8F5),
    heat = Color(0xFFF4785A),
    onAccent = Color(0xFF06131A)
)

val LocalAccents: ProvidableCompositionLocal<Accents> = staticCompositionLocalOf { LightAccents }
val LocalTxt: ProvidableCompositionLocal<Txt> = staticCompositionLocalOf { Txt() }
val LocalFmt: ProvidableCompositionLocal<Fmt> = staticCompositionLocalOf { Fmt(Txt()) }

// ----------------------------------------------------------------- typography

private val AppTypography = Typography().let { base ->
    base.copy(
        displaySmall = base.displaySmall.copy(
            fontWeight = FontWeight.Bold, letterSpacing = (-0.8).sp
        ),
        headlineLarge = base.headlineLarge.copy(
            fontWeight = FontWeight.Bold, letterSpacing = (-0.6).sp
        ),
        headlineMedium = base.headlineMedium.copy(
            fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp
        ),
        headlineSmall = base.headlineSmall.copy(
            fontWeight = FontWeight.SemiBold, letterSpacing = (-0.3).sp
        ),
        titleLarge = base.titleLarge.copy(fontWeight = FontWeight.SemiBold),
        titleMedium = base.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        labelLarge = base.labelLarge.copy(fontWeight = FontWeight.SemiBold),
        labelSmall = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            letterSpacing = 0.8.sp
        )
    )
}

object AppTheme {
    val accents: Accents
        @Composable @ReadOnlyComposable get() = LocalAccents.current
    val txt: Txt
        @Composable @ReadOnlyComposable get() = LocalTxt.current
    val fmt: Fmt
        @Composable @ReadOnlyComposable get() = LocalFmt.current
}

@Composable
fun FietsweerTheme(
    themeMode: ThemeMode,
    dynamicColor: Boolean,
    txt: Txt,
    content: @Composable () -> Unit
) {
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val context = LocalContext.current
    val scheme: ColorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        dark -> BrandDark
        else -> BrandLight
    }
    val accents = if (dark) DarkAccents else LightAccents
    val fmt = remember(txt) { Fmt(txt) }

    CompositionLocalProvider(
        LocalAccents provides accents,
        LocalTxt provides txt,
        LocalFmt provides fmt
    ) {
        MaterialTheme(
            colorScheme = scheme,
            typography = AppTypography,
            content = content
        )
    }
}
