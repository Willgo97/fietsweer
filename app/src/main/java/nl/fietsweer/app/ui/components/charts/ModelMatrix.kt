package nl.fietsweer.app.ui.components.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import nl.fietsweer.app.domain.RideAssessment
import nl.fietsweer.app.ui.theme.AppTheme
import nl.fietsweer.app.ui.theme.microLabel

@Composable
fun ModelMatrix(
    slots: List<RideAssessment>,
    modifier: Modifier = Modifier
) {
    if (slots.isEmpty() || slots.first().modelVerdicts.isEmpty()) return
    val accents = AppTheme.accents
    val strings = AppTheme.strings

    Column(modifier.fillMaxWidth()) {
        slots.first().modelVerdicts.forEachIndexed { modelIndex, model ->
            MatrixRow(
                label = model.label,
                labelStyle = MaterialTheme.typography.microLabel.copy(fontWeight = FontWeight.Normal),
                labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                rowHeight = 15.dp,
                cellHeight = 11.dp,
                cornerDp = 1f,
                slots = slots
            ) { slot ->
                if (slot.modelVerdicts.getOrNull(modelIndex)?.wet == true) accents.wet else accents.dry
            }
        }

        Spacer(Modifier.height(8.dp))

        MatrixRow(
            label = strings.combined,
            labelStyle = MaterialTheme.typography.microLabel,
            labelColor = MaterialTheme.colorScheme.onSurface,
            rowHeight = 20.dp,
            cellHeight = 16.dp,
            cornerDp = 1.5f,
            slots = slots
        ) { slot -> accents.forRisk(slot.risk) }
    }
}

@Composable
private fun MatrixRow(
    label: String,
    labelStyle: TextStyle,
    labelColor: Color,
    rowHeight: Dp,
    cellHeight: Dp,
    cornerDp: Float,
    slots: List<RideAssessment>,
    colorFor: (RideAssessment) -> Color
) {
    val density = LocalDensity.current.density
    Row(
        Modifier
            .fillMaxWidth()
            .height(rowHeight),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            style = labelStyle,
            color = labelColor,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.width(118.dp)
        )
        Spacer(Modifier.width(6.dp))
        Canvas(
            Modifier
                .weight(1f)
                .height(cellHeight)
        ) {
            val cellWidth = size.width / slots.size
            slots.forEachIndexed { i, slot ->
                drawRoundRect(
                    color = colorFor(slot).copy(alpha = if (slot.isNight) 0.38f else 1f),
                    topLeft = Offset(i * cellWidth, 0f),
                    size = Size((cellWidth - 0.6f * density).coerceAtLeast(0.8f), size.height),
                    cornerRadius = CornerRadius(cornerDp * density)
                )
            }
        }
    }
}
