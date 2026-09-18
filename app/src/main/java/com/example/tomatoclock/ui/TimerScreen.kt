package com.example.tomatoclock.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.tomatoclock.R
import com.example.tomatoclock.TimerMode
import com.example.tomatoclock.TimerState
import com.example.tomatoclock.TimerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimerScreen(
    onNavigateToSettings: () -> Unit,
    viewModel: TimerViewModel = hiltViewModel()
) {
    val timerMode by viewModel.timerMode.collectAsState()
    val timerState by viewModel.timerState.collectAsState()
    val timeRemaining by viewModel.timeRemaining.collectAsState()
    val totalTime by viewModel.totalTimeInSeconds.collectAsState()
    val currentCycle by viewModel.currentCycle.collectAsState()

    // Assuming we fetch total cycles from a higher level or just hardcode for demo,
    // but in a real app we'd fetch it from ViewModel. For now just show current.
    
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
            
            // Cycle info (Mocking total cycles as 4 for display, ideally from viewmodel)
            Text(
                text = stringResource(R.string.cycle_count, currentCycle, 4),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Circular Progress Indicator and Time
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(300.dp)
            ) {
                CircularProgressIndicator(
                    progress = { 1f }, 
                    modifier = Modifier.fillMaxSize(),
                    color = colorPrimary.copy(alpha = 0.2f),
                    strokeWidth = 12.dp,
                    strokeCap = StrokeCap.Round
                )
                
                // Active progress
                CircularProgressIndicator(
                    progress = { if (totalTime > 0) timeRemaining.toFloat() / totalTime.toFloat() else 1f },
                    modifier = Modifier.fillMaxSize(),
                    color = colorPrimary,
                    strokeWidth = 12.dp,
                    strokeCap = StrokeCap.Round
                )

                Text(
                    text = timeString,
                    fontSize = 72.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
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
                            onClick = { viewModel.startTimer() },
                            modifier = Modifier.size(80.dp),
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(containerColor = colorPrimary)
                        ) {
                            Text(stringResource(R.string.action_start))
                        }
                    }
                    TimerState.RUNNING -> {
                        Button(
                            onClick = { viewModel.pauseTimer() },
                            modifier = Modifier.size(80.dp),
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                        ) {
                            Text(stringResource(R.string.action_pause), color = MaterialTheme.colorScheme.onSecondaryContainer)
                        }
                        
                        OutlinedButton(
                            onClick = { viewModel.stopTimer() },
                            modifier = Modifier.size(80.dp),
                            shape = CircleShape
                        ) {
                            Text(stringResource(R.string.action_stop))
                        }
                    }
                    TimerState.PAUSED -> {
                        Button(
                            onClick = { viewModel.startTimer() },
                            modifier = Modifier.size(80.dp),
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(containerColor = colorPrimary)
                        ) {
                            Text(stringResource(R.string.action_resume))
                        }
                        
                        OutlinedButton(
                            onClick = { viewModel.stopTimer() },
                            modifier = Modifier.size(80.dp),
                            shape = CircleShape
                        ) {
                            Text(stringResource(R.string.action_stop))
                        }
                    }
                    TimerState.FINISHED -> {
                        Button(
                            onClick = { viewModel.nextPhase() },
                            modifier = Modifier.height(80.dp).padding(horizontal = 16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = colorPrimary)
                        ) {
                            Text(stringResource(R.string.action_next))
                        }
                        
                        OutlinedButton(
                            onClick = { viewModel.snooze() },
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
