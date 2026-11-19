package app.frame.video.domain

import androidx.paging.PagingData
import app.frame.video.data.local.ChannelEntity
import app.frame.video.data.local.PlaylistEntity
import app.frame.video.data.local.VideoEntity
import kotlinx.coroutines.flow.Flow

data class LibraryCounts(val videos: Int = 0, val channels: Int = 0, val playlists: Int = 0, val watched: Int = 0) {
    val isEmpty get() = videos == 0 && channels == 0 && playlists == 0 && watched == 0
}

interface LibraryRepository {
    fun pagedHistory(): Flow<PagingData<VideoEntity>>
    fun pagedLiked(): Flow<PagingData<VideoEntity>>
    fun pagedChannels(): Flow<PagingData<ChannelEntity>>
    fun pagedPlaylists(): Flow<PagingData<PlaylistEntity>>
    fun pagedPlaylistVideos(playlistId: String): Flow<PagingData<VideoEntity>>
    fun searchVideos(query: String): Flow<PagingData<VideoEntity>>
    fun observePlaylist(id: String): Flow<PlaylistEntity?>
    fun observeRecentHistory(limit: Int): Flow<List<VideoEntity>>
    fun observeCounts(): Flow<LibraryCounts>
    suspend fun getVideo(id: String): VideoEntity?
    suspend fun deleteAll()

    fun observeIsLiked(videoId: String): Flow<Boolean>
    suspend fun setLiked(videoId: String, liked: Boolean)

    /**
     * Makes sure a library row exists for a video, so history and likes can list it.
     * A known [title] replaces a placeholder title (the video ID); a real title is never overwritten.
     */
    suspend fun ensureVideo(videoId: String, title: String?, thumbnailUrl: String?)

    /** Adds a watch-history entry for an in-app play and returns its id. */
    suspend fun recordPlay(videoId: String, startSeconds: Long): Long
    suspend fun updatePlayPosition(entryId: Long, seconds: Long)

    /** The position saved by the most recent play of this video, if any. */
    suspend fun lastPositionSeconds(videoId: String): Long?
}
