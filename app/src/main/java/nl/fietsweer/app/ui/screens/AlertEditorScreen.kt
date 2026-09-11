package nl.fietsweer.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import nl.fietsweer.app.data.Alert
import nl.fietsweer.app.data.Coverage
import nl.fietsweer.app.notify.AlertScheduler
import nl.fietsweer.app.ui.components.DayPicker
import nl.fietsweer.app.ui.components.SectionCard
import nl.fietsweer.app.ui.components.SegmentedChoice
import nl.fietsweer.app.ui.components.TimeChip
import nl.fietsweer.app.ui.components.TimePickerDialog
import nl.fietsweer.app.ui.theme.AppTheme
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertEditorScreen(
    original: Alert,
    isNew: Boolean,
    onClose: () -> Unit,
    onSave: (Alert) -> Unit,
    onDelete: (String) -> Unit
) {
    val t = AppTheme.txt
    val fmt = AppTheme.fmt

    var draft by remember { mutableStateOf(original) }
    var showTime by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isNew) t.newAlert else t.editAlert) },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, t.back)
                    }
                },
                actions = {
                    if (!isNew) {
                        IconButton(onClick = { confirmDelete = true }) {
                            Icon(
                                Icons.Rounded.DeleteOutline, t.delete,
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            )
        }
    ) { inner ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(inner)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {

            SectionCard(title = t.alertTime) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    TimeChip(draft.hour, draft.minute, onClick = { showTime = true })
                }
            }

            SectionCard(title = t.alertDays) {
                DayPicker(draft.days, onChange = { draft = draft.copy(days = it) })
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    QuickDays(t.weekdays) { draft = draft.copy(days = setOf(1, 2, 3, 4, 5)) }
                    QuickDays(t.weekend) { draft = draft.copy(days = setOf(6, 7)) }
                    QuickDays(t.everyDay) { draft = draft.copy(days = (1..7).toSet()) }
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    AlertScheduler.nextTrigger(draft.copy(enabled = true))
                        ?.let { t.nextFire(fmt.dayTime(it)) } ?: t.neverRepeats,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            SectionCard(title = t.alertCoverage) {
                SegmentedChoice(
                    options = listOf(
                        Coverage.OUTBOUND to t.coverageOutbound,
                        Coverage.RETURN to t.coverageReturn,
                        Coverage.BOTH to t.coverageBoth
                    ),
                    selected = draft.coverage,
                    onSelect = { draft = draft.copy(coverage = it) },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            SectionCard {
                OutlinedTextField(
                    value = draft.label,
                    onValueChange = { draft = draft.copy(label = it.take(40)) },
                    label = { Text(t.alertLabel) },
                    placeholder = { Text(t.alertLabelHint) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                )
                Spacer(Modifier.height(14.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(t.onlyWhenNeeded, style = MaterialTheme.typography.bodyLarge)
                        Text(
                            t.onlyWhenNeededBody,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Switch(
                        checked = draft.onlyWhenNeeded,
                        onCheckedChange = { draft = draft.copy(onlyWhenNeeded = it) }
                    )
                }
            }

            Button(
                onClick = { onSave(draft.copy(enabled = true)) },
                enabled = draft.days.isNotEmpty(),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Rounded.Check, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(t.save)
            }
            Spacer(Modifier.height(20.dp))
        }
    }

    if (showTime) {
        TimePickerDialog(
            hour = draft.hour,
            minute = draft.minute,
            title = t.alertTime,
            onDismiss = { showTime = false },
            onConfirm = { h, m ->
                draft = draft.copy(hour = h, minute = m)
                showTime = false
            }
        )
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(t.deleteAlertConfirm) },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; onDelete(draft.id) }) {
                    Text(t.delete, color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text(t.cancel) }
            }
        )
    }
}

@Composable
private fun QuickDays(label: String, onClick: () -> Unit) {
    TextButton(onClick = onClick, shape = RoundedCornerShape(12.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge)
    }
}
