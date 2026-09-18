package com.hxlxz.tomatoclock

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsDataStore(private val context: Context) {

    companion object {
        val FOCUS_TIME = intPreferencesKey("focus_time_minutes")
        val SHORT_BREAK_TIME = intPreferencesKey("short_break_time_minutes")
        val LONG_BREAK_TIME = intPreferencesKey("long_break_time_minutes")
        val CYCLES = intPreferencesKey("cycles_count")
        val SNOOZE_TIME = intPreferencesKey("snooze_time_minutes")
        
        // Reminder Settings
        val ALERT_MODE = intPreferencesKey("alert_mode") // 0: Sound+Vib, 1: Sound, 2: Vib, 3: SingleSound, 4: Silent
        val WAKE_SCREEN = androidx.datastore.preferences.core.booleanPreferencesKey("wake_screen")
        
        // Auto Transition Settings
        val AUTO_START_BREAK = androidx.datastore.preferences.core.booleanPreferencesKey("auto_start_break")
        val AUTO_START_FOCUS = androidx.datastore.preferences.core.booleanPreferencesKey("auto_start_focus")
    }

    val focusTimeFlow: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[FOCUS_TIME] ?: 25
    }

    val shortBreakTimeFlow: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[SHORT_BREAK_TIME] ?: 5
    }

    val longBreakTimeFlow: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[LONG_BREAK_TIME] ?: 15
    }

    val cyclesFlow: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[CYCLES] ?: 4
    }

    val snoozeTimeFlow: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[SNOOZE_TIME] ?: 5
    }
    
    val alertModeFlow: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[ALERT_MODE] ?: 0 // Default to Sound + Vibrate
    }
    
    val wakeScreenFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[WAKE_SCREEN] ?: true // Default to waking up screen
    }
    
    val autoStartBreakFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[AUTO_START_BREAK] ?: false
    }
    
    val autoStartFocusFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[AUTO_START_FOCUS] ?: false
    }

    suspend fun saveFocusTime(minutes: Int) {
        context.dataStore.edit { preferences ->
            preferences[FOCUS_TIME] = minutes
        }
    }

    suspend fun saveShortBreakTime(minutes: Int) {
        context.dataStore.edit { preferences ->
            preferences[SHORT_BREAK_TIME] = minutes
        }
    }

    suspend fun saveLongBreakTime(minutes: Int) {
        context.dataStore.edit { preferences ->
            preferences[LONG_BREAK_TIME] = minutes
        }
    }

    suspend fun saveCycles(count: Int) {
        context.dataStore.edit { preferences ->
            preferences[CYCLES] = count
        }
    }

    suspend fun saveSnoozeTime(minutes: Int) {
        context.dataStore.edit { preferences ->
            preferences[SNOOZE_TIME] = minutes
        }
    }
    
    suspend fun saveAlertMode(mode: Int) {
        context.dataStore.edit { preferences ->
            preferences[ALERT_MODE] = mode
        }
    }
    
    suspend fun saveWakeScreen(wake: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[WAKE_SCREEN] = wake
        }
    }
    
    suspend fun saveAutoStartBreak(autoStart: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[AUTO_START_BREAK] = autoStart
        }
    }
    
    suspend fun saveAutoStartFocus(autoStart: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[AUTO_START_FOCUS] = autoStart
        }
    }
}
