package com.hxlxz.tomatoclock

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.hxlxz.tomatoclock.ui.SettingsScreen
import com.hxlxz.tomatoclock.ui.TimerScreen
import com.hxlxz.tomatoclock.ui.theme.TomatoClockTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import android.Manifest
import android.annotation.SuppressLint
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts
import android.widget.Toast
import android.util.Log
import android.view.WindowManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var settingsDataStore: SettingsDataStore

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (!isGranted) {
            Log.w("MainActivity", "POST_NOTIFICATIONS permission denied by user. Foreground service may operate with degraded experience.")
            Toast.makeText(this, "需要通知权限以在后台运行倒计时", Toast.LENGTH_LONG).show()
        }
    }

    @SuppressLint("ObsoleteSdkInt")
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                settingsDataStore.wakeScreenFlow.collect { wakeScreen ->
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                        setShowWhenLocked(wakeScreen)
                        setTurnScreenOn(wakeScreen)
                    } else {
                        @Suppress("DEPRECATION")
                        if (wakeScreen) {
                            window.addFlags(
                                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                            )
                        } else {
                            window.clearFlags(
                                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                            )
                        }
                    }
                }
            }
        }
        
        setContent {
            TomatoClockTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()

                    NavHost(navController = navController, startDestination = "timer") {
                        composable("timer") {
                            TimerScreen(
                                onNavigateToSettings = { navController.navigate("settings") }
                            )
                        }
                        composable("settings") {
                            SettingsScreen(
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        // Start Foreground Service to ensure it runs
        try {
            val serviceIntent = Intent(this, TimerService::class.java)
            startService(serviceIntent)
        } catch (e: Exception) {
            Log.e("MainActivity", "Failed to start TimerService", e)
        }
    }
}
