package app.frame.video.data.download

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class DirectDownloadPolicyTest {
    @Test
    fun acceptsSupportedDirectHttpsVideoFiles() {
        val file = DirectDownloadPolicy.validate(" https://media.example.org/cats/video.MP4?token=abc ")
        assertEquals("video/mp4", file.mimeType)
        assertEquals("video.MP4", file.fileName)
        assertEquals("https://media.example.org/cats/video.MP4?token=abc", file.url)
    }

    @Test
    fun acceptsWebmAndMkvMimeTypes() {
        assertEquals("video/webm", DirectDownloadPolicy.validate("https://media.example.org/a.webm").mimeType)
        assertEquals("video/x-matroska", DirectDownloadPolicy.validate("https://media.example.org/a.mkv").mimeType)
    }

    @Test
    fun rejectsNonHttpsLinks() {
        assertThrows(IllegalArgumentException::class.java) {
            DirectDownloadPolicy.validate("http://media.example.org/video.mp4")
        }
    }

    @Test
    fun rejectsYoutubePagesAndCdnHosts() {
        listOf(
            "https://youtube.com/watch?v=abcdefghijk",
            "https://m.youtube.com/video.mp4",
            "https://youtu.be/abcdefghijk",
            "https://r1---sn.googlevideo.com/video.mp4",
            "https://youtube-nocookie.com/video.mp4",
            "https://youtube.com./video.mp4"
        ).forEach { url ->
            assertThrows(IllegalArgumentException::class.java) { DirectDownloadPolicy.validate(url) }
        }
    }

    @Test
    fun rejectsUnsupportedExtensionsAndInvalidHosts() {
        assertThrows(IllegalArgumentException::class.java) {
            DirectDownloadPolicy.validate("https://media.example.org/audio.mp3")
        }
        assertThrows(IllegalArgumentException::class.java) {
            DirectDownloadPolicy.validate("https:///video.mp4")
        }
        assertThrows(IllegalArgumentException::class.java) {
            DirectDownloadPolicy.validate("//media.example.org/video.mp4")
        }
    }
}
