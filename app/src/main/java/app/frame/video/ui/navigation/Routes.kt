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
    const val DETAILS = "details/{videoId}"
    const val PLAYER = "player/{videoId}"
    const val PLAYLIST = "playlist/{playlistId}"
    fun details(videoId: String) = "details/${Uri.encode(videoId)}"
    fun player(videoId: String) = "player/${Uri.encode(videoId)}"
    fun playlist(id: String) = "playlist/${Uri.encode(id)}"
}
