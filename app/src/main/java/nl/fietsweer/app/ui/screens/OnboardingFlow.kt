package nl.fietsweer.app.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.automirrored.rounded.DirectionsBike
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import nl.fietsweer.app.data.Alert
import nl.fietsweer.app.data.Coverage
import nl.fietsweer.app.data.Settings
import nl.fietsweer.app.domain.Engine
import nl.fietsweer.app.domain.Geo
import nl.fietsweer.app.domain.LatLon
import nl.fietsweer.app.ui.components.DayPicker
import nl.fietsweer.app.ui.components.LabeledSlider
import nl.fietsweer.app.ui.components.SectionCard
import nl.fietsweer.app.ui.components.SegmentedChoice
import nl.fietsweer.app.ui.components.TimeChip
import nl.fietsweer.app.ui.components.TimePickerDialog
import nl.fietsweer.app.ui.map.LocationPickerScreen
import nl.fietsweer.app.ui.map.MapTheme
import nl.fietsweer.app.ui.theme.AppTheme
import java.util.UUID
import kotlin.math.roundToInt
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue

private const val STEPS = 5

@Composable
fun OnboardingFlow(
    settings: Settings,
    mapTheme: MapTheme,
    onUpdate: ((Settings) -> Settings) -> Unit,
    onFinish: () -> Unit
) {
    val t = AppTheme.txt
    val accents = AppTheme.accents
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

            1 -> LocationPickerScreen(
                title = t.setHomeTitle,
                initial = settings.home,
                fallback = settings.home?.toLatLon() ?: LatLon(52.1326, 5.2913),
                mapTheme = mapTheme,
                accent = accents.dry,
                onCancel = { step = 0 },
                onConfirm = { p ->
                    onUpdate { it.copy(home = p) }
                    step = 2
                }
            )

            2 -> LocationPickerScreen(
                title = t.setWorkTitle,
                initial = settings.work,
                fallback = settings.work?.toLatLon()
                    ?: settings.home?.toLatLon()
                    ?: LatLon(52.1326, 5.2913),
                mapTheme = mapTheme,
                accent = accents.rain,
                onCancel = { step = 1 },
                onConfirm = { p ->
                    onUpdate { it.copy(work = p) }
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

// ------------------------------------------------------------------- welcome

@Composable
private fun WelcomeStep(onStart: () -> Unit) {
    val t = AppTheme.txt
    val accents = AppTheme.accents

    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    listOf(
                        MaterialTheme.colorScheme.primary,
                        accents.cold,
                        accents.dry
                    )
                )
            )
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(
                Color.White.copy(alpha = 0.08f),
                radius = size.width * 0.75f,
                center = Offset(size.width * 0.95f, size.height * 0.12f)
            )
            drawCircle(
                Color.White.copy(alpha = 0.06f),
                radius = size.width * 0.5f,
                center = Offset(size.width * 0.08f, size.height * 0.78f)
            )
        }
        Column(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(28.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.Start
        ) {
            Box(
                Modifier
                    .size(76.dp)
                    .background(Color.White.copy(alpha = 0.18f), RoundedCornerShape(24.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.AutoMirrored.Rounded.DirectionsBike, null,
                    tint = Color.White,
                    modifier = Modifier.size(40.dp)
                )
            }
            Spacer(Modifier.height(28.dp))
            Text(
                t.welcomeTitle,
                style = MaterialTheme.typography.displaySmall,
                color = Color.White
            )
            Spacer(Modifier.height(6.dp))
            Text(
                t.tagline,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White.copy(alpha = 0.9f)
            )
            Spacer(Modifier.height(22.dp))
            Text(
                t.welcomeBody,
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.92f)
            )
            Spacer(Modifier.height(36.dp))
            Button(
                onClick = onStart,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    t.welcomeStart,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(vertical = 6.dp)
                )
            }
        }
    }
}

// --------------------------------------------------------------------- times

