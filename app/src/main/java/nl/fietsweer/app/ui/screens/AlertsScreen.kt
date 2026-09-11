package nl.fietsweer.app.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings as AndroidSettings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.BatteryAlert
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import nl.fietsweer.app.data.Alert
import nl.fietsweer.app.data.Coverage
import nl.fietsweer.app.data.Settings
import nl.fietsweer.app.notify.AlertScheduler
import nl.fietsweer.app.notify.Notifier
import nl.fietsweer.app.ui.components.EmptyState
import nl.fietsweer.app.ui.components.InfoCard
import nl.fietsweer.app.ui.components.SectionCard
import nl.fietsweer.app.ui.theme.AppTheme
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue

@Composable
fun AlertsScreen(
    settings: Settings,
    contentPadding: PaddingValues,
    onEdit: (Alert) -> Unit,
    onToggle: (Alert) -> Unit,
    onNew: () -> Unit,
    onTest: () -> Unit
) {
    val t = AppTheme.txt
    val fmt = AppTheme.fmt
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

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp, end = 16.dp,
            top = contentPadding.calculateTopPadding() + 8.dp,
            bottom = contentPadding.calculateBottomPadding() + 24.dp
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    t.alertsTitle,
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.weight(1f)
                )
                Button(onClick = onNew, shape = RoundedCornerShape(14.dp)) {
                    Icon(Icons.Rounded.Add, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(t.add)
                }
            }
        }

        if (!canPost) {
            item {
                InfoCard(
                    title = t.permNotifications,
                    body = t.permNotificationsBody,
                    actionLabel = t.grant,
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
                    title = t.permExact,
                    body = t.permExactBody,
                    actionLabel = t.grant,
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
                    title = t.permBattery,
                    body = t.permBatteryBody,
                    actionLabel = t.grant,
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
                    t.alertsEmpty,
                    t.alertsEmptyBody,
                    t.newAlert,
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
                Icon(Icons.AutoMirrored.Rounded.Send, null, modifier = Modifier.size(17.dp))
                Spacer(Modifier.width(8.dp))
                Text(t.testNotification)
            }
        }
    }
}

@Composable
private fun AlertRow(alert: Alert, onClick: () -> Unit, onToggle: (Boolean) -> Unit) {
    val t = AppTheme.txt
    val fmt = AppTheme.fmt

    SectionCard(modifier = Modifier.clickable(onClick = onClick), contentPadding = 14) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    String.format(java.util.Locale.ROOT, "%02d:%02d", alert.hour, alert.minute),
                    style = MaterialTheme.typography.displaySmall,
                    color = if (alert.enabled) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    buildString {
                        if (alert.label.isNotBlank()) { append(alert.label); append(" · ") }
                        append(fmt.daysSummary(alert.days))
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(checked = alert.enabled, onCheckedChange = onToggle)
        }
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.secondaryContainer) {
                Text(
                    when (alert.coverage) {
                        Coverage.OUTBOUND -> t.coverageOutbound
                        Coverage.RETURN -> t.coverageReturn
                        Coverage.BOTH -> t.coverageBoth
                    },
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
            if (alert.onlyWhenNeeded) {
                Spacer(Modifier.width(8.dp))
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceContainerHighest) {
                    Text(
                        t.onlyWhenNeededShort,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Normal,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        }
        if (alert.enabled) {
            AlertScheduler.nextTrigger(alert)?.let {
                Spacer(Modifier.height(8.dp))
                Text(
                    t.nextFire(fmt.relativeShort(it)),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Normal,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun ignoresBatteryOptimisation(context: Context): Boolean {
    val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return true
    return pm.isIgnoringBatteryOptimizations(context.packageName)
}
