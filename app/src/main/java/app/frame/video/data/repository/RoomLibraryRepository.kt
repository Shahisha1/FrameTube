package app.frame.video.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import app.frame.video.data.local.LibraryDao
import app.frame.video.data.local.LikeEntity
import app.frame.video.data.local.VideoEntity
import app.frame.video.data.local.WatchHistoryEntity
import app.frame.video.data.local.ChannelEntity
import app.frame.video.data.local.SubscriptionEntity
import app.frame.video.data.local.SearchHistoryEntity
import app.frame.video.domain.LibraryCounts
import app.frame.video.domain.LibraryRepository
import kotlinx.coroutines.flow.combine

class RoomLibraryRepository(private val dao: LibraryDao) : LibraryRepository {
    private fun <T : Any> pager(source: () -> PagingSource<Int, T>) =
        Pager(PagingConfig(pageSize = 50, enablePlaceholders = false), pagingSourceFactory = source).flow

    override fun pagedHistory() = pager { dao.pagedHistory() }
    override fun pagedLiked() = pager { dao.pagedLiked() }
    override fun pagedChannels() = pager { dao.pagedChannels() }
    override fun observeSubscribedChannels() = dao.observeSubscribedChannels()
    override fun pagedPlaylists() = pager { dao.pagedPlaylists() }
    override fun pagedPlaylistVideos(playlistId: String) = pager { dao.pagedPlaylistVideos(playlistId) }
    override fun searchVideos(query: String) = pager { dao.pagedSearch(escapeLike(query), query) }
    override fun observePlaylist(id: String) = dao.observePlaylist(id)
    override fun observeRecentHistory(limit: Int) = dao.observeRecentHistory(limit)
    override fun observeSearchHistory(limit: Int) = dao.observeSearchHistory(limit)
    override suspend fun recordSearchQuery(query: String) = dao.recordSearchQuery(query, System.currentTimeMillis())
    override fun observeCounts() = combine(
        dao.countVideos(), dao.countChannels(), dao.countPlaylists(), dao.countWatched()
    ) { v, c, p, w -> LibraryCounts(v, c, p, w) }
    override suspend fun getVideo(id: String) = dao.getVideo(id)
    override fun observeIsLiked(videoId: String) = dao.observeIsLiked(videoId)
    override suspend fun setLiked(videoId: String, liked: Boolean) {
        if (liked) dao.insertLike(LikeEntity(videoId, System.currentTimeMillis()))
        else dao.deleteLike(videoId)
    }
    override suspend fun ensureVideo(videoId: String, title: String?, thumbnailUrl: String?) {
        val now = System.currentTimeMillis()
        val realTitle = title?.takeIf { it.isNotBlank() && it != videoId }
        dao.insertVideos(listOf(VideoEntity(
            id = videoId, title = realTitle ?: videoId, thumbnailUrl = thumbnailUrl,
            source = "APP", metadataUpdatedAt = if (realTitle != null) now else null
        )))
        if (realTitle != null) dao.fillPlaceholderMetadata(videoId, realTitle, thumbnailUrl, now)
    }
    override suspend fun recordPlay(videoId: String, startSeconds: Long): Long =
        dao.insertHistoryEntry(WatchHistoryEntity(videoId = videoId, watchedAt = System.currentTimeMillis(), positionSeconds = startSeconds))
    override suspend fun updatePlayPosition(entryId: Long, seconds: Long) = dao.updateHistoryPosition(entryId, seconds)
    override suspend fun lastPositionSeconds(videoId: String) = dao.lastPositionSeconds(videoId)

    override suspend fun getChannel(channelId: String) = dao.getChannel(channelId)
    override fun observeChannel(channelId: String) = dao.observeChannel(channelId)
    override fun observeIsSubscribed(channelId: String) = dao.observeIsSubscribed(channelId)
    override suspend fun saveChannel(channel: ChannelEntity) = dao.upsertChannel(channel)
    override suspend fun setSubscribed(channelId: String, subscribed: Boolean) {
        if (subscribed) dao.upsertSubscription(SubscriptionEntity(channelId, System.currentTimeMillis()))
        else dao.deleteSubscription(channelId)
    }
    override suspend fun deleteAll() = dao.clearAll()

    companion object {
        /** Escapes LIKE wildcards so user text is matched literally (queries use ESCAPE '\'). */
        fun escapeLike(q: String) = q.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")
    }
}
