package app.frame.video.data.remote.newpipe

import app.frame.video.domain.provider.ChannelPageData
import app.frame.video.domain.provider.ChannelProfile
import app.frame.video.domain.provider.ChannelProvider
import app.frame.video.domain.provider.MetadataAvailability
import app.frame.video.domain.provider.VideoMetadataResult
import org.schabi.newpipe.extractor.stream.StreamInfoItem

class NewPipeChannelProvider : ChannelProvider {
    override val providerId: String = "newpipe"

    override suspend fun getChannelPage(url: String): ChannelPageData {
        val info = NewPipeClient.channel(url)
        val profile = ChannelProfile(
            id = info.id,
            name = info.name,
            url = info.url,
            description = info.description?.takeIf(String::isNotBlank),
            avatarUrl = info.avatars.firstOrNull()?.url,
            bannerUrl = info.banners.firstOrNull()?.url,
            subscriberCount = info.subscriberCount.takeIf { it >= 0L },
            verified = info.isVerified
        )
        val videos = NewPipeClient.channelVideos(info).mapNotNull { item ->
            val id = VIDEO_URL_ID.find(item.url)?.groupValues?.getOrNull(1)
                ?: VIDEO_ID.find(item.url)?.value
                ?: return@mapNotNull null
            VideoMetadataResult(
                videoId = id,
                title = item.name,
                channelId = info.id,
                thumbnailUrl = item.thumbnails.firstOrNull()?.url,
                availability = MetadataAvailability.AVAILABLE
            )
        }.distinctBy { it.videoId }
        return ChannelPageData(profile, videos)
    }

    private companion object {
        val VIDEO_ID = Regex("[A-Za-z0-9_-]{11}")
        val VIDEO_URL_ID = Regex("(?:v=|youtu\\.be/|shorts/)([A-Za-z0-9_-]{11})")
    }
}
