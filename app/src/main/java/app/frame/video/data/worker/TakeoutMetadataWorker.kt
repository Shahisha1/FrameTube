package app.frame.video.data.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import app.frame.video.FrameApplication
import app.frame.video.domain.provider.MetadataAvailability
import kotlinx.coroutines.CancellationException
import java.util.concurrent.TimeUnit

/** Resolves imported ID-only entries in the background, independently of the import transaction. */
class TakeoutMetadataWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val app = applicationContext as? FrameApplication ?: return Result.failure()
        val dao = app.container.database.libraryDao()
        val provider = app.container.providerManager.metadataProvider("newpipe") ?: return Result.success()
        val batch = dao.videosNeedingMetadata(BATCH_SIZE)
        if (batch.isEmpty()) return Result.success()
        var transientFailure = false
        for (video in batch) {
            try {
                val metadata = provider.getVideoMetadata(video.id)
                if (metadata.availability == MetadataAvailability.AVAILABLE && !metadata.title.isNullOrBlank() && metadata.title != video.id) {
                    dao.updateVideoMetadata(video.id, metadata.title, metadata.thumbnailUrl, System.currentTimeMillis())
                } else if (metadata.availability == MetadataAvailability.NETWORK_ERROR) {
                    transientFailure = true
                } else {
                    dao.markMetadataAttempted(video.id, System.currentTimeMillis())
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                transientFailure = true
            }
        }
        if (transientFailure && runAttemptCount < MAX_RETRIES) return Result.retry()
        if (transientFailure) batch.forEach { dao.markMetadataAttempted(it.id, System.currentTimeMillis()) }
        if (dao.videosNeedingMetadata(1).isNotEmpty()) {
            val next = OneTimeWorkRequestBuilder<TakeoutMetadataWorker>().setInitialDelay(NEXT_BATCH_DELAY_SECONDS, TimeUnit.SECONDS).build()
            WorkManager.getInstance(applicationContext).enqueueUniqueWork(UNIQUE_WORK, ExistingWorkPolicy.APPEND_OR_REPLACE, next)
        }
        return Result.success()
    }

    companion object {
        const val UNIQUE_WORK = "frame-takeout-metadata-enrichment"
        private const val BATCH_SIZE = 8
        private const val MAX_RETRIES = 3
        private const val NEXT_BATCH_DELAY_SECONDS = 3L
        fun schedule(context: Context) {
            val request = OneTimeWorkRequestBuilder<TakeoutMetadataWorker>().build()
            WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(UNIQUE_WORK, ExistingWorkPolicy.KEEP, request)
        }
    }
}

/** App-facing scheduler keeps WorkManager details out of the presentation layer. */
class MetadataEnrichmentScheduler(private val context: Context) {
    fun schedule() = TakeoutMetadataWorker.schedule(context)
}
