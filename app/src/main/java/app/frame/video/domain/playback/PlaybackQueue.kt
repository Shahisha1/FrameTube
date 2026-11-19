package app.frame.video.domain.playback

enum class PlaybackQueueMode {
    SINGLE,
    USER,
    AUTOPLAY,
    SHORTS
}

data class PlaybackQueueEntry(
    val videoId: String,
    val title: String,
    val thumbnailUrl: String? = null
)

data class PlaybackQueueSnapshot(
    val entries: List<PlaybackQueueEntry> = emptyList(),
    val currentIndex: Int = -1,
    val mode: PlaybackQueueMode = PlaybackQueueMode.SINGLE
) {
    val current: PlaybackQueueEntry?
        get() = entries.getOrNull(currentIndex)

    val hasNext: Boolean
        get() = currentIndex >= 0 && currentIndex < entries.lastIndex

    val hasPrevious: Boolean
        get() = currentIndex > 0
}

/** Mutable queue rules kept independent from Android and Media3. */
class PlaybackQueue {
    private var entries: List<PlaybackQueueEntry> = emptyList()
    private var currentIndex: Int = -1
    private var mode: PlaybackQueueMode = PlaybackQueueMode.SINGLE

    fun snapshot() = PlaybackQueueSnapshot(entries, currentIndex, mode)

    fun replace(
        items: List<PlaybackQueueEntry>,
        startIndex: Int = 0,
        mode: PlaybackQueueMode = PlaybackQueueMode.USER
    ): PlaybackQueueSnapshot {
        val requestedCurrentId = items.getOrNull(startIndex)?.videoId
        entries = items.filter { it.videoId.isNotBlank() }.distinctBy { it.videoId }
        currentIndex = entries.indexOfFirst { it.videoId == requestedCurrentId }
            .takeIf { it >= 0 }
            ?: if (entries.isEmpty()) -1 else startIndex.coerceIn(0, entries.lastIndex)
        this.mode = if (entries.size <= 1 && mode != PlaybackQueueMode.SHORTS) {
            PlaybackQueueMode.SINGLE
        } else mode
        return snapshot()
    }

    fun select(videoId: String): Boolean {
        val index = entries.indexOfFirst { it.videoId == videoId }
        if (index < 0) return false
        currentIndex = index
        return true
    }

    fun append(items: List<PlaybackQueueEntry>): PlaybackQueueSnapshot {
        val knownIds = entries.mapTo(mutableSetOf()) { it.videoId }
        entries = entries + items.filter { it.videoId.isNotBlank() && knownIds.add(it.videoId) }
        if (currentIndex < 0 && entries.isNotEmpty()) currentIndex = 0
        if (entries.size > 1 && mode == PlaybackQueueMode.SINGLE) mode = PlaybackQueueMode.USER
        return snapshot()
    }

    fun next(): PlaybackQueueEntry? {
        if (currentIndex < 0 || currentIndex >= entries.lastIndex) return null
        currentIndex += 1
        return entries[currentIndex]
    }

    fun previous(): PlaybackQueueEntry? {
        if (currentIndex <= 0) return null
        currentIndex -= 1
        return entries[currentIndex]
    }

    fun removeAt(index: Int): PlaybackQueueSnapshot {
        if (index !in entries.indices) return snapshot()
        entries = entries.toMutableList().also { it.removeAt(index) }
        currentIndex = when {
            entries.isEmpty() -> -1
            index < currentIndex -> currentIndex - 1
            index == currentIndex -> currentIndex.coerceAtMost(entries.lastIndex)
            else -> currentIndex
        }
        if (entries.size <= 1 && mode != PlaybackQueueMode.SHORTS) mode = PlaybackQueueMode.SINGLE
        return snapshot()
    }

    fun move(fromIndex: Int, toIndex: Int): PlaybackQueueSnapshot {
        if (fromIndex !in entries.indices || toIndex !in entries.indices || fromIndex == toIndex) return snapshot()
        val moved = entries.toMutableList()
        val currentId = entries.getOrNull(currentIndex)?.videoId
        val item = moved.removeAt(fromIndex)
        moved.add(toIndex, item)
        entries = moved
        currentIndex = entries.indexOfFirst { it.videoId == currentId }
        return snapshot()
    }

    fun clear(): PlaybackQueueSnapshot {
        entries = emptyList()
        currentIndex = -1
        mode = PlaybackQueueMode.SINGLE
        return snapshot()
    }
}
