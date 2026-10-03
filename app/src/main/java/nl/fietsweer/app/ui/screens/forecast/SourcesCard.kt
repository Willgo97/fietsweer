package nl.fietsweer.app.ui.screens.forecast

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import nl.fietsweer.app.data.RouteForecast
import nl.fietsweer.app.data.SourceState
import nl.fietsweer.app.ui.components.Caption
import nl.fietsweer.app.ui.components.Dot
import nl.fietsweer.app.ui.components.SectionCard
import nl.fietsweer.app.ui.theme.AppTheme

@Composable
internal fun SourcesCard(forecast: RouteForecast) {
    val strings = AppTheme.strings
    val accents = AppTheme.accents
    SectionCard(title = strings.sources) {
        forecast.sources.forEach { source ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Dot(
                    when (source.state) {
                        SourceState.OK -> accents.dry
                        SourceState.EMPTY -> accents.uncertain
                        SourceState.FAILED -> accents.wet
                        SourceState.SKIPPED -> MaterialTheme.colorScheme.outline
                    },
                    8.dp
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    source.label,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.weight(1f)
                )
                if (source.note.isNotBlank()) Caption(source.note)
            }
        }
        Spacer(Modifier.height(8.dp))
        Caption(strings.aboutData)
    }
}
