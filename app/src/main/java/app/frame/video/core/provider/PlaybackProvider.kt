package app.frame.video.core.provider

interface PlaybackProvider {
    val providerId: String
    fun supports(videoId: String): Boolean
    /** Looks up a playable source. Never throws for expected failures; returns [PlaybackResolution.Failed]. */
    suspend fun resolve(videoId: String): PlaybackResolution
}

enum class PlaybackState {
    IDLE, LOADING, PLAYING, PAUSED, ENDED,
    UNAVAILABLE, RESTRICTED, EMBEDDING_RESTRICTED, NETWORK_ERROR, NOT_CONFIGURED, UNSUPPORTED
}

sealed interface PlaybackResolution {
    data class Ready(val uri: String, val mimeType: String?, val sourceLabel: String) : PlaybackResolution
    data class Failed(val state: PlaybackState, val message: String) : PlaybackResolution
}
