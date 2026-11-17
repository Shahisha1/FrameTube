package app.frame.video.data.importer

import app.frame.video.data.local.FrameDatabase
import app.frame.video.data.local.ImportBatchEntity
import app.frame.video.data.local.ImportIssueEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.util.UUID

/**
 * Two steps so the user can review before anything is written:
 *  1. [parse] reads the archive into memory (cancellable, writes nothing).
 *  2. [commit] writes everything in ONE database transaction (all or nothing).
 *
 * Re-import behaviour:
 *  - playlists: replaced by the new export, so videos removed from a playlist disappear from it
 *  - subscriptions, watch history, search history, videos: added if new, existing rows untouched
 *    (removed items are NOT deleted; use Settings > Delete library data for a clean start)
 */
class TakeoutImporter(private val database: FrameDatabase) {

    suspend fun parse(open: () -> InputStream): TakeoutParser.Parsed = withContext(Dispatchers.IO) {
        val ctx = currentCoroutineContext()
        open().use { TakeoutParser().parse(it) { ctx.ensureActive() } }
    }

    suspend fun commit(parsed: TakeoutParser.Parsed, sourceName: String): TakeoutImportResult {
        val dao = database.libraryDao()
        val batchId = UUID.randomUUID().toString()
        val started = System.currentTimeMillis()
        return try {
            dao.insertImportedLibrary(
                channels = parsed.channels,
                subscriptions = parsed.subscriptions,
                playlists = parsed.playlists,
                videos = parsed.videos,
                entries = parsed.entries,
                history = parsed.history,
                searches = parsed.searches
            )
            dao.upsertImportBatch(
                ImportBatchEntity(
                    id = batchId, sourceName = sourceName, startedAt = started,
                    finishedAt = System.currentTimeMillis(), status = "COMPLETED",
                    importedCount = parsed.result.totalImported,
                    skippedCount = parsed.result.skipped,
                    errorCount = parsed.result.issues.size
                )
            )
            if (parsed.result.issues.isNotEmpty()) {
                dao.insertImportIssues(parsed.result.issues.map {
                    ImportIssueEntity(batchId = batchId, fileName = "Takeout archive", message = it)
                })
            }
            parsed.result
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            dao.upsertImportBatch(
                ImportBatchEntity(
                    id = batchId, sourceName = sourceName, startedAt = started,
                    finishedAt = System.currentTimeMillis(), status = "FAILED", errorCount = 1
                )
            )
            throw e
        }
    }
}
