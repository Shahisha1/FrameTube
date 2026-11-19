package app.frame.video.data.remote.newpipe

import app.frame.video.domain.provider.ChannelPageData
import app.frame.video.domain.provider.ChannelProfile
import app.frame.video.domain.provider.ChannelProvider
import app.frame.video.domain.provider.MetadataAvailability
import app.frame.video.domain.provider.VideoMetadataResult
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
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

        // Both tabs share the extracted channel profile; their blocking page
        // requests run on NewPipeClient's IO dispatcher.
        val (videos, shorts) = coroutineScope {
            val videosTask = async { NewPipeClient.channelVideos(info) }
            val shortsTask = async { NewPipeClient.channelShorts(info) }
            videosTask.await() to shortsTask.await()
        }

        return ChannelPageData(
            profile = profile,
            videos = videos.mapNotNull { it.toVideo(info.id, forceShort = false) }.distinctBy { it.videoId },
            shorts = shorts.mapNotNull { it.toVideo(info.id, forceShort = true) }.distinctBy { it.videoId }
        )
    }

    private fun StreamInfoItem.toVideo(channelId: String, forceShort: Boolean): VideoMetadataResult? {
        val id = VIDEO_URL_ID.find(url)?.groupValues?.getOrNull(1)
            ?: VIDEO_ID.find(url)?.value
            ?: return null
        return VideoMetadataResult(
            videoId = id,
            title = name,
            channelId = channelId,
            thumbnailUrl = thumbnails.firstOrNull()?.url,
            availability = MetadataAvailability.AVAILABLE,
            durationSeconds = duration.takeIf { it > 0L },
            isShortFormContent = forceShort || isShortFormContent,
            channelName = uploaderName.takeIf { it.isNotBlank() },
            channelAvatarUrl = uploaderAvatars.firstOrNull()?.url,
            viewCount = viewCount.takeIf { it >= 0L },
            textualUploadDate = textualUploadDate?.takeIf { it.isNotBlank() },
            isUploaderVerified = isUploaderVerified
        )
    }

    private companion object {
        val VIDEO_ID = Regex("[A-Za-z0-9_-]{11}")
        val VIDEO_URL_ID = Regex("(?:v=|youtu\\.be/|shorts/)([A-Za-z0-9_-]{11})")
    }
}
