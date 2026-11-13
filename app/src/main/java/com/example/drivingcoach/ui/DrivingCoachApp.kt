package com.example.drivingcoach.ui

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.core.view.WindowCompat
import com.example.drivingcoach.R
import com.example.drivingcoach.data.DriverOverviewState
import com.example.drivingcoach.data.mock.MockDriverOverviewData
import com.example.drivingcoach.data.mock.OverviewScenario
import com.example.drivingcoach.ui.model.ScorePeriod
import com.example.drivingcoach.ui.model.findHabitDetail
import com.example.drivingcoach.ui.theme.DrivingCoachUITheme
import com.example.drivingcoach.vehicle.PresentationPolicy
import com.example.drivingcoach.vehicle.VehicleUxState

@Composable
fun DrivingCoachApp(
    vehicleUxState: VehicleUxState,
    foreground: Boolean,
    overviewState: DriverOverviewState = DriverOverviewState.Empty,
    onRetryOverview: () -> Unit = {},
    trainingState: TrainingUiState = TrainingUiState(),
    onRequestSpeedPermission: () -> Unit = {},
    onStartTraining: (Int) -> Unit = {},
    onFinishTraining: () -> Unit = {},
    onResetTraining: () -> Unit = {},
) {
    val context = LocalContext.current
    val store = remember(context) { context.getSharedPreferences("coach_presentation", Context.MODE_PRIVATE) }
    var theme by rememberSaveable { mutableIntStateOf(store.getInt("theme", 0).coerceIn(0, 2)) }
    var motion by rememberSaveable { mutableStateOf(store.getBoolean("motion", true)) }
    var haptics by rememberSaveable { mutableStateOf(store.getBoolean("haptics", true)) }
    var focus by rememberSaveable { mutableIntStateOf(store.getInt("focus", 0).coerceIn(0, 2)) }
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var selectedTrip by rememberSaveable { mutableIntStateOf(-1) }
    var selectedHabitKey by rememberSaveable { mutableStateOf<String?>(null) }
    val homeScrollState = rememberScrollState()
    var previewRestricted by rememberSaveable { mutableStateOf(false) }
    var previewActive by rememberSaveable { mutableStateOf(false) }
    var trainingOpen by rememberSaveable { mutableStateOf(false) }
    var scorePeriod by rememberSaveable { mutableIntStateOf(ScorePeriod.WEEK.ordinal) }
    val policy = PresentationPolicy.resolve(vehicleUxState, previewRestricted, motion, foreground)
    val habits = (overviewState as? DriverOverviewState.Ready)?.overview?.habits.orEmpty()
    val canShowHabitDetails = tab == 0 && policy.allowDetails && policy.showSampleData &&
        overviewState is DriverOverviewState.Ready
    val selectedHabit = if (canShowHabitDetails) findHabitDetail(habits, selectedHabitKey) else null
    LaunchedEffect(selectedHabitKey, selectedHabit != null) {
        if (selectedHabitKey != null && selectedHabit == null) selectedHabitKey = null
    }
    val feedback = LocalHapticFeedback.current
    fun touch() {
        if (haptics) feedback.performHapticFeedback(HapticFeedbackType.LongPress)
    }
    // Remove detail content immediately on restriction; do not animate an outgoing detail screen.
    LaunchedEffect(policy.allowDetails) {
        if (!policy.allowDetails) {
            tab = 0
            selectedTrip = -1
            selectedHabitKey = null
            previewActive = false
        }
    }
    BackHandler(enabled = trainingOpen || tab != 0 || selectedTrip >= 0 ||
        selectedHabitKey != null || previewRestricted) {
        when {
            previewRestricted && policy.showSampleData -> previewRestricted = false
            trainingOpen -> { onResetTraining(); trainingOpen = false }
            selectedHabitKey != null -> selectedHabitKey = null
            selectedTrip >= 0 -> selectedTrip = -1
            else -> tab = 0
        }
    }
    val dark = when (theme) { 1 -> false; 2 -> true; else -> isSystemInDarkTheme() }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            (context as? Activity)?.window?.let { window ->
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = !dark
                    isAppearanceLightNavigationBars = !dark
                }
            }
        }
    }
    DrivingCoachUITheme(darkTheme = dark) {
        CompositionLocalProvider(LocalCoachMotion provides policy.allowMotion) {
            Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                BoxWithConstraints(Modifier.fillMaxSize().safeDrawingPadding()) {
                    val rail = maxWidth >= 840.dp && maxHeight >= 480.dp &&
                        LocalDensity.current.fontScale <= 1.3f
                    Row(Modifier.fillMaxSize()) {
                        if (rail) {
                            Column(
                                Modifier.width(140.dp).fillMaxHeight().padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Spacer(Modifier.height(14.dp))
                                Surface(shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
                                    color = MaterialTheme.colorScheme.primary) {
                                    CoachIcon(CoachSymbol.COACH,
                                        Modifier.padding(16.dp).size(32.dp),
                                        tint = MaterialTheme.colorScheme.onPrimary)
                                }
                                Spacer(Modifier.height(40.dp))
                                CoachNavigation(tab = if (policy.allowDetails) tab else 0,
                                    vertical = true, enabled = policy.allowDetails,
                                    onSelect = { touch(); onResetTraining(); trainingOpen = false
                                        selectedTrip = -1; selectedHabitKey = null; tab = it })
                            }
                        }
                        Column(Modifier.weight(1f).fillMaxHeight()) {
                            BrandHeader(vehicleUxState, policy)
                            Box(Modifier.weight(1f).fillMaxWidth()) {
                                if (!policy.allowDetails) {
                                    RestrictedCoachScreen(
                                        unknown = vehicleUxState == VehicleUxState.UNAVAILABLE,
                                        preview = policy.showSampleData,
                                        onExitPreview = { touch(); previewRestricted = false },
                                    )
                                } else if (trainingOpen) {
                                    SpeedTrainingScreen(
                                        state = trainingState,
                                        onRequestPermission = onRequestSpeedPermission,
                                        onStart = onStartTraining,
                                        onFinish = onFinishTraining,
                                        onReset = onResetTraining,
                                        onBack = { onResetTraining(); trainingOpen = false },
                                    )
                                } else {
                                    AnimatedContent(
                                        targetState = tab,
                                        modifier = Modifier.fillMaxSize(),
                                        transitionSpec = {
                                            if (policy.allowMotion) {
                                                fadeIn(tween(180)) togetherWith fadeOut(tween(100))
                                            } else EnterTransition.None togetherWith ExitTransition.None
                                        },
                                        label = "coachDestination",
                                    ) { destination ->
                                        when (destination) {
                                            1 -> HistoryScreen(
                                                sampleData = policy.showSampleData,
                                                selectedTrip = selectedTrip,
                                                onSelectTrip = { touch(); selectedTrip = it },
                                                onBack = { touch(); selectedTrip = -1 },
                                            )
                                            2 -> SettingsScreen(
                                                theme = theme, motion = motion, haptics = haptics,
                                                preview = policy.showSampleData,
                                                onTheme = {
                                                    touch(); theme = it
                                                    store.edit { putInt("theme", it) }
                                                },
                                                onMotion = {
                                                    touch(); motion = it
                                                    store.edit { putBoolean("motion", it) }
                                                },
                                                onHaptics = {
                                                    touch(); haptics = it
                                                    store.edit { putBoolean("haptics", it) }
                                                },
                                                onPreviewDriving = { touch(); previewRestricted = true },
                                            )
                                            else -> HabitAnalysisDestination(
                                                selectedKindName = selectedHabitKey,
                                                habits = habits,
                                                enabled = canShowHabitDetails,
                                                onBack = { touch(); selectedHabitKey = null },
                                            ) { CoachHomeScreen(
                                                sampleData = policy.showSampleData,
                                                overviewState = overviewState,
                                                scorePeriod = ScorePeriod.entries[scorePeriod],
                                                onScorePeriod = { touch(); scorePeriod = it.ordinal },
                                                onRetry = { touch(); onRetryOverview() },
                                                focus = focus,
                                                previewActive = previewActive,
                                                onFocus = {
                                                    touch(); focus = it
                                                    store.edit { putInt("focus", it) }
                                                },
                                                onPreview = { touch(); previewActive = !previewActive },
                                                onHistory = { touch(); tab = 1 },
                                                onTraining = { touch(); trainingOpen = true },
                                                onSelectHabit = { touch(); selectedHabitKey = it.name },
                                                scrollState = homeScrollState,
                                            ) }
                                        }
                                    }
                                }
                            }
                            if (!rail) {
                                CoachNavigation(tab = if (policy.allowDetails) tab else 0,
                                    vertical = false, enabled = policy.allowDetails,
                                    onSelect = { touch(); onResetTraining(); trainingOpen = false
                                        selectedTrip = -1; selectedHabitKey = null; tab = it },
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BrandHeader(vehicle: VehicleUxState, policy: PresentationPolicy) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 28.dp, vertical = 22.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleLarge)
            Text(stringResource(R.string.brand_caption),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        StatusPill(stringResource(
            when {
                policy.showSampleData -> R.string.preview_badge
                vehicle == VehicleUxState.AVAILABLE -> R.string.available_badge
                vehicle == VehicleUxState.RESTRICTED -> R.string.restricted_badge
                else -> R.string.unavailable_badge
            },
        ))
    }
}

@Composable
private fun CoachNavigation(
    tab: Int,
    vertical: Boolean,
    enabled: Boolean,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    LiquidSelection(
        labels = listOf(R.string.tab_coach, R.string.tab_history, R.string.tab_settings).map { stringResource(it) },
        symbols = listOf(CoachSymbol.COACH, CoachSymbol.HISTORY, CoachSymbol.SETTINGS),
        selected = tab,
        vertical = vertical,
        enabled = enabled,
        onSelect = onSelect,
        modifier = modifier.fillMaxWidth(),
    )
}

@Preview(name = "Pleos wide · day", widthDp = 1280, heightDp = 900, locale = "ko")
@Preview(name = "Split window · day", widthDp = 520, heightDp = 1000, locale = "ko")
@Preview(name = "Split window · night", widthDp = 520, heightDp = 1000, locale = "ko",
    uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "Phone · English", widthDp = 400, heightDp = 900, locale = "en")
@Composable
private fun CoachPreview() {
    DrivingCoachApp(VehicleUxState.PREVIEW, foreground = true,
        overviewState = MockDriverOverviewData.state(OverviewScenario.NORMAL))
}

@Preview(name = "Driving restrictions", widthDp = 960, heightDp = 640, locale = "ko")
@Composable
private fun RestrictedPreview() {
    DrivingCoachApp(VehicleUxState.RESTRICTED, foreground = true)
}
