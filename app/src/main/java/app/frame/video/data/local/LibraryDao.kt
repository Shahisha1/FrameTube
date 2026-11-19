package app.frame.video.data.local

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface LibraryDao {
    @Query("SELECT * FROM videos ORDER BY title COLLATE NOCASE")
    fun observeVideos(): Flow<List<VideoEntity>>

    @Query("SELECT * FROM videos WHERE id = :videoId LIMIT 1")
    suspend fun getVideo(videoId: String): VideoEntity?

    /** Imported records with an ID in place of a title are enriched asynchronously. */
    @Query("SELECT * FROM videos WHERE title = id AND metadataUpdatedAt IS NULL LIMIT :limit")
    suspend fun videosNeedingMetadata(limit: Int): List<VideoEntity>
    @Query("UPDATE videos SET title = :title, thumbnailUrl = :thumbnailUrl, source = 'TAKEOUT_ENRICHED', metadataUpdatedAt = :updatedAt WHERE id = :videoId")
    suspend fun updateVideoMetadata(videoId: String, title: String, thumbnailUrl: String?, updatedAt: Long)
    @Query("UPDATE videos SET metadataUpdatedAt = :attemptedAt WHERE id = :videoId")
    suspend fun markMetadataAttempted(videoId: String, attemptedAt: Long)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertVideos(items: List<VideoEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertVideos(items: List<VideoEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertChannels(items: List<ChannelEntity>)

    @Query("SELECT * FROM channels ORDER BY title COLLATE NOCASE")
    fun observeChannels(): Flow<List<ChannelEntity>>

    @Query("SELECT c.* FROM channels c INNER JOIN subscriptions s ON s.channelId = c.id ORDER BY COALESCE(s.subscribedAt, 0) DESC, c.title COLLATE NOCASE")
    fun observeSubscribedChannels(): Flow<List<ChannelEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSubscriptions(items: List<SubscriptionEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPlaylists(items: List<PlaylistEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPlaylistEntries(items: List<PlaylistEntryEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertWatchHistory(items: List<WatchHistoryEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSearchHistory(items: List<SearchHistoryEntity>)

    @Query("SELECT * FROM search_history ORDER BY searchedAt DESC LIMIT :limit")
    fun observeSearchHistory(limit: Int): Flow<List<SearchHistoryEntity>>

    @Query("DELETE FROM search_history WHERE query = :query")
    suspend fun deleteSearchQuery(query: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSearchQuery(item: SearchHistoryEntity)

    @Transaction
    suspend fun recordSearchQuery(query: String, searchedAt: Long) {
        deleteSearchQuery(query)
        insertSearchQuery(SearchHistoryEntity(query = query, searchedAt = searchedAt))
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertImportBatch(item: ImportBatchEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertImportIssues(items: List<ImportIssueEntity>)

    @Query("SELECT * FROM likes")
    suspend fun getLikes(): List<LikeEntity>

    @Query("SELECT * FROM subscriptions")
    suspend fun getSubscriptions(): List<SubscriptionEntity>

    @Query("SELECT * FROM playlists")
    suspend fun getPlaylists(): List<PlaylistEntity>

    @Query("SELECT * FROM playlist_entries")
    suspend fun getPlaylistEntries(): List<PlaylistEntryEntity>

    @Query("SELECT * FROM watch_history")
    suspend fun getWatchHistory(): List<WatchHistoryEntity>

    @Query("SELECT * FROM search_history")
    suspend fun getSearchHistory(): List<SearchHistoryEntity>

    @Query("SELECT v.* FROM videos v INNER JOIN likes l ON l.videoId = v.id ORDER BY l.likedAt DESC")
    fun observeLikedVideos(): Flow<List<VideoEntity>>

    @Query("SELECT v.* FROM videos v INNER JOIN watch_history h ON h.videoId = v.id ORDER BY h.watchedAt DESC")
    fun observeHistory(): Flow<List<VideoEntity>>

    @Query("SELECT * FROM playlists ORDER BY title COLLATE NOCASE")
    fun observePlaylists(): Flow<List<PlaylistEntity>>

    @Query("SELECT v.* FROM videos v INNER JOIN playlist_entries e ON e.videoId = v.id WHERE e.playlistId = :playlistId ORDER BY e.position")
    fun observePlaylistVideos(playlistId: String): Flow<List<VideoEntity>>

    @Query("SELECT * FROM videos WHERE title LIKE '%' || :query || '%' ORDER BY title COLLATE NOCASE LIMIT :limit")
    suspend fun searchTitles(query: String, limit: Int = 100): List<VideoEntity>

    // ---- Paged reads for the library screens ----
    @Query("SELECT v.* FROM videos v JOIN (SELECT videoId, MAX(watchedAt) AS lastAt FROM watch_history GROUP BY videoId) h ON h.videoId = v.id ORDER BY h.lastAt DESC")
    fun pagedHistory(): PagingSource<Int, VideoEntity>

    @Query("SELECT v.* FROM videos v JOIN likes l ON l.videoId = v.id ORDER BY l.likedAt DESC")
    fun pagedLiked(): PagingSource<Int, VideoEntity>

    @Query("SELECT * FROM channels ORDER BY title COLLATE NOCASE")
    fun pagedChannels(): PagingSource<Int, ChannelEntity>

    @Query("SELECT * FROM playlists ORDER BY title COLLATE NOCASE")
    fun pagedPlaylists(): PagingSource<Int, PlaylistEntity>

    @Query("SELECT v.* FROM videos v JOIN playlist_entries e ON e.videoId = v.id WHERE e.playlistId = :playlistId ORDER BY e.position")
    fun pagedPlaylistVideos(playlistId: String): PagingSource<Int, VideoEntity>

    @Query("SELECT v.* FROM videos v LEFT JOIN channels c ON c.id = v.channelId WHERE v.title LIKE '%' || :escaped || '%' ESCAPE '\\' OR c.title LIKE '%' || :escaped || '%' ESCAPE '\\' OR v.id = :raw ORDER BY v.title COLLATE NOCASE")
    fun pagedSearch(escaped: String, raw: String): PagingSource<Int, VideoEntity>

    @Query("SELECT * FROM playlists WHERE id = :id LIMIT 1")
    fun observePlaylist(id: String): Flow<PlaylistEntity?>

    @Query("SELECT v.* FROM videos v JOIN (SELECT videoId, MAX(watchedAt) AS lastAt FROM watch_history GROUP BY videoId) h ON h.videoId = v.id ORDER BY h.lastAt DESC LIMIT :limit")
    fun observeRecentHistory(limit: Int): Flow<List<VideoEntity>>


    // In-app likes and playback progress (M1/M2).
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertHistoryEntry(item: WatchHistoryEntity): Long

    @Query("UPDATE watch_history SET positionSeconds = :seconds WHERE id = :id")
    suspend fun updateHistoryPosition(id: Long, seconds: Long)

    @Query("SELECT positionSeconds FROM watch_history WHERE videoId = :videoId ORDER BY watchedAt DESC, id DESC LIMIT 1")
    suspend fun lastPositionSeconds(videoId: String): Long?

    @Query("UPDATE videos SET title = :title, thumbnailUrl = COALESCE(thumbnailUrl, :thumbnailUrl), metadataUpdatedAt = :updatedAt WHERE id = :videoId AND title = id")
    suspend fun fillPlaceholderMetadata(videoId: String, title: String, thumbnailUrl: String?, updatedAt: Long)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertLike(item: LikeEntity)

    @Query("DELETE FROM likes WHERE videoId = :videoId")
    suspend fun deleteLike(videoId: String)

    @Query("SELECT EXISTS(SELECT 1 FROM likes WHERE videoId = :videoId)")
    fun observeIsLiked(videoId: String): Flow<Boolean>

    // Channel pages and local subscriptions (M3).
    @Query("SELECT * FROM channels WHERE id = :channelId LIMIT 1")
    suspend fun getChannel(channelId: String): ChannelEntity?

    @Query("SELECT * FROM channels WHERE id = :channelId LIMIT 1")
    fun observeChannel(channelId: String): Flow<ChannelEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertChannel(item: ChannelEntity)

    @Query("SELECT EXISTS(SELECT 1 FROM subscriptions WHERE channelId = :channelId)")
    fun observeIsSubscribed(channelId: String): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSubscription(item: SubscriptionEntity)

    @Query("DELETE FROM subscriptions WHERE channelId = :channelId")
    suspend fun deleteSubscription(channelId: String)

    @Query("SELECT COUNT(*) FROM videos") fun countVideos(): Flow<Int>
    @Query("SELECT COUNT(*) FROM channels") fun countChannels(): Flow<Int>
    @Query("SELECT COUNT(*) FROM playlists") fun countPlaylists(): Flow<Int>
    @Query("SELECT COUNT(DISTINCT videoId) FROM watch_history") fun countWatched(): Flow<Int>

    // ---- Delete everything imported (Settings > Delete library data) ----
    @Query("DELETE FROM playlist_entries") suspend fun deletePlaylistEntries()
    @Query("DELETE FROM watch_history") suspend fun deleteWatchHistory()
    @Query("DELETE FROM likes") suspend fun deleteLikes()
    @Query("DELETE FROM subscriptions") suspend fun deleteSubscriptions()
    @Query("DELETE FROM search_history") suspend fun deleteSearchHistory()
    @Query("DELETE FROM playlists") suspend fun deletePlaylists()
    @Query("DELETE FROM videos") suspend fun deleteVideos()
    @Query("DELETE FROM channels") suspend fun deleteChannels()
    @Query("DELETE FROM import_issues") suspend fun deleteImportIssues()
    @Query("DELETE FROM import_batches") suspend fun deleteImportBatches()

    @Transaction
    suspend fun clearAll() {
        deletePlaylistEntries(); deleteWatchHistory(); deleteLikes(); deleteSubscriptions()
        deleteSearchHistory(); deletePlaylists(); deleteVideos(); deleteChannels()
        deleteImportIssues(); deleteImportBatches()
    }

    @Transaction
    suspend fun insertImportedLibrary(
        channels: List<ChannelEntity>,
        subscriptions: List<SubscriptionEntity>,
        playlists: List<PlaylistEntity>,
        videos: List<VideoEntity>,
        entries: List<PlaylistEntryEntity>,
        history: List<WatchHistoryEntity>,
        searches: List<SearchHistoryEntity>
    ) {
        upsertChannels(channels)
        insertSubscriptions(subscriptions)
        upsertPlaylists(playlists)
        insertVideos(videos)
        insertPlaylistEntries(entries)
        insertWatchHistory(history)
        insertSearchHistory(searches)
    }
}
