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
    fun providerExceptionDoesNotBlockLaterProvider() = runBlocking {
        val fallback = FallbackPlaybackProvider(
            listOf(
                ThrowingProvider(),
                StubProvider(PlaybackResolution.Ready("https://cdn.example/video.mp4", "video/mp4", "recovered"))
            )
        )

        val result = fallback.resolve("abcdefghijk")

        assertEquals("recovered", (result as PlaybackResolution.Ready).sourceLabel)
    }

    @Test
    fun cancellationIsNotTreatedAsAProviderFailure() {
        try {
            runBlocking {
                FallbackPlaybackProvider(listOf(CancelledProvider())).resolve("abcdefghijk")
            }
            throw AssertionError("Expected cancellation to propagate")
        } catch (_: kotlinx.coroutines.CancellationException) {
            // Expected: caller cancellation must not be swallowed.
        }
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

    private class ThrowingProvider : PlaybackProvider {
        override val providerId = "throwing"
        override fun supports(videoId: String) = true
        override suspend fun resolve(videoId: String): PlaybackResolution = error("unexpected provider error")
    }

    private class CancelledProvider : PlaybackProvider {
        override val providerId = "cancelled"
        override fun supports(videoId: String) = true
        override suspend fun resolve(videoId: String): PlaybackResolution =
            throw kotlinx.coroutines.CancellationException("caller cancelled")
    }
}
