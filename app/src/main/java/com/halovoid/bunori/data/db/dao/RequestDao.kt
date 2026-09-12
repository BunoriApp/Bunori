package com.halovoid.bunori.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import com.halovoid.bunori.data.db.entities.RequestEntity
import com.halovoid.bunori.data.db.entities.RequestStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface RequestDao {
    @Query("SELECT * FROM requests")
    fun getAllRequests(): Flow<List<RequestEntity>>

    @Query("SELECT * FROM requests WHERE dependsOn IS NULL ORDER BY createdAt DESC")
    fun getRootRequests(): Flow<List<RequestEntity>>

    @Query("SELECT * FROM requests WHERE dependsOn IS NULL AND novelUrl = :url ORDER BY createdAt DESC")
    fun getRootRequestByNovelFlow(url: String): Flow<List<RequestEntity>>

    @Query("SELECT * FROM requests WHERE id = :id")
    suspend fun getRequestById(id: String) : RequestEntity?

    @Query("SELECT * FROM requests WHERE id = :id")
    fun getRequestByIdFlow(id: String): Flow<RequestEntity?>

    @Query("SELECT * FROM requests WHERE dependsOn = :id")
    fun getRequestsByDependenceFlow(id: String): Flow<List<RequestEntity>>

    @Query("SELECT * FROM requests WHERE dependsOn = :id")
    suspend fun getRequestsByDependence(id: String) : List<RequestEntity>

    @Query("DELETE FROM requests WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("""
        UPDATE requests
        SET
            progressSuccess = (SELECT COALESCE(SUM(progressSuccess), 0) FROM requests WHERE dependsOn = :parentId),
            progressFailed = (SELECT COALESCE(SUM(progressFailed), 0) FROM requests WHERE dependsOn = :parentId),
            progressCancelled = (SELECT COALESCE(SUM(progressCancelled), 0) FROM requests WHERE dependsOn = :parentId),
            rstatus = CASE 
                WHEN (SELECT COALESCE(SUM(progressSuccess + progressFailed + progressCancelled), 0) FROM requests WHERE dependsOn = :parentId) >= progressTotal 
                THEN (CASE WHEN status = 'CANCELLED' THEN 'CANCELLED' ELSE 'SUCCESS' END)
                WHEN status = 'CANCELLED' THEN 'CANCELLING'
                WHEN (SELECT COALESCE(SUM(progressSuccess + progressFailed + progressCancelled), 0) FROM requests WHERE dependsOn = :parentId) > 0
                THEN 'RUNNING'
                ELSE rstatus 
            END
        WHERE id = :parentId
    """)
    suspend fun syncProgress(parentId: String)

    @Query("UPDATE requests SET progressTotal = :total WHERE id = :id")
    suspend fun updateProgressTotal(id: String, total: Int)

    @Transaction
    suspend fun cancelRequest(requestId: String) {
        val request = getRequestById(requestId) ?: return
        val hasChildren = hasChildren(requestId)
        updateRequest(request.copy(
            status = RequestStatus.CANCELLED,
            rstatus = if (hasChildren) RequestStatus.CANCELLING else RequestStatus.CANCELLED,
            progressCancelled = if (hasChildren) request.progressCancelled else request.progressTotal,
            updatedAt = System.currentTimeMillis()
        ))
        propagateProgress(requestId)
    }

    @Query("UPDATE requests SET status = :status, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateStatus(id: String, status: RequestStatus, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE requests SET status = :newStatus, error = NULL, updatedAt = :updatedAt WHERE id = :id AND status = :expectedStatus")
    suspend fun resetStatus(id: String, newStatus: RequestStatus, expectedStatus: RequestStatus, updatedAt: Long = System.currentTimeMillis()): Int

    /**
     * Propagates the progress down the tree so that each data node has its own correct progress bar
     */
    @Transaction
    suspend fun propagateProgress(childRequestId: String) {
        val child = getRequestById(childRequestId) ?: return
        val parentId = child.dependsOn ?: return

        syncProgress(parentId)

        propagateProgress(parentId)
    }

    @Transaction
    suspend fun replayRequest(requestId: String) {
        batchReset(requestId, true)
    }

    @Transaction
    suspend fun resumeRequest(requestId: String) {
        batchReset(requestId, false)
    }

    @Transaction
    suspend fun batchReset(requestId: String, isFullWipe: Boolean) {
        val now = System.currentTimeMillis()
        
        // 1. Reset Children
        if (isFullWipe) {
            resetAllChildren(requestId, now)
        } else {
            resetUnfinishedChildren(requestId, now)
        }

        // 2. Reset the request itself
        val request = getRequestById(requestId) ?: return
        updateRequest(request.copy(
            status = RequestStatus.PENDING,
            rstatus = RequestStatus.PENDING,
            error = null,
            completedAt = null,
            updatedAt = now
        ))
        
        // 3. If it's a leaf, reset its progress counts manually
        if (!hasChildren(requestId)) {
            resetLeafProgress(requestId)
        }

        // 4. Final Sync and Propagate
        syncProgress(requestId)
        propagateProgress(requestId)
    }

    @Query("UPDATE requests SET status = 'PENDING', rstatus = 'PENDING', progressSuccess = 0, progressFailed = 0, progressCancelled = 0, error = NULL, completedAt = NULL, updatedAt = :now WHERE dependsOn = :parentId")
    suspend fun resetAllChildren(parentId: String, now: Long)

    @Query("UPDATE requests SET status = 'PENDING', rstatus = 'PENDING', progressSuccess = 0, progressFailed = 0, progressCancelled = 0, error = NULL, completedAt = NULL, updatedAt = :now WHERE dependsOn = :parentId AND status != 'SUCCESS'")
    suspend fun resetUnfinishedChildren(parentId: String, now: Long)

    @Query("UPDATE requests SET progressSuccess = 0, progressFailed = 0, progressCancelled = 0 WHERE id = :id")
    suspend fun resetLeafProgress(id: String)

    @Query("""
        SELECT EXISTS(
            SELECT 1 FROM requests
            WHERE dependsOn = :requestId
            LIMIT 1
        )
    """)
    suspend fun hasChildren(requestId: String): Boolean

    @Upsert
    suspend fun insertRequests(request: List<RequestEntity>)

    @Update
    suspend fun updateRequest(request: RequestEntity)
}
