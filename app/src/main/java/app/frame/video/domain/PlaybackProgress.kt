package app.frame.video.domain

/** Rules for saving and restoring where the user stopped watching. Pure logic, no Android types. */
object PlaybackProgress {
    /** Positions shorter than this are treated as "not started". */
    const val MIN_RESUME_SECONDS = 5L

    /** Stopping this close to the end counts as finished. */
    const val END_THRESHOLD_MS = 10_000L

    /** Where to start playing, given the last saved position. */
    fun resumePositionMs(savedSeconds: Long?): Long {
        val seconds = savedSeconds ?: return 0L
        return if (seconds < MIN_RESUME_SECONDS) 0L else seconds * 1000L
    }

    /**
     * The position to store in whole seconds. A finished video is stored as 0 so it starts over next time.
     * [durationMs] may be zero or negative while the player does not know it yet.
     */
    fun secondsToSave(positionMs: Long, durationMs: Long, ended: Boolean): Long {
        if (ended || positionMs <= 0L) return 0L
        if (durationMs > 0L && durationMs - positionMs <= END_THRESHOLD_MS) return 0L
        return positionMs / 1000L
    }
}
