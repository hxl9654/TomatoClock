package com.hxlxz.tomatoclock.ui

import com.hxlxz.tomatoclock.AlarmPlayer
import com.hxlxz.tomatoclock.SettingsDataStore
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    
    private lateinit var dataStore: SettingsDataStore
    private lateinit var alarmPlayer: AlarmPlayer
    private lateinit var viewModel: SettingsViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        dataStore = mockk(relaxed = true)
        alarmPlayer = mockk(relaxed = true)
        viewModel = SettingsViewModel(dataStore, alarmPlayer)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `previewRingtone should call alarmPlayer preview with correct index`() {
        // Arrange
        val testIndex = 2
        
        // Act
        viewModel.previewRingtone(testIndex)
        
        // Assert
        verify(exactly = 1) { alarmPlayer.preview(testIndex) }
    }

    @Test
    fun `save methods should call corresponding dataStore methods`() = runTest {
        viewModel.saveFocusTime(30)
        viewModel.saveShortBreakTime(10)
        viewModel.saveLongBreakTime(20)
        viewModel.saveCycles(5)
        viewModel.saveSnoozeTime(8)
        viewModel.saveAutoStartBreak(true)
        viewModel.saveAutoStartFocus(true)
        viewModel.saveWakeScreen(false)
        viewModel.saveAlertMode(2)
        viewModel.saveRingtone(3)
        viewModel.saveFlashScreen(false)
        
        advanceUntilIdle()
        
        coVerify(exactly = 1) { dataStore.saveFocusTime(30) }
        coVerify(exactly = 1) { dataStore.saveShortBreakTime(10) }
        coVerify(exactly = 1) { dataStore.saveLongBreakTime(20) }
        coVerify(exactly = 1) { dataStore.saveCycles(5) }
        coVerify(exactly = 1) { dataStore.saveSnoozeTime(8) }
        coVerify(exactly = 1) { dataStore.saveAutoStartBreak(true) }
        coVerify(exactly = 1) { dataStore.saveAutoStartFocus(true) }
        coVerify(exactly = 1) { dataStore.saveWakeScreen(false) }
        coVerify(exactly = 1) { dataStore.saveAlertMode(2) }
        coVerify(exactly = 1) { dataStore.saveRingtone(3) }
        coVerify(exactly = 1) { dataStore.saveFlashScreen(false) }
    }
}
