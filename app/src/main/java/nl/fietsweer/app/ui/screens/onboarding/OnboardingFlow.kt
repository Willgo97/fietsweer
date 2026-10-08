package nl.fietsweer.app.ui.screens.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import nl.fietsweer.app.data.Settings
import nl.fietsweer.app.ui.components.SectionLabel
import nl.fietsweer.app.ui.map.MapTheme
import nl.fietsweer.app.ui.map.RouteEnd
import nl.fietsweer.app.ui.map.RouteEndPicker
import nl.fietsweer.app.ui.theme.AppTheme

private const val STEP_COUNT = 5

@Composable
fun OnboardingFlow(
    settings: Settings,
    mapTheme: MapTheme,
    onUpdate: ((Settings) -> Settings) -> Unit,
    onFinish: () -> Unit
) {
    val strings = AppTheme.strings
    var step by remember { mutableIntStateOf(if (settings.home != null) 1 else 0) }

    BackHandler(enabled = step > 0) { step-- }

    AnimatedContent(
        targetState = step,
        transitionSpec = {
            val forward = targetState > initialState
            (slideInHorizontally { if (forward) it / 3 else -it / 3 } + fadeIn()) togetherWith
                (slideOutHorizontally { if (forward) -it / 4 else it / 4 } + fadeOut())
        },
        label = "onboarding"
    ) { current ->
        when (current) {
            0 -> WelcomeStep { step = 1 }

            1 -> RouteEndPicker(
                end = RouteEnd.HOME,
                title = strings.setHomeTitle,
                settings = settings,
                mapTheme = mapTheme,
                onCancel = { step = 0 },
                onConfirm = { place ->
                    onUpdate { it.copy(home = place) }
                    step = 2
                }
            )

            2 -> RouteEndPicker(
                end = RouteEnd.WORK,
                title = strings.setWorkTitle,
                settings = settings,
                mapTheme = mapTheme,
                onCancel = { step = 1 },
                onConfirm = { place ->
                    onUpdate { it.copy(work = place) }
                    step = 3
                }
            )

            3 -> TimesStep(settings, onUpdate, onBack = { step = 2 }, onNext = { step = 4 })

            else -> NotifyStep(
                settings = settings,
                onUpdate = onUpdate,
                onBack = { step = 3 },
                onFinish = onFinish
            )
        }
    }
}

@Composable
internal fun StepScaffold(
    stepIndex: Int,
    title: String,
    body: String,
    onBack: () -> Unit,
    primaryLabel: String,
    onPrimary: () -> Unit,
    primaryIcon: Boolean = false,
    primaryEnabled: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    val strings = AppTheme.strings
    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            for (i in 1..STEP_COUNT) {
                Box(
                    Modifier
                        .weight(1f)
                        .height(4.dp)
                        .padding(end = 4.dp)
                        .background(
                            if (i <= stepIndex) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceContainerHighest,
                            CircleShape
                        )
                )
            }
        }
        Spacer(Modifier.height(18.dp))
        SectionLabel(strings.stepOf(stepIndex, STEP_COUNT))
        Spacer(Modifier.height(6.dp))
        Text(title, style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        Text(
            body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(22.dp))

        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth()
        ) { content() }

        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onBack) { Text(strings.back) }
            Spacer(Modifier.weight(1f))
            Button(onClick = onPrimary, enabled = primaryEnabled, shape = RoundedCornerShape(16.dp)) {
                if (primaryIcon) {
                    Icon(Icons.Rounded.Check, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                }
                Text(primaryLabel, modifier = Modifier.padding(vertical = 4.dp))
            }
        }
    }
}
