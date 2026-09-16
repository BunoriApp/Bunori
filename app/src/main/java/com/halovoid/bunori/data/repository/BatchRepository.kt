package com.halovoid.bunori.data.repository

import android.content.Context
import com.halovoid.bunori.data.db.AppDatabase
import com.halovoid.bunori.data.db.dao.BatchWithStats
import com.halovoid.bunori.data.db.entities.BatchEntity
import com.halovoid.bunori.data.db.entities.JobStatus
import com.halovoid.bunori.data.db.entities.JobType
import com.halovoid.bunori.data.db.entities.TaskEntity
import com.halovoid.bunori.data.handlers.utility.parsedMetadata
import com.halovoid.bunori.data.scheduler.services.SchedulerService
import com.halovoid.bunori.domain.models.Batch
import com.halovoid.bunori.domain.models.Chapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import org.json.JSONObject

class BatchRepository private constructor(private val context: Context) {
    private val db = AppDatabase.getDatabase(context)
    val batchDao = db.batchDao()
    val taskDao = db.taskDao()
    private val chapterRepository = ChapterRepository.getInstance(context)

    private val _cancellingRequestIds = MutableStateFlow<Set<String>>(emptySet())
    val cancellingRequestIds: StateFlow<Set<String>> = _cancellingRequestIds.asStateFlow()

    private val _activeActionIds = MutableStateFlow<Set<String>>(emptySet())
    val activeActionIds: StateFlow<Set<String>> = _activeActionIds.asStateFlow()

    fun getRootRequests(): Flow<List<Batch>> = batchDao.getBatchesWithStatsFlow().map { list ->
        list.map { it.toDomain() }
    }

    fun getRootRequestByNovelFlow(url: String): Flow<List<Batch>> =
        batchDao.getBatchesWithStatsByNovelFlow(url).map { list ->
            list.map { it.toDomain() }
        }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    fun getRequestByIdFlow(id: String): Flow<Batch?> =
        batchDao.getBatchWithStatsByIdFlow(id).flatMapLatest { batchWithStats ->
            if (batchWithStats != null) {
                flowOf(batchWithStats.toDomain())
            } else {
                taskDao.getTaskByIdFlow(id).map { task -> task?.toDomain() }
            }
        }

    fun getRequestsByDependenceFlow(batchId: String): Flow<List<Batch>> =
        taskDao.getTasksByBatchIdFlow(batchId).map { list ->
            list.map { it.toDomain() }
        }

    suspend fun insertRequests(batches: List<BatchEntity>) = withContext(Dispatchers.IO) {
        for (batch in batches) {
            val task = TaskEntity(
                id = "${batch.id}_init",
                batchId = batch.id,
                name = if (batch.type == JobType.RANGE_DOWNLOAD) "Preparing chapters..." else batch.name,
                url = null,
                novelUrl = batch.novelUrl,
                type = batch.type,
                priority = batch.priority,
                metadata = batch.metadata
            )
            batchDao.insertBatch(batch)
            taskDao.insertTask(task)
        }
    }

    suspend fun insertBatchWithChapterTasks(
        batch: BatchEntity,
        chapters: List<Chapter>
    ) = withContext(Dispatchers.IO) {
        val crawlerName = batch.parsedMetadata.crawlerName ?: ""
        val tasks = chapters.map { chapter ->
            val taskMetadata = JSONObject().apply {
                put("chapterId", chapter.id)
                put("crawlerName", crawlerName)
            }.toString()

            TaskEntity(
                id = "${batch.id}_ch_${chapter.index}_${chapter.id}",
                batchId = batch.id,
                name = chapter.title.ifBlank { "Chapter ${chapter.index}" },
                url = chapter.url,
                novelUrl = chapter.novelUrl,
                type = JobType.CHAPTER,
                priority = batch.priority,
                metadata = taskMetadata,
                status = JobStatus.PENDING
            )
        }
        batchDao.insertBatch(batch)
        taskDao.insertTasks(tasks)
    }

    suspend fun pauseRequest(batchId: String) = withContext(Dispatchers.IO) {
        _activeActionIds.update { it + batchId }
        try {
            batchDao.updateStatus(batchId, JobStatus.PAUSED)
            taskDao.updateUnfinishedStatusForBatch(batchId, JobStatus.PAUSED)
            SchedulerService.pauseJob(context, batchId)
        } finally {
            _activeActionIds.update { it - batchId }
        }
    }

    suspend fun resumeRequest(batchId: String) = withContext(Dispatchers.IO) {
        _activeActionIds.update { it + batchId }
        try {
            batchDao.updateStatus(batchId, JobStatus.RUNNING)
            taskDao.updateStatusForBatch(batchId, JobStatus.PAUSED, JobStatus.PENDING)
            SchedulerService.resumeJob(context, batchId)
        } finally {
            _activeActionIds.update { it - batchId }
        }
    }

    suspend fun replayRequest(batchId: String) = withContext(Dispatchers.IO) {
        _activeActionIds.update { it + batchId }
        try {
            batchDao.updateStatus(batchId, JobStatus.PENDING)
            taskDao.resetAllTasksForBatch(batchId)
            SchedulerService.replayJob(context, batchId)
        } finally {
            _activeActionIds.update { it - batchId }
        }
    }

    suspend fun cancelRequest(batchId: String) = withContext(Dispatchers.IO) {
        _cancellingRequestIds.update { it + batchId }
        try {
            batchDao.updateStatus(batchId, JobStatus.CANCELLED)
            taskDao.updateUnfinishedStatusForBatch(batchId, JobStatus.CANCELLED)
            SchedulerService.cancelJob(context, batchId)
        } finally {
            _cancellingRequestIds.update { it - batchId }
        }
    }

    suspend fun deleteRequest(batchId: String) = withContext(Dispatchers.IO) {
        batchDao.deleteById(batchId)
        taskDao.deleteByBatchId(batchId)
    }

    companion object {
        @Volatile
        private var INSTANCE: BatchRepository? = null

        fun getInstance(context: Context): BatchRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: BatchRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}

fun BatchWithStats.toDomain(): Batch = Batch(
    id = batch.id,
    name = batch.name,
    parentNovel = batch.novelUrl,
    url = null,
    novelUrl = batch.novelUrl,
    priority = batch.priority,
    type = batch.type,
    createdAt = batch.createdAt,
    updatedAt = batch.updatedAt,
    completedAt = batch.completedAt,
    progressTotal = totalTasks,
    progressSuccess = completedTasks,
    progressFailed = failedTasks,
    progressCancelled = 0,
    status = batch.status,
    rstatus = batch.status,
    metadata = batch.metadata,
    error = batch.error
)

fun TaskEntity.toDomain(): Batch = Batch(
    id = id,
    name = name,
    parentNovel = novelUrl,
    dependsOn = batchId,
    url = url,
    novelUrl = novelUrl,
    priority = priority,
    type = type,
    createdAt = createdAt,
    updatedAt = updatedAt,
    completedAt = completedAt,
    progressTotal = 1,
    progressSuccess = if (status == JobStatus.SUCCESS) 1 else 0,
    progressFailed = if (status == JobStatus.FAILED) 1 else 0,
    progressCancelled = if (status == JobStatus.CANCELLED) 1 else 0,
    status = status,
    rstatus = status,
    metadata = metadata,
    error = error
)
