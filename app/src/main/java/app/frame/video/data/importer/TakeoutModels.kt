package app.frame.video.data.importer

data class TakeoutImportResult(
    val subscriptions: Int = 0,
    val playlists: Int = 0,
    val playlistEntries: Int = 0,
    val watchedVideos: Int = 0,
    val searches: Int = 0,
    val skipped: Int = 0,
    val issues: List<String> = emptyList(),
    val filesFound: List<String> = emptyList()
) {
    val totalImported: Int
        get() = subscriptions + playlists + playlistEntries + watchedVideos + searches
}
