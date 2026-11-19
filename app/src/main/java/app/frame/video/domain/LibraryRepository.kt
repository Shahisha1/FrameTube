package app.frame.video.domain

import androidx.paging.PagingData
import app.frame.video.data.local.ChannelEntity
import app.frame.video.data.local.PlaylistEntity
import app.frame.video.data.local.SearchHistoryEntity
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
    fun observeSearchHistory(limit: Int): Flow<List<SearchHistoryEntity>>
    suspend fun recordSearchQuery(query: String)
    fun observeCounts(): Flow<LibraryCounts>
    suspend fun getVideo(id: String): VideoEntity?
    fun observeIsLiked(videoId: String): Flow<Boolean>
    suspend fun setLiked(videoId: String, liked: Boolean)
    suspend fun ensureVideo(videoId: String, title: String?, thumbnailUrl: String?)
    suspend fun recordPlay(videoId: String, startSeconds: Long): Long
    suspend fun updatePlayPosition(entryId: Long, seconds: Long)
    suspend fun lastPositionSeconds(videoId: String): Long?

    suspend fun getChannel(channelId: String): ChannelEntity?
    fun observeChannel(channelId: String): Flow<ChannelEntity?>
    fun observeIsSubscribed(channelId: String): Flow<Boolean>
    suspend fun saveChannel(channel: ChannelEntity)
    suspend fun setSubscribed(channelId: String, subscribed: Boolean)
    suspend fun deleteAll()
}
