package com.halovoid.bunori

import android.app.Application
import com.halovoid.bunori.api.core.scrapper.Scrapper
import com.halovoid.bunori.crash.CrashActivity
import com.halovoid.bunori.crash.GlobalExceptionHandler
import com.halovoid.bunori.extension.manager.ExtensionManager
import com.halovoid.bunori.ui.feature.crawler.cloudflare.CloudflareResolverImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class BunoriApplication : Application() {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        GlobalExceptionHandler.initialize(this, CrashActivity::class.java)
        com.halovoid.bunori.api.core.network.NetworkClient.init(this)

        // Initialize Cloudflare Resolver
        CloudflareResolverImpl.initialize(this)
        Scrapper.globalResolver = CloudflareResolverImpl.getInstance()

        // Load installed extensions as early as possible
        applicationScope.launch {
            ExtensionManager.getInstance(this@BunoriApplication)
                .loadInstalledExtensions()
        }

    }
}
