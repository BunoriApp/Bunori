package com.halovoid.bunori.data.repository

import android.content.Context
import com.halovoid.bunori.data.db.AppDatabase
import com.halovoid.bunori.data.db.dao.BatchWithStats
import com.halovoid.bunori.data.db.entities.BatchEntity
import com.halovoid.bunori.data.db.entities.RequestEntity
import com.halovoid.bunori.data.db.entities.RequestStatus
import com.halovoid.bunori.data.db.entities.RequestType
import com.halovoid.bunori.data.db.entities.TaskEntity
import com.halovoid.bunori.data.handlers.utility.parsedMetadata
import com.halovoid.bunori.data.scheduler.services.SchedulerService
import com.halovoid.bunori.domain.models.Request
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import org.json.JSONObject

class RequestRepository private constructor(private val context: Context) {
    private val db = AppDatabase.getDatabase(context)
    val batchDao = db.batchDao()
    val taskDao = db.taskDao()
    private val chapterRepository = ChapterRepository.getInstance(context)

    private val _cancellingRequestIds = MutableStateFlow<Set<String>>(emptySet())
    val cancellingRequestIds: StateFlow<Set<String>> = _cancellingRequestIds.asStateFlow()

    private val _activeActionIds = MutableStateFlow<Set<String>>(emptySet())
    val activeActionIds: StateFlow<Set<String>> = _activeActionIds.asStateFlow()

    fun getRootRequests(): Flow<List<Request>> = batchDao.getBatchesWithStatsFlow().map { list ->
        list.map { it.toDomain() }
    }

    fun getRootRequestByNovelFlow(url: String): Flow<List<Request>> = 
        batchDao.getBatchesWithStatsByNovelFlow(url).map { list ->
            list.map { it.toDomain() }
        }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    fun getRequestByIdFlow(id: String): Flow<Request?> = 
        batchDao.getBatchWithStatsByIdFlow(id).flatMapLatest { batchWithStats ->
            if (batchWithStats != null) {
                flowOf(batchWithStats.toDomain())
            } else {
                taskDao.getTaskByIdFlow(id).map { task -> task?.toDomain() }
            }
        }

    fun getRequestsByDependenceFlow(batchId: String): Flow<List<Request>> = 
        taskDao.getTasksByBatchIdFlow(batchId).map { list ->
            list.map { it.toDomain() }
        }

    suspend fun insertRequests(requests: List<RequestEntity>) = withContext(Dispatchers.IO) {
        for (request in requests) {
            val batch = BatchEntity(
                id = request.id,
                name = request.name,
                novelUrl = request.novelUrl,
                type = request.type,
                priority = request.priority,
                metadata = request.metadata
            )
            val task = TaskEntity(
                id = "${request.id}_init",
                batchId = request.id,
                name = if (request.type == RequestType.RANGE_DOWNLOAD) "Preparing chapters..." else request.name,
                url = request.url,
                novelUrl = request.novelUrl,
                type = request.type,
                priority = request.priority,
                metadata = request.metadata
            )
            batchDao.insertBatch(batch)
            taskDao.insertTask(task)
        }
    }

    suspend fun pauseRequest(batchId: String) = withContext(Dispatchers.IO) {
        _activeActionIds.update { it + batchId }
        try {
            batchDao.updateStatus(batchId, RequestStatus.PAUSED)
            taskDao.updateUnfinishedStatusForBatch(batchId, RequestStatus.PAUSED)
            SchedulerService.pauseJob(context, batchId)
        } finally {
            _activeActionIds.update { it - batchId }
        }
    }

    suspend fun resumeRequest(batchId: String) = withContext(Dispatchers.IO) {
        _activeActionIds.update { it + batchId }
        try {
            batchDao.updateStatus(batchId, RequestStatus.RUNNING)
            taskDao.updateStatusForBatch(batchId, RequestStatus.PAUSED, RequestStatus.PENDING)
            SchedulerService.resumeJob(context, batchId)
        } finally {
            _activeActionIds.update { it - batchId }
        }
    }

    suspend fun replayRequest(batchId: String) = withContext(Dispatchers.IO) {
        _activeActionIds.update { it + batchId }
        try {
            batchDao.updateStatus(batchId, RequestStatus.PENDING)
            taskDao.resetAllTasksForBatch(batchId)
            SchedulerService.replayJob(context, batchId)
        } finally {
            _activeActionIds.update { it - batchId }
        }
    }

    suspend fun cancelRequest(batchId: String) = withContext(Dispatchers.IO) {
        _cancellingRequestIds.update { it + batchId }
        try {
            batchDao.updateStatus(batchId, RequestStatus.CANCELLED)
            taskDao.updateUnfinishedStatusForBatch(batchId, RequestStatus.CANCELLED)
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
        private var INSTANCE: RequestRepository? = null

        fun getInstance(context: Context): RequestRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: RequestRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}

fun BatchWithStats.toDomain(): Request = Request(
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

fun TaskEntity.toDomain(): Request = Request(
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
    progressSuccess = if (status == RequestStatus.SUCCESS) 1 else 0,
    progressFailed = if (status == RequestStatus.FAILED) 1 else 0,
    progressCancelled = if (status == RequestStatus.CANCELLED) 1 else 0,
    status = status,
    rstatus = status,
    metadata = metadata,
    error = error
)
