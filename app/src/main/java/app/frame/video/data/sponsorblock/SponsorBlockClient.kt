package app.frame.video.data.sponsorblock

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class SponsorSegment(
    val startMs: Long,
    val endMs: Long,
    val category: String
)

class SponsorBlockClient(
    private val endpoint: String = "https://sponsor.ajay.app/api/skipSegments"
) {
    suspend fun getSegments(videoId: String, categories: Set<String>): List<SponsorSegment> {
        if (!VIDEO_ID.matches(videoId) || categories.isEmpty()) return emptyList()
        return withContext(Dispatchers.IO) {
            val categoryJson = JSONArray(categories.filter { it in CATEGORIES }.sorted()).toString()
            val encodedCategories = URLEncoder.encode(categoryJson, Charsets.UTF_8.name())
            val url = URL("$endpoint?videoID=$videoId&categories=$encodedCategories&actionTypes=%5B%22skip%22%5D")
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 5_000
                readTimeout = 5_000
                setRequestProperty("User-Agent", "FrameTube/1.0")
                setRequestProperty("Accept", "application/json")
            }
            try {
                val status = connection.responseCode
                if (status == HttpURLConnection.HTTP_NOT_FOUND) return@withContext emptyList()
                if (status !in 200..299) throw IOException("SponsorBlock returned HTTP $status")
                val body = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                parseSegments(body, categories)
            } finally {
                connection.disconnect()
            }
        }
    }

    companion object {
        private val VIDEO_ID = Regex("^[A-Za-z0-9_-]{11}$")
        val CATEGORIES = setOf(
            "sponsor", "selfpromo", "interaction", "intro", "outro",
            "preview", "music_offtopic", "filler"
        )

        internal fun parseSegments(json: String, categories: Set<String>): List<SponsorSegment> {
            val items = JSONArray(json)
            return buildList {
                for (index in 0 until items.length()) {
                    val item = items.optJSONObject(index) ?: continue
                    if (item.optString("actionType") != "skip") continue
                    val category = item.optString("category")
                    if (category !in categories) continue
                    val range = item.optJSONArray("segment") ?: continue
                    if (range.length() < 2) continue
                    val start = (range.optDouble(0, -1.0) * 1_000).toLong()
                    val end = (range.optDouble(1, -1.0) * 1_000).toLong()
                    if (start >= 0 && end > start) add(SponsorSegment(start, end, category))
                }
            }.sortedBy { it.startMs }
        }
    }
}
