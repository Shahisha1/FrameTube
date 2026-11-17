package app.frame.video.core.provider.piped

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONException
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.net.URLEncoder

/**
 * Minimal client for Piped's documented GET /streams/:videoId endpoint.
 * Sends no cookies, no Authorization header and no account data. Only the video ID leaves the device.
 */
class PipedApi {
    suspend fun streams(baseUrl: String, videoId: String): PipedResult = withContext(Dispatchers.IO) {
        if (!VIDEO_ID.matches(videoId)) return@withContext PipedResult.Failure(PipedFailureKind.NOT_FOUND, "Invalid video ID")
        var conn: HttpURLConnection? = null
        try {
            conn = URL("$baseUrl/streams/${URLEncoder.encode(videoId, "UTF-8")}").openConnection() as HttpURLConnection
            conn.connectTimeout = 10_000
            conn.readTimeout = 15_000
            conn.setRequestProperty("Accept", "application/json")
            conn.setRequestProperty("User-Agent", "Frame/0.1")
            val code = conn.responseCode
            val body = (if (code in 200..299) conn.inputStream else conn.errorStream)?.use { readCapped(it) }
            if (code in 200..299) {
                try {
                    PipedResult.Success(PipedParser.parseStreams(body.orEmpty()))
                } catch (e: JSONException) {
                    PipedResult.Failure(PipedFailureKind.BAD_RESPONSE, "Unreadable response")
                }
            } else {
                val message = PipedParser.errorMessage(body)
                PipedResult.Failure(PipedParser.classify(code, message), message ?: "HTTP $code")
            }
        } catch (e: IOException) {
            PipedResult.Failure(PipedFailureKind.NETWORK, e.message ?: "Network error")
        } finally {
            conn?.disconnect()
        }
    }

    private fun readCapped(input: InputStream): String {
        val out = ByteArrayOutputStream()
        val buf = ByteArray(8192)
        while (true) {
            val n = input.read(buf)
            if (n < 0) break
            out.write(buf, 0, n)
            if (out.size() > MAX_BODY) throw IOException("Response too large")
        }
        return out.toString(Charsets.UTF_8.name())
    }

    companion object {
        private const val MAX_BODY = 4 * 1024 * 1024
        private val VIDEO_ID = Regex("[A-Za-z0-9_-]{6,20}")

        /** Returns a clean https base URL without trailing slash, or null if the text is not a usable instance address. */
        fun normalizeInstanceUrl(raw: String): String? {
            val t = raw.trim().trimEnd('/')
            if (!t.startsWith("https://", ignoreCase = true)) return null
            return try {
                val uri = URI(t)
                if (uri.host.isNullOrBlank() || uri.rawQuery != null || uri.rawFragment != null) null else t
            } catch (_: Exception) { null }
        }
    }
}
