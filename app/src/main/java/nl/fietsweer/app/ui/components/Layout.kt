package nl.fietsweer.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.FlowRowScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

val CARD_GAP = 5.dp

@Composable
fun ScreenList(contentPadding: PaddingValues, content: LazyListScope.() -> Unit) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = CARD_GAP, end = CARD_GAP,
            top = contentPadding.calculateTopPadding() + CARD_GAP,
            bottom = contentPadding.calculateBottomPadding()
        ),
        verticalArrangement = Arrangement.spacedBy(CARD_GAP),
        content = content
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChipFlow(
    modifier: Modifier = Modifier,
    content: @Composable FlowRowScope.() -> Unit
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        content = content
    )
}


// One screen, no scrolling: the child at stretchIndex takes whatever height is left,
// and the column only scrolls when even its minimum does not fit.
@Composable
fun FillScreen(contentPadding: PaddingValues, stretchIndex: Int, stretchMin: Dp, content: @Composable () -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val top = contentPadding.calculateTopPadding() + CARD_GAP
        val bottom = contentPadding.calculateBottomPadding()
        val available = maxHeight - top - bottom
        Layout(
            content,
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = CARD_GAP, end = CARD_GAP, top = top, bottom = bottom)
        ) { measurables, constraints ->
            val gap = CARD_GAP.roundToPx()
            val width = constraints.maxWidth
            val loose = Constraints(minWidth = width, maxWidth = width)
            val placeables = arrayOfNulls<Placeable>(measurables.size)
            measurables.forEachIndexed { i, m -> if (i != stretchIndex) placeables[i] = m.measure(loose) }
            if (stretchIndex in measurables.indices) {
                val used = placeables.sumOf { it?.height ?: 0 } + gap * (measurables.size - 1)
                val height = maxOf(available.roundToPx() - used, stretchMin.roundToPx())
                placeables[stretchIndex] = measurables[stretchIndex].measure(Constraints.fixed(width, height))
            }
            val total = placeables.sumOf { it!!.height } + gap * (measurables.size - 1)
            layout(width, total) {
                var y = 0
                for (placeable in placeables) {
                    placeable!!.place(0, y)
                    y += placeable.height + gap
                }
            }
        }
    }
}
