package app.frame.video.core.provider.newpipe

import app.frame.video.core.provider.MetadataAvailability
import app.frame.video.core.provider.VideoMetadataResult
import app.frame.video.core.provider.VideoProvider
import app.frame.video.core.provider.VideoSearchResult
import kotlinx.coroutines.CancellationException
import org.schabi.newpipe.extractor.stream.StreamInfoItem

/** YouTube search and metadata provider. Results are mapped to Frame-owned models. */
class NewPipeVideoProvider : VideoProvider {
    override val providerId: String = "newpipe"

    override suspend fun getVideoMetadata(videoId: String): VideoMetadataResult {
        if (!VIDEO_ID.matches(videoId)) {
            return VideoMetadataResult(videoId, null, null, null, MetadataAvailability.NOT_FOUND)
        }
        return try {
            val info = NewPipeClient.stream(videoId)
            VideoMetadataResult(
                videoId = info.id,
                title = info.name,
                channelId = null,
                thumbnailUrl = info.thumbnails.firstOrNull()?.url,
                availability = MetadataAvailability.AVAILABLE
            )
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            VideoMetadataResult(
                videoId, null, null, null,
                if (error is java.io.IOException) MetadataAvailability.NETWORK_ERROR else MetadataAvailability.UNKNOWN
            )
        }
    }

    override suspend fun searchVideos(query: String, pageToken: String?): VideoSearchResult {
        if (query.isBlank()) return VideoSearchResult(emptyList(), null)
        return try {
            val info = NewPipeClient.search(query)
            val items = info.relatedItems
                .filterIsInstance<StreamInfoItem>()
                .mapNotNull { item ->
                    val id = VIDEO_ID.find(item.url)?.value ?: return@mapNotNull null
                    VideoMetadataResult(
                        videoId = id,
                        title = item.name,
                        channelId = null,
                        thumbnailUrl = item.thumbnails.firstOrNull()?.url,
                        availability = MetadataAvailability.AVAILABLE
                    )
                }
            VideoSearchResult(items, null)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            VideoSearchResult(emptyList(), null)
        }
    }

    private companion object {
        val VIDEO_ID = Regex("[A-Za-z0-9_-]{11}")
    }
}
