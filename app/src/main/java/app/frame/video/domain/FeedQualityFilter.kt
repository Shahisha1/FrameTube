package app.frame.video.domain

import app.frame.video.domain.provider.VideoMetadataResult

/** Applies local feed preferences without mutating provider results. */
fun filterRecommendations(
    videos: List<VideoMetadataResult>,
    hideShorts: Boolean,
    mutedKeywords: String,
    maxPerChannel: Int
): List<VideoMetadataResult> {
    val blockedTerms = mutedKeywords.split(',').map { it.trim() }.filter { it.isNotEmpty() }
    val channelCounts = mutableMapOf<String, Int>()
    val perChannelLimit = maxPerChannel.coerceIn(1, 10)
    return videos.filter { video ->
        val title = video.title.orEmpty()
        val shortForm = video.isShortFormContent || (video.durationSeconds != null && video.durationSeconds <= 60L)
        val durationAllowed = !hideShorts || !shortForm
        val titleAllowed = blockedTerms.none { term -> title.contains(term, ignoreCase = true) }
        val channel = video.channelId.orEmpty()
        val channelAllowed = channel.isBlank() || (channelCounts[channel] ?: 0) < perChannelLimit
        if (durationAllowed && titleAllowed && channelAllowed && channel.isNotBlank()) {
            channelCounts[channel] = (channelCounts[channel] ?: 0) + 1
        }
        durationAllowed && titleAllowed && channelAllowed
    }
}
