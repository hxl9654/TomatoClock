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
        val SOUND_MODE = intPreferencesKey("sound_mode") // 0: Continuous, 1: Single, 2: Off
        val VIBRATION_MODE = intPreferencesKey("vibration_mode") // 0: Continuous, 1: Single, 2: Off
        val RINGTONE = intPreferencesKey("ringtone") // 0: Digital, 1: Chime, 2: Soft Synth
        val WAKE_SCREEN = androidx.datastore.preferences.core.booleanPreferencesKey("wake_screen")
        val FLASH_SCREEN = androidx.datastore.preferences.core.booleanPreferencesKey("flash_screen")
        
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
    
    val soundModeFlow: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[SOUND_MODE] ?: 0 // Default to Continuous
    }
    
    val vibrationModeFlow: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[VIBRATION_MODE] ?: 0 // Default to Continuous
    }
    
    val ringtoneFlow: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[RINGTONE] ?: 1 // Default to Chime
    }
    
    val wakeScreenFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[WAKE_SCREEN] ?: true // Default to waking up screen
    }
    
    val flashScreenFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[FLASH_SCREEN] ?: true // Default to flashing screen
    }
    
    val autoStartBreakFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[AUTO_START_BREAK] ?: false
    }
    
    val autoStartFocusFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[AUTO_START_FOCUS] ?: false
    }

    suspend fun saveFocusTime(minutes: Int) {
        context.dataStore.edit { preferences ->
            preferences[FOCUS_TIME] = minutes.coerceIn(1, 120)
        }
    }

    suspend fun saveShortBreakTime(minutes: Int) {
        context.dataStore.edit { preferences ->
            preferences[SHORT_BREAK_TIME] = minutes.coerceIn(1, 60)
        }
    }

    suspend fun saveLongBreakTime(minutes: Int) {
        context.dataStore.edit { preferences ->
            preferences[LONG_BREAK_TIME] = minutes.coerceIn(1, 60)
        }
    }

    suspend fun saveCycles(count: Int) {
        context.dataStore.edit { preferences ->
            preferences[CYCLES] = count.coerceIn(1, 10)
        }
    }

    suspend fun saveSnoozeTime(minutes: Int) {
        context.dataStore.edit { preferences ->
            preferences[SNOOZE_TIME] = minutes.coerceIn(1, 30)
        }
    }
    
    suspend fun saveSoundMode(mode: Int) {
        context.dataStore.edit { preferences ->
            preferences[SOUND_MODE] = mode.coerceIn(0, SoundMode.entries.size - 1)
        }
    }

    suspend fun saveVibrationMode(mode: Int) {
        context.dataStore.edit { preferences ->
            preferences[VIBRATION_MODE] = mode.coerceIn(0, VibrationMode.entries.size - 1)
        }
    }
    
    suspend fun saveRingtone(ringtone: Int) {
        context.dataStore.edit { preferences ->
            // 校验范围: 0 (DIGITAL) ~ 4 (NATURE_WOOD)，与 Ringtone 枚举值对应
            preferences[RINGTONE] = ringtone.coerceIn(0, Ringtone.entries.size - 1)
        }
    }
    
    suspend fun saveWakeScreen(wake: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[WAKE_SCREEN] = wake
        }
    }
    
    suspend fun saveFlashScreen(flash: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[FLASH_SCREEN] = flash
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
