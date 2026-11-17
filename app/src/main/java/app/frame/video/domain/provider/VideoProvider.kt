package app.frame.video.domain.provider

/** Domain contract. Implementations live in the data/remote layer; UI never depends on extractor types. */
interface VideoProvider {
    val providerId: String
    suspend fun getVideoMetadata(videoId: String): VideoMetadataResult
    suspend fun searchVideos(query: String, pageToken: String? = null): VideoSearchResult
}
data class VideoMetadataResult(val videoId: String, val title: String?, val channelId: String?, val thumbnailUrl: String?, val availability: MetadataAvailability)
data class VideoSearchResult(val items: List<VideoMetadataResult>, val nextPageToken: String?, val errorMessage: String? = null)
enum class MetadataAvailability { AVAILABLE, NOT_FOUND, PRIVATE_OR_RESTRICTED, NETWORK_ERROR, UNKNOWN }
