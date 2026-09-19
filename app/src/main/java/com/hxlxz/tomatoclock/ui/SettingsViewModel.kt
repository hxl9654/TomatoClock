package com.hxlxz.tomatoclock.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hxlxz.tomatoclock.AlarmPlayer
import com.hxlxz.tomatoclock.SettingsDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val dataStore: SettingsDataStore,
    private val alarmPlayer: AlarmPlayer
) : ViewModel() {

    val focusTimeFlow = dataStore.focusTimeFlow
    val shortBreakTimeFlow = dataStore.shortBreakTimeFlow
    val longBreakTimeFlow = dataStore.longBreakTimeFlow
    val cyclesFlow = dataStore.cyclesFlow
    val snoozeTimeFlow = dataStore.snoozeTimeFlow
    
    val autoStartBreakFlow = dataStore.autoStartBreakFlow
    val autoStartFocusFlow = dataStore.autoStartFocusFlow
    val wakeScreenFlow = dataStore.wakeScreenFlow
    val alertModeFlow = dataStore.alertModeFlow
    val ringtoneFlow = dataStore.ringtoneFlow
    val flashScreenFlow = dataStore.flashScreenFlow

    fun saveFocusTime(time: Int) = viewModelScope.launch { dataStore.saveFocusTime(time) }
    fun saveShortBreakTime(time: Int) = viewModelScope.launch { dataStore.saveShortBreakTime(time) }
    fun saveLongBreakTime(time: Int) = viewModelScope.launch { dataStore.saveLongBreakTime(time) }
    fun saveCycles(cycles: Int) = viewModelScope.launch { dataStore.saveCycles(cycles) }
    fun saveSnoozeTime(time: Int) = viewModelScope.launch { dataStore.saveSnoozeTime(time) }
    
    fun saveAutoStartBreak(autoStart: Boolean) = viewModelScope.launch { dataStore.saveAutoStartBreak(autoStart) }
    fun saveAutoStartFocus(autoStart: Boolean) = viewModelScope.launch { dataStore.saveAutoStartFocus(autoStart) }
    fun saveWakeScreen(wake: Boolean) = viewModelScope.launch { dataStore.saveWakeScreen(wake) }
    fun saveAlertMode(mode: Int) = viewModelScope.launch { dataStore.saveAlertMode(mode) }
    fun saveRingtone(ringtone: Int) = viewModelScope.launch { dataStore.saveRingtone(ringtone) }
    fun saveFlashScreen(flash: Boolean) = viewModelScope.launch { dataStore.saveFlashScreen(flash) }
    
    fun previewRingtone(index: Int) {
        alarmPlayer.preview(index)
    }
}
