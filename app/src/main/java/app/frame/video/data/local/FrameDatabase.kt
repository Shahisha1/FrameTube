package app.frame.video.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        ChannelEntity::class, VideoEntity::class, PlaylistEntity::class,
        PlaylistEntryEntity::class, WatchHistoryEntity::class, LikeEntity::class,
        SubscriptionEntity::class, SearchHistoryEntity::class,
        ImportBatchEntity::class, ImportIssueEntity::class
    ],
    version = 2,
    exportSchema = true
)
abstract class FrameDatabase : RoomDatabase() {
    abstract fun libraryDao(): LibraryDao
}
