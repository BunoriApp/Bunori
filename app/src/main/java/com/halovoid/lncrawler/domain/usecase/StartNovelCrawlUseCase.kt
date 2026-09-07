package com.halovoid.lncrawler.domain.usecase

import android.content.Context
import com.halovoid.lncrawler.data.factory.RequestFactory
import com.halovoid.lncrawler.data.repository.RequestRepository
import com.halovoid.lncrawler.data.scheduler.services.SchedulerService

/**
 * Single business action for queuing a novel crawl/metadata request.
 */
class StartNovelCrawlUseCase(
    private val requestRepository: RequestRepository,
    private val requestFactory: RequestFactory = RequestFactory()
) {
    suspend operator fun invoke(context: Context, crawlerName: String, url: String, title: String) {
        val request = requestFactory.metadataFromUrl(crawlerName, url, title)
        requestRepository.insertRequests(listOf(request))
        SchedulerService.startService(context)
    }
}
