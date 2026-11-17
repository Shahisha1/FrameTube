package app.frame.video.data.local

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

private val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS search_history (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, query TEXT NOT NULL, searchedAt INTEGER NOT NULL)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_search_history_searchedAt ON search_history(searchedAt)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_search_history_query_searchedAt ON search_history(query, searchedAt)")
        db.execSQL("DELETE FROM watch_history WHERE id NOT IN (SELECT MIN(id) FROM watch_history GROUP BY videoId, watchedAt)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_watch_history_videoId_watchedAt ON watch_history(videoId, watchedAt)")
    }
}

object FrameDatabaseProvider {
    @Volatile private var instance: FrameDatabase? = null

    fun get(context: Context): FrameDatabase = instance ?: synchronized(this) {
        instance ?: Room.databaseBuilder(
            context.applicationContext,
            FrameDatabase::class.java,
            "frame-library.db"
        ).addMigrations(MIGRATION_1_2)
            .build().also { instance = it }
    }
}
