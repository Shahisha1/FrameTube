package app.frame.video.data.importer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.time.OffsetDateTime
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class TakeoutParserEdgeCasesTest {
    private fun zip(vararg files: Pair<String, String>): ByteArrayInputStream {
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { z -> files.forEach { (n, c) -> z.putNextEntry(ZipEntry(n)); z.write(c.toByteArray()); z.closeEntry() } }
        return ByteArrayInputStream(out.toByteArray())
    }
    private val root = "Takeout/YouTube and YouTube Music/"

    @Test fun csvWithBomCrlfAndQuotedCommas() {
        val csv = "﻿Channel Id,Channel Url,Channel Title\r\nUC111111,http://x/UC111111,\"Smith, \"\"Bob\"\" & Co\"\r\n"
        val p = TakeoutParser().parse(zip(root + "subscriptions/subscriptions.csv" to csv))
        assertEquals(1, p.subscriptions.size)
        assertEquals("Smith, \"Bob\" & Co", p.channels.first().title)
    }

    @Test fun narrowNoBreakSpaceBeforePmStillParsesDate() {
        val html = "<div class='outer-cell'><div class='content-cell'>Watched <a href='https://www.youtube.com/watch?v=abcdefghijk'>T</a><br>Jan 2, 2026, 3:04:05 PM GMT+05:45</div></div>"
        val p = TakeoutParser().parse(zip(root + "history/watch-history.html" to html))
        val expected = OffsetDateTime.parse("2026-01-02T15:04:05+05:45").toInstant().toEpochMilli()
        assertEquals(expected, p.history.single().watchedAt)
    }

    @Test fun undatedWatchRecordsAreReportedNotHidden() {
        val html = "<div class='outer-cell'><div class='content-cell'>Watched <a href='https://www.youtube.com/watch?v=abcdefghijk'>T</a><br>no date here</div></div>"
        val p = TakeoutParser().parse(zip(root + "history/watch-history.html" to html))
        assertEquals(1, p.history.size)
        assertTrue(p.result.issues.any { "unreadable date" in it })
    }

    @Test fun historyTitleReplacesPlaylistPlaceholder() {
        val html = "<div class='outer-cell'><div class='content-cell'>Watched <a href='https://www.youtube.com/watch?v=abcdefghijk'>Real Title</a><br>Jan 2, 2026, 3:04:05 PM GMT+05:45</div></div>"
        val p = TakeoutParser().parse(zip(
            root + "playlists/playlists.csv" to "Playlist ID,Playlist Title (Original)\nPL1,Mix\n",
            root + "playlists/Mix-videos.csv" to "Video ID,Playlist Video Creation Timestamp\nabcdefghijk,2026-01-01T10:00:00+00:00\n",
            root + "history/watch-history.html" to html
        ))
        assertEquals("Real Title", p.videos.single().title)
    }

    @Test fun similarPlaylistFileNamesDoNotMix() {
        val p = TakeoutParser().parse(zip(
            root + "playlists/playlists.csv" to "Playlist ID,Playlist Title (Original)\nPL1,List\nPL2,My List\n",
            root + "playlists/My List-videos.csv" to "Video ID,Playlist Video Creation Timestamp\nmmmmmmmmmmm,2026-01-01T10:00:00+00:00\n",
            root + "playlists/List-videos.csv" to "Video ID,Playlist Video Creation Timestamp\nllllllllllll,2026-01-01T10:00:00+00:00\n"
        ))
        assertEquals("PL1", p.entries.single { it.videoId == "llllllllllll" }.playlistId)
        assertEquals("PL2", p.entries.single { it.videoId == "mmmmmmmmmmm" }.playlistId)
    }

    @Test fun reportsFilesFoundAndIgnoresUnknownFiles() {
        val p = TakeoutParser().parse(zip(
            root + "subscriptions/subscriptions.csv" to "Channel Id,Channel Url,Channel Title\nUC1,u,T\n",
            "Takeout/Other/unrelated.csv" to "a,b\n1,2\n"
        ))
        assertEquals(1, p.result.filesFound.size)
    }

    @Test fun malformedHtmlDoesNotCrash() {
        val p = TakeoutParser().parse(zip(root + "history/search-history.html" to "<<<not html"))
        assertEquals(0, p.history.size)
    }

    @Test(expected = IllegalStateException::class)
    fun cancellationStopsParsing() {
        TakeoutParser().parse(zip(root + "subscriptions/subscriptions.csv" to "Channel Id\nUC1\n")) { throw IllegalStateException("cancelled") }
    }
}
