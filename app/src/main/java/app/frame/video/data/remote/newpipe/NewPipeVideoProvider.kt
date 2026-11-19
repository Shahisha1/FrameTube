package app.frame.video.data.remote.newpipe

import android.util.Log
import app.frame.video.domain.provider.ChannelSearchResult
import app.frame.video.domain.provider.MetadataAvailability
import app.frame.video.domain.provider.PlaylistSearchResult
import app.frame.video.domain.provider.VideoMetadataResult
import app.frame.video.domain.provider.VideoProvider
import app.frame.video.domain.provider.VideoSearchResult
import kotlinx.coroutines.CancellationException
import org.schabi.newpipe.extractor.InfoItem
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import org.schabi.newpipe.extractor.Page
import org.schabi.newpipe.extractor.linkhandler.SearchQueryHandler
import java.io.IOException
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/** Maps NewPipe Extractor output into app-owned domain models. */
class NewPipeVideoProvider : VideoProvider {
    override val providerId: String = "newpipe"
    private data class Cursor(val handler: SearchQueryHandler, val page: Page)
    private val cursors = ConcurrentHashMap<String, Cursor>()

    override suspend fun getVideoMetadata(videoId: String): VideoMetadataResult {
        if (!VIDEO_ID.matches(videoId)) return VideoMetadataResult(videoId, null, null, null, MetadataAvailability.NOT_FOUND)
        return try {
            val info = NewPipeClient.stream(videoId)
            VideoMetadataResult(info.id, info.name, null, info.thumbnails.firstOrNull()?.url, MetadataAvailability.AVAILABLE)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            Log.e(TAG, "Metadata extraction failed for $videoId", error)
            VideoMetadataResult(videoId, null, null, null, if (error is IOException) MetadataAvailability.NETWORK_ERROR else MetadataAvailability.UNKNOWN)
        }
    }

    override suspend fun searchVideos(query: String, pageToken: String?, contentFilter: String?): VideoSearchResult {
        val term = query.trim()
        if (term.isBlank()) return VideoSearchResult(emptyList(), null)
        return try {
            val page = if (pageToken == null) {
                NewPipeClient.search(term, contentFilter)
            } else {
                val cursor = cursors[pageToken]
                    ?: return VideoSearchResult(emptyList(), null, "Search page expired. Run the search again.")
                NewPipeClient.searchMore(cursor.handler, cursor.page).also { cursors.remove(pageToken) }
            }
            val videos = mutableListOf<VideoMetadataResult>()
            val channels = mutableListOf<ChannelSearchResult>()
            val playlists = mutableListOf<PlaylistSearchResult>()
            page.items.forEach { item -> mapItem(item, videos, channels, playlists) }
            val next = page.nextPage?.let {
                if (cursors.size > 32) cursors.clear()
                UUID.randomUUID().toString().also { token -> cursors[token] = Cursor(page.queryHandler, it) }
            }
            VideoSearchResult(
                items = videos.distinctBy { it.videoId },
                nextPageToken = next,
                channels = channels.distinctBy { it.url },
                playlists = playlists.distinctBy { it.url }
            )
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            Log.e(TAG, "YouTube search failed for '$term'", error)
            val message = if (error is IOException) "Could not connect to YouTube. Check your connection and try again."
                else "NewPipe could not load YouTube results. YouTube may have changed or temporarily restricted this request."
            VideoSearchResult(emptyList(), null, message)
        }
    }

    override suspend fun suggestions(query: String): List<String> = try {
        if (query.trim().length < 2) emptyList() else NewPipeClient.suggestions(query.trim())
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: Exception) {
        Log.w(TAG, "Search suggestions unavailable", error)
        emptyList()
    }

    private fun mapItem(
        item: InfoItem,
        videos: MutableList<VideoMetadataResult>,
        channels: MutableList<ChannelSearchResult>,
        playlists: MutableList<PlaylistSearchResult>
    ) {
        val thumbnail = item.thumbnails.firstOrNull()?.url
        when (item.infoType.name) {
            "STREAM" -> {
                val id = VIDEO_URL_ID.find(item.url)?.groupValues?.get(1) ?: VIDEO_ID.find(item.url)?.value
                if (id != null) {
                    val stream = item as? StreamInfoItem
                    videos += VideoMetadataResult(
                        videoId = id,
                        title = item.name,
                        channelId = null,
                        thumbnailUrl = thumbnail,
                        availability = MetadataAvailability.AVAILABLE,
                        durationSeconds = stream?.duration?.takeIf { it > 0L },
                        isShortFormContent = stream?.isShortFormContent == true
                    )
                }
            }
            "CHANNEL" -> channels += ChannelSearchResult(item.url, item.name, thumbnail)
            "PLAYLIST" -> playlists += PlaylistSearchResult(item.url, item.name, thumbnail)
        }
    }

    private companion object {
        const val TAG = "FrameNewPipe"
        val VIDEO_ID = Regex("[A-Za-z0-9_-]{11}")
        val VIDEO_URL_ID = Regex("(?:v=|youtu\\.be/|shorts/)([A-Za-z0-9_-]{11})")
    }
}
