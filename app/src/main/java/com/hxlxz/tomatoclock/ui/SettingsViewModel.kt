package com.hxlxz.tomatoclock.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hxlxz.tomatoclock.AlarmPlayer
import com.hxlxz.tomatoclock.SettingsDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.IOException
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val dataStore: SettingsDataStore,
    private val alarmPlayer: AlarmPlayer
) : ViewModel() {

    val focusTimeFlow = dataStore.focusTimeFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 25)
    val shortBreakTimeFlow = dataStore.shortBreakTimeFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 5)
    val longBreakTimeFlow = dataStore.longBreakTimeFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 15)
    val cyclesFlow = dataStore.cyclesFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 4)
    val snoozeTimeFlow = dataStore.snoozeTimeFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 5)
    
    val autoStartBreakFlow = dataStore.autoStartBreakFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val autoStartFocusFlow = dataStore.autoStartFocusFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val wakeScreenFlow = dataStore.wakeScreenFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    val alertModeFlow = dataStore.alertModeFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    val ringtoneFlow = dataStore.ringtoneFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1)
    val flashScreenFlow = dataStore.flashScreenFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    private fun safeSave(action: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                action()
            } catch (e: IOException) {
                Log.e("SettingsViewModel", "Failed to save to DataStore", e)
            } catch (e: Exception) {
                Log.e("SettingsViewModel", "Unexpected error saving to DataStore", e)
            }
        }
    }

    fun saveFocusTime(time: Int) = safeSave { dataStore.saveFocusTime(time) }
    fun saveShortBreakTime(time: Int) = safeSave { dataStore.saveShortBreakTime(time) }
    fun saveLongBreakTime(time: Int) = safeSave { dataStore.saveLongBreakTime(time) }
    fun saveCycles(cycles: Int) = safeSave { dataStore.saveCycles(cycles) }
    fun saveSnoozeTime(time: Int) = safeSave { dataStore.saveSnoozeTime(time) }
    
    fun saveAutoStartBreak(autoStart: Boolean) = safeSave { dataStore.saveAutoStartBreak(autoStart) }
    fun saveAutoStartFocus(autoStart: Boolean) = safeSave { dataStore.saveAutoStartFocus(autoStart) }
    fun saveWakeScreen(wake: Boolean) = safeSave { dataStore.saveWakeScreen(wake) }
    fun saveAlertMode(mode: Int) = safeSave { dataStore.saveAlertMode(mode) }
    fun saveRingtone(ringtone: Int) = safeSave { dataStore.saveRingtone(ringtone) }
    fun saveFlashScreen(flash: Boolean) = safeSave { dataStore.saveFlashScreen(flash) }
    
    fun previewRingtone(index: Int) {
        alarmPlayer.preview(index)
    }
}
