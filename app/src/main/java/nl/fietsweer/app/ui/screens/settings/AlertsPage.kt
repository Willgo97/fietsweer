package nl.fietsweer.app.ui.screens.settings

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.BatteryAlert
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import nl.fietsweer.app.data.Alert
import nl.fietsweer.app.data.Settings
import nl.fietsweer.app.domain.AdviceText
import nl.fietsweer.app.domain.nextTriggerMs
import nl.fietsweer.app.notify.AlertScheduler
import nl.fietsweer.app.notify.Notifier
import nl.fietsweer.app.ui.components.ButtonLabel
import nl.fietsweer.app.ui.components.Caption
import nl.fietsweer.app.ui.components.EmptyState
import nl.fietsweer.app.ui.components.InfoCard
import nl.fietsweer.app.ui.components.Pill
import nl.fietsweer.app.ui.components.ScreenList
import nl.fietsweer.app.ui.components.SectionCard
import nl.fietsweer.app.ui.components.SubPageHeader
import nl.fietsweer.app.ui.theme.AppTheme
import android.provider.Settings as AndroidSettings

@Composable
fun AlertsPage(
    settings: Settings,
    contentPadding: PaddingValues,
    onEdit: (Alert) -> Unit,
    onToggle: (Alert) -> Unit,
    onNew: () -> Unit,
    onTest: () -> Unit,
    onBack: () -> Unit
) {
    val strings = AppTheme.strings
    val accents = AppTheme.accents
    val context = LocalContext.current

    var permissionTick by remember { mutableIntStateOf(0) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { permissionTick++ }

    val canPost = remember(permissionTick) { Notifier.canPost(context) }
    val canExact = remember(permissionTick) { AlertScheduler.canScheduleExact(context) }
    val batteryOk = remember(permissionTick) { ignoresBatteryOptimisation(context) }

    val askNotification = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { permissionTick++ }

    ScreenList(contentPadding) {
        item {
            SubPageHeader(
                strings.alertsTitle,
                trailing = {
                    Button(onClick = onNew, shape = RoundedCornerShape(14.dp)) {
                        ButtonLabel(strings.add, Icons.Rounded.Add)
                    }
                },
                onBack = onBack
            )
        }

        if (!canPost) {
            item {
                InfoCard(
                    title = strings.permNotifications,
                    body = strings.permNotificationsBody,
                    actionLabel = strings.grant,
                    icon = Icons.Rounded.NotificationsOff,
                    onAction = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            askNotification.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            context.startActivity(
                                Intent(AndroidSettings.ACTION_APP_NOTIFICATION_SETTINGS)
                                    .putExtra(AndroidSettings.EXTRA_APP_PACKAGE, context.packageName)
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        }
                    }
                )
            }
        }

        if (!canExact) {
            item {
                InfoCard(
                    title = strings.permExact,
                    body = strings.permExactBody,
                    actionLabel = strings.grant,
                    accent = accents.uncertain,
                    icon = Icons.Rounded.Schedule,
                    onAction = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            runCatching {
                                context.startActivity(
                                    Intent(AndroidSettings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                                        .setData(Uri.parse("package:${context.packageName}"))
                                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                )
                            }
                        }
                    }
                )
            }
        }

        if (!batteryOk) {
            item {
                InfoCard(
                    title = strings.permBattery,
                    body = strings.permBatteryBody,
                    actionLabel = strings.grant,
                    accent = accents.uncertain,
                    icon = Icons.Rounded.BatteryAlert,
                    onAction = {
                        runCatching {
                            context.startActivity(
                                Intent(AndroidSettings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                                    .setData(Uri.parse("package:${context.packageName}"))
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        }
                    }
                )
            }
        }

        if (settings.alerts.isEmpty()) {
            item {
                EmptyState(
                    Icons.Rounded.NotificationsActive,
                    strings.alertsEmpty,
                    strings.alertsEmptyBody,
                    strings.newAlert,
                    onNew
                )
            }
        } else {
            items(items = settings.alerts, key = { it.id }) { alert ->
                AlertRow(
                    alert = alert,
                    onClick = { onEdit(alert) },
                    onToggle = { onToggle(alert.copy(enabled = it)) }
                )
            }
        }

        item {
            OutlinedButton(
                onClick = onTest,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                ButtonLabel(strings.testNotification, Icons.AutoMirrored.Rounded.Send)
            }
        }
    }
}

@Composable
private fun AlertRow(alert: Alert, onClick: () -> Unit, onToggle: (Boolean) -> Unit) {
    val strings = AppTheme.strings
    val format = AppTheme.format

    SectionCard(modifier = Modifier.clickable(onClick = onClick), contentPadding = 14) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    format.clock(alert.hour, alert.minute),
                    style = MaterialTheme.typography.displaySmall,
                    color = if (alert.enabled) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    buildString {
                        if (alert.label.isNotBlank()) { append(alert.label); append(" · ") }
                        append(format.daysSummary(alert.days))
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(checked = alert.enabled, onCheckedChange = onToggle)
        }
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Pill(
                AdviceText.coverageName(alert.coverage, strings),
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            )
            if (alert.onlyWhenNeeded) {
                Spacer(Modifier.width(8.dp))
                Pill(
                    strings.onlyWhenNeededShort,
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Normal
                )
            }
        }
        if (alert.enabled) {
            alert.nextTriggerMs()?.let {
                Spacer(Modifier.height(8.dp))
                Caption(strings.nextFire(format.relativeShort(it)))
            }
        }
    }
}

private fun ignoresBatteryOptimisation(context: Context): Boolean {
    val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return true
    return powerManager.isIgnoringBatteryOptimizations(context.packageName)
}
