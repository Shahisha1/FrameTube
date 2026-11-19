package app.frame.video.domain

import app.frame.video.domain.provider.MetadataAvailability
import app.frame.video.domain.provider.VideoMetadataResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeRecommendationEngineTest {
    @Test
    fun derivesTopicQueriesFromRecentTitlesAndAddsDiscoveryFallbacks() {
        val gamingQueries = HomeRecommendationEngine.buildQueries(
            listOf("How to build a Gaming PC for beginners")
        )
        val keyboardQueries = HomeRecommendationEngine.buildQueries(
            listOf("A quiet mechanical keyboard build")
        )

        assertTrue(gamingQueries.any { it.contains("gaming pc") })
        assertTrue(keyboardQueries.any { it.contains("mechanical keyboard") })
        assertEquals("popular videos", gamingQueries[gamingQueries.size - 2])
        assertEquals("trending videos", gamingQueries.last())
    }

    @Test
    fun newUsersReceiveFallbackQueriesWithoutHistory() {
        assertEquals(
            listOf("popular videos", "trending videos"),
            HomeRecommendationEngine.buildQueries(emptyList())
        )
    }

    @Test
    fun interleavesBatchesAndRemovesDuplicateVideos() {
        fun video(id: String) = VideoMetadataResult(
            videoId = id,
            title = id,
            channelId = null,
            thumbnailUrl = null,
            availability = MetadataAvailability.AVAILABLE
        )

        val result = HomeRecommendationEngine.interleave(
            listOf(
                listOf(video("a"), video("b"), video("c")),
                listOf(video("b"), video("d"), video("e")),
                listOf(video("f"), video("g"))
            ),
            limit = 5
        )

        assertEquals(listOf("a", "b", "f", "d", "g"), result.map { it.videoId })
    }

    @Test
    fun interleaveHonoursLimitAndEmptyBatches() {
        assertTrue(HomeRecommendationEngine.interleave(emptyList()).isEmpty())
        assertEquals(
            listOf("a"),
            HomeRecommendationEngine.interleave(
                listOf(listOf(VideoMetadataResult("a", "A", null, null, MetadataAvailability.AVAILABLE))),
                limit = 1
            ).map { it.videoId }
        )
    }
}
