package com.halovoid.lncrawler

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.halovoid.lncrawler.data.repository.UpdateRepository
import com.halovoid.lncrawler.data.scheduler.services.SchedulerService
import com.halovoid.lncrawler.ui.MainScreen
import com.halovoid.lncrawler.ui.core.theme.LNCrawlerTheme
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
            LNCrawlerTheme {
                MainScreen()
            }
        }
    }
}
