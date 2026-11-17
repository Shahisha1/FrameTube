package app.frame.video.data.remote.newpipe

import android.util.Log
import app.frame.video.domain.provider.MetadataAvailability
import app.frame.video.domain.provider.VideoMetadataResult
import app.frame.video.domain.provider.VideoProvider
import app.frame.video.domain.provider.VideoSearchResult
import kotlinx.coroutines.CancellationException
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import java.io.IOException

/** Maps NewPipe Extractor output into app-owned domain models. */
class NewPipeVideoProvider : VideoProvider {
    override val providerId: String = "newpipe"

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

    override suspend fun searchVideos(query: String, pageToken: String?): VideoSearchResult {
        val term = query.trim()
        if (term.isBlank()) return VideoSearchResult(emptyList(), null)
        return try {
            val info = NewPipeClient.search(term)
            val rawItems = info.relatedItems
            val streamItems = rawItems.filterIsInstance<StreamInfoItem>()
            val items = streamItems.mapNotNull { item ->
                val id = VIDEO_URL_ID.find(item.url)?.groupValues?.get(1)
                    ?: VIDEO_ID.find(item.url)?.value
                if (id == null) {
                    Log.w(TAG, "Ignoring search stream with an unrecognized URL: ${item.url}")
                    return@mapNotNull null
                }
                VideoMetadataResult(id, item.name, null, item.thumbnails.firstOrNull()?.url, MetadataAvailability.AVAILABLE)
            }.distinctBy { it.videoId }

            val extractorErrors = info.errors
            Log.d(
                TAG,
                "YouTube search '$term': rawItems=${rawItems.size}, streamItems=${streamItems.size}, " +
                    "mappedVideos=${items.size}, errors=${extractorErrors.size}, " +
                    "types=${rawItems.groupingBy { it.infoType }.eachCount()}, " +
                    "errorDetails=${extractorErrors.joinToString { it.message ?: it.javaClass.simpleName }}"
            )

            val emptyResultError = if (items.isEmpty() && extractorErrors.isNotEmpty()) {
                "YouTube returned no videos because NewPipe reported: " +
                    extractorErrors.take(2).joinToString { it.message ?: it.javaClass.simpleName }
            } else null
            VideoSearchResult(items, null, emptyResultError)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            Log.e(TAG, "YouTube search failed for '$term'", error)
            val message = if (error is IOException) "Could not connect to YouTube. Check your connection and try again."
                else "NewPipe could not load YouTube results. YouTube may have changed or temporarily restricted this request."
            VideoSearchResult(emptyList(), null, message)
        }
    }

    private companion object {
        const val TAG = "FrameNewPipe"
        val VIDEO_ID = Regex("[A-Za-z0-9_-]{11}")
        val VIDEO_URL_ID = Regex("(?:v=|youtu\\.be/|shorts/)([A-Za-z0-9_-]{11})")
    }
}
