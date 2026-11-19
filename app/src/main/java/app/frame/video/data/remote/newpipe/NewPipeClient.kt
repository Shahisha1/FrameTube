package app.frame.video.data.remote.newpipe

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.localization.Localization
import org.schabi.newpipe.extractor.search.SearchInfo
import org.schabi.newpipe.extractor.stream.StreamInfo
import org.schabi.newpipe.extractor.channel.ChannelInfo
import org.schabi.newpipe.extractor.channel.tabs.ChannelTabInfo
import org.schabi.newpipe.extractor.stream.StreamInfoItem

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

    suspend fun search(query: String): SearchInfo = withContext(Dispatchers.IO) {
        ensureInitialized()
        SearchInfo.getInfo(ServiceList.YouTube.getSearchExtractor(query))
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
