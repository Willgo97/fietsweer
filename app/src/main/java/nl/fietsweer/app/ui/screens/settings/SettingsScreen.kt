package nl.fietsweer.app.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.DirectionsBike
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Umbrella
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import nl.fietsweer.app.data.RouteForecast
import nl.fietsweer.app.data.Settings
import nl.fietsweer.app.data.ThemeMode
import nl.fietsweer.app.ui.components.CARD_GAP
import nl.fietsweer.app.ui.components.ScreenList
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

    if (page == SettingsPage.MENU) {
        SettingsMenu(settings, versionName, contentPadding, onPage)
        return
    }

    ScreenList(contentPadding) {
        item { SubPageHeader(pageTitle(page)) { onPage(SettingsPage.MENU) } }
        item {
            when (page) {
                SettingsPage.ROUTE -> RoutePage(settings, mapTheme, onPickHome, onPickWork)
                SettingsPage.TIMES -> TimesPage(settings, onUpdate)
                SettingsPage.RIDING -> RidingPage(settings, onUpdate)
                SettingsPage.ADVICE -> AdvicePage(settings, onUpdate)
                SettingsPage.LOOK -> LookPage(settings, onUpdate)
                SettingsPage.WIDGET -> WidgetPage(settings, forecast, onUpdate)
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

private class MenuEntry(val page: SettingsPage, val icon: ImageVector, val summary: String?)

// One screen: three groups whose rows share the height, so the menu reaches the tab bar.
@Composable
private fun SettingsMenu(
    settings: Settings,
    versionName: String,
    contentPadding: PaddingValues,
    onPage: (SettingsPage) -> Unit
) {
    val strings = AppTheme.strings
    val format = AppTheme.format
    val activeAlerts = settings.alerts.count { it.enabled }
    val groups = listOf(
        strings.groupCommute to listOf(
            MenuEntry(SettingsPage.ROUTE, Icons.Rounded.Place, routeSummary(settings)),
            MenuEntry(
                SettingsPage.TIMES, Icons.Rounded.Schedule,
                "${format.clock(settings.outboundHour, settings.outboundMinute)} · ${format.clock(settings.returnHour, settings.returnMinute)}"
            ),
            MenuEntry(SettingsPage.RIDING, Icons.AutoMirrored.Rounded.DirectionsBike, format.speedWithUnit(settings.speedKmh.toDouble()))
        ),
        strings.groupAdvice to listOf(
            MenuEntry(SettingsPage.ADVICE, Icons.Rounded.Umbrella, strings.adviceSummary(settings.rainJacketPercent)),
            MenuEntry(SettingsPage.ALERTS, Icons.Rounded.Notifications, strings.alertsActive(activeAlerts)),
            MenuEntry(SettingsPage.WIDGET, Icons.Rounded.Widgets, null)
        ),
        strings.groupApp to listOf(
            MenuEntry(SettingsPage.LOOK, Icons.Rounded.Palette, "${themeName(settings)} · ${strings.accentName(settings.accent)}"),
            MenuEntry(SettingsPage.ABOUT, Icons.Rounded.Info, strings.version(versionName))
        )
    )

    Column(
        Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(CARD_GAP),
        verticalArrangement = Arrangement.spacedBy(CARD_GAP)
    ) {
        for ((title, entries) in groups) {
            SectionCard(
                title = title,
                contentPadding = 16,
                verticalPadding = 12,
                modifier = Modifier.weight(entries.size + 0.6f)
            ) {
                entries.forEachIndexed { i, entry ->
                    if (i > 0) SoftDivider(Modifier.padding(start = 34.dp))
                    MenuRow(pageTitle(entry.page), entry.icon, entry.summary, Modifier.weight(1f)) { onPage(entry.page) }
                }
            }
        }
    }
}

@Composable
private fun MenuRow(title: String, icon: ImageVector, summary: String?, modifier: Modifier, onClick: () -> Unit) {
    Row(
        modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, maxLines = 1)
            if (summary != null) {
                Text(
                    summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Icon(Icons.Rounded.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun routeSummary(settings: Settings): String? {
    val home = settings.home ?: return null
    val work = settings.work ?: return null
    return "${home.name} → ${work.name}"
}

@Composable
private fun themeName(settings: Settings): String {
    val strings = AppTheme.strings
    return when (settings.theme) {
        ThemeMode.SYSTEM -> strings.themeSystem
        ThemeMode.LIGHT -> strings.themeLight
        ThemeMode.DARK -> strings.themeDark
    }
}
