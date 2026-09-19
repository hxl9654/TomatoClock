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
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
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
        io.mockk.mockkStatic(android.util.Log::class)
        io.mockk.every { android.util.Log.e(any(), any(), any()) } returns 0

        Dispatchers.setMain(testDispatcher)
        dataStore = mockk(relaxed = true)
        alarmPlayer = mockk(relaxed = true)
        viewModel = SettingsViewModel(dataStore, alarmPlayer)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        io.mockk.unmockkStatic(android.util.Log::class)
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
        viewModel.saveWakeScreen(false)
        viewModel.saveSoundMode(1)
        viewModel.saveVibrationMode(2)
        viewModel.saveRingtone(3)
        viewModel.saveFlashScreen(false)
        
        advanceUntilIdle()
        
        coVerify(exactly = 1) { dataStore.saveFocusTime(30) }
        coVerify(exactly = 1) { dataStore.saveShortBreakTime(10) }
        coVerify(exactly = 1) { dataStore.saveLongBreakTime(20) }
        coVerify(exactly = 1) { dataStore.saveCycles(5) }
        coVerify(exactly = 1) { dataStore.saveSnoozeTime(8) }
        coVerify(exactly = 1) { dataStore.saveWakeScreen(false) }
        coVerify(exactly = 1) { dataStore.saveSoundMode(1) }
        coVerify(exactly = 1) { dataStore.saveVibrationMode(2) }
        coVerify(exactly = 1) { dataStore.saveRingtone(3) }
        coVerify(exactly = 1) { dataStore.saveFlashScreen(false) }
    }

    @Test
    fun `safeSave handles IOException and emits uiEvent`() = runTest {
        io.mockk.coEvery { dataStore.saveFocusTime(any()) } throws java.io.IOException("Test IO Exception")
        val events = mutableListOf<String>()
        val job = launch(kotlinx.coroutines.test.UnconfinedTestDispatcher()) {
            viewModel.uiEvents.toList(events)
        }
        
        viewModel.saveFocusTime(30)
        advanceUntilIdle()
        
        assert(events.isNotEmpty())
        assert(events[0] == "保存失败，请重试")
        
        job.cancel()
    }

    @Test
    fun `saveFocusTime when dataStore throws IOException does not crash`() = runTest(testDispatcher) {
        // [Fix-TG-2] 验证 safeSave{} 的异常捕获逻辑：
        // 当 DataStore 抛出 IOException 时，ViewModel 应静默处理并不崩溃。
        io.mockk.coEvery { dataStore.saveFocusTime(any()) } throws java.io.IOException("DataStore write failed")

        // 调用不应抛出异常
        viewModel.saveFocusTime(30)
        advanceUntilIdle()

        // 验证调用了 dataStore（即协程确实执行了，只是被 catch 了）
        coVerify(exactly = 1) { dataStore.saveFocusTime(30) }
        // 验证确实调用了 Log.e
        verify(exactly = 1) { android.util.Log.e("SettingsViewModel", "Failed to save to DataStore", any<java.io.IOException>()) }
        // 测试通过即证明 ViewModel 没有崩溃
    }

    @Test
    fun `saveFocusTime when dataStore throws generic Exception does not crash`() = runTest(testDispatcher) {
        // [Fix-TG-2] 验证 safeSave{} 对通用 Exception 的捕获：
        io.mockk.coEvery { dataStore.saveFocusTime(any()) } throws RuntimeException("Unexpected error")

        viewModel.saveFocusTime(25)
        advanceUntilIdle()

        coVerify(exactly = 1) { dataStore.saveFocusTime(25) }
        // 验证确实调用了 Log.e
        verify(exactly = 1) { android.util.Log.e("SettingsViewModel", "Unexpected error saving to DataStore", any<RuntimeException>()) }
        // 测试通过即证明 ViewModel 没有崩溃
    }
}
