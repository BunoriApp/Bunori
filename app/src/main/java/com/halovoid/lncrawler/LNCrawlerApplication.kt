package com.halovoid.lncrawler

import android.app.Application
import com.halovoid.lncrawler.api.core.scrapper.Scrapper
import com.halovoid.lncrawler.crash.CrashActivity
import com.halovoid.lncrawler.crash.GlobalExceptionHandler
import com.halovoid.lncrawler.api.loader.SourceLoader
import com.halovoid.lncrawler.ui.feature.crawler.cloudflare.CloudflareResolverImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class LNCrawlerApplication : Application() {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        GlobalExceptionHandler.initialize(this, CrashActivity::class.java)
        com.halovoid.lncrawler.api.core.network.NetworkClient.init(this)

        // Initialize Cloudflare Resolver
        CloudflareResolverImpl.initialize(this)
        Scrapper.globalResolver = CloudflareResolverImpl.getInstance()

        // Load local sources as early as possible
        applicationScope.launch {
            SourceLoader(this@LNCrawlerApplication).loadLocalSources()
        }
    }
}
