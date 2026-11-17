package app.frame.video.data.importer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class TakeoutParserTest {
    @Test
    fun parsesSubscriptionsPlaylistEntriesWatchAndSearchActivity() {
        val html = "<html><body>" +
            "<div class='outer-cell'><div class='content-cell'>Watched <a href='https://www.youtube.com/watch?v=video123456'>A watched video</a><br>Jan 2, 2026, 3:04:05 PM GMT+05:45</div></div>" +
            "<div class='outer-cell'><div class='content-cell'>Searched for <a href='https://www.youtube.com/results?search_query=frame'>frame</a><br>Jan 2, 2026, 2:04:05 PM GMT+05:45</div></div>" +
            "</body></html>"
        val bytes = ByteArrayOutputStream().also { output ->
            ZipOutputStream(output).use { zip ->
                zip.addText("Takeout/YouTube and YouTube Music/subscriptions/subscriptions.csv",
                    "Channel Id,Channel Url,Channel Title\nUCabc123456,http://www.youtube.com/channel/UCabc123456,Example Channel\n")
                zip.addText("Takeout/YouTube and YouTube Music/playlists/playlists.csv",
                    "Playlist ID,Playlist Title (Original),Playlist Visibility\nPLabc123456,Example List,Private\n")
                zip.addText("Takeout/YouTube and YouTube Music/playlists/Example List-videos.csv",
                    "Video ID,Playlist Video Creation Timestamp\n abcDEF12345 ,2026-01-01T10:00:00+00:00\n")
                zip.addText("Takeout/YouTube and YouTube Music/history/search-history.html", html)
            }
        }.toByteArray()

        val parsed = TakeoutParser().parse(ByteArrayInputStream(bytes))
        assertEquals(1, parsed.subscriptions.size)
        assertEquals(1, parsed.playlists.size)
        assertEquals(1, parsed.entries.size)
        assertEquals("abcDEF12345", parsed.entries.first().videoId)
        assertEquals(1, parsed.history.size)
        assertEquals("video123456", parsed.history.first().videoId)
        assertEquals(1, parsed.searches.size)
        assertEquals("frame", parsed.searches.first().query)
        assertTrue(parsed.result.issues.isEmpty())
    }

    private fun ZipOutputStream.addText(name: String, value: String) {
        putNextEntry(ZipEntry(name))
        write(value.toByteArray(Charsets.UTF_8))
        closeEntry()
    }
}
