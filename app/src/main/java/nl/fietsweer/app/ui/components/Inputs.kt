package nl.fietsweer.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import nl.fietsweer.app.data.Coverage
import nl.fietsweer.app.data.Leg
import nl.fietsweer.app.data.Settings
import nl.fietsweer.app.domain.AdviceText
import nl.fietsweer.app.domain.Engine
import nl.fietsweer.app.ui.theme.AppTheme
import kotlin.math.roundToInt

@Composable
fun <T> SegmentedChoice(
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest
    ) {
        Row(Modifier.padding(3.dp)) {
            for ((value, label) in options) {
                val active = value == selected
                val backgroundColor by animateColorAsState(
                    if (active) MaterialTheme.colorScheme.primary else Color.Transparent,
                    tween(200), label = "segBg"
                )
                val contentColor by animateColorAsState(
                    if (active) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    tween(200), label = "segFg"
                )
                Box(
                    Modifier
                        .weight(1f)
                        .background(backgroundColor, RoundedCornerShape(11.dp))
                        .clickable { onSelect(value) }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        label,
                        style = MaterialTheme.typography.labelLarge,
                        color = contentColor,
                        maxLines = 1,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
fun CoverageChoice(selected: Coverage, onSelect: (Coverage) -> Unit) {
    val strings = AppTheme.strings
    SegmentedChoice(
        options = Coverage.entries.map { it to AdviceText.coverageName(it, strings) },
        selected = selected,
        onSelect = onSelect,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
fun DayPicker(
    days: Set<Int>,
    onChange: (Set<Int>) -> Unit,
    modifier: Modifier = Modifier
) {
    val letters = AppTheme.strings.dayLettersShort
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        for (day in 1..7) {
            val active = day in days
            val backgroundColor by animateColorAsState(
                if (active) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surfaceContainerHighest,
                tween(180), label = "dayBg"
            )
            Box(
                Modifier
                    .weight(1f)
                    .height(42.dp)
                    .background(backgroundColor, CircleShape)
                    .clickable {
                        onChange(if (active) days - day else days + day)
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    letters[day - 1],
                    style = MaterialTheme.typography.labelLarge,
                    color = if (active) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerDialog(
    hour: Int,
    minute: Int,
    title: String,
    onDismiss: () -> Unit,
    onConfirm: (Int, Int) -> Unit
) {
    val strings = AppTheme.strings
    val state = rememberTimePickerState(initialHour = hour, initialMinute = minute, is24Hour = true)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                TimePicker(state = state)
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(state.hour, state.minute) }) { Text(strings.done) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(strings.cancel) } }
    )
}

@Composable
fun RideTimeDialog(
    leg: Leg,
    settings: Settings,
    onUpdate: ((Settings) -> Settings) -> Unit,
    onDismiss: () -> Unit
) {
    val strings = AppTheme.strings
    TimePickerDialog(
        settings.hourFor(leg), settings.minuteFor(leg),
        if (leg == Leg.OUTBOUND) strings.outboundTime else strings.returnTime,
        onDismiss = onDismiss,
        onConfirm = { hour, minute ->
            onUpdate {
                if (leg == Leg.OUTBOUND) it.copy(outboundHour = hour, outboundMinute = minute)
                else it.copy(returnHour = hour, returnMinute = minute)
            }
            onDismiss()
        }
    )
}

@Composable
fun TimeChip(
    hour: Int,
    minute: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        Text(
            AppTheme.format.clock(hour, minute),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp)
        )
    }
}

@Composable
fun LabeledSlider(
    label: String,
    valueText: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    steps: Int,
    onChange: (Float) -> Unit,
    onChangeFinished: () -> Unit = {}
) {
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                label,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
            Text(
                valueText,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }
        AppSlider(
            value = value,
            onValueChange = onChange,
            valueRange = range,
            steps = steps,
            onValueChangeFinished = onChangeFinished
        )
    }
}

@Composable
fun CyclingSpeedSlider(settings: Settings, onUpdate: ((Settings) -> Settings) -> Unit) {
    val strings = AppTheme.strings
    val format = AppTheme.format
    var speed by remember(settings.speedKmh) { mutableFloatStateOf(settings.speedKmh.toFloat()) }
    val distance = remember(settings.home, settings.work) { settings.routeKm }

    LabeledSlider(
        label = strings.cyclingSpeed,
        valueText = strings.rideTimeIs(
            format.kilometres(distance),
            Engine.rideDurationMinutes(distance, speed.toDouble())
        ),
        value = speed,
        range = 10f..32f,
        steps = 0,
        onChange = { speed = it },
        onChangeFinished = { onUpdate { it.copy(speedKmh = speed.roundToInt()) } }
    )
    Text(
        format.speedWithUnit(speed.toDouble()),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
fun RowScope.ButtonLabel(text: String, icon: ImageVector) {
    Icon(icon, null, modifier = Modifier.size(18.dp))
    Spacer(Modifier.width(8.dp))
    Text(text)
}
