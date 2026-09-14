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
import com.halovoid.bunori.data.db.dao.BatchDao
import com.halovoid.bunori.data.db.dao.TaskDao
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

class SchedulerService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private lateinit var scheduler: JobScheduler
    private lateinit var batchDao: BatchDao
    private lateinit var taskDao: TaskDao

    companion object {
        const val CHANNEL_ID = "scheduler_channel"
        const val NOTIFICATION_ID = 1
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
        const val ACTION_CANCEL_JOB = "ACTION_CANCEL_JOB"
        const val ACTION_PAUSE_JOB = "ACTION_PAUSE_JOB"
        const val ACTION_RESUME_JOB = "ACTION_RESUME_JOB"
        const val ACTION_REPLAY_JOB = "ACTION_REPLAY_JOB"

        const val EXTRA_JOB_ID = "EXTRA_JOB_ID"

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

        fun cancelJob(context: Context, jobId: String) {
            val intent = Intent(context, SchedulerService::class.java).apply {
                action = ACTION_CANCEL_JOB
                putExtra(EXTRA_JOB_ID, jobId)
            }
            context.startService(intent)
        }

        fun pauseJob(context: Context, jobId: String) {
            val intent = Intent(context, SchedulerService::class.java).apply {
                action = ACTION_PAUSE_JOB
                putExtra(EXTRA_JOB_ID, jobId)
            }
            context.startService(intent)
        }

        fun resumeJob(context: Context, jobId: String) {
            val intent = Intent(context, SchedulerService::class.java).apply {
                action = ACTION_RESUME_JOB
                putExtra(EXTRA_JOB_ID, jobId)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun replayJob(context: Context, jobId: String) {
            val intent = Intent(context, SchedulerService::class.java).apply {
                action = ACTION_REPLAY_JOB
                putExtra(EXTRA_JOB_ID, jobId)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        val db = AppDatabase.getDatabase(this)
        batchDao = db.batchDao()
        taskDao = db.taskDao()

        val novelRepository = NovelRepository.getInstance(this)
        val chapterRepository = ChapterRepository.getInstance(this)
        val preferenceRepository = PreferenceRepository.getInstance(this)
        val storageRepository = StorageRepositoryImpl.getInstance(this)
        val artifactRepository = ArtifactRepository.getInstance(this)

        val epubGenerator = EpubGenerator(storageRepository)
        val pdfGenerator = PdfGenerator(storageRepository)
        val generatorFactory = ArtifactGeneratorFactory(listOf(epubGenerator, pdfGenerator))

        val registry = JobHandlerRegistry()
        val crawlerFactory = CrawlerFactory

        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(CloudflareInterceptor(CloudflareResolverImpl.getInstance()))
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()

        val scrapper = Scrapper(okHttpClient)

        registry.register(RequestType.CHAPTER, ChapterHandler(
            scrapper, chapterRepository, storageRepository, crawlerFactory
        ))
        registry.register(RequestType.NOVEL_METADATA, NovelMetadataHandler(
            crawlerFactory, novelRepository, chapterRepository, storageRepository
        ))
        registry.register(RequestType.ARTIFACT, ArtifactHandler(
            novelRepository, chapterRepository, crawlerFactory, storageRepository, generatorFactory, artifactRepository
        ))
        registry.register(RequestType.BACKUP, BackupService(applicationContext))
        registry.register(RequestType.RANGE_DOWNLOAD, RangeDownloadHandler(chapterRepository, taskDao))

        scheduler = JobScheduler(batchDao, taskDao, registry, preferenceRepository = preferenceRepository)
        scheduler.setOnEmptyListener {
            stopSelf()
        }

        createNotificationChannel()

        serviceScope.launch {
            SourceLoader(this@SchedulerService).loadLocalSources()
        }

        observeProgress()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val jobId = intent?.getStringExtra(EXTRA_JOB_ID)
        when (intent?.action) {
            ACTION_START -> {
                ensureForeground()
                scheduler.start()
            }
            ACTION_STOP -> {
                scheduler.stop()
                stopSelf()
            }
            ACTION_CANCEL_JOB -> {
                if (jobId != null) {
                    scheduler.cancelActiveJob(jobId)
                }
            }
            ACTION_PAUSE_JOB -> {
                if (jobId != null) {
                    scheduler.pauseJob(jobId)
                }
            }
            ACTION_RESUME_JOB -> {
                ensureForeground()
                if (jobId != null) {
                    scheduler.resumeJob(jobId)
                } else {
                    scheduler.start()
                }
            }
            ACTION_REPLAY_JOB -> {
                ensureForeground()
                if (jobId != null) {
                    scheduler.replayJob(jobId)
                } else {
                    scheduler.start()
                }
            }
        }
        return START_STICKY
    }

    private fun ensureForeground() {
        val initialConfig = NotificationConfig("Processing...", "Active background tasks", 0, 0, true)
        startForeground(NOTIFICATION_ID, createNotification(initialConfig, 0))
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

        val largeIcon = BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher)

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
        batchDao.getBatchesWithStatsFlow()
            .onEach { batches ->
                val active = batches.filter { it.batch.status == RequestStatus.RUNNING }
                    .sortedByDescending { it.batch.updatedAt }
                if (active.isEmpty()) return@onEach

                val primary = active.first()
                val config = NotificationConfig(
                    title = when (primary.batch.type) {
                        RequestType.RANGE_DOWNLOAD -> "Downloading Chapters"
                        RequestType.ARTIFACT -> "Creating Artifact"
                        RequestType.NOVEL_METADATA -> "Refreshing Novel"
                        else -> "LN Crawler Task"
                    },
                    content = primary.batch.name,
                    progressCurrent = primary.completedTasks,
                    progressTotal = primary.totalTasks,
                    isIndeterminate = primary.totalTasks <= 0
                )
                val othersCount = active.size - 1

                val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
                manager.notify(NOTIFICATION_ID, createNotification(config, othersCount))
            }
            .launchIn(serviceScope)
    }
}
