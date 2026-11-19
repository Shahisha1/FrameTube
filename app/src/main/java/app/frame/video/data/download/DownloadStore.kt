package app.frame.video.data.download

import android.app.DownloadManager
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Environment

data class DownloadItem(
    val id: Long,
    val title: String,
    val status: Int,
    val downloadedBytes: Long,
    val totalBytes: Long,
    val reason: Int,
    val uri: Uri?
) {
    val progress: Float
        get() = if (totalBytes > 0L) (downloadedBytes.toFloat() / totalBytes).coerceIn(0f, 1f) else 0f
    val complete: Boolean get() = status == DownloadManager.STATUS_SUCCESSFUL && uri != null
}

class DownloadStore(context: Context) {
    private val app = context.applicationContext
    private val manager = app.getSystemService(DownloadManager::class.java)
    private val prefs = app.getSharedPreferences("frame_downloads", Context.MODE_PRIVATE)

    @Synchronized
    fun enqueue(rawUrl: String, title: String): Long {
        val file = DirectDownloadPolicy.validate(rawUrl)
        val uri = Uri.parse(file.url)

        val request = DownloadManager.Request(uri)
            .setTitle(title.trim().ifBlank { file.fileName })
            .setDescription("Saved for offline playback in Frame")
            .setMimeType(file.mimeType)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(false)
            .setDestinationInExternalFilesDir(
                app,
                Environment.DIRECTORY_MOVIES,
                "${System.currentTimeMillis()}_${file.fileName}"
            )
        val id = manager.enqueue(request)
        val ids = prefs.getStringSet(KEY_IDS, emptySet()).orEmpty().toMutableSet()
        ids.add(id.toString())
        prefs.edit().putStringSet(KEY_IDS, ids).apply()
        return id
    }

    fun list(): List<DownloadItem> {
        val ids = prefs.getStringSet(KEY_IDS, emptySet()).orEmpty()
        return ids.mapNotNull { it.toLongOrNull()?.let(::query) }.sortedByDescending { it.id }
    }

    fun uri(id: Long): Uri? = manager.getUriForDownloadedFile(id)

    fun remove(id: Long) {
        manager.remove(id)
        val ids = prefs.getStringSet(KEY_IDS, emptySet()).orEmpty().toMutableSet()
        ids.remove(id.toString())
        prefs.edit().putStringSet(KEY_IDS, ids).apply()
    }

    private fun query(id: Long): DownloadItem? {
        val cursor = manager.query(DownloadManager.Query().setFilterById(id)) ?: return null
        return cursor.use {
            if (!it.moveToFirst()) return null
            val status = it.int(DownloadManager.COLUMN_STATUS)
            DownloadItem(
                id = id,
                title = it.string(DownloadManager.COLUMN_TITLE).orEmpty(),
                status = status,
                downloadedBytes = it.long(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR),
                totalBytes = it.long(DownloadManager.COLUMN_TOTAL_SIZE_BYTES),
                reason = it.int(DownloadManager.COLUMN_REASON),
                uri = if (status == DownloadManager.STATUS_SUCCESSFUL) manager.getUriForDownloadedFile(id) else null
            )
        }
    }

    private fun Cursor.string(column: String): String? = getColumnIndex(column).takeIf { it >= 0 }?.let { getString(it) }
    private fun Cursor.int(column: String): Int = getColumnIndex(column).takeIf { it >= 0 }?.let { getInt(it) } ?: 0
    private fun Cursor.long(column: String): Long = getColumnIndex(column).takeIf { it >= 0 }?.let { getLong(it) } ?: 0L

    private companion object { const val KEY_IDS = "download_ids" }
}
