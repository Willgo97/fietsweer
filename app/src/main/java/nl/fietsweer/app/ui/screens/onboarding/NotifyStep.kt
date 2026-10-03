package nl.fietsweer.app.ui.screens.onboarding

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import nl.fietsweer.app.data.Alert
import nl.fietsweer.app.data.Settings
import nl.fietsweer.app.ui.components.CoverageChoice
import nl.fietsweer.app.ui.components.DayPicker
import nl.fietsweer.app.ui.components.SectionCard
import nl.fietsweer.app.ui.components.TimeChip
import nl.fietsweer.app.ui.components.TimePickerDialog
import nl.fietsweer.app.ui.theme.AppTheme

@Composable
internal fun NotifyStep(
    settings: Settings,
    onUpdate: ((Settings) -> Settings) -> Unit,
    onBack: () -> Unit,
    onFinish: () -> Unit
) {
    val strings = AppTheme.strings

    var draft by remember {
        mutableStateOf(
            settings.alerts.firstOrNull() ?: Alert.create()
        )
    }
    var editingTime by remember { mutableStateOf(false) }
    var granted by remember { mutableStateOf(true) }

    val requestPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted = it }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    StepScaffold(
        stepIndex = 5,
        title = strings.setNotifyTitle,
        body = strings.setNotifyBody,
        onBack = onBack,
        primaryLabel = strings.finishGo,
        primaryIcon = true,
        onPrimary = {
            onUpdate { it.withAlert(draft).copy(setupDone = true) }
            onFinish()
        }
    ) {
        SectionCard(title = strings.alertTime) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                TimeChip(draft.hour, draft.minute) { editingTime = true }
            }
            Spacer(Modifier.height(16.dp))
            DayPicker(draft.days, onChange = { draft = draft.copy(days = it) })
            Spacer(Modifier.height(16.dp))
            CoverageChoice(draft.coverage) { draft = draft.copy(coverage = it) }
        }
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Rounded.NotificationsActive, null,
                tint = if (granted) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.error,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                if (granted) strings.notificationsAllowed else strings.permNotificationsBody,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    if (editingTime) {
        TimePickerDialog(
            draft.hour, draft.minute, strings.alertTime,
            onDismiss = { editingTime = false },
            onConfirm = { hour, minute -> draft = draft.copy(hour = hour, minute = minute); editingTime = false }
        )
    }
}
