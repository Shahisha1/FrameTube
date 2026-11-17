package app.frame.video.core.provider.newpipe

import app.frame.video.core.provider.PlaybackProvider
import app.frame.video.core.provider.PlaybackResolution
import app.frame.video.core.provider.PlaybackState
import kotlinx.coroutines.CancellationException
import org.schabi.newpipe.extractor.stream.StreamInfo
import java.io.IOException

/** Direct YouTube extraction through the official NewPipe Extractor library. */
class NewPipePlaybackProvider : PlaybackProvider {
    override val providerId: String = "newpipe"

    override fun supports(videoId: String): Boolean = VIDEO_ID.matches(videoId)

    override suspend fun resolve(videoId: String): PlaybackResolution {
        if (!supports(videoId)) {
            return PlaybackResolution.Failed(PlaybackState.UNSUPPORTED, "This is not a valid YouTube video ID.")
        }

        return try {
            val info = NewPipeClient.stream(videoId)
            choosePlayable(info)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            PlaybackResolution.Failed(classify(error), safeMessage(error))
        }
    }

    private fun choosePlayable(info: StreamInfo): PlaybackResolution {
        // Prefer adaptive manifests. They carry both audio and video tracks.
        info.getDashMpdUrl()?.takeIf(String::isNotBlank)?.let {
            return PlaybackResolution.Ready(it, "application/dash+xml", "YouTube · NewPipe · DASH")
        }
        info.getHlsUrl()?.takeIf(String::isNotBlank)?.let {
            return PlaybackResolution.Ready(it, "application/x-mpegURL", "YouTube · NewPipe · HLS")
        }

        // Only choose progressive streams with both audio and video.
        val progressive = info.getVideoStreams()
            .filterNot { it.isVideoOnly() }
            .maxByOrNull { it.getHeight() }
        if (progressive != null) {
            return PlaybackResolution.Ready(
                progressive.getUrl(),
                progressive.getFormat()?.mimeType,
                "YouTube · NewPipe · ${progressive.getResolution()}"
            )
        }

        return PlaybackResolution.Failed(
            PlaybackState.UNSUPPORTED,
            "YouTube did not provide a stream Frame can play. Try again later."
        )
    }

    private fun classify(error: Exception): PlaybackState {
        val message = error.message.orEmpty().lowercase()
        return when {
            error is IOException -> PlaybackState.NETWORK_ERROR
            listOf("private", "age-restricted", "age restricted", "sign in", "members-only", "members only", "not available in your country").any { it in message } ->
                PlaybackState.RESTRICTED
            listOf("unavailable", "not found", "removed", "deleted", "does not exist").any { it in message } ->
                PlaybackState.UNAVAILABLE
            else -> PlaybackState.UNAVAILABLE
        }
    }

    private fun safeMessage(error: Exception): String {
        val message = error.message?.takeIf { it.isNotBlank() }
        return when {
            message == null -> "NewPipe could not extract this video. YouTube may have changed or restricted it."
            classify(error) == PlaybackState.NETWORK_ERROR -> "Could not reach YouTube. Check your connection and try again."
            classify(error) == PlaybackState.RESTRICTED -> "This video is private or restricted. It stays in your library."
            classify(error) == PlaybackState.UNAVAILABLE -> "This video could not be extracted. It may be removed or temporarily unavailable."
            else -> "NewPipe could not extract this video. Try again later."
        }
    }

    private companion object {
        val VIDEO_ID = Regex("[A-Za-z0-9_-]{11}")
    }
}
