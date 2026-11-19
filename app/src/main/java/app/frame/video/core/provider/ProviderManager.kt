package app.frame.video.core.provider

import app.frame.video.domain.provider.VideoProvider
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Selects providers and briefly caches resolved playback URLs.
 *
 * Shorts can pre-resolve the next item's source so the active page does not
 * have to repeat extraction after a swipe. Only successful resolutions are
 * cached; failures remain retryable.
 */
class ProviderManager(
    private val metadataProviders: List<VideoProvider>,
    private val playbackProviders: List<PlaybackProvider>
) {
    private data class CachedPlayback(
        val resolution: PlaybackResolution.Ready,
        val expiresAtMillis: Long
    )

    private val playbackCache = ConcurrentHashMap<String, CachedPlayback>()
    private val playbackLocks = ConcurrentHashMap<String, Mutex>()

    fun metadataProvider(providerId: String): VideoProvider? =
        metadataProviders.firstOrNull { it.providerId == providerId }

    fun playbackProvider(videoId: String): PlaybackProvider? =
        playbackProviders.firstOrNull { it.supports(videoId) }

    suspend fun resolvePlayback(videoId: String): PlaybackResolution {
        val lock = playbackLocks.computeIfAbsent(videoId) { Mutex() }
        return lock.withLock {
            val now = System.currentTimeMillis()
            playbackCache[videoId]?.let { cached ->
                if (cached.expiresAtMillis > now) return@withLock cached.resolution
                playbackCache.remove(videoId, cached)
            }

            val provider = playbackProvider(videoId)
                ?: return@withLock PlaybackResolution.Failed(
                    PlaybackState.UNSUPPORTED,
                    "No playback source is available for this video."
                )
            val result = provider.resolve(videoId)
            if (result is PlaybackResolution.Ready) {
                playbackCache[videoId] = CachedPlayback(result, System.currentTimeMillis() + CACHE_TTL_MILLIS)
            }
            trimCaches(System.currentTimeMillis())
            result
        }
    }

    /** Resolves and caches a source without creating a player or recording a view. */
    suspend fun prefetchPlayback(videoId: String): PlaybackResolution = resolvePlayback(videoId)

    private fun trimCaches(now: Long) {
        playbackCache.entries
            .filter { it.value.expiresAtMillis <= now }
            .forEach { playbackCache.remove(it.key, it.value) }

        if (playbackCache.size > MAX_CACHED_SOURCES) {
            playbackCache.keys.take(playbackCache.size - MAX_CACHED_SOURCES).forEach(playbackCache::remove)
        }
        if (playbackLocks.size > MAX_LOCKS) {
            playbackLocks.entries
                .filter { !it.value.isLocked && it.key !in playbackCache }
                .take(playbackLocks.size - MAX_LOCKS)
                .forEach { playbackLocks.remove(it.key, it.value) }
        }
    }

    private companion object {
        const val CACHE_TTL_MILLIS = 60_000L
        const val MAX_CACHED_SOURCES = 128
        const val MAX_LOCKS = 256
    }
}
