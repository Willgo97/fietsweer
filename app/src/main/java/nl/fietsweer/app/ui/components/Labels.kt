package nl.fietsweer.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import nl.fietsweer.app.ui.theme.caption

@Composable
fun SectionLabel(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    maxLines: Int = Int.MAX_VALUE
) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = color,
        maxLines = maxLines,
        modifier = modifier
    )
}

@Composable
fun Caption(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    textAlign: TextAlign? = null,
    maxLines: Int = Int.MAX_VALUE
) {
    Text(
        text,
        style = MaterialTheme.typography.caption,
        color = color,
        textAlign = textAlign,
        maxLines = maxLines,
        modifier = modifier
    )
}

@Composable
fun Dot(color: Color, size: Dp) {
    Box(
        Modifier
            .size(size)
            .background(color, CircleShape)
    )
}

@Composable
fun LegendDot(text: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Dot(color, 9.dp)
        Spacer(Modifier.width(5.dp))
        Caption(text)
    }
}

@Composable
fun Pill(
    text: String,
    containerColor: Color,
    contentColor: Color,
    fontWeight: FontWeight = FontWeight.Medium
) {
    Surface(shape = CircleShape, color = containerColor) {
        Text(
            text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = fontWeight,
            color = contentColor,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}
