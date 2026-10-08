package nl.fietsweer.app.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
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
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    Text(
        text,
        style = MaterialTheme.typography.caption,
        color = color,
        modifier = modifier
    )
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
