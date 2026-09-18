package com.hxlxz.tomatoclock.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hxlxz.tomatoclock.SettingsDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val dataStore: SettingsDataStore
) : ViewModel() {

    val focusTimeFlow = dataStore.focusTimeFlow
    val shortBreakTimeFlow = dataStore.shortBreakTimeFlow
    val longBreakTimeFlow = dataStore.longBreakTimeFlow
    val cyclesFlow = dataStore.cyclesFlow
    val snoozeTimeFlow = dataStore.snoozeTimeFlow
    
    val autoStartBreakFlow = dataStore.autoStartBreakFlow
    val autoStartFocusFlow = dataStore.autoStartFocusFlow
    val wakeScreenFlow = dataStore.wakeScreenFlow

    fun saveFocusTime(time: Int) = viewModelScope.launch { dataStore.saveFocusTime(time) }
    fun saveShortBreakTime(time: Int) = viewModelScope.launch { dataStore.saveShortBreakTime(time) }
    fun saveLongBreakTime(time: Int) = viewModelScope.launch { dataStore.saveLongBreakTime(time) }
    fun saveCycles(cycles: Int) = viewModelScope.launch { dataStore.saveCycles(cycles) }
    fun saveSnoozeTime(time: Int) = viewModelScope.launch { dataStore.saveSnoozeTime(time) }
    
    fun saveAutoStartBreak(autoStart: Boolean) = viewModelScope.launch { dataStore.saveAutoStartBreak(autoStart) }
    fun saveAutoStartFocus(autoStart: Boolean) = viewModelScope.launch { dataStore.saveAutoStartFocus(autoStart) }
    fun saveWakeScreen(wake: Boolean) = viewModelScope.launch { dataStore.saveWakeScreen(wake) }
}
