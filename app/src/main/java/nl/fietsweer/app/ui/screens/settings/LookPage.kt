package nl.fietsweer.app.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import nl.fietsweer.app.data.AccentColor
import nl.fietsweer.app.data.Language
import nl.fietsweer.app.data.MapStyle
import nl.fietsweer.app.data.Settings
import nl.fietsweer.app.data.ThemeMode
import nl.fietsweer.app.ui.components.IconHeading
import nl.fietsweer.app.ui.components.SectionCard
import nl.fietsweer.app.ui.components.SegmentedChoice
import nl.fietsweer.app.ui.theme.AppTheme
import nl.fietsweer.app.ui.theme.isDark
import nl.fietsweer.app.ui.theme.supportsWallpaperColours
import nl.fietsweer.app.ui.theme.swatchColour

@Composable
internal fun LookPage(settings: Settings, onUpdate: ((Settings) -> Settings) -> Unit) {
    val strings = AppTheme.strings
    SectionCard {
        ChoiceGroup(
            icon = Icons.Rounded.DarkMode,
            title = strings.theme,
            options = ThemeMode.entries.map { it to strings.themeName(it) },
            selected = settings.theme,
            onSelect = { choice -> onUpdate { it.copy(theme = choice) } }
        )

        Spacer(Modifier.height(16.dp))
        IconHeading(Icons.Rounded.Palette, strings.accentColour)
        Spacer(Modifier.height(10.dp))
        AccentSwatches(settings.accent, settings.theme.isDark()) { choice -> onUpdate { it.copy(accent = choice) } }
        Spacer(Modifier.height(16.dp))

        ChoiceGroup(
            icon = Icons.Rounded.Language,
            title = strings.language,
            options = listOf(
                Language.SYSTEM to strings.langSystem,
                Language.NL to strings.langNl,
                Language.EN to strings.langEn
            ),
            selected = settings.language,
            onSelect = { choice -> onUpdate { it.copy(language = choice) } }
        )

        Spacer(Modifier.height(16.dp))
        ChoiceGroup(
            icon = Icons.Rounded.Map,
            title = strings.mapStyleTitle,
            options = listOf(
                MapStyle.AUTO to strings.mapAuto,
                MapStyle.LIGHT to strings.mapLight,
                MapStyle.DARK to strings.mapDark,
                MapStyle.SOFT to strings.mapSoft
            ),
            selected = settings.mapStyle,
            onSelect = { choice -> onUpdate { it.copy(mapStyle = choice) } }
        )
    }
}

@Composable
private fun <T> ChoiceGroup(
    icon: ImageVector,
    title: String,
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit
) {
    IconHeading(icon, title)
    Spacer(Modifier.height(8.dp))
    SegmentedChoice(options, selected, onSelect, Modifier.fillMaxWidth())
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AccentSwatches(selected: AccentColor, dark: Boolean, onSelect: (AccentColor) -> Unit) {
    val context = LocalContext.current
    val colors = MaterialTheme.colorScheme
    val strings = AppTheme.strings
    val choices = AccentColor.entries.filter { it != AccentColor.WALLPAPER || supportsWallpaperColours }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        for (accent in choices) {
            val active = accent == selected
            Box(
                Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(swatchColour(accent, dark, context))
                    .border(if (active) 3.dp else 1.dp, if (active) colors.onSurface else colors.outlineVariant, CircleShape)
                    .clickable { onSelect(accent) }
                    .semantics { contentDescription = strings.accentName(accent) },
                contentAlignment = Alignment.Center
            ) {
                when {
                    active -> Icon(Icons.Rounded.Check, null, Modifier.size(20.dp), tint = colors.surface)
                    accent == AccentColor.WALLPAPER -> Icon(Icons.Rounded.AutoAwesome, null, Modifier.size(18.dp), tint = colors.surface)
                }
            }
        }
    }
}
