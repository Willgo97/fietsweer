package nl.fietsweer.app.ui.screens.settings

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import nl.fietsweer.app.ui.components.ButtonLabel
import nl.fietsweer.app.ui.components.Caption
import nl.fietsweer.app.ui.components.SectionCard
import nl.fietsweer.app.ui.theme.AppTheme

@Composable
internal fun AboutPage(versionName: String, onResetSetup: () -> Unit) {
    val strings = AppTheme.strings
    SectionCard {
        Text(strings.aboutBody, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(10.dp))
        Caption(strings.aboutData)
        Spacer(Modifier.height(10.dp))
        Caption(strings.version(versionName))
        Spacer(Modifier.height(6.dp))
        TextButton(onClick = onResetSetup) {
            ButtonLabel(strings.resetSetup, Icons.Rounded.RestartAlt)
        }
    }
}
