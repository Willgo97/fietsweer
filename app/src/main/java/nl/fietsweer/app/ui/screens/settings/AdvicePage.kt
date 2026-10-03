package nl.fietsweer.app.ui.screens.settings

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Radar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import nl.fietsweer.app.data.Settings
import nl.fietsweer.app.ui.components.LabeledSlider
import nl.fietsweer.app.ui.components.SectionCard
import nl.fietsweer.app.ui.components.SegmentedChoice
import nl.fietsweer.app.ui.components.SoftDivider
import nl.fietsweer.app.ui.components.SwitchRow
import nl.fietsweer.app.ui.theme.AppTheme
import kotlin.math.roundToInt

@Composable
internal fun AdvicePage(settings: Settings, onUpdate: ((Settings) -> Settings) -> Unit) {
    val strings = AppTheme.strings
    val format = AppTheme.format
    var rainJacketPercent by remember(settings.rainJacketPercent) {
        mutableFloatStateOf(settings.rainJacketPercent.toFloat())
    }
    var vestTemp by remember(settings.vestBelow) {
        mutableFloatStateOf(settings.vestBelow.toFloat())
    }
    var winterTemp by remember(settings.winterCoatBelow) {
        mutableFloatStateOf(settings.winterCoatBelow.toFloat())
    }

    SectionCard {
        Text(strings.whatIsWet, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(8.dp))
        SegmentedChoice(
            options = listOf(
                0.05 to strings.wetEveryDrop,
                0.2 to strings.wetDrizzle,
                0.6 to strings.wetShower
            ),
            selected = settings.wetThreshold,
            onSelect = { choice -> onUpdate { it.copy(wetThreshold = choice) } },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(18.dp))
        LabeledSlider(
            label = strings.rainJacketFrom,
            valueText = strings.rainJacketFromValue(rainJacketPercent.roundToInt()),
            value = rainJacketPercent,
            range = 10f..70f,
            steps = 0,
            onChange = { rainJacketPercent = it },
            onChangeFinished = {
                onUpdate { it.copy(rainJacketPercent = rainJacketPercent.roundToInt()) }
            }
        )
        Spacer(Modifier.height(10.dp))
        LabeledSlider(
            label = strings.vestBelowLabel,
            valueText = strings.feltOnBike(format.temp(vestTemp.toDouble())),
            value = vestTemp,
            range = 8f..26f,
            steps = 0,
            onChange = { vestTemp = it },
            onChangeFinished = {
                onUpdate {
                    val vest = vestTemp.toDouble()
                    it.copy(
                        vestBelow = vest,
                        winterCoatBelow = minOf(it.winterCoatBelow, vest - 2.0)
                    )
                }
            }
        )
        Spacer(Modifier.height(10.dp))
        LabeledSlider(
            label = strings.winterBelowLabel,
            valueText = strings.feltOnBike(format.temp(winterTemp.toDouble())),
            value = winterTemp,
            range = -6f..18f,
            steps = 0,
            onChange = { winterTemp = it },
            onChangeFinished = {
                onUpdate {
                    val winter = winterTemp.toDouble()
                    it.copy(
                        winterCoatBelow = winter,
                        vestBelow = maxOf(it.vestBelow, winter + 2.0)
                    )
                }
            }
        )
        Spacer(Modifier.height(8.dp))
        SoftDivider()
        SwitchRow(
            title = strings.useRadarTitle,
            icon = Icons.Rounded.Radar,
            checked = settings.useRadar,
            onCheckedChange = { checked -> onUpdate { it.copy(useRadar = checked) } }
        )
    }
}
