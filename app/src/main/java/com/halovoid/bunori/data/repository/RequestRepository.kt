package com.halovoid.bunori.data.repository

import android.content.Context
import com.halovoid.bunori.data.db.AppDatabase
import com.halovoid.bunori.data.db.entities.RequestEntity
import com.halovoid.bunori.data.scheduler.services.SchedulerService
import com.halovoid.bunori.domain.models.Request
import com.halovoid.bunori.domain.models.toDomain
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext

/**
 * Shared repository for managing Request states, including in-memory UI states like cancellation.
 */
class RequestRepository private constructor(private val context: Context) {
    private val db = AppDatabase.getDatabase(context)
    val requestDao = db.requestDao()

    private val _cancellingRequestIds = MutableStateFlow<Set<String>>(emptySet())
    val cancellingRequestIds: StateFlow<Set<String>> = _cancellingRequestIds.asStateFlow()

    private val _activeActionIds = MutableStateFlow<Set<String>>(emptySet())
    val activeActionIds: StateFlow<Set<String>> = _activeActionIds.asStateFlow()

    fun getRootRequests(): Flow<List<Request>> = requestDao.getRootRequests().map { entities ->
        entities.map { it.toDomain() }
    }

    fun getRootRequestByNovelFlow(url: String): Flow<List<Request>> = 
        requestDao.getRootRequestByNovelFlow(url).map { entities ->
            entities.map { it.toDomain() }
        }

    fun getRequestByIdFlow(id: String): Flow<Request?> = requestDao.getRequestByIdFlow(id).map { it?.toDomain() }

    fun getRequestsByDependenceFlow(id: String): Flow<List<Request>> = 
        requestDao.getRequestsByDependenceFlow(id).map { entities ->
            entities.map { it.toDomain() }
        }

    suspend fun insertRequests(requests: List<RequestEntity>) {
        requestDao.insertRequests(requests)
    }

    suspend fun cancelRequest(requestId: String) = withContext(kotlinx.coroutines.Dispatchers.IO) {
        _cancellingRequestIds.update { it + requestId }
        try {
            requestDao.cancelRequest(requestId)
            SchedulerService.cancelJob(context, requestId)
        } finally {
            _cancellingRequestIds.update { it - requestId }
        }
    }

    suspend fun replayRequest(requestId: String) = withContext(kotlinx.coroutines.Dispatchers.IO) {
        _activeActionIds.update { it + requestId }
        try {
            requestDao.replayRequest(requestId)
            SchedulerService.startService(context)
        } finally {
            _activeActionIds.update { it - requestId }
        }
    }

    suspend fun resumeRequest(requestId: String) = withContext(kotlinx.coroutines.Dispatchers.IO) {
        _activeActionIds.update { it + requestId }
        try {
            requestDao.resumeRequest(requestId)
            SchedulerService.startService(context)
        } finally {
            _activeActionIds.update { it - requestId }
        }
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
