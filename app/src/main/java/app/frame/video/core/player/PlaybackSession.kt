package app.frame.video.core.player

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import app.frame.video.core.provider.PlaybackResolution
import app.frame.video.core.provider.ProviderManager
import app.frame.video.domain.playback.PlaybackQueue
import app.frame.video.domain.playback.PlaybackQueueEntry
import app.frame.video.domain.playback.PlaybackQueueMode
import app.frame.video.domain.playback.PlaybackQueueSnapshot
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class PlaybackSessionState(
    val currentVideoId: String? = null,
    val currentTitle: String? = null,
    val sourceLabel: String? = null,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val queue: PlaybackQueueSnapshot = PlaybackQueueSnapshot(),
    val error: String? = null
)

/**
 * App-scoped playback owner shared by the normal player and Shorts.
 * Screens attach surfaces to this player; they must never release it.
 */
class PlaybackSession(
    context: Context,
    private val providers: ProviderManager
) {
    val player: ExoPlayer = ExoPlayer.Builder(context.applicationContext).build()

    private val queue = PlaybackQueue()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val _state = MutableStateFlow(PlaybackSessionState())
    val state: StateFlow<PlaybackSessionState> = _state.asStateFlow()
    private var resolutionJob: Job? = null
    private var requestGeneration = 0

    private val listener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) = publish()
        override fun onPlaybackStateChanged(playbackState: Int) {
            publish()
            if (playbackState == Player.STATE_ENDED) advanceAfterCompletion()
        }
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) = publish()
        override fun onPlayerError(error: PlaybackException) {
            _state.value = _state.value.copy(
                isPlaying = false,
                isBuffering = false,
                error = "Playback stopped (${error.errorCodeName}). Check your connection and try again."
            )
        }
    }

    init {
        player.addListener(listener)
        scope.launch {
            while (isActive) {
                publish()
                delay(400)
            }
        }
    }

    fun setQueue(
        entries: List<PlaybackQueueEntry>,
        mode: PlaybackQueueMode,
        startIndex: Int = 0
    ) {
        queue.replace(entries, startIndex, mode)
        publish()
    }

    fun playSingle(entry: PlaybackQueueEntry, source: PlaybackResolution.Ready, positionMs: Long = 0L) {
        invalidatePendingResolution()
        queue.replace(listOf(entry), 0, PlaybackQueueMode.SINGLE)
        applyResolved(entry, source, positionMs)
    }

    fun playQueued(entry: PlaybackQueueEntry, source: PlaybackResolution.Ready, positionMs: Long = 0L) {
        invalidatePendingResolution()
        val selected = if (queue.select(entry.videoId)) {
            queue.snapshot().current ?: entry
        } else {
            queue.replace(listOf(entry), 0, PlaybackQueueMode.SINGLE)
            entry
        }
        applyResolved(selected, source, positionMs)
    }

    fun appendToQueue(entries: List<PlaybackQueueEntry>) {
        queue.append(entries)
        publish()
    }

    fun removeFromQueue(index: Int) {
        val before = queue.snapshot()
        val updated = queue.removeAt(index)
        if (before.current?.videoId != updated.current?.videoId && updated.current != null) {
            requestQueueEntry(updated.current!!)
        } else publish()
    }

    fun moveQueueItem(fromIndex: Int, toIndex: Int) {
        queue.move(fromIndex, toIndex)
        publish()
    }

    fun playNext() {
        val next = queue.next() ?: return
        requestQueueEntry(next)
    }

    fun retryCurrent() {
        queue.snapshot().current?.let(::requestQueueEntry)
    }

    fun playPrevious() {
        if (player.currentPosition > 5_000L) {
            player.seekTo(0L)
            publish()
            return
        }
        val previous = queue.previous() ?: return
        requestQueueEntry(previous)
    }

    fun pause() {
        player.pause()
        publish()
    }

    fun resume() {
        player.play()
        publish()
    }

    fun togglePlayback() {
        if (player.isPlaying) pause() else resume()
    }

    fun seekTo(positionMs: Long) {
        player.seekTo(positionMs.coerceAtLeast(0L))
        publish()
    }

    fun clearQueue() {
        queue.clear()
        publish()
    }

    fun release() {
        invalidatePendingResolution()
        player.removeListener(listener)
        scope.cancel()
        player.release()
    }

    private fun advanceAfterCompletion() {
        val mode = queue.snapshot().mode
        if (mode == PlaybackQueueMode.SHORTS || mode == PlaybackQueueMode.USER || mode == PlaybackQueueMode.AUTOPLAY) {
            playNext()
        } else publish()
    }

    private fun requestQueueEntry(entry: PlaybackQueueEntry) {
        invalidatePendingResolution()
        val generation = requestGeneration
        resolutionJob = scope.launch {
            try {
                when (val result = providers.resolvePlayback(entry.videoId)) {
                    is PlaybackResolution.Ready -> {
                        if (generation == requestGeneration) applyResolved(entry, result, 0L)
                    }
                    is PlaybackResolution.Failed -> {
                        if (generation == requestGeneration) {
                            _state.value = _state.value.copy(
                                currentVideoId = entry.videoId,
                                currentTitle = entry.title,
                                isPlaying = false,
                                isBuffering = false,
                                error = result.message,
                                queue = queue.snapshot()
                            )
                        }
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                if (generation == requestGeneration) {
                    _state.value = _state.value.copy(
                        currentVideoId = entry.videoId,
                        currentTitle = entry.title,
                        isPlaying = false,
                        isBuffering = false,
                        error = error.message ?: "Could not resolve the next video.",
                        queue = queue.snapshot()
                    )
                }
            }
        }
    }

    private fun invalidatePendingResolution() {
        requestGeneration += 1
        resolutionJob?.cancel()
        resolutionJob = null
    }

    private fun applyResolved(
        entry: PlaybackQueueEntry,
        source: PlaybackResolution.Ready,
        positionMs: Long
    ) {
        val current = player.currentMediaItem
        val sameSource = current?.mediaId == entry.videoId &&
            current.localConfiguration?.uri?.toString() == source.uri &&
            player.playbackState != Player.STATE_IDLE

        if (sameSource) {
            if (positionMs > 0L && kotlin.math.abs(player.currentPosition - positionMs) > 1_500L) {
                player.seekTo(positionMs)
            }
            player.play()
        } else {
            val item = MediaItem.Builder()
                .setMediaId(entry.videoId)
                .setUri(source.uri)
                .apply { source.mimeType?.let { setMimeType(it) } }
                .build()
            player.setMediaItem(item, positionMs.coerceAtLeast(0L))
            player.prepare()
            player.play()
        }
        _state.value = _state.value.copy(
            currentVideoId = entry.videoId,
            currentTitle = entry.title,
            sourceLabel = source.sourceLabel,
            isPlaying = player.isPlaying,
            isBuffering = player.playbackState == Player.STATE_BUFFERING,
            positionMs = player.currentPosition,
            durationMs = player.duration.coerceAtLeast(0L),
            queue = queue.snapshot(),
            error = null
        )
    }

    private fun publish() {
        val current = player.currentMediaItem
        _state.value = _state.value.copy(
            currentVideoId = current?.mediaId ?: _state.value.currentVideoId,
            isPlaying = player.isPlaying,
            isBuffering = player.playbackState == Player.STATE_BUFFERING,
            positionMs = player.currentPosition.coerceAtLeast(0L),
            durationMs = player.duration.coerceAtLeast(0L),
            queue = queue.snapshot()
        )
    }
}
