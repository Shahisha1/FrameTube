package app.frame.video.domain.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackQueueTest {
    private fun item(id: String) = PlaybackQueueEntry(id, "Video $id")

    @Test
    fun removesDuplicateIdsAndKeepsTheRequestedCurrentItem() {
        val queue = PlaybackQueue()
        val state = queue.replace(
            listOf(item("a"), item("b"), item("a"), item("c")),
            startIndex = 1,
            mode = PlaybackQueueMode.SHORTS
        )
        assertEquals(listOf("a", "b", "c"), state.entries.map { it.videoId })
        assertEquals("b", state.current?.videoId)
        assertEquals(PlaybackQueueMode.SHORTS, state.mode)
    }

    @Test
    fun nextAndPreviousMoveWithinBounds() {
        val queue = PlaybackQueue()
        queue.replace(listOf(item("a"), item("b"), item("c")), 0, PlaybackQueueMode.USER)
        assertNull(queue.previous())
        assertEquals("b", queue.next()?.videoId)
        assertEquals("a", queue.previous()?.videoId)
        assertEquals("b", queue.next()?.videoId)
        assertEquals("c", queue.next()?.videoId)
        assertNull(queue.next())
    }

    @Test
    fun appendDoesNotAddExistingIdsAndPromotesSingleQueueToUserQueue() {
        val queue = PlaybackQueue()
        queue.replace(listOf(item("a")), mode = PlaybackQueueMode.SINGLE)
        val state = queue.append(listOf(item("a"), item("b"), item("c")))
        assertEquals(listOf("a", "b", "c"), state.entries.map { it.videoId })
        assertEquals(PlaybackQueueMode.USER, state.mode)
        assertEquals("a", state.current?.videoId)
    }

    @Test
    fun removeAndReorderKeepCurrentItemIdentity() {
        val queue = PlaybackQueue()
        queue.replace(listOf(item("a"), item("b"), item("c")), 1, PlaybackQueueMode.USER)
        queue.move(1, 2)
        assertEquals("b", queue.snapshot().current?.videoId)
        queue.removeAt(2)
        assertEquals("c", queue.snapshot().current?.videoId)
        assertFalse(queue.snapshot().hasNext)
    }
}
