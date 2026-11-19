package app.frame.video.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class PlaybackProgressTest {
    @Test fun noSavedPositionStartsAtZero() = assertEquals(0L, PlaybackProgress.resumePositionMs(null))
    @Test fun veryShortPositionStartsAtZero() {
        assertEquals(0L, PlaybackProgress.resumePositionMs(0))
        assertEquals(0L, PlaybackProgress.resumePositionMs(4))
    }
    @Test fun savedPositionBecomesMilliseconds() {
        assertEquals(5_000L, PlaybackProgress.resumePositionMs(5))
        assertEquals(125_000L, PlaybackProgress.resumePositionMs(125))
    }
    @Test fun midVideoPositionIsSavedInWholeSeconds() =
        assertEquals(61L, PlaybackProgress.secondsToSave(61_900, 600_000, false))
    @Test fun endedVideoIsSavedAsZero() =
        assertEquals(0L, PlaybackProgress.secondsToSave(600_000, 600_000, true))
    @Test fun stoppingNearTheEndCountsAsFinished() {
        assertEquals(0L, PlaybackProgress.secondsToSave(595_000, 600_000, false))
        assertEquals(0L, PlaybackProgress.secondsToSave(590_000, 600_000, false))
        assertEquals(589L, PlaybackProgress.secondsToSave(589_900, 600_000, false))
    }
    @Test fun unknownDurationStillSavesPosition() {
        assertEquals(42L, PlaybackProgress.secondsToSave(42_000, Long.MIN_VALUE, false))
        assertEquals(42L, PlaybackProgress.secondsToSave(42_000, 0, false))
    }
    @Test fun nonPositivePositionIsSavedAsZero() {
        assertEquals(0L, PlaybackProgress.secondsToSave(0, 600_000, false))
        assertEquals(0L, PlaybackProgress.secondsToSave(-5, 600_000, false))
    }
}
