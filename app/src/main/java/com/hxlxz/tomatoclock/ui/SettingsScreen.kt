package com.hxlxz.tomatoclock.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
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
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import com.hxlxz.tomatoclock.R
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(
        checkNotNull(
            LocalViewModelStoreOwner.current
        ) {
                "No ViewModelStoreOwner was provided via LocalViewModelStoreOwner"
            }, null
    ),
    onNavigateBack: () -> Unit,
    onNavigateToLicense: () -> Unit
) {
    val focusTime by viewModel.focusTimeFlow.collectAsStateWithLifecycle(initialValue = 25)
    val shortBreakTime by viewModel.shortBreakTimeFlow.collectAsStateWithLifecycle(initialValue = 5)
    val longBreakTime by viewModel.longBreakTimeFlow.collectAsStateWithLifecycle(initialValue = 15)
    val cycles by viewModel.cyclesFlow.collectAsStateWithLifecycle(initialValue = 4)
    val snoozeTime by viewModel.snoozeTimeFlow.collectAsStateWithLifecycle(initialValue = 5)
    
    val wakeScreen by viewModel.wakeScreenFlow.collectAsStateWithLifecycle(initialValue = true)
    val flashScreen by viewModel.flashScreenFlow.collectAsStateWithLifecycle(initialValue = true)
    val soundMode by viewModel.soundModeFlow.collectAsStateWithLifecycle(initialValue = 0)
    val vibrationMode by viewModel.vibrationModeFlow.collectAsStateWithLifecycle(initialValue = 0)
    val ringtone by viewModel.ringtoneFlow.collectAsStateWithLifecycle(initialValue = 1)

    SettingsScreenContent(
        focusTime = focusTime,
        shortBreakTime = shortBreakTime,
        longBreakTime = longBreakTime,
        cycles = cycles,
        snoozeTime = snoozeTime,
        wakeScreen = wakeScreen,
        flashScreen = flashScreen,
        soundMode = soundMode,
        vibrationMode = vibrationMode,
        ringtone = ringtone,
        onNavigateBack = onNavigateBack,
        onNavigateToLicense = onNavigateToLicense,
        onFocusTimeChange = { viewModel.saveFocusTime(it) },
        onShortBreakTimeChange = { viewModel.saveShortBreakTime(it) },
        onLongBreakTimeChange = { viewModel.saveLongBreakTime(it) },
        onCyclesChange = { viewModel.saveCycles(it) },
        onSnoozeTimeChange = { viewModel.saveSnoozeTime(it) },
        onWakeScreenChange = { viewModel.saveWakeScreen(it) },
        onFlashScreenChange = { viewModel.saveFlashScreen(it) },
        onSoundModeChange = { viewModel.saveSoundMode(it) },
        onVibrationModeChange = { viewModel.saveVibrationMode(it) },
        onRingtoneChange = { viewModel.saveRingtone(it) },
        onRingtonePreview = { viewModel.previewRingtone(it) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreenContent(
    focusTime: Int,
    shortBreakTime: Int,
    longBreakTime: Int,
    cycles: Int,
    snoozeTime: Int,
    wakeScreen: Boolean,
    flashScreen: Boolean,
    soundMode: Int,
    vibrationMode: Int,
    ringtone: Int,
    onNavigateBack: () -> Unit,
    onNavigateToLicense: () -> Unit,
    onFocusTimeChange: (Int) -> Unit,
    onShortBreakTimeChange: (Int) -> Unit,
    onLongBreakTimeChange: (Int) -> Unit,
    onCyclesChange: (Int) -> Unit,
    onSnoozeTimeChange: (Int) -> Unit,
    onWakeScreenChange: (Boolean) -> Unit,
    onFlashScreenChange: (Boolean) -> Unit,
    onSoundModeChange: (Int) -> Unit,
    onVibrationModeChange: (Int) -> Unit,
    onRingtoneChange: (Int) -> Unit,
    onRingtonePreview: (Int) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.navigate_back))
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
                onValueChange = onFocusTimeChange
            )

            NumberInputSetting(
                label = stringResource(R.string.settings_short_break),
                value = shortBreakTime,
                valueRange = 1..30,
                onValueChange = onShortBreakTimeChange
            )

            NumberInputSetting(
                label = stringResource(R.string.settings_long_break),
                value = longBreakTime,
                valueRange = 1..60,
                onValueChange = onLongBreakTimeChange
            )

            NumberInputSetting(
                label = stringResource(R.string.settings_cycles),
                value = cycles,
                valueRange = 1..10,
                onValueChange = onCyclesChange
            )

            NumberInputSetting(
                label = stringResource(R.string.settings_snooze_time),
                value = snoozeTime,
                valueRange = 1..30,
                onValueChange = onSnoozeTimeChange
            )

            Spacer(modifier = Modifier.height(32.dp))
            Text(
                text = stringResource(R.string.settings_behavior),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))

            SwitchSetting(
                label = stringResource(R.string.settings_wake_screen),
                checked = wakeScreen,
                onCheckedChange = onWakeScreenChange
            )
            
            SwitchSetting(
                label = stringResource(R.string.settings_flash_screen),
                checked = flashScreen,
                onCheckedChange = onFlashScreenChange
            )

            val soundModeOptions = listOf(
                stringResource(R.string.sound_mode_continuous),
                stringResource(R.string.sound_mode_single),
                stringResource(R.string.sound_mode_off)
            )
            
            SegmentedButtonSetting(
                label = stringResource(R.string.settings_sound_mode),
                options = soundModeOptions,
                selectedIndex = soundMode,
                onOptionSelected = onSoundModeChange
            )

            val vibrationModeOptions = listOf(
                stringResource(R.string.vibration_mode_continuous),
                stringResource(R.string.vibration_mode_single),
                stringResource(R.string.vibration_mode_off)
            )
            
            SegmentedButtonSetting(
                label = stringResource(R.string.settings_vibration_mode),
                options = vibrationModeOptions,
                selectedIndex = vibrationMode,
                onOptionSelected = onVibrationModeChange
            )
            
            val ringtoneOptions = listOf(
                stringResource(R.string.ringtone_digital),
                stringResource(R.string.ringtone_chime),
                stringResource(R.string.ringtone_soft),
                stringResource(R.string.ringtone_zen_bowl),
                stringResource(R.string.ringtone_nature_wood)
            )
            
            DropdownSetting(
                label = stringResource(R.string.settings_ringtone),
                options = ringtoneOptions,
                selectedIndex = ringtone,
                onOptionSelected = { 
                    onRingtoneChange(it) 
                    onRingtonePreview(it)
                }
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            Text(
                text = stringResource(R.string.settings_about),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            TextButton(
                onClick = onNavigateToLicense,
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
            ) {
                Text(
                    text = stringResource(R.string.settings_license),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Start,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
            TextButton(
                onClick = { uriHandler.openUri("https://github.com/hxl9654/TomatoClock") },
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
            ) {
                Text(
                    text = stringResource(R.string.settings_github),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Start,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
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
            RepeatableIconButton(
                onClick = {
                    val current = textValue.toIntOrNull() ?: value
                    if (current > valueRange.first) {
                        onValueChange(current - 1)
                    }
                },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(Icons.Default.Remove, contentDescription = "减少")
            }
            
            BasicTextField(
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
                    .width(64.dp)
                    .padding(horizontal = 8.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .padding(vertical = 8.dp),
                textStyle = LocalTextStyle.current.copy(
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                )
            )
            
            RepeatableIconButton(
                onClick = {
                    val current = textValue.toIntOrNull() ?: value
                    if (current < valueRange.last) {
                        onValueChange(current + 1)
                    }
                },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "增加")
            }
        }
    }
}

/**
 * 支持长按连续触发的图标按钮。
 * 短按：触发一次 onClick。
 * 长按：按下 500ms 后开始每 80ms 重复触发 onClick，松开停止。
 */
@Composable
private fun RepeatableIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val currentOnClick by rememberUpdatedState(onClick)

    LaunchedEffect(isPressed) {
        if (isPressed) {
            delay(500L.milliseconds) // 长按触发前的初始等待
            while (true) {
                currentOnClick()
                delay(80L.milliseconds) // 连续触发间隔
            }
        }
    }

    FilledTonalIconButton(
        onClick = onClick,
        modifier = modifier,
        interactionSource = interactionSource,
        content = { content() }
    )
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
private fun RepeatableIconButtonPreview() {
    com.hxlxz.tomatoclock.ui.theme.TomatoClockTheme {
        RepeatableIconButton(onClick = {}) {
            Icon(Icons.Default.Add, contentDescription = "增加")
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropdownSetting(
    label: String,
    options: List<String>,
    selectedIndex: Int,
    onOptionSelected: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        
        Box(modifier = Modifier.weight(1.5f)) {
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = it }
            ) {
                OutlinedTextField(
                    value = options.getOrNull(selectedIndex) ?: "",
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                    modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    options.forEachIndexed { index, selectionOption ->
                        DropdownMenuItem(
                            text = { Text(selectionOption) },
                            onClick = {
                                onOptionSelected(index)
                                expanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SegmentedButtonSetting(
    label: String,
    options: List<String>,
    selectedIndex: Int,
    onOptionSelected: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    ) {
        Text(
            text = label, 
            style = MaterialTheme.typography.bodyLarge, 
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            options.forEachIndexed { index, text ->
                SegmentedButton(
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                    onClick = { onOptionSelected(index) },
                    selected = index == selectedIndex
                ) {
                    Text(text)
                }
            }
        }
    }
}
