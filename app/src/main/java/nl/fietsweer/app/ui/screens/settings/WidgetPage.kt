package nl.fietsweer.app.ui.screens.settings

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.widget.FrameLayout
import android.widget.RemoteViews
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.FilledTonalButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import nl.fietsweer.app.data.RouteForecast
import nl.fietsweer.app.data.Settings
import nl.fietsweer.app.ui.components.ButtonLabel
import nl.fietsweer.app.ui.components.SectionCard
import nl.fietsweer.app.ui.components.SectionLabel
import nl.fietsweer.app.ui.theme.AppTheme
import nl.fietsweer.app.widget.JacketWidget
import nl.fietsweer.app.widget.WidgetRenderer

@Composable
internal fun WidgetPage(settings: Settings, forecast: RouteForecast?) {
    val strings = AppTheme.strings
    val context = LocalContext.current
    SectionCard {
        val snapshot = remember(settings, forecast) {
            WidgetRenderer.snapshotFor(settings, forecast)
        }
        WidgetPreview(strings.widgetSizeNormal, 132) {
            WidgetRenderer.singleLayout(it, settings, snapshot, compact = false)
        }
        Spacer(Modifier.height(14.dp))
        WidgetPreview(strings.widgetSizeSlim, 64) {
            WidgetRenderer.singleLayout(it, settings, snapshot, compact = true)
        }
        Spacer(Modifier.height(12.dp))
        FilledTonalButton(
            onClick = {
                val manager = AppWidgetManager.getInstance(context)
                val provider = ComponentName(context, JacketWidget::class.java)
                if (manager.isRequestPinAppWidgetSupported) {
                    manager.requestPinAppWidget(provider, null, null)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp)
        ) {
            ButtonLabel(strings.widgetAdd, Icons.Rounded.Widgets)
        }
    }
}

@Composable
private fun WidgetPreview(
    label: String,
    height: Int,
    build: (Context) -> RemoteViews
) {
    SectionLabel(label)
    Spacer(Modifier.height(6.dp))
    Box(
        Modifier
            .fillMaxWidth()
            .height(height.dp)
    ) {
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
