package app.frame.video.domain.provider

data class ChannelProfile(
    val id: String,
    val name: String,
    val url: String,
    val description: String?,
    val avatarUrl: String?,
    val bannerUrl: String?,
    val subscriberCount: Long?,
    val verified: Boolean
)

data class ChannelPageData(
    val profile: ChannelProfile,
    val videos: List<VideoMetadataResult>,
    val shorts: List<VideoMetadataResult> = emptyList()
)

/** Channel extraction contract; extractor types stay inside the data layer. */
interface ChannelProvider {
    val providerId: String
    suspend fun getChannelPage(url: String): ChannelPageData
}
