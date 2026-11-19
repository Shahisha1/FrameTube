package app.frame.video.data.download

import java.net.URI
import java.util.Locale

data class DirectVideoFile(
    val url: String,
    val mimeType: String,
    val fileName: String
)

/** Allows only direct HTTPS video files. It never accepts YouTube pages or extracted stream hosts. */
object DirectDownloadPolicy {
    fun validate(rawUrl: String): DirectVideoFile {
        val cleanUrl = rawUrl.trim()
        val uri = try {
            URI(cleanUrl)
        } catch (_: Exception) {
            throw IllegalArgumentException("Enter a valid direct HTTPS file link.")
        }
        require(uri.scheme.equals("https", ignoreCase = true)) { "Use a secure https:// video link." }
        val host = uri.host?.lowercase(Locale.ROOT).orEmpty()
        require(host.isNotBlank()) { "That link has no valid host." }
        val blockedHosts = listOf("youtube.com", "youtu.be", "youtube-nocookie.com", "googlevideo.com")
        require(blockedHosts.none { host == it || host.endsWith(".$it") }) {
            "Frame does not download YouTube streams. Use a direct file link you have permission to save."
        }

        val pathName = uri.path.orEmpty().substringAfterLast('/')
        val extension = pathName.substringAfterLast('.', "").lowercase(Locale.ROOT)
        val mime = when (extension) {
            "mp4", "m4v" -> "video/mp4"
            "webm" -> "video/webm"
            "mkv" -> "video/x-matroska"
            "mov" -> "video/quicktime"
            "avi" -> "video/x-msvideo"
            else -> null
        }
        require(mime != null) { "Use a direct MP4, WebM, MKV, MOV or AVI file link." }
        val safeName = pathName.take(80).ifBlank { "frame-video.$extension" }
            .replace(Regex("[^A-Za-z0-9._-]"), "_")
        return DirectVideoFile(cleanUrl, mime, safeName)
    }
}
