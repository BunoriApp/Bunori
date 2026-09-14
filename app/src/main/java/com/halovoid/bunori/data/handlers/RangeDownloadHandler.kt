package com.halovoid.bunori.data.handlers

import com.halovoid.bunori.data.db.dao.TaskDao
import com.halovoid.bunori.data.db.entities.JobStatus
import com.halovoid.bunori.data.db.entities.JobType
import com.halovoid.bunori.data.db.entities.TaskEntity
import com.halovoid.bunori.data.handlers.utility.parsedMetadata
import com.halovoid.bunori.data.repository.ChapterRepository
import com.halovoid.bunori.data.scheduler.jobs.JobHandler
import com.halovoid.bunori.data.scheduler.jobs.JobResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

class RangeDownloadHandler(
    private val chapterRepository: ChapterRepository,
    private val taskDao: TaskDao
) : JobHandler {
    override suspend fun handle(task: TaskEntity): JobResult = withContext(Dispatchers.IO) {
        val metadata = task.parsedMetadata
        val startIndex = metadata.startIndex ?: 1
        val endIndex = metadata.endIndex ?: Int.MAX_VALUE

        val chapters = chapterRepository.getChaptersByNovelUrl(task.novelUrl)
            .filter { it.index in startIndex..endIndex }

        if (chapters.isEmpty()) {
            return@withContext JobResult.Failure(
                Exception("No chapters found in range ($startIndex-$endIndex)")
            )
        }

        val chapterTasks = chapters.map { chapter ->
            val taskMetadata = JSONObject().apply {
                put("chapterId", chapter.id)
                put("crawlerName", metadata.crawlerName)
            }.toString()

            TaskEntity(
                id = "${task.batchId}_ch_${chapter.index}",
                batchId = task.batchId,
                name = chapter.title.ifBlank { "Chapter ${chapter.index}" },
                url = chapter.url,
                novelUrl = chapter.novelUrl,
                type = JobType.CHAPTER,
                priority = task.priority,
                metadata = taskMetadata,
                status = JobStatus.PENDING
            )
        }

        taskDao.insertTasks(chapterTasks)
        taskDao.deleteTaskById(task.id)

        JobResult.Success
    }
}
