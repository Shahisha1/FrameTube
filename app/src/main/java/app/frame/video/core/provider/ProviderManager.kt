package app.frame.video.core.provider

/** Selects only providers explicitly enabled for the requested capability. */
class ProviderManager(
    private val metadataProviders: List<VideoProvider>,
    private val playbackProviders: List<PlaybackProvider>
) {
    fun metadataProvider(providerId: String): VideoProvider? = metadataProviders.firstOrNull { it.providerId == providerId }
    fun playbackProvider(videoId: String): PlaybackProvider? = playbackProviders.firstOrNull { it.supports(videoId) }
}
