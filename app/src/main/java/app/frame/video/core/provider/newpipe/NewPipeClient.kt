package app.frame.video.core.provider.newpipe

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.localization.Localization
import org.schabi.newpipe.extractor.search.SearchInfo
import org.schabi.newpipe.extractor.stream.StreamInfo
import java.io.IOException

/**
 * Owns NewPipe's process-wide setup and keeps blocking extraction off the main thread.
 */
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
        val url = "https://www.youtube.com/watch?v=$videoId"
        StreamInfo.getInfo(ServiceList.YouTube, url)
    }

    suspend fun search(query: String): SearchInfo = withContext(Dispatchers.IO) {
        ensureInitialized()
        val extractor = ServiceList.YouTube.getSearchExtractor(query)
        SearchInfo.getInfo(extractor)
    }
}
