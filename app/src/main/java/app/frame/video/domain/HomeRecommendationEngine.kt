package app.frame.video.domain

import app.frame.video.domain.provider.VideoMetadataResult
import java.util.Locale

/**
 * Builds small, human-readable discovery queries from recent viewing activity.
 *
 * This is deliberately deterministic and local: no profile is uploaded and no
 * claim is made that it reproduces YouTube's private recommendation algorithm.
 */
object HomeRecommendationEngine {
    private val ignoredWords = setOf(
        "a", "an", "and", "are", "as", "at", "be", "been", "but", "by",
        "for", "from", "how", "i", "in", "into", "is", "it", "its", "my",
        "of", "on", "or", "our", "the", "their", "this", "to", "was",
        "we", "what", "when", "with", "you", "your", "me", "vs",
        "video", "videos", "watch", "watching", "official", "full",
        "episode", "part", "shorts", "short", "new", "best", "top",
        "trailer", "review", "reaction", "explained"
    )

    private val genericTokens = Regex("^[0-9]+$")
    private val shortTechnicalTerms = setOf("pc", "ai", "vr", "tv", "ui", "ux", "os", "gpu", "cpu", "rpg", "fps", "api", "sql", "mac", "ios")

    /**
     * Returns up to [maxPersonalizedQueries] topic phrases, weighted toward
     * newer titles and repeated topics. Generic discovery queries are appended
     * so first-time users still receive a useful feed.
     */
    fun buildQueries(
        recentTitles: List<String>,
        maxPersonalizedQueries: Int = 3
    ): List<String> {
        val scores = linkedMapOf<String, Double>()

        recentTitles.take(12).forEachIndexed { historyIndex, title ->
            val tokens = title.lowercase(Locale.ROOT)
                .replace(Regex("[^\\p{L}\\p{N}]+"), " ")
                .trim()
                .split(Regex("\\s+"))
                .filter { (it.length > 2 || it in shortTechnicalTerms) && it !in ignoredWords && !genericTokens.matches(it) }
                .take(12)

            if (tokens.size == 1) {
                val phrase = tokens.first()
                scores[phrase] = (scores[phrase] ?: 0.0) + 1.0 / (historyIndex + 1)
            }

            for (size in 2..3) {
                if (tokens.size < size) continue
                for (start in 0..tokens.size - size) {
                    val phrase = tokens.subList(start, start + size).joinToString(" ")
                    val phraseWeight = if (size == 3) 1.15 else 1.0
                    val recencyWeight = 1.0 / (historyIndex + 1)
                    scores[phrase] = (scores[phrase] ?: 0.0) + phraseWeight * recencyWeight
                }
            }
        }

        val personalized = scores.entries
            .sortedWith(compareByDescending<Map.Entry<String, Double>> { it.value }
                .thenByDescending { it.key.length })
            .map { it.key }
            .distinct()
            .take(maxPersonalizedQueries.coerceIn(0, 6))

        return (personalized + listOf("popular videos", "trending videos"))
            .distinct()
    }

    /**
     * Round-robins provider batches so one broad query cannot dominate the feed.
     */
    fun interleave(
        batches: List<List<VideoMetadataResult>>,
        limit: Int = 36
    ): List<VideoMetadataResult> {
        if (limit <= 0) return emptyList()
        val result = ArrayList<VideoMetadataResult>(limit)
        val seen = HashSet<String>()
        val maxBatchSize = batches.maxOfOrNull { it.size } ?: 0

        for (index in 0 until maxBatchSize) {
            for (batch in batches) {
                val candidate = batch.getOrNull(index) ?: continue
                if (seen.add(candidate.videoId)) result += candidate
                if (result.size == limit) return result
            }
        }
        return result
    }
}
