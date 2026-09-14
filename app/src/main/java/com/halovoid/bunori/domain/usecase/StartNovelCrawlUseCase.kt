package com.halovoid.bunori.domain.usecase

import android.content.Context
import com.halovoid.bunori.data.factory.RequestFactory
import com.halovoid.bunori.data.repository.BatchRepository
import com.halovoid.bunori.data.scheduler.services.SchedulerService

/**
 * Single business action for queuing a novel crawl/metadata request.
 */
class StartNovelCrawlUseCase(
    private val batchRepository: BatchRepository,
    private val requestFactory: RequestFactory = RequestFactory()
) {
    suspend operator fun invoke(context: Context, crawlerName: String, url: String, title: String) {
        val request = requestFactory.metadataFromUrl(crawlerName, url, title)
        batchRepository.insertRequests(listOf(request))
        SchedulerService.startService(context)
    }
}
