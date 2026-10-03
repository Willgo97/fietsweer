package nl.fietsweer.app.ui.screens.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.DirectionsBike
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Umbrella
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import nl.fietsweer.app.data.RouteForecast
import nl.fietsweer.app.data.Settings
import nl.fietsweer.app.ui.components.NavigationRow
import nl.fietsweer.app.ui.components.ScreenList
import nl.fietsweer.app.ui.components.ScreenTitle
import nl.fietsweer.app.ui.components.SectionCard
import nl.fietsweer.app.ui.components.SoftDivider
import nl.fietsweer.app.ui.components.SubPageHeader
import nl.fietsweer.app.ui.map.MapTheme
import nl.fietsweer.app.ui.theme.AppTheme

enum class SettingsPage { MENU, ROUTE, TIMES, ALERTS, RIDING, ADVICE, LOOK, WIDGET, ABOUT }

@Composable
fun SettingsScreen(
    page: SettingsPage,
    onPage: (SettingsPage) -> Unit,
    settings: Settings,
    forecast: RouteForecast?,
    versionName: String,
    mapTheme: MapTheme,
    contentPadding: PaddingValues,
    onUpdate: ((Settings) -> Settings) -> Unit,
    onPickHome: () -> Unit,
    onPickWork: () -> Unit,
    onResetSetup: () -> Unit
) {
    val strings = AppTheme.strings

    ScreenList(contentPadding) {
        if (page == SettingsPage.MENU) {
            item { ScreenTitle(strings.tabSettings) }
            item { SettingsMenu(onPage) }
            return@ScreenList
        }
        item { SubPageHeader(pageTitle(page)) { onPage(SettingsPage.MENU) } }
        item {
            when (page) {
                SettingsPage.ROUTE -> RoutePage(settings, mapTheme, onPickHome, onPickWork)
                SettingsPage.TIMES -> TimesPage(settings, onUpdate)
                SettingsPage.RIDING -> RidingPage(settings, onUpdate)
                SettingsPage.ADVICE -> AdvicePage(settings, onUpdate)
                SettingsPage.LOOK -> LookPage(settings, onUpdate)
                SettingsPage.WIDGET -> WidgetPage(settings, forecast)
                SettingsPage.ABOUT -> AboutPage(versionName, onResetSetup)
                SettingsPage.MENU, SettingsPage.ALERTS -> Unit
            }
        }
    }
}

@Composable
private fun pageTitle(page: SettingsPage): String {
    val strings = AppTheme.strings
    return when (page) {
        SettingsPage.MENU -> strings.tabSettings
        SettingsPage.ROUTE -> strings.settingsRoute
        SettingsPage.TIMES -> strings.settingsTimes
        SettingsPage.ALERTS -> strings.tabAlerts
        SettingsPage.RIDING -> strings.settingsRiding
        SettingsPage.ADVICE -> strings.settingsAdvice
        SettingsPage.LOOK -> strings.settingsLook
        SettingsPage.WIDGET -> strings.settingsWidget
        SettingsPage.ABOUT -> strings.settingsAbout
    }
}

@Composable
private fun SettingsMenu(onPage: (SettingsPage) -> Unit) {
    val rows = listOf(
        SettingsPage.ROUTE to Icons.Rounded.Place,
        SettingsPage.TIMES to Icons.Rounded.Schedule,
        SettingsPage.ALERTS to Icons.Rounded.Notifications,
        SettingsPage.RIDING to Icons.AutoMirrored.Rounded.DirectionsBike,
        SettingsPage.ADVICE to Icons.Rounded.Umbrella,
        SettingsPage.LOOK to Icons.Rounded.Palette,
        SettingsPage.WIDGET to Icons.Rounded.Widgets,
        SettingsPage.ABOUT to Icons.Rounded.Info
    )
    SectionCard(contentPadding = 6) {
        rows.forEachIndexed { i, (page, icon) ->
            if (i > 0) {
                SoftDivider(Modifier.padding(start = 48.dp))
            }
            Box(Modifier.padding(horizontal = 10.dp)) {
                NavigationRow(pageTitle(page), icon) { onPage(page) }
            }
        }
    }
}
