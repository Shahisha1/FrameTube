package app.frame.video.domain

/** Rules for saving and restoring where the user stopped watching. */
object PlaybackProgress {
    const val MIN_RESUME_SECONDS = 5L
    const val END_THRESHOLD_MS = 10_000L

    fun resumePositionMs(savedSeconds: Long?): Long {
        val seconds = savedSeconds ?: return 0L
        return if (seconds < MIN_RESUME_SECONDS) 0L else seconds * 1_000L
    }

    fun secondsToSave(positionMs: Long, durationMs: Long, ended: Boolean): Long {
        if (ended || positionMs <= 0L) return 0L
        if (durationMs > 0L && durationMs - positionMs <= END_THRESHOLD_MS) return 0L
        return positionMs / 1_000L
    }
}
