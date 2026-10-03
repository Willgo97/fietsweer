package nl.fietsweer.app.ui.components.charts

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import nl.fietsweer.app.ui.components.Caption
import nl.fietsweer.app.ui.theme.AppTheme

@Composable
internal fun TimeAxis(times: List<Long>) {
    val format = AppTheme.format
    Row(Modifier.fillMaxWidth()) {
        times.forEachIndexed { i, timeMs ->
            Caption(
                format.time(timeMs),
                modifier = Modifier.weight(1f),
                textAlign = when (i) {
                    0 -> TextAlign.Start
                    times.lastIndex -> TextAlign.End
                    else -> TextAlign.Center
                }
            )
        }
    }
}

internal const val AXIS_LABEL_COUNT = 5
