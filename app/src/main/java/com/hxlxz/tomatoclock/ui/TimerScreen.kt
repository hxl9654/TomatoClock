package com.hxlxz.tomatoclock.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.Canvas
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.core.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hxlxz.tomatoclock.R
import com.hxlxz.tomatoclock.TimerMode
import com.hxlxz.tomatoclock.TimerState
import com.hxlxz.tomatoclock.TimerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimerScreen(
    onNavigateToSettings: () -> Unit,
    viewModel: TimerViewModel = hiltViewModel()
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
        onSnooze = { viewModel.snooze() }
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
    onSnooze: () -> Unit
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
    val timeString = String.format("%02d:%02d", minutes, seconds)

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
            
            Text(
                text = modeText,
                style = MaterialTheme.typography.headlineMedium,
                color = colorPrimary
            )
            
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
            onSnooze = {}
        )
    }
}
