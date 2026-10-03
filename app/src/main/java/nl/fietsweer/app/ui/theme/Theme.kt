package nl.fietsweer.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import nl.fietsweer.app.data.ThemeMode
import nl.fietsweer.app.domain.Formatter
import nl.fietsweer.app.domain.Strings

val LocalAccents: ProvidableCompositionLocal<Accents> = staticCompositionLocalOf { LightAccents }
val LocalStrings: ProvidableCompositionLocal<Strings> = staticCompositionLocalOf { Strings() }
val LocalFormatter: ProvidableCompositionLocal<Formatter> = staticCompositionLocalOf { Formatter(Strings()) }

val Typography.caption: TextStyle get() = labelSmall.copy(fontWeight = FontWeight.Normal)

val Typography.microLabel: TextStyle get() = labelSmall.copy(fontSize = 10.sp)

@Composable
fun ThemeMode.isDark(): Boolean = when (this) {
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
}

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
    val strings: Strings
        @Composable @ReadOnlyComposable get() = LocalStrings.current
    val format: Formatter
        @Composable @ReadOnlyComposable get() = LocalFormatter.current
}

@Composable
fun FietsweerTheme(
    themeMode: ThemeMode,
    dynamicColor: Boolean,
    strings: Strings,
    content: @Composable () -> Unit
) {
    val dark = themeMode.isDark()
    val context = LocalContext.current
    val scheme: ColorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        dark -> BrandDark
        else -> BrandLight
    }
    val accents = if (dark) DarkAccents else LightAccents
    val format = remember(strings) { Formatter(strings) }

    CompositionLocalProvider(
        LocalAccents provides accents,
        LocalStrings provides strings,
        LocalFormatter provides format
    ) {
        MaterialTheme(
            colorScheme = scheme,
            typography = AppTypography,
            content = content
        )
    }
}
