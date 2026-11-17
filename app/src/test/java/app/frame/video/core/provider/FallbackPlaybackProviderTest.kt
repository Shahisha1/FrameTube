package app.frame.video.core.provider

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class FallbackPlaybackProviderTest {
    @Test
    fun laterProviderCanRecover() = runBlocking {
        val fallback = FallbackPlaybackProvider(
            listOf(
                StubProvider(PlaybackResolution.Failed(PlaybackState.NETWORK_ERROR, "primary failed")),
                StubProvider(PlaybackResolution.Ready("https://cdn.example/video.m3u8", "application/x-mpegURL", "fallback"))
            )
        )

        val result = fallback.resolve("abcdefghijk")

        assertEquals("fallback", (result as PlaybackResolution.Ready).sourceLabel)
    }

    @Test
    fun firstUsefulFailureIsKeptWhenAllProvidersFail() = runBlocking {
        val fallback = FallbackPlaybackProvider(
            listOf(
                StubProvider(PlaybackResolution.Failed(PlaybackState.RESTRICTED, "restricted")),
                StubProvider(PlaybackResolution.Failed(PlaybackState.NETWORK_ERROR, "offline"))
            )
        )

        val result = fallback.resolve("abcdefghijk")

        assertEquals(PlaybackState.RESTRICTED, (result as PlaybackResolution.Failed).state)
    }

    private class StubProvider(
        private val result: PlaybackResolution
    ) : PlaybackProvider {
        override val providerId = "stub"
        override fun supports(videoId: String) = true
        override suspend fun resolve(videoId: String) = result
    }
}
