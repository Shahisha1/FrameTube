package app.frame.video.core.provider.piped

import org.json.JSONException
import org.json.JSONObject

data class PipedStream(val url: String, val mimeType: String?, val videoOnly: Boolean, val height: Int)

data class PipedStreams(
    val title: String?,
    val uploader: String?,
    val durationSeconds: Long,
    val livestream: Boolean,
    val hlsUrl: String?,
    val dashUrl: String?,
    val videoStreams: List<PipedStream>
)

sealed interface PipedPlayable {
    val url: String
    val mimeType: String?
    data class Hls(override val url: String) : PipedPlayable { override val mimeType: String get() = "application/x-mpegURL" }
    data class Dash(override val url: String) : PipedPlayable { override val mimeType: String get() = "application/dash+xml" }
    data class Progressive(override val url: String, override val mimeType: String?) : PipedPlayable
}

enum class PipedFailureKind { NOT_FOUND, RESTRICTED, NETWORK, SERVER, BAD_RESPONSE }

sealed interface PipedResult {
    data class Success(val streams: PipedStreams) : PipedResult
    data class Failure(val kind: PipedFailureKind, val message: String) : PipedResult
}

/** Pure functions over Piped's documented /streams/:videoId JSON, so they can be unit-tested. */
object PipedParser {
    fun parseStreams(json: String): PipedStreams {
        val o = JSONObject(json)
        val arr = o.optJSONArray("videoStreams")
        val streams = ArrayList<PipedStream>()
        if (arr != null) {
            for (i in 0 until arr.length()) {
                val s = arr.optJSONObject(i) ?: continue
                val url = s.str("url") ?: continue
                streams += PipedStream(url, s.str("mimeType"), s.optBoolean("videoOnly", false), s.optInt("height", 0))
            }
        }
        return PipedStreams(
            title = o.str("title"),
            uploader = o.str("uploader"),
            durationSeconds = o.optLong("duration", 0),
            livestream = o.optBoolean("livestream", false),
            hlsUrl = o.str("hls"),
            dashUrl = o.str("dash"),
            videoStreams = streams
        )
    }

    fun errorMessage(body: String?): String? {
        if (body.isNullOrBlank()) return null
        return try {
            val o = JSONObject(body)
            o.str("message") ?: o.str("error")
        } catch (_: JSONException) { null }
    }

    /** Prefer a manifest (HLS), then the best muxed (audio+video) file, then DASH. Video-only streams are never chosen alone. */
    fun choose(s: PipedStreams): PipedPlayable? {
        s.hlsUrl?.let { return PipedPlayable.Hls(it) }
        s.videoStreams.filter { !it.videoOnly }.maxByOrNull { it.height }?.let { return PipedPlayable.Progressive(it.url, it.mimeType) }
        s.dashUrl?.let { return PipedPlayable.Dash(it) }
        return null
    }

    // HEURISTIC: Piped's error text is not documented, so this is best-effort wording matching.
    fun classify(httpCode: Int, message: String?): PipedFailureKind {
        val m = message.orEmpty().lowercase()
        val restricted = listOf("private", "sign in", "age-restricted", "age restricted", "members", "blocked", "country", "region", "copyright", "not available in")
        val missing = listOf("not found", "unavailable", "removed", "deleted", "does not exist", "terminated")
        return when {
            restricted.any { it in m } -> PipedFailureKind.RESTRICTED
            httpCode == 404 || missing.any { it in m } -> PipedFailureKind.NOT_FOUND
            else -> PipedFailureKind.SERVER
        }
    }

    private fun JSONObject.str(name: String): String? = if (isNull(name)) null else optString(name).ifBlank { null }
}
