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
            when (val result = provider.resolve(videoId)) {
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
