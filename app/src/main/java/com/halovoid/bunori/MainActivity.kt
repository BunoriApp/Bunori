package com.halovoid.bunori

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.halovoid.bunori.data.repository.UpdateRepository
import com.halovoid.bunori.data.scheduler.services.SchedulerService
import com.halovoid.bunori.ui.MainScreen
import com.halovoid.bunori.ui.core.theme.BunoriTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        lifecycleScope.launch {
            SchedulerService.startService(this@MainActivity)
            try {
                UpdateRepository.getInstance(this@MainActivity).checkForUpdates()
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "Failed to check for updates on startup: ${e.message}", e)
            }
        }

        enableEdgeToEdge()
        setContent {
            BunoriTheme {
                MainScreen()
            }
        }
    }
}
