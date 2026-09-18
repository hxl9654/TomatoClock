package com.hxlxz.tomatoclock

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SettingsDataStoreTest {

    private lateinit var context: Context
    private lateinit var dataStore: SettingsDataStore

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        dataStore = SettingsDataStore(context)
    }

    @After
    fun teardown() = runTest {
        context.dataStore.edit { it.clear() }
    }

    @Test
    fun `default values are correct`() = runTest {
        assertEquals(25, dataStore.focusTimeFlow.first())
        assertEquals(5, dataStore.shortBreakTimeFlow.first())
        assertEquals(15, dataStore.longBreakTimeFlow.first())
        assertEquals(4, dataStore.cyclesFlow.first())
        assertEquals(5, dataStore.snoozeTimeFlow.first())
        assertEquals(0, dataStore.alertModeFlow.first())
        assertEquals(true, dataStore.wakeScreenFlow.first())
        assertEquals(false, dataStore.autoStartBreakFlow.first())
        assertEquals(false, dataStore.autoStartFocusFlow.first())
    }

    @Test
    fun `save and read focus time`() = runTest {
        dataStore.saveFocusTime(45)
        assertEquals(45, dataStore.focusTimeFlow.first())
    }

    @Test
    fun `save and read short break time`() = runTest {
        dataStore.saveShortBreakTime(10)
        assertEquals(10, dataStore.shortBreakTimeFlow.first())
    }

    @Test
    fun `save and read long break time`() = runTest {
        dataStore.saveLongBreakTime(30)
        assertEquals(30, dataStore.longBreakTimeFlow.first())
    }

    @Test
    fun `save and read cycles`() = runTest {
        dataStore.saveCycles(6)
        assertEquals(6, dataStore.cyclesFlow.first())
    }

    @Test
    fun `save and read snooze time`() = runTest {
        dataStore.saveSnoozeTime(10)
        assertEquals(10, dataStore.snoozeTimeFlow.first())
    }

    @Test
    fun `save and read alert mode`() = runTest {
        dataStore.saveAlertMode(3)
        assertEquals(3, dataStore.alertModeFlow.first())
    }

    @Test
    fun `save and read wake screen`() = runTest {
        dataStore.saveWakeScreen(false)
        assertEquals(false, dataStore.wakeScreenFlow.first())
    }

    @Test
    fun `save and read auto start break`() = runTest {
        dataStore.saveAutoStartBreak(true)
        assertEquals(true, dataStore.autoStartBreakFlow.first())
    }

    @Test
    fun `save and read auto start focus`() = runTest {
        dataStore.saveAutoStartFocus(true)
        assertEquals(true, dataStore.autoStartFocusFlow.first())
    }
}
