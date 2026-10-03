package nl.fietsweer.app.ui.screens.settings

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Air
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import nl.fietsweer.app.data.Settings
import nl.fietsweer.app.ui.components.CyclingSpeedSlider
import nl.fietsweer.app.ui.components.SectionCard
import nl.fietsweer.app.ui.components.SoftDivider
import nl.fietsweer.app.ui.components.SwitchRow
import nl.fietsweer.app.ui.theme.AppTheme

@Composable
internal fun RidingPage(settings: Settings, onUpdate: ((Settings) -> Settings) -> Unit) {
    SectionCard {
        CyclingSpeedSlider(settings, onUpdate)
        Spacer(Modifier.height(4.dp))
        SoftDivider()
        SwitchRow(
            title = AppTheme.strings.windAdjust,
            icon = Icons.Rounded.Air,
            checked = settings.windAdjustSpeed,
            onCheckedChange = { checked -> onUpdate { it.copy(windAdjustSpeed = checked) } }
        )
    }
}
