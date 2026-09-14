package com.halovoid.bunori.data.scheduler

import com.halovoid.bunori.data.db.entities.TaskEntity
import com.halovoid.bunori.data.db.entities.JobStatus
import com.halovoid.bunori.data.db.entities.JobType
import com.halovoid.bunori.data.scheduler.jobs.ReadyQueue
import org.junit.Assert.*
import org.junit.Test

class ReadyQueueTest {

    private fun createChapterRequest(
        id: String,
        novelUrl: String,
        index: Int,
        priority: Int = 0,
        createdAt: Long = 1000L
    ): TaskEntity {
        return TaskEntity(
            id = "${id}_chapter_${index}_${index}",
            batchId = id,
            name = "Chapter $index",
            url = "$novelUrl/chapter-$index",
            novelUrl = novelUrl,
            priority = priority,
            type = JobType.CHAPTER,
            createdAt = createdAt,
            completedAt = null,
            status = JobStatus.PENDING
        )
    }

    @Test
    fun testSingleNovelMaintainsChapterOrder() {
        val queue = ReadyQueue()
        val jobs = listOf(
            createChapterRequest("range1", "https://site.com/novelA", 1),
            createChapterRequest("range1", "https://site.com/novelA", 2),
            createChapterRequest("range1", "https://site.com/novelA", 3)
        )
        queue.pushAll(jobs)

        assertEquals("range1_chapter_1_1", queue.pop()?.id)
        assertEquals("range1_chapter_2_2", queue.pop()?.id)
        assertEquals("range1_chapter_3_3", queue.pop()?.id)
        assertNull(queue.pop())
        assertTrue(queue.isEmpty())
    }

    @Test
    fun testMultipleNovelsInterleaveRoundRobin() {
        val queue = ReadyQueue()
        val novelAJobs = listOf(
            createChapterRequest("rangeA", "https://site.com/novelA", 1, createdAt = 1000L),
            createChapterRequest("rangeA", "https://site.com/novelA", 2, createdAt = 1000L),
            createChapterRequest("rangeA", "https://site.com/novelA", 3, createdAt = 1000L)
        )
        val novelBJobs = listOf(
            // Novel B was added later (createdAt 2000L)
            createChapterRequest("rangeB", "https://site.com/novelB", 1, createdAt = 2000L),
            createChapterRequest("rangeB", "https://site.com/novelB", 2, createdAt = 2000L)
        )
        queue.pushAll(novelAJobs + novelBJobs)

        // Must round-robin between Novel A and Novel B!
        assertEquals("rangeA_chapter_1_1", queue.pop()?.id)
        assertEquals("rangeB_chapter_1_1", queue.pop()?.id)
        assertEquals("rangeA_chapter_2_2", queue.pop()?.id)
        assertEquals("rangeB_chapter_2_2", queue.pop()?.id)
        // Novel B is exhausted, Novel A finishes
        assertEquals("rangeA_chapter_3_3", queue.pop()?.id)
        assertNull(queue.pop())
    }

    @Test
    fun testHigherPriorityRunsFirst() {
        val queue = ReadyQueue()
        val lowPriority = listOf(
            createChapterRequest("rangeA", "https://site.com/novelA", 1, priority = 0),
            createChapterRequest("rangeA", "https://site.com/novelA", 2, priority = 0)
        )
        val highPriority = listOf(
            createChapterRequest("single", "https://site.com/novelB", 10, priority = 5)
        )
        queue.pushAll(lowPriority + highPriority)

        // Priority 5 pops before priority 0
        assertEquals("single_chapter_10_10", queue.pop()?.id)
        assertEquals("rangeA_chapter_1_1", queue.pop()?.id)
        assertEquals("rangeA_chapter_2_2", queue.pop()?.id)
        assertNull(queue.pop())
    }
}
