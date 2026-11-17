package app.frame.video.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "channels")
data class ChannelEntity(
    @PrimaryKey val id: String,
    val title: String,
    val channelUrl: String? = null,
    val thumbnailUrl: String? = null,
    val importedAt: Long? = null
)

@Entity(tableName = "videos", indices = [Index("channelId"), Index("title")])
data class VideoEntity(
    @PrimaryKey val id: String,
    val title: String,
    val channelId: String? = null,
    val description: String? = null,
    val thumbnailUrl: String? = null,
    val durationSeconds: Long? = null,
    val originalUrl: String? = null,
    val source: String = "SAMPLE",
    val metadataUpdatedAt: Long? = null
)

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String? = null,
    val source: String = "LOCAL",
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "playlist_entries",
    primaryKeys = ["playlistId", "videoId"],
    foreignKeys = [
        ForeignKey(entity = PlaylistEntity::class, parentColumns = ["id"], childColumns = ["playlistId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = VideoEntity::class, parentColumns = ["id"], childColumns = ["videoId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("videoId")]
)
data class PlaylistEntryEntity(val playlistId: String, val videoId: String, val position: Int)

@Entity(tableName = "watch_history", indices = [Index("watchedAt"), Index("videoId"), Index(value = ["videoId", "watchedAt"], unique = true)])
data class WatchHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val videoId: String,
    val watchedAt: Long,
    val positionSeconds: Long = 0
)

@Entity(tableName = "likes")
data class LikeEntity(@PrimaryKey val videoId: String, val likedAt: Long)

@Entity(tableName = "subscriptions")
data class SubscriptionEntity(@PrimaryKey val channelId: String, val subscribedAt: Long? = null)

@Entity(tableName = "search_history", indices = [Index("searchedAt"), Index(value = ["query", "searchedAt"], unique = true)])
data class SearchHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val query: String,
    val searchedAt: Long
)

@Entity(tableName = "import_batches")
data class ImportBatchEntity(
    @PrimaryKey val id: String,
    val sourceName: String,
    val startedAt: Long,
    val finishedAt: Long? = null,
    val status: String,
    val importedCount: Int = 0,
    val skippedCount: Int = 0,
    val errorCount: Int = 0
)

@Entity(tableName = "import_issues", indices = [Index("batchId")])
data class ImportIssueEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val batchId: String,
    val fileName: String,
    val recordReference: String? = null,
    val message: String
)
