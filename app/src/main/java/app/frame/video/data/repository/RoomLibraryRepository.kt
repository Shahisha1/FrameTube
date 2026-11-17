package app.frame.video.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import app.frame.video.data.local.LibraryDao
import app.frame.video.domain.LibraryCounts
import app.frame.video.domain.LibraryRepository
import kotlinx.coroutines.flow.combine

class RoomLibraryRepository(private val dao: LibraryDao) : LibraryRepository {
    private fun <T : Any> pager(source: () -> PagingSource<Int, T>) =
        Pager(PagingConfig(pageSize = 50, enablePlaceholders = false), pagingSourceFactory = source).flow

    override fun pagedHistory() = pager { dao.pagedHistory() }
    override fun pagedLiked() = pager { dao.pagedLiked() }
    override fun pagedChannels() = pager { dao.pagedChannels() }
    override fun pagedPlaylists() = pager { dao.pagedPlaylists() }
    override fun pagedPlaylistVideos(playlistId: String) = pager { dao.pagedPlaylistVideos(playlistId) }
    override fun searchVideos(query: String) = pager { dao.pagedSearch(escapeLike(query), query) }
    override fun observePlaylist(id: String) = dao.observePlaylist(id)
    override fun observeRecentHistory(limit: Int) = dao.observeRecentHistory(limit)
    override fun observeCounts() = combine(
        dao.countVideos(), dao.countChannels(), dao.countPlaylists(), dao.countWatched()
    ) { v, c, p, w -> LibraryCounts(v, c, p, w) }
    override suspend fun getVideo(id: String) = dao.getVideo(id)
    override suspend fun deleteAll() = dao.clearAll()

    companion object {
        /** Escapes LIKE wildcards so user text is matched literally (queries use ESCAPE '\'). */
        fun escapeLike(q: String) = q.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")
    }
}
