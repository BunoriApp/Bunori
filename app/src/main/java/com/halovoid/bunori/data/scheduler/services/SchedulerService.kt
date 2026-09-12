package com.halovoid.bunori.data.scheduler.services

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.halovoid.bunori.MainActivity
import com.halovoid.bunori.R
import com.halovoid.bunori.api.backup.BackupService
import com.halovoid.bunori.data.artifact.ArtifactGenerator
import com.halovoid.bunori.data.artifact.ArtifactGeneratorFactory
import com.halovoid.bunori.data.artifact.generators.EpubGenerator
import com.halovoid.bunori.data.artifact.generators.PdfGenerator
import com.halovoid.bunori.api.core.crawler.CrawlerFactory
import com.halovoid.bunori.api.core.network.CloudflareInterceptor
import com.halovoid.bunori.api.core.scrapper.Scrapper
import com.halovoid.bunori.api.loader.SourceLoader
import com.halovoid.bunori.data.db.AppDatabase
import com.halovoid.bunori.data.db.dao.RequestDao
import com.halovoid.bunori.data.db.entities.RequestEntity
import com.halovoid.bunori.data.db.entities.RequestStatus
import com.halovoid.bunori.data.db.entities.RequestType
import com.halovoid.bunori.data.handlers.ArtifactHandler
import com.halovoid.bunori.data.handlers.ChapterHandler
import com.halovoid.bunori.data.handlers.NovelMetadataHandler
import com.halovoid.bunori.data.handlers.RangeDownloadHandler
import com.halovoid.bunori.data.repository.*
import com.halovoid.bunori.data.scheduler.jobs.JobHandlerRegistry
import com.halovoid.bunori.data.scheduler.jobs.JobScheduler
import com.halovoid.bunori.ui.feature.crawler.cloudflare.CloudflareResolverImpl
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

private data class NotificationConfig(
    val title: String,
    val content: String,
    val progressCurrent: Int,
    val progressTotal: Int,
    val isIndeterminate: Boolean
)

private fun RequestEntity.toNotificationConfig(): NotificationConfig {
    val title = when (this.type) {
        RequestType.RANGE_DOWNLOAD -> "Downloading Chapters"
        RequestType.ARTIFACT -> "Creating Artifact"
        RequestType.NOVEL_METADATA -> "Refreshing Novel"
        else -> "LN Crawler Task"
    }

    return NotificationConfig(
        title = title,
        content = this.name,
        progressCurrent = this.progressSuccess,
        progressTotal = this.progressTotal,
        isIndeterminate = this.progressTotal <= 0
    )
}
class SchedulerService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private lateinit var scheduler: JobScheduler
    private lateinit var requestDao: RequestDao

    companion object {
        private const val CHANNEL_ID = "scheduler_channel"
        private const val NOTIFICATION_ID = 1
        private const val ACTION_START = "ACTION_START"
        private const val ACTION_STOP = "ACTION_STOP"

        private const val ACTION_CANCEL_JOB = "ACTION_CANCEL_JOB"

        private const val EXTRA_REQUEST_ID = "EXTRA_REQUEST_ID"

        fun startService(context: Context) {
            val intent = Intent(context, SchedulerService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, SchedulerService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }

        fun cancelJob(context: Context, requestId: String) {
            val intent = Intent(context, SchedulerService::class.java).apply {
                action = ACTION_CANCEL_JOB
                putExtra(EXTRA_REQUEST_ID, requestId)
            }
            context.startService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        val db = AppDatabase.getDatabase(this)
        requestDao = db.requestDao()
        val chapterDao = db.chapterDao()

        // 1. Initializing Repositories
        val novelRepository = NovelRepository.getInstance(this)
        val chapterRepository = ChapterRepository.getInstance(this)
        val preferenceRepository = PreferenceRepository.getInstance(this)
        val storageRepository = StorageRepositoryImpl.getInstance(this)
        val artifactRepository = ArtifactRepository.getInstance(this)

        // 2. Initialize Artifact System
        val epubGenerator = EpubGenerator(storageRepository)
        val pdfGenerator = PdfGenerator(storageRepository)
        val generators = listOf<ArtifactGenerator>(epubGenerator, pdfGenerator)
        val generatorFactory = ArtifactGeneratorFactory(generators)

        // 3. Initialize Handler and Registry
        val registry = JobHandlerRegistry()
        val crawlerFactory = CrawlerFactory
        
        // Initialize OkHttpClient with Cloudflare Interceptor
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(CloudflareInterceptor(CloudflareResolverImpl.getInstance()))
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
            
        val scrapper = Scrapper(okHttpClient)

        // 4. Register Handlers
        registry.register(RequestType.CHAPTER, ChapterHandler(
            requestDao, scrapper, chapterRepository, storageRepository, crawlerFactory
        ))
        registry.register(RequestType.NOVEL_METADATA, NovelMetadataHandler(
            crawlerFactory, novelRepository, chapterRepository, storageRepository, requestDao
        ))
        registry.register(RequestType.ARTIFACT, ArtifactHandler(
            novelRepository, chapterRepository,
            crawlerFactory, storageRepository, generatorFactory, artifactRepository,
            requestDao
        ))
        registry.register(RequestType.RANGE_DOWNLOAD, RangeDownloadHandler(
            chapterRepository, requestDao
        ))
        registry.register(RequestType.BACKUP, BackupService(applicationContext))

        // 5. Set Up Scheduler
        scheduler = JobScheduler(requestDao, registry, preferenceRepository = preferenceRepository)
        scheduler.setOnEmptyListener {
            stopSelf()
        }

        createNotificationChannel()
        
        // Ensure sources are loaded even if service starts independently
        serviceScope.launch {
            SourceLoader(this@SchedulerService).loadLocalSources()
        }
        
        observeProgress()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val initialConfig = NotificationConfig("Initializing...", "Starting scheduler", 0, 0, true)
                startForeground(NOTIFICATION_ID, createNotification(initialConfig, 0))
                scheduler.start()
            }
            ACTION_STOP -> {
                scheduler.stop()
                stopSelf()
            }
            ACTION_CANCEL_JOB -> {
                val requestId = intent.getStringExtra(EXTRA_REQUEST_ID)
                if (requestId != null) {
                    scheduler.cancelActiveJob(requestId)
                }
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        scheduler.stop()
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Job Scheduler",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows progress of background crawl and download tasks"
            }
            val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(config: NotificationConfig, othersCount: Int): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, 
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val contextText = if (othersCount > 0) {
            "${config.content} (+ $othersCount others)"
        } else {
            config.content
        }

        val largeIcon =
            BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(config.title)
            .setContentText(contextText)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setLargeIcon(largeIcon)
            .setContentIntent(pendingIntent)
            .setProgress(
                config.progressTotal,
                config.progressCurrent,
                config.isIndeterminate
            )
            .setOngoing(true)
            .build()
    }

    private fun observeProgress() {
        requestDao.getRootRequests()
            .onEach { requests ->
                val activeRequests = requests.filter { it.progressSuccess + it.progressFailed + it.progressCancelled < it.progressTotal }
                    .sortedWith (
                        compareByDescending<RequestEntity> {
                            it.status == RequestStatus.RUNNING
                        }.thenByDescending { it.updatedAt }
                    )
                if (activeRequests.isEmpty()) return@onEach

                val primaryRequest = activeRequests.first()
                val config = primaryRequest.toNotificationConfig()
                val othersCount = activeRequests.size - 1
                
                val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
                manager.notify(NOTIFICATION_ID, createNotification(config, othersCount))
            }
            .launchIn(serviceScope)
    }
}
