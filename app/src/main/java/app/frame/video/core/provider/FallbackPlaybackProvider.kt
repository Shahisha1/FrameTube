package app.frame.video.core.provider

/**
 * Tries providers in order. A later provider may recover from an extraction failure,
 * but an earlier provider's useful error is kept if every provider fails.
 */
class FallbackPlaybackProvider(
    private val providers: List<PlaybackProvider>
) : PlaybackProvider {
    override val providerId: String = "fallback"

    override fun supports(videoId: String): Boolean = providers.any { it.supports(videoId) }

    override suspend fun resolve(videoId: String): PlaybackResolution {
        var firstFailure: PlaybackResolution.Failed? = null
        for (provider in providers) {
            if (!provider.supports(videoId)) continue
            val result = try {
                provider.resolve(videoId)
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                PlaybackResolution.Failed(
                    PlaybackState.NETWORK_ERROR,
                    error.message?.takeIf { it.isNotBlank() } ?: "Playback provider failed unexpectedly."
                )
            }
            when (result) {
                is PlaybackResolution.Ready -> return result
                is PlaybackResolution.Failed -> if (firstFailure == null) firstFailure = result
            }
        }
        return firstFailure ?: PlaybackResolution.Failed(
            PlaybackState.UNSUPPORTED,
            "No playback provider supports this video."
        )
    }
}
