package com.hxlxz.tomatoclock.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.compose.ui.platform.LocalConfiguration
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import com.hxlxz.tomatoclock.R
import com.hxlxz.tomatoclock.TimerMode
import com.hxlxz.tomatoclock.TimerState
import com.hxlxz.tomatoclock.TimerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimerScreen(
    onNavigateToSettings: () -> Unit,
    viewModel: TimerViewModel = hiltViewModel(
        checkNotNull(
            LocalViewModelStoreOwner.current
        ) {
                "No ViewModelStoreOwner was provided via LocalViewModelStoreOwner"
            }, null
    )
) {
    val timerMode by viewModel.timerMode.collectAsStateWithLifecycle()
    val timerState by viewModel.timerState.collectAsStateWithLifecycle()
    val timeRemaining by viewModel.timeRemaining.collectAsStateWithLifecycle()
    val totalTime by viewModel.totalTimeInSeconds.collectAsStateWithLifecycle()
    val currentCycle by viewModel.currentCycle.collectAsStateWithLifecycle()
    val totalCycles by viewModel.totalCycles.collectAsStateWithLifecycle()
    val flashScreen by viewModel.flashScreen.collectAsStateWithLifecycle(initialValue = true)

    TimerScreenContent(
        timerMode = timerMode,
        timerState = timerState,
        timeRemaining = timeRemaining,
        totalTime = totalTime,
        currentCycle = currentCycle,
        totalCycles = totalCycles,
        flashScreen = flashScreen,
        onNavigateToSettings = onNavigateToSettings,
        onStart = { viewModel.startTimer() },
        onPause = { viewModel.pauseTimer() },
        onStop = { viewModel.stopTimer() },
        onNextPhase = { viewModel.nextPhase() },
        onSnooze = { viewModel.snooze() },
        onAddFiveMinutes = { viewModel.addFiveMinutes() },
        onSkipPhase = { viewModel.skipCurrentPhase() }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimerScreenContent(
    timerMode: TimerMode,
    timerState: TimerState,
    timeRemaining: Long,
    totalTime: Long,
    currentCycle: Int,
    totalCycles: Int,
    flashScreen: Boolean,
    onNavigateToSettings: () -> Unit,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onStop: () -> Unit,
    onNextPhase: () -> Unit,
    onSnooze: () -> Unit,
    onAddFiveMinutes: () -> Unit,
    onSkipPhase: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "flash")
    val alphaAnim = if (timerState == TimerState.FINISHED && flashScreen) {
        infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 0.1f,
            animationSpec = infiniteRepeatable(
                animation = tween(800),
                repeatMode = RepeatMode.Reverse
            ),
            label = "flashAlpha"
        ).value
    } else {
        1f
    }
    
    val minutes = timeRemaining / 60
    val seconds = timeRemaining % 60
    val locale = LocalConfiguration.current.locales[0]
    val timeString = String.format(locale, "%02d:%02d", minutes, seconds)

    val modeText = when (timerMode) {
        TimerMode.FOCUS -> stringResource(R.string.state_focus)
        TimerMode.SHORT_BREAK -> stringResource(R.string.state_short_break)
        TimerMode.LONG_BREAK -> stringResource(R.string.state_long_break)
    }
    
    val colorPrimary = when (timerMode) {
        TimerMode.FOCUS -> MaterialTheme.colorScheme.primary
        TimerMode.SHORT_BREAK -> MaterialTheme.colorScheme.secondary
        TimerMode.LONG_BREAK -> MaterialTheme.colorScheme.tertiary
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.settings_title))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (timerState == TimerState.RUNNING) {
                    FilledTonalIconButton(onClick = onAddFiveMinutes) {
                        Icon(Icons.Default.Add, contentDescription = stringResource(R.string.action_add_time), tint = colorPrimary)
                    }
                } else {
                    Spacer(modifier = Modifier.width(48.dp))
                }
                
                Text(
                    text = modeText,
                    style = MaterialTheme.typography.headlineMedium,
                    color = colorPrimary,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                
                if (timerState == TimerState.RUNNING) {
                    FilledTonalIconButton(onClick = onSkipPhase) {
                        Icon(Icons.Default.SkipNext, contentDescription = stringResource(R.string.action_skip_phase), tint = colorPrimary)
                    }
                } else {
                    Spacer(modifier = Modifier.width(48.dp))
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Cycle info: dynamically read from ViewModel
            Text(
                text = stringResource(R.string.cycle_count, currentCycle, totalCycles),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Circular Progress Indicator and Time
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(300.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    // Background track
                    drawCircle(
                        color = colorPrimary.copy(alpha = 0.2f),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 12.dp.toPx())
                    )
                    
                    // Active progress (remaining ratio)
                    val progress = if (totalTime > 0) (timeRemaining.toFloat() / totalTime.toFloat()).coerceIn(0f, 1f) else 1f
                    val sweepAngle = 360f * progress
                    val startAngle = -90f
                    
                    drawArc(
                        color = colorPrimary.copy(alpha = alphaAnim),
                        startAngle = startAngle,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(
                            width = 12.dp.toPx(), 
                            cap = StrokeCap.Round
                        )
                    )
                }

                Text(
                    text = timeString,
                    fontSize = 72.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = alphaAnim)
                )
            }

            Spacer(modifier = Modifier.height(64.dp))

            // Controls
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                when (timerState) {
                    TimerState.IDLE -> {
                        Button(
                            onClick = onStart,
                            modifier = Modifier.size(80.dp),
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(containerColor = colorPrimary)
                        ) {
                            Text(stringResource(R.string.action_start))
                        }
                    }
                    TimerState.RUNNING -> {
                        Button(
                            onClick = onPause,
                            modifier = Modifier.size(80.dp),
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                        ) {
                            Text(stringResource(R.string.action_pause), color = MaterialTheme.colorScheme.onSecondaryContainer)
                        }
                        
                        OutlinedButton(
                            onClick = onStop,
                            modifier = Modifier.size(80.dp),
                            shape = CircleShape
                        ) {
                            Text(stringResource(R.string.action_stop))
                        }
                    }
                    TimerState.PAUSED -> {
                        Button(
                            onClick = onStart,
                            modifier = Modifier.size(80.dp),
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(containerColor = colorPrimary)
                        ) {
                            Text(stringResource(R.string.action_resume))
                        }
                        
                        OutlinedButton(
                            onClick = onStop,
                            modifier = Modifier.size(80.dp),
                            shape = CircleShape
                        ) {
                            Text(stringResource(R.string.action_stop))
                        }
                    }
                    TimerState.FINISHED -> {
                        Button(
                            onClick = onNextPhase,
                            modifier = Modifier.height(80.dp).padding(horizontal = 16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = colorPrimary)
                        ) {
                            Text(stringResource(R.string.action_next))
                        }
                        
                        OutlinedButton(
                            onClick = onSnooze,
                            modifier = Modifier.height(80.dp).padding(horizontal = 16.dp)
                        ) {
                            Text(stringResource(R.string.action_snooze))
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, name = "专注中 - 运行状态")
@Composable
private fun TimerScreenRunningPreview() {
    com.hxlxz.tomatoclock.ui.theme.TomatoClockTheme {
        TimerScreenContent(
            timerMode = TimerMode.FOCUS,
            timerState = TimerState.RUNNING,
            timeRemaining = 1200L,
            totalTime = 1500L,
            currentCycle = 1,
            totalCycles = 4,
            flashScreen = false,
            onNavigateToSettings = {},
            onStart = {},
            onPause = {},
            onStop = {},
            onNextPhase = {},
            onSnooze = {},
            onAddFiveMinutes = {},
            onSkipPhase = {}
        )
    }
}
