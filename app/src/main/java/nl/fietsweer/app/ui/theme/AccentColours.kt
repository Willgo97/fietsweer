package nl.fietsweer.app.ui.theme

import android.content.Context
import android.os.Build
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import nl.fietsweer.app.data.AccentColor

val supportsWallpaperColours: Boolean get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

// Same set as the other apps; null means the scheme's own colours (brand or wallpaper).
fun accentColour(accent: AccentColor, dark: Boolean): Color? = when (accent) {
    AccentColor.BRAND, AccentColor.WALLPAPER -> null
    AccentColor.BLUE -> if (dark) Color(0xFF88B8E6) else Color(0xFF2C5B8C)
    AccentColor.GREEN -> if (dark) Color(0xFF89C8A2) else Color(0xFF36684A)
    AccentColor.GOLD -> if (dark) Color(0xFFD9B566) else Color(0xFF8A6431)
    AccentColor.COPPER -> if (dark) Color(0xFFE8A46E) else Color(0xFF9E5522)
    AccentColor.BORDEAUX -> if (dark) Color(0xFFE58C7E) else Color(0xFF9A3430)
    AccentColor.ROSE -> if (dark) Color(0xFFEE8CA9) else Color(0xFFB0355C)
    AccentColor.PURPLE -> if (dark) Color(0xFFBB9EDE) else Color(0xFF67488B)
    AccentColor.INK -> if (dark) Color(0xFFB7B2AA) else Color(0xFF4B463F)
}

fun swatchColour(accent: AccentColor, dark: Boolean, context: Context): Color = when (accent) {
    AccentColor.BRAND -> (if (dark) BrandDark else BrandLight).primary
    AccentColor.WALLPAPER ->
        if (supportsWallpaperColours) (if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)).primary
        else (if (dark) BrandDark else BrandLight).primary
    else -> accentColour(accent, dark)!!
}

// The tertiary colour stays the brand's: the charts draw temperature in it.
fun ColorScheme.withAccent(accent: Color): ColorScheme {
    val onAccent = if (accent.luminance() > 0.4f) Color.Black else Color.White
    return copy(
        primary = accent,
        onPrimary = onAccent,
        primaryContainer = lerp(surface, accent, 0.35f),
        onPrimaryContainer = onSurface,
        secondary = accent,
        onSecondary = onAccent,
        secondaryContainer = lerp(surface, accent, 0.3f),
        onSecondaryContainer = lerp(onSurface, accent, 0.25f),
        inversePrimary = accent,
        surfaceTint = accent
    )
}
