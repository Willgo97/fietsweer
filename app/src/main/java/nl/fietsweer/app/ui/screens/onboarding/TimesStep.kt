package nl.fietsweer.app.ui.screens.onboarding

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import nl.fietsweer.app.data.Leg
import nl.fietsweer.app.data.Settings
import nl.fietsweer.app.ui.components.Caption
import nl.fietsweer.app.ui.components.CyclingSpeedSlider
import nl.fietsweer.app.ui.components.RideTimeDialog
import nl.fietsweer.app.ui.components.SectionCard
import nl.fietsweer.app.ui.components.TimeChip
import nl.fietsweer.app.ui.theme.AppTheme

@Composable
internal fun TimesStep(
    settings: Settings,
    onUpdate: ((Settings) -> Settings) -> Unit,
    onBack: () -> Unit,
    onNext: () -> Unit
) {
    val strings = AppTheme.strings
    var editingLeg by remember { mutableStateOf<Leg?>(null) }

    StepScaffold(
        stepIndex = 4,
        title = strings.setTimesTitle,
        body = strings.setTimesBody,
        onBack = onBack,
        primaryLabel = strings.next,
        onPrimary = onNext
    ) {
        SectionCard {
            RideTimeRow(strings.outboundTime, settings.home?.name, settings.outboundHour, settings.outboundMinute) {
                editingLeg = Leg.OUTBOUND
            }
            Spacer(Modifier.height(16.dp))
            RideTimeRow(strings.returnTime, settings.work?.name, settings.returnHour, settings.returnMinute) {
                editingLeg = Leg.RETURN
            }
        }

        Spacer(Modifier.height(14.dp))

        SectionCard(title = strings.settingsRiding) {
            CyclingSpeedSlider(settings, onUpdate)
        }
    }

    editingLeg?.let { leg ->
        RideTimeDialog(leg, settings, onUpdate, onDismiss = { editingLeg = null })
    }
}

@Composable
private fun RideTimeRow(label: String, placeName: String?, hour: Int, minute: Int, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Caption(placeName ?: "")
        }
        TimeChip(hour, minute, onClick = onClick)
    }
}
