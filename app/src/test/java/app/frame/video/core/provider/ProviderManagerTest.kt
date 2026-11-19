package app.frame.video.core.provider

import app.frame.video.domain.provider.VideoProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlinx.coroutines.runBlocking

class ProviderManagerTest {
    @Test
    fun prefetchIsReusedWhenTheVideoBecomesActive() = runBlocking {
        var calls = 0
        val provider = CountingProvider {
            calls += 1
            PlaybackResolution.Ready("https://example.test/stream", "video/mp4", "test")
        }
        val manager = ProviderManager(emptyList<VideoProvider>(), listOf(provider))

        val prefetched = manager.prefetchPlayback("abcdefghijk")
        val active = manager.resolvePlayback("abcdefghijk")

        assertEquals(prefetched, active)
        assertEquals(1, calls)
    }

    @Test
    fun failedResolutionIsNotCachedAndCanBeRetried() = runBlocking {
        var calls = 0
        val provider = CountingProvider {
            calls += 1
            if (calls == 1) {
                PlaybackResolution.Failed(PlaybackState.NETWORK_ERROR, "offline")
            } else {
                PlaybackResolution.Ready("https://example.test/stream", "video/mp4", "test")
            }
        }
        val manager = ProviderManager(emptyList<VideoProvider>(), listOf(provider))

        assertTrue(manager.resolvePlayback("abcdefghijk") is PlaybackResolution.Failed)
        assertTrue(manager.resolvePlayback("abcdefghijk") is PlaybackResolution.Ready)
        assertEquals(2, calls)
    }

    private class CountingProvider(
        private val result: suspend () -> PlaybackResolution
    ) : PlaybackProvider {
        override val providerId: String = "test"
        override fun supports(videoId: String): Boolean = true
        override suspend fun resolve(videoId: String): PlaybackResolution = result()
    }
}
