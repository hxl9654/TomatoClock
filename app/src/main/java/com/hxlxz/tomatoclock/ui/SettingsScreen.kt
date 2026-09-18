package com.hxlxz.tomatoclock.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hxlxz.tomatoclock.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {

    val focusTime by viewModel.focusTimeFlow.collectAsStateWithLifecycle(initialValue = 25)
    val shortBreakTime by viewModel.shortBreakTimeFlow.collectAsStateWithLifecycle(initialValue = 5)
    val longBreakTime by viewModel.longBreakTimeFlow.collectAsStateWithLifecycle(initialValue = 15)
    val cycles by viewModel.cyclesFlow.collectAsStateWithLifecycle(initialValue = 4)
    val snoozeTime by viewModel.snoozeTimeFlow.collectAsStateWithLifecycle(initialValue = 5)
    
    val autoStartBreak by viewModel.autoStartBreakFlow.collectAsStateWithLifecycle(initialValue = false)
    val autoStartFocus by viewModel.autoStartFocusFlow.collectAsStateWithLifecycle(initialValue = false)
    val wakeScreen by viewModel.wakeScreenFlow.collectAsStateWithLifecycle(initialValue = true)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text(
                text = stringResource(R.string.settings_durations),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))

            NumberInputSetting(
                label = stringResource(R.string.settings_focus_time),
                value = focusTime,
                valueRange = 1..120,
                onValueChange = { viewModel.saveFocusTime(it) }
            )

            NumberInputSetting(
                label = stringResource(R.string.settings_short_break),
                value = shortBreakTime,
                valueRange = 1..30,
                onValueChange = { viewModel.saveShortBreakTime(it) }
            )

            NumberInputSetting(
                label = stringResource(R.string.settings_long_break),
                value = longBreakTime,
                valueRange = 5..60,
                onValueChange = { viewModel.saveLongBreakTime(it) }
            )

            NumberInputSetting(
                label = stringResource(R.string.settings_cycles),
                value = cycles,
                valueRange = 1..10,
                onValueChange = { viewModel.saveCycles(it) }
            )

            NumberInputSetting(
                label = stringResource(R.string.settings_snooze_time),
                value = snoozeTime,
                valueRange = 1..30,
                onValueChange = { viewModel.saveSnoozeTime(it) }
            )

            Spacer(modifier = Modifier.height(32.dp))
            Text(
                text = stringResource(R.string.settings_behavior),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))

            SwitchSetting(
                label = stringResource(R.string.settings_auto_break),
                checked = autoStartBreak,
                onCheckedChange = { viewModel.saveAutoStartBreak(it) }
            )

            SwitchSetting(
                label = stringResource(R.string.settings_auto_focus),
                checked = autoStartFocus,
                onCheckedChange = { viewModel.saveAutoStartFocus(it) }
            )

            SwitchSetting(
                label = stringResource(R.string.settings_wake_screen),
                checked = wakeScreen,
                onCheckedChange = { viewModel.saveWakeScreen(it) }
            )
        }
    }
}

@Composable
fun NumberInputSetting(
    label: String,
    value: Int,
    valueRange: IntRange,
    onValueChange: (Int) -> Unit
) {
    var textValue by remember(value) { mutableStateOf(value.toString()) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label, 
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        
        Row(verticalAlignment = Alignment.CenterVertically) {
            FilledTonalIconButton(
                onClick = { 
                    val current = textValue.toIntOrNull() ?: value
                    if (current > valueRange.first) {
                        onValueChange(current - 1)
                    }
                },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(Icons.Default.Remove, contentDescription = "Decrease")
            }
            
            OutlinedTextField(
                value = textValue,
                onValueChange = { newValue ->
                    textValue = newValue
                    newValue.toIntOrNull()?.let { intVal ->
                        if (intVal in valueRange) {
                            onValueChange(intVal)
                        }
                    }
                },
                modifier = Modifier
                    .width(80.dp)
                    .padding(horizontal = 8.dp),
                textStyle = LocalTextStyle.current.copy(textAlign = androidx.compose.ui.text.style.TextAlign.Center),
                singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                )
            )
            
            FilledTonalIconButton(
                onClick = { 
                    val current = textValue.toIntOrNull() ?: value
                    if (current < valueRange.last) {
                        onValueChange(current + 1)
                    }
                },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Increase")
            }
        }
    }
}

@Composable
fun SwitchSetting(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}
