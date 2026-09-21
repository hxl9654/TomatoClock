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
import androidx.core.content.ContextCompat
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
            Toast.makeText(this, getString(R.string.toast_permission_denied), Toast.LENGTH_LONG).show()
        }
    }

    @SuppressLint("ObsoleteSdkInt")
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
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

        // 【CR-4修复】startService 移至 onCreate，避免 onStart 在每次从
        // SettingsScreen 返回时重复触发（onStart 覆盖了 Activity 重建/Resume 路径）。
        // TimerService 声明为 START_STICKY，系统会在进程复活后自动重启它。
        try {
            val serviceIntent = Intent(this, TimerService::class.java)
            ContextCompat.startForegroundService(this, serviceIntent)
        } catch (e: Exception) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && e is android.app.ForegroundServiceStartNotAllowedException) {
                Log.e("MainActivity", "Foreground service start not allowed in background", e)
                Toast.makeText(this, getString(R.string.toast_foreground_service_failed), Toast.LENGTH_LONG).show()
            } else {
                Log.e("MainActivity", "Failed to start TimerService", e)
            }
        }
    }
}
