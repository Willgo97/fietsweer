package nl.fietsweer.app.ui.screens.settings

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.widget.FrameLayout
import android.widget.RemoteViews
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import nl.fietsweer.app.data.RouteForecast
import nl.fietsweer.app.data.Settings
import nl.fietsweer.app.data.WidgetStyle
import nl.fietsweer.app.ui.components.ButtonLabel
import nl.fietsweer.app.ui.components.SectionCard
import nl.fietsweer.app.ui.components.SectionLabel
import nl.fietsweer.app.ui.theme.AppTheme
import nl.fietsweer.app.widget.BadgeWidget
import nl.fietsweer.app.widget.JacketWidget
import nl.fietsweer.app.widget.WidgetArtwork
import nl.fietsweer.app.widget.WidgetRenderer
import nl.fietsweer.app.widget.WidgetSize

@Composable
internal fun WidgetPage(settings: Settings, forecast: RouteForecast?, onUpdate: ((Settings) -> Settings) -> Unit) {
    val strings = AppTheme.strings
    val context = LocalContext.current
    val content = remember(settings, forecast) { WidgetRenderer.contentFor(settings, forecast) }
    val artwork = remember(content) { content?.let { WidgetRenderer.artworkFor(context, it) } ?: WidgetArtwork(emptyMap()) }
    val snapshot = content?.snapshot

    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        SectionCard(title = strings.widgetRowTitle, subtitle = strings.widgetRowSub) {
            for (style in WidgetStyle.entries) {
                val selected = settings.widgetStyle == style
                Box(
                    Modifier
                        .padding(vertical = 4.dp)
                        .border(
                            if (selected) 2.dp else 1.dp,
                            if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                            RoundedCornerShape(26.dp)
                        )
                        .padding(3.dp)
                        .clickable { onUpdate { it.copy(widgetStyle = style) } }
                ) {
                    WidgetPreview(Modifier.fillMaxWidth().height(64.dp)) {
                        WidgetRenderer.layout(it, settings, snapshot, artwork, WidgetSize.ROW, style)
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            AddButton(strings.widgetAdd) { pin(context, JacketWidget::class.java) }
        }

        SectionCard(title = strings.widgetBiggerTitle, subtitle = strings.widgetBiggerSub) {
            WidgetPreview(Modifier.fillMaxWidth().height(170.dp)) {
                WidgetRenderer.layout(it, settings, snapshot, artwork, WidgetSize.LARGE)
            }
            Spacer(Modifier.height(10.dp))
            Row {
                WidgetPreview(Modifier.size(92.dp)) {
                    WidgetRenderer.badgeViews(it, snapshot, artwork)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    SectionLabel(strings.badgeTitle)
                    Spacer(Modifier.height(8.dp))
                    AddButton(strings.badgeAdd) { pin(context, BadgeWidget::class.java) }
                }
            }
        }
    }
}

private fun pin(context: Context, provider: Class<*>) {
    val manager = AppWidgetManager.getInstance(context)
    if (manager.isRequestPinAppWidgetSupported) manager.requestPinAppWidget(ComponentName(context, provider), null, null)
}

@Composable
private fun AddButton(label: String, onClick: () -> Unit) {
    FilledTonalButton(onClick = onClick, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
        ButtonLabel(label, Icons.Rounded.Widgets)
    }
}

@Composable
private fun WidgetPreview(modifier: Modifier, build: (Context) -> RemoteViews) {
    Box(modifier) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context -> FrameLayout(context) },
            update = { frame ->
                frame.removeAllViews()
                frame.addView(build(frame.context).apply(frame.context, frame))
            }
        )
    }
}
