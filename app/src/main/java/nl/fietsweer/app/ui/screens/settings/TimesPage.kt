package nl.fietsweer.app.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import nl.fietsweer.app.data.Leg
import nl.fietsweer.app.data.MAX_SLACK_MINUTES
import nl.fietsweer.app.data.Settings
import nl.fietsweer.app.domain.AdviceText
import nl.fietsweer.app.ui.components.LabeledSlider
import nl.fietsweer.app.ui.components.RideTimeDialog
import nl.fietsweer.app.ui.components.SectionCard
import nl.fietsweer.app.ui.components.SectionLabel
import nl.fietsweer.app.ui.components.SoftDivider
import nl.fietsweer.app.ui.theme.AppTheme
import kotlin.math.roundToInt

@Composable
internal fun TimesPage(settings: Settings, onUpdate: ((Settings) -> Settings) -> Unit) {
    val strings = AppTheme.strings
    var editingLeg by remember { mutableStateOf<Leg?>(null) }

    SectionCard {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TimeTile(
                strings.outboundTime,
                settings.outboundHour, settings.outboundMinute,
                Modifier.weight(1f)
            ) { editingLeg = Leg.OUTBOUND }
            TimeTile(
                strings.returnTime,
                settings.returnHour, settings.returnMinute,
                Modifier.weight(1f)
            ) { editingLeg = Leg.RETURN }
        }
        Spacer(Modifier.height(20.dp))
        SectionLabel(strings.settingsFlex)
        Spacer(Modifier.height(10.dp))
        FlexEditor(Leg.OUTBOUND, settings, onUpdate)
        Spacer(Modifier.height(14.dp))
        SoftDivider()
        Spacer(Modifier.height(14.dp))
        FlexEditor(Leg.RETURN, settings, onUpdate)
    }

    editingLeg?.let { leg ->
        RideTimeDialog(leg, settings, onUpdate, onDismiss = { editingLeg = null })
    }
}

@Composable
private fun FlexEditor(
    leg: Leg,
    settings: Settings,
    onUpdate: ((Settings) -> Settings) -> Unit
) {
    val strings = AppTheme.strings
    val format = AppTheme.format

    var earlyMinutes by remember(leg, settings.earlyMinutesFor(leg)) {
        mutableFloatStateOf(settings.earlyMinutesFor(leg).toFloat())
    }
    var lateMinutes by remember(leg, settings.lateMinutesFor(leg)) {
        mutableFloatStateOf(settings.lateMinutesFor(leg).toFloat())
    }

    val plannedMinuteOfDay = settings.hourFor(leg) * 60 + settings.minuteFor(leg)
    fun slackText(minutes: Float): String =
        if (minutes < 1f) strings.flexNone else format.hoursMinutes(minutes.roundToInt())

    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            AdviceText.legName(leg, strings),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        Text(
            "${format.clock(plannedMinuteOfDay - earlyMinutes.roundToInt())} – " +
                format.clock(plannedMinuteOfDay + lateMinutes.roundToInt()),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
    }
    Spacer(Modifier.height(4.dp))
    LabeledSlider(
        label = strings.flexEarlier,
        valueText = slackText(earlyMinutes),
        value = earlyMinutes,
        range = 0f..MAX_SLACK_MINUTES.toFloat(),
        steps = 11,
        onChange = { earlyMinutes = it },
        onChangeFinished = { onUpdate { it.withEarlyMinutes(leg, earlyMinutes.roundToInt()) } }
    )
    LabeledSlider(
        label = strings.flexLater,
        valueText = slackText(lateMinutes),
        value = lateMinutes,
        range = 0f..MAX_SLACK_MINUTES.toFloat(),
        steps = 11,
        onChange = { lateMinutes = it },
        onChangeFinished = { onUpdate { it.withLateMinutes(leg, lateMinutes.roundToInt()) } }
    )
}

@Composable
private fun TimeTile(
    label: String,
    hour: Int,
    minute: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Column(Modifier.padding(14.dp)) {
            SectionLabel(label, maxLines = 2)
            Spacer(Modifier.height(6.dp))
            Text(
                AppTheme.format.clock(hour, minute),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