@Composable
private fun TimesStep(
    settings: Settings,
    onUpdate: ((Settings) -> Settings) -> Unit,
    onBack: () -> Unit,
    onNext: () -> Unit
) {
    val t = AppTheme.txt
    val fmt = AppTheme.fmt
    var editingOutbound by remember { mutableStateOf(false) }
    var editingReturn by remember { mutableStateOf(false) }
    var speed by remember(settings.speedKmh) { mutableFloatStateOf(settings.speedKmh.toFloat()) }

    val distance = remember(settings.home, settings.work) {
        val h = settings.home; val w = settings.work
        if (h == null || w == null) 0.0 else Geo.haversineKm(h.toLatLon(), w.toLatLon()) * Geo.DETOUR_FACTOR
    }

    StepScaffold(
        stepIndex = 4,
        title = t.setTimesTitle,
        body = t.setTimesBody,
        onBack = onBack,
        primaryLabel = t.next,
        onPrimary = onNext
    ) {
        SectionCard {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(t.outboundTime, style = MaterialTheme.typography.bodyMedium)
                    Text(
                        settings.home?.name ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                TimeChip(settings.outboundHour, settings.outboundMinute) { editingOutbound = true }
            }
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(t.returnTime, style = MaterialTheme.typography.bodyMedium)
                    Text(
                        settings.work?.name ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                TimeChip(settings.returnHour, settings.returnMinute) { editingReturn = true }
            }
        }

        Spacer(Modifier.height(14.dp))

        SectionCard(title = t.settingsRiding) {
            LabeledSlider(
                label = t.cyclingSpeed,
                valueText = t.rideTimeIs(
                    fmt.km(distance),
                    Engine.rideDurationMin(distance, speed.toDouble())
                ),
                value = speed,
                range = 10f..32f,
                steps = 0,
                onChange = { speed = it },
                onChangeFinished = { onUpdate { s -> s.copy(speedKmh = speed.roundToInt()) } }
            )
            Text(
                "${speed.roundToInt()} ${t.speedUnit}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    if (editingOutbound) {
        TimePickerDialog(
            settings.outboundHour, settings.outboundMinute, t.outboundTime,
            onDismiss = { editingOutbound = false },
            onConfirm = { h, m ->
                onUpdate { it.copy(outboundHour = h, outboundMinute = m) }
                editingOutbound = false
            }
        )
    }
    if (editingReturn) {
        TimePickerDialog(
            settings.returnHour, settings.returnMinute, t.returnTime,
            onDismiss = { editingReturn = false },
            onConfirm = { h, m ->
                onUpdate { it.copy(returnHour = h, returnMinute = m) }
                editingReturn = false
            }
        )
    }
}

// -------------------------------------------------------------- notification

@Composable
private fun NotifyStep(
    settings: Settings,
    onUpdate: ((Settings) -> Settings) -> Unit,
    onBack: () -> Unit,
    onFinish: () -> Unit
) {
    val t = AppTheme.txt
    val fmt = AppTheme.fmt

    var draft by remember {
        mutableStateOf(
            settings.alerts.firstOrNull() ?: Alert(
                id = UUID.randomUUID().toString(),
                label = "",
                hour = 7, minute = 15,
                days = setOf(1, 2, 3, 4, 5),
                coverage = Coverage.BOTH
            )
        )
    }
    var editingTime by remember { mutableStateOf(false) }
    var granted by remember { mutableStateOf(true) }

    val ask = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { ok -> granted = ok }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ask.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    StepScaffold(
        stepIndex = 5,
        title = t.setNotifyTitle,
        body = t.setNotifyBody,
        onBack = onBack,
        primaryLabel = t.finishGo,
        primaryIcon = true,
        onPrimary = {
            onUpdate { s ->
                val list = s.alerts.toMutableList()
                val i = list.indexOfFirst { it.id == draft.id }
                if (i >= 0) list[i] = draft else list.add(draft)
                s.copy(alerts = list, setupDone = true)
            }
            onFinish()
        }
    ) {
        SectionCard(title = t.alertTime) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                TimeChip(draft.hour, draft.minute) { editingTime = true }
            }
            Spacer(Modifier.height(16.dp))
            DayPicker(draft.days, onChange = { draft = draft.copy(days = it) })
            Spacer(Modifier.height(16.dp))
            SegmentedChoice(
                options = listOf(
                    Coverage.OUTBOUND to t.coverageOutbound,
                    Coverage.RETURN to t.coverageReturn,
                    Coverage.BOTH to t.coverageBoth
                ),
                selected = draft.coverage,
                onSelect = { draft = draft.copy(coverage = it) },
                modifier = Modifier.fillMaxWidth()
            )
        }
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Rounded.NotificationsActive, null,
                tint = if (granted) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.error,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                if (granted) t.notificationsAllowed else t.permNotificationsBody,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    if (editingTime) {
        TimePickerDialog(
            draft.hour, draft.minute, t.alertTime,
            onDismiss = { editingTime = false },
            onConfirm = { h, m -> draft = draft.copy(hour = h, minute = m); editingTime = false }
        )
    }
}

// ------------------------------------------------------------------ scaffold

@Composable
private fun StepScaffold(
    stepIndex: Int,
    title: String,
    body: String,
    onBack: () -> Unit,
    primaryLabel: String,
    onPrimary: () -> Unit,
    primaryIcon: Boolean = false,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit
) {
    val t = AppTheme.txt
    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            for (i in 1..STEPS) {
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
        Text(
            t.stepOf(stepIndex, STEPS).uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
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
            TextButton(onClick = onBack) { Text(t.back) }
            Spacer(Modifier.weight(1f))
            Button(onClick = onPrimary, shape = RoundedCornerShape(16.dp)) {
                if (primaryIcon) {
                    Icon(Icons.Rounded.Check, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                }
                Text(primaryLabel, modifier = Modifier.padding(vertical = 4.dp))
            }
        }
    }
}
