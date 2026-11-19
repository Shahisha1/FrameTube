package app.frame.video.data.remote.newpipe

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.Page
import org.schabi.newpipe.extractor.StreamingService
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.InfoItem
import org.schabi.newpipe.extractor.linkhandler.SearchQueryHandler
import org.schabi.newpipe.extractor.localization.Localization
import org.schabi.newpipe.extractor.search.SearchInfo
import org.schabi.newpipe.extractor.stream.StreamInfo
import org.schabi.newpipe.extractor.channel.ChannelInfo
import org.schabi.newpipe.extractor.channel.tabs.ChannelTabInfo
import org.schabi.newpipe.extractor.stream.StreamInfoItem

internal data class ExtractedSearchPage(
    val items: List<InfoItem>,
    val nextPage: Page?,
    val queryHandler: SearchQueryHandler
)

/** Owns process-wide NewPipe setup and isolates blocking extraction on Dispatchers.IO. */
internal object NewPipeClient {
    private val lock = Any()
    @Volatile private var initialized = false

    private fun ensureInitialized() {
        if (initialized) return
        synchronized(lock) {
            if (!initialized) {
                NewPipe.init(FrameNewPipeDownloader(), Localization("en", "US"))
                initialized = true
            }
        }
    }

    suspend fun stream(videoId: String): StreamInfo = withContext(Dispatchers.IO) {
        ensureInitialized()
        StreamInfo.getInfo(ServiceList.YouTube, "https://www.youtube.com/watch?v=$videoId")
    }

    suspend fun search(query: String, contentFilter: String? = null): ExtractedSearchPage = withContext(Dispatchers.IO) {
        ensureInitialized()
        val service = ServiceList.YouTube
        val extractor = if (contentFilter.isNullOrBlank()) service.getSearchExtractor(query)
            else service.getSearchExtractor(query, listOf(contentFilter), "")
        val info = SearchInfo.getInfo(extractor)
        ExtractedSearchPage(info.relatedItems.orEmpty(), info.nextPage, extractor.linkHandler)
    }

    suspend fun searchMore(handler: SearchQueryHandler, page: Page): ExtractedSearchPage = withContext(Dispatchers.IO) {
        ensureInitialized()
        val result = SearchInfo.getMoreItems(ServiceList.YouTube, handler, page)
        ExtractedSearchPage(result.items.orEmpty(), result.nextPage, handler)
    }

    suspend fun suggestions(query: String): List<String> = withContext(Dispatchers.IO) {
        ensureInitialized()
        ServiceList.YouTube.suggestionExtractor.suggestionList(query).orEmpty()
            .map(String::trim).filter(String::isNotBlank).distinct().take(8)
    }

    suspend fun channel(url: String): ChannelInfo = withContext(Dispatchers.IO) {
        ensureInitialized()
        ChannelInfo.getInfo(ServiceList.YouTube, url)
    }

    suspend fun channelVideos(info: ChannelInfo): List<StreamInfoItem> = withContext(Dispatchers.IO) {
        ensureInitialized()
        val videoTab = info.tabs.firstOrNull { tab ->
            tab.contentFilters.any { it.equals("videos", ignoreCase = true) }
        } ?: info.tabs.firstOrNull()
        if (videoTab == null) emptyList()
        else ChannelTabInfo.getInfo(ServiceList.YouTube, videoTab).relatedItems.orEmpty()
            .filterIsInstance<StreamInfoItem>()
    }
}
