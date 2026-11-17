package app.frame.video.data.importer

import app.frame.video.data.local.*
import org.jsoup.Jsoup
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.net.URI
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID
import java.util.zip.ZipInputStream

/**
 * Parser for the inspected Google Takeout YouTube export format.
 * CSV parsing handles quoted commas, escaped quotes and CRLF.
 * HTML activity records are identified by their visible action text and link URL.
 */
class TakeoutParser {
    data class Parsed(
        val channels: List<ChannelEntity>,
        val subscriptions: List<SubscriptionEntity>,
        val playlists: List<PlaylistEntity>,
        val videos: List<VideoEntity>,
        val entries: List<PlaylistEntryEntity>,
        val history: List<WatchHistoryEntity>,
        val searches: List<SearchHistoryEntity>,
        val result: TakeoutImportResult
    )

    fun parse(input: InputStream, checkCancelled: () -> Unit = {}): Parsed {
        val issues = mutableListOf<String>()
        val files = linkedMapOf<String, ByteArray>()
        var totalBytes = 0L
        ZipInputStream(input).use { zip ->
            while (true) {
                checkCancelled()
                val entry = zip.nextEntry ?: break
                val name = entry.name
                val lower = name.lowercase(Locale.ROOT)
                // Only read the text files we understand. Media and everything else is skipped without buffering.
                val wanted = !entry.isDirectory && !name.contains("..") && (lower.endsWith(".csv") || lower.endsWith(".html"))
                if (wanted) {
                    val out = java.io.ByteArrayOutputStream()
                    val buffer = ByteArray(8192)
                    var size = 0L
                    var tooBig = false
                    while (true) {
                        val read = zip.read(buffer)
                        if (read < 0) break
                        if (!tooBig) {
                            size += read
                            if (size > MAX_ENTRY_BYTES || totalBytes + size > MAX_TOTAL_BYTES) { tooBig = true; out.reset() }
                            else out.write(buffer, 0, read)
                        }
                        checkCancelled()
                    }
                    if (tooBig) issues += "$name: skipped, file is too large"
                    else { files[name] = out.toByteArray(); totalBytes += size }
                }
                zip.closeEntry()
            }
        }

        fun csvAt(path: String): List<Map<String, String>> {
            val bytes = files[path] ?: return emptyList()
            return try {
                val table = parseCsv(bytes.toString(Charsets.UTF_8).removePrefix("\uFEFF"))
                val headers = table.firstOrNull().orEmpty().map { it.trim() }
                table.drop(1).map { row ->
                    headers.mapIndexedNotNull { index, key ->
                        key.takeIf { it.isNotEmpty() }?.let { it to row.getOrElse(index) { "" }.trim() }
                    }.toMap()
                }
            } catch (e: Exception) {
                issues += "$path: ${e.message ?: "CSV parse error"}"
                emptyList()
            }
        }
        fun csv(suffix: String): List<Map<String, String>> {
            val key = files.keys.firstOrNull { it.endsWith("/$suffix", ignoreCase = true) } ?: return emptyList()
            return csvAt(key)
        }

        val ownChannel = csv("channels/channel.csv").firstOrNull()
        val channelId = ownChannel?.get("Channel ID").orEmpty()
        val channelTitle = ownChannel?.get("Channel Title (Original)").orEmpty()
        val channels = if (channelId.isNotBlank()) listOf(ChannelEntity(channelId, channelTitle.ifBlank { channelId }, importedAt = System.currentTimeMillis())) else emptyList()

        val subscriptionRows = csv("subscriptions/subscriptions.csv")
        val subscriptions = subscriptionRows.mapNotNull { row ->
            val id = row["Channel Id"].orEmpty().trim()
            id.takeIf { it.isNotBlank() }?.let { SubscriptionEntity(it) }
        }
        val importedChannels = subscriptionRows.mapNotNull { row ->
            val id = row["Channel Id"].orEmpty().trim()
            id.takeIf { it.isNotBlank() }?.let {
                ChannelEntity(it, row["Channel Title"].orEmpty().trim().ifBlank { it }, row["Channel Url"].orEmpty().trim().ifBlank { null }, importedAt = System.currentTimeMillis())
            }
        }

        val playlistRows = csv("playlists/playlists.csv")
        val playlists = playlistRows.mapNotNull { row ->
            val id = row["Playlist ID"].orEmpty().trim()
            id.takeIf { it.isNotBlank() }?.let {
                PlaylistEntity(
                    id = it,
                    title = row["Playlist Title (Original)"].orEmpty().trim().ifBlank { "Untitled playlist" },
                    description = row["Playlist Description (Original)"].orEmpty().trim().ifBlank { null },
                    source = "TAKEOUT",
                    updatedAt = parseInstant(row["Playlist Update Timestamp"]) ?: System.currentTimeMillis()
                )
            }
        }

        val entries = mutableListOf<PlaylistEntryEntity>()
        val videoEntities = linkedMapOf<String, VideoEntity>()
        val playlistNameToId = playlists.associateBy { it.title.trim().lowercase(Locale.ROOT) }
        var entryCount = 0
        files.keys.filter { it.contains("/playlists/") && it.endsWith("-videos.csv", true) }.forEach { path ->
            val fileTitle = path.substringAfterLast('/').removeSuffix("-videos.csv").trim()
            val playlist = playlistNameToId[fileTitle.lowercase(Locale.ROOT)]
            if (playlist == null) {
                issues += "$path: no matching playlist metadata row"
                return@forEach
            }
            val rows = csvAt(path)
            rows.forEachIndexed { index, row ->
                val id = row["Video ID"].orEmpty().trim()
                if (id.isNotBlank()) {
                    videoEntities.putIfAbsent(id, VideoEntity(id, id, originalUrl = "https://www.youtube.com/watch?v=$id", source = "TAKEOUT_ID_ONLY"))
                    entries += PlaylistEntryEntity(playlist.id, id, index)
                    entryCount++
                }
            }
        }

        val historyEntries = files.entries.filter { it.key.endsWith("/history/search-history.html", true) || it.key.endsWith("/history/watch-history.html", true) }
        var undated = 0
        val watchRecords = mutableListOf<WatchHistoryEntity>()
        val searchRecords = mutableListOf<SearchHistoryEntity>()
        for (historyEntry in historyEntries) {
            try {
                val doc = Jsoup.parse(historyEntry.value.toString(Charsets.UTF_8))
                doc.select("div.outer-cell").forEach { card ->
                    val content = card.selectFirst("div.content-cell") ?: return@forEach
                    val actionText = content.ownText().trim().replace('\u00a0', ' ').replace('\u202f', ' ')
                    val anchor = content.selectFirst("a[href]")
                    val href = anchor?.attr("href").orEmpty()
                    val label = anchor?.text()?.trim().orEmpty()
                    val dateText = content.text().replace('\u00a0', ' ').replace('\u202f', ' ')
                    val timestamp = parseActivityDate(dateText)
                    val watched = actionText.startsWith("Watched", true)
                    val searched = actionText.startsWith("Searched for", true)
                    if (watched && href.isNotBlank()) {
                        val id = videoIdFromUrl(href)
                        if (id != null) {
                            val existing = videoEntities[id]
                            if (existing == null || existing.title == existing.id) {
                                videoEntities[id] = VideoEntity(id, label.ifBlank { id }, originalUrl = href, source = "TAKEOUT_HISTORY")
                            }
                            if (timestamp == null) undated++
                            watchRecords += WatchHistoryEntity(videoId = id, watchedAt = timestamp ?: 0L)
                        } else issues += "History item had an unrecognized video URL: $href"
                    } else if (searched && label.isNotBlank()) {
                        searchRecords += SearchHistoryEntity(query = label, searchedAt = timestamp ?: 0L)
                    }
                }
            } catch (e: Exception) {
                issues += "history/search-history.html: ${e.message ?: "HTML parse error"}"
            }
        }

        if (undated > 0) issues += "$undated watch records had an unreadable date; they were kept with no date and repeats of the same video collapse into one."
        val uniqueHistory = watchRecords.distinctBy { it.videoId to it.watchedAt }
        val uniqueSearches = searchRecords.distinctBy { it.query to it.searchedAt }
        val known = listOf("subscriptions/subscriptions.csv", "playlists/playlists.csv", "channels/channel.csv", "history/search-history.html", "history/watch-history.html")
        val filesFound = files.keys.filter { k -> known.any { k.endsWith(it, true) } || (k.contains("/playlists/") && k.endsWith("-videos.csv", true)) }.sorted()
        val result = TakeoutImportResult(
            filesFound = filesFound,
            subscriptions = subscriptions.size,
            playlists = playlists.size,
            playlistEntries = entries.size,
            watchedVideos = uniqueHistory.size,
            searches = uniqueSearches.size,
            skipped = issues.size,
            issues = issues
        )
        return Parsed(
            channels = (channels + importedChannels).distinctBy { it.id },
            subscriptions = subscriptions.distinctBy { it.channelId },
            playlists = playlists,
            videos = videoEntities.values.toList(),
            entries = entries.distinctBy { it.playlistId to it.videoId },
            history = uniqueHistory,
            searches = uniqueSearches,
            result = result
        )
    }

