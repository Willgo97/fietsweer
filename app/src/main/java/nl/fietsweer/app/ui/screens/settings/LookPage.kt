package nl.fietsweer.app.ui.screens.settings

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import nl.fietsweer.app.data.Language
import nl.fietsweer.app.data.MapStyle
import nl.fietsweer.app.data.Settings
import nl.fietsweer.app.data.ThemeMode
import nl.fietsweer.app.ui.components.IconHeading
import nl.fietsweer.app.ui.components.SectionCard
import nl.fietsweer.app.ui.components.SegmentedChoice
import nl.fietsweer.app.ui.components.SwitchRow
import nl.fietsweer.app.ui.theme.AppTheme

@Composable
internal fun LookPage(settings: Settings, onUpdate: ((Settings) -> Settings) -> Unit) {
    val strings = AppTheme.strings
    SectionCard {
        ChoiceGroup(
            icon = Icons.Rounded.DarkMode,
            title = strings.theme,
            options = listOf(
                ThemeMode.SYSTEM to strings.themeSystem,
                ThemeMode.LIGHT to strings.themeLight,
                ThemeMode.DARK to strings.themeDark
            ),
            selected = settings.theme,
            onSelect = { choice -> onUpdate { it.copy(theme = choice) } }
        )

        Spacer(Modifier.height(6.dp))
        SwitchRow(
            title = strings.dynamicColour,
            icon = Icons.Rounded.Palette,
            checked = settings.dynamicColor,
            onCheckedChange = { checked -> onUpdate { it.copy(dynamicColor = checked) } }
        )

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
