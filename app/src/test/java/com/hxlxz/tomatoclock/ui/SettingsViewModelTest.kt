package com.hxlxz.tomatoclock.ui

import com.hxlxz.tomatoclock.AlarmPlayer
import com.hxlxz.tomatoclock.SettingsDataStore
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
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
}