    private fun parseCsv(text: String): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        var row = mutableListOf<String>()
        val cell = StringBuilder()
        var quoted = false
        var i = 0
        while (i < text.length) {
            val c = text[i]
            when {
                c == '"' && quoted && i + 1 < text.length && text[i + 1] == '"' -> { cell.append('"'); i++ }
                c == '"' -> quoted = !quoted
                c == ',' && !quoted -> { row += cell.toString(); cell.setLength(0) }
                (c == '\n' || c == '\r') && !quoted -> {
                    row += cell.toString()
                    cell.setLength(0)
                    if (row.any { it.isNotBlank() }) rows += row
                    row = mutableListOf()
                    if (c == '\r' && i + 1 < text.length && text[i + 1] == '\n') i++
                }
                else -> cell.append(c)
            }
            i++
        }
        if (cell.isNotEmpty() || row.isNotEmpty()) {
            row += cell.toString()
            if (row.any { it.isNotBlank() }) rows += row
        }
        return rows
    }

    private fun videoIdFromUrl(url: String): String? {
        return try {
            val uri = URI(url)
            val query = uri.rawQuery.orEmpty().split("&").mapNotNull {
                val parts = it.split("=", limit = 2)
                if (parts.size == 2) parts[0] to java.net.URLDecoder.decode(parts[1], "UTF-8") else null
            }.toMap()
            when {
                uri.host?.endsWith("youtube.com") == true -> query["v"]?.takeIf { it.matches(Regex("[A-Za-z0-9_-]{6,}")) }
                uri.host == "youtu.be" -> uri.path.trim('/').takeIf { it.matches(Regex("[A-Za-z0-9_-]{6,}")) }
                else -> null
            }
        } catch (_: Exception) { null }
    }

    private fun parseInstant(value: String?): Long? =
        try { value?.takeIf { it.isNotBlank() }?.let { OffsetDateTime.parse(it).toInstant().toEpochMilli() } } catch (_: Exception) { null }

    private fun parseActivityDate(text: String): Long? {
        val match = Regex("(Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)\\s+\\d{1,2},\\s+\\d{4},\\s+\\d{1,2}:\\d{2}:\\d{2}\\s+[AP]M\\s+GMT[+-]\\d{2}:\\d{2}", RegexOption.IGNORE_CASE).find(text)
            ?: return null
        val normalized = match.value.replace(Regex("\\s+"), " ")
        val formatter = DateTimeFormatter.ofPattern("MMM d, yyyy, h:mm:ss a 'GMT'xxx", Locale.US)
        return try { OffsetDateTime.parse(normalized, formatter).toInstant().toEpochMilli() } catch (_: Exception) { null }
    }

    companion object {
        private const val MAX_ENTRY_BYTES = 64L * 1024 * 1024
        private const val MAX_TOTAL_BYTES = 256L * 1024 * 1024
    }
}
