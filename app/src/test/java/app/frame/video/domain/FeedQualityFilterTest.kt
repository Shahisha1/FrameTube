package app.frame.video.domain

import app.frame.video.domain.provider.MetadataAvailability
import app.frame.video.domain.provider.VideoMetadataResult
import org.junit.Assert.assertEquals
import org.junit.Test

class FeedQualityFilterTest {
    private fun video(id: String, title: String, channel: String, duration: Long?, shorts: Boolean = false) =
        VideoMetadataResult(id, title, channel, null, MetadataAvailability.AVAILABLE, duration, shorts)

    @Test
    fun hidesShortsMutedTermsAndCapsRepeatedChannels() {
        val videos = listOf(
            video("aaaaaaaaaaa", "Long guide", "one", 300),
            video("bbbbbbbbbbb", "Short clip", "one", 300, shorts = true),
            video("ccccccccccc", "Another guide", "one", 180),
            video("ddddddddddd", "Sponsored haul", "two", 240),
            video("eeeeeeeeeee", "Travel film", "two", 600)
        )
        val result = filterRecommendations(videos, true, "sponsored, haul", 1)
        assertEquals(listOf("aaaaaaaaaaa", "eeeeeeeeeee"), result.map { it.videoId })
    }

    @Test
    fun keepsUnknownDurationUnlessExtractorMarksItShort() {
        val unknown = video("aaaaaaaaaaa", "Unknown length", "channel", null)
        val markedShort = video("bbbbbbbbbbb", "Vertical upload", "channel2", null, shorts = true)
        assertEquals(listOf(unknown), filterRecommendations(listOf(unknown, markedShort), true, "", 3))
    }
}
