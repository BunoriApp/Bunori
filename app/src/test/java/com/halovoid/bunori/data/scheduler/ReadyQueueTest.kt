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

    @Test
    fun testSaturatedCrawlerIsSkippedWithoutLosingTasks() {
        val queue = ReadyQueue()
        val host1Jobs = listOf(
            TaskEntity(
                id = "h1_ch_1", batchId = "b1", name = "Ch 1", url = "u1", novelUrl = "novel1",
                priority = 0, type = JobType.CHAPTER, metadata = "{\"crawlerName\":\"Host1\"}"
            ),
            TaskEntity(
                id = "h1_ch_2", batchId = "b1", name = "Ch 2", url = "u2", novelUrl = "novel1",
                priority = 0, type = JobType.CHAPTER, metadata = "{\"crawlerName\":\"Host1\"}"
            )
        )
        val host2Jobs = listOf(
            TaskEntity(
                id = "h2_ch_1", batchId = "b2", name = "Ch 1", url = "u3", novelUrl = "novel2",
                priority = 0, type = JobType.CHAPTER, metadata = "{\"crawlerName\":\"Host2\"}"
            )
        )
        queue.pushAll(host1Jobs + host2Jobs)

        // When Host1 is saturated, pop should skip Host1 and return Host2
        val saturated = setOf("Host1")
        val popped = queue.pop(saturated)
        assertEquals("h2_ch_1", popped?.id)

        // Host2 is now exhausted. Calling pop with Host1 saturated returns null
        assertNull(queue.pop(saturated))

        // When Host1 is no longer saturated, all its tasks are still there in order
        assertEquals("h1_ch_1", queue.pop()?.id)
        assertEquals("h1_ch_2", queue.pop()?.id)
        assertNull(queue.pop())
    }

    @Test
    fun testPriorityInversionAvoidanceWithSaturatedCrawler() {
        val queue = ReadyQueue()
        val highPriorityHost1 = listOf(
            TaskEntity(
                id = "h1_high_1", batchId = "b1", name = "High 1", url = "u1", novelUrl = "novel1",
                priority = 10, type = JobType.CHAPTER, metadata = "{\"crawlerName\":\"Host1\"}"
            )
        )
        val lowPriorityHost2 = listOf(
            TaskEntity(
                id = "h2_low_1", batchId = "b2", name = "Low 1", url = "u2", novelUrl = "novel2",
                priority = 0, type = JobType.CHAPTER, metadata = "{\"crawlerName\":\"Host2\"}"
            )
        )
        queue.pushAll(highPriorityHost1 + lowPriorityHost2)

        // When Host1 is saturated, fall through priority 10 down to priority 0 to keep Host2 busy
        val popped = queue.pop(saturatedCrawlers = setOf("Host1"))
        assertEquals("h2_low_1", popped?.id)

        // High priority Host1 task was preserved and not discarded
        assertEquals("h1_high_1", queue.pop()?.id)
        assertNull(queue.pop())
    }
}
