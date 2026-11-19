package app.frame.video.ui.navigation

import android.net.Uri

object Routes {
    const val HOME = "home"
    const val SUBSCRIPTIONS = "subscriptions"
    const val PLAYLISTS = "playlists"
    const val HISTORY = "history"
    const val LIKED = "liked"
    const val SEARCH = "search"
    const val SETTINGS = "settings"
    const val IMPORT = "import"
    const val DOWNLOADS = "downloads"
    const val DETAILS = "details/{videoId}"
    const val PLAYER = "player/{videoId}"
    const val LOCAL_PLAYER = "local-player/{downloadId}"
    const val PLAYLIST = "playlist/{playlistId}"
    const val CHANNEL = "channel/{channelId}"
    fun details(videoId: String) = "details/${Uri.encode(videoId)}"
    fun player(videoId: String) = "player/${Uri.encode(videoId)}"
    fun localPlayer(downloadId: Long) = "local-player/$downloadId"
    fun playlist(id: String) = "playlist/${Uri.encode(id)}"
    fun channel(id: String) = "channel/${Uri.encode(id)}"
}
