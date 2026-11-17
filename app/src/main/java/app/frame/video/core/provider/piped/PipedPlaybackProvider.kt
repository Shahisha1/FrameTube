package app.frame.video.core.provider.piped

import app.frame.video.core.datastore.PipedConfig
import app.frame.video.core.provider.PlaybackProvider
import app.frame.video.core.provider.PlaybackResolution
import app.frame.video.core.provider.PlaybackState

/** UNOFFICIAL playback via a user-chosen Piped instance. Off unless the user enabled it in Settings. */
class PipedPlaybackProvider(
    private val api: PipedApi,
    private val config: suspend () -> PipedConfig
) : PlaybackProvider {
    override val providerId = "piped"
    override fun supports(videoId: String) = videoId.isNotBlank()

    override suspend fun resolve(videoId: String): PlaybackResolution {
        val cfg = config()
        val base = PipedApi.normalizeInstanceUrl(cfg.instanceUrl)
        if (!cfg.enabled || base == null) {
            return PlaybackResolution.Failed(PlaybackState.NOT_CONFIGURED, "Unofficial playback is off. Turn it on in Settings and add a Piped instance address.")
        }
        return when (val r = api.streams(base, videoId)) {
            is PipedResult.Success -> {
                val playable = PipedParser.choose(r.streams)
                    ?: return PlaybackResolution.Failed(PlaybackState.UNSUPPORTED, "The instance did not return a playable stream for this video.")
                PlaybackResolution.Ready(playable.url, playable.mimeType, "Unofficial · via $base")
            }
            is PipedResult.Failure -> when (r.kind) {
                PipedFailureKind.NOT_FOUND -> PlaybackResolution.Failed(PlaybackState.UNAVAILABLE, "This video is unavailable. It may be deleted. It stays in your library.")
                PipedFailureKind.RESTRICTED -> PlaybackResolution.Failed(PlaybackState.RESTRICTED, "This video is private or restricted. It stays in your library.")
                PipedFailureKind.NETWORK -> PlaybackResolution.Failed(PlaybackState.NETWORK_ERROR, "Could not reach the Piped instance. Check your connection and the instance address.")
                PipedFailureKind.SERVER -> PlaybackResolution.Failed(PlaybackState.UNAVAILABLE, "The Piped instance returned an error. Try again or use another instance.")
                PipedFailureKind.BAD_RESPONSE -> PlaybackResolution.Failed(PlaybackState.UNSUPPORTED, "The instance sent a reply Frame could not read.")
            }
        }
    }
}
