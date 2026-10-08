package nl.fietsweer.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp

private val THUMB_SIZE = 18.dp

// A thin track with a small round thumb, used for every slider in the app.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    modifier: Modifier = Modifier,
    steps: Int = 0,
    onValueChangeFinished: () -> Unit = {}
) {
    val active = MaterialTheme.colorScheme.primary
    val inactive = MaterialTheme.colorScheme.surfaceContainerHighest
    Slider(
        value = value,
        onValueChange = onValueChange,
        valueRange = valueRange,
        steps = steps,
        onValueChangeFinished = onValueChangeFinished,
        modifier = modifier,
        thumb = {
            Box(
                Modifier
                    .size(THUMB_SIZE)
                    .background(active, CircleShape)
            )
        },
        track = { state ->
            val span = (state.valueRange.endInclusive - state.valueRange.start).takeIf { it > 0f } ?: 1f
            val fraction = ((state.value - state.valueRange.start) / span).coerceIn(0f, 1f)
            Canvas(
                Modifier
                    .fillMaxWidth()
                    .height(4.dp)
            ) {
                // The thumb's centre travels from half a thumb in to half a thumb from the end.
                val inset = THUMB_SIZE.toPx() / 2
                val y = size.height / 2
                val end = inset + (size.width - 2 * inset) * fraction
                drawLine(inactive, Offset(inset, y), Offset(size.width - inset, y), strokeWidth = size.height, cap = StrokeCap.Round)
                drawLine(active, Offset(inset, y), Offset(end, y), strokeWidth = size.height, cap = StrokeCap.Round)
            }
        }
    )
}
