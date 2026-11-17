package app.frame.video.core.provider.piped

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PipedTest {
    private val sample = """
        {"title":"Sample","uploader":"Someone","duration":125,"livestream":false,"hls":null,"dash":"https://p.example/dash.mpd",
         "videoStreams":[
           {"url":"https://p.example/v1","mimeType":"video/mp4","videoOnly":true,"height":1080},
           {"url":"https://p.example/v2","mimeType":"video/mp4","videoOnly":false,"height":360},
           {"url":"https://p.example/v3","mimeType":"video/mp4","videoOnly":false,"height":720}]}
    """.trimIndent()

    @Test fun parsesFieldsAndTreatsJsonNullAsMissing() {
        val s = PipedParser.parseStreams(sample)
        assertEquals("Sample", s.title)
        assertEquals(125L, s.durationSeconds)
        assertNull(s.hlsUrl)
        assertEquals("https://p.example/dash.mpd", s.dashUrl)
        assertEquals(3, s.videoStreams.size)
    }

    @Test fun choosesBestMuxedStreamNotVideoOnly() {
        val p = PipedParser.choose(PipedParser.parseStreams(sample))
        assertTrue(p is PipedPlayable.Progressive)
        assertEquals("https://p.example/v3", p!!.url)
    }

    @Test fun prefersHlsWhenPresentAndFallsBackToDash() {
        val hls = PipedParser.choose(PipedParser.parseStreams("""{"hls":"https://p.example/live.m3u8","videoStreams":[]}"""))
        assertTrue(hls is PipedPlayable.Hls)
        val dash = PipedParser.choose(PipedParser.parseStreams("""{"dash":"https://p.example/d.mpd","videoStreams":[{"url":"u","videoOnly":true,"height":1080}]}"""))
        assertTrue(dash is PipedPlayable.Dash)
    }

    @Test fun nothingPlayableReturnsNull() {
        assertNull(PipedParser.choose(PipedParser.parseStreams("""{"videoStreams":[{"url":"u","videoOnly":true,"height":720}]}""")))
    }

    @Test fun classifiesErrors() {
        assertEquals(PipedFailureKind.NOT_FOUND, PipedParser.classify(404, null))
        assertEquals(PipedFailureKind.NOT_FOUND, PipedParser.classify(500, "This video has been removed"))
        assertEquals(PipedFailureKind.RESTRICTED, PipedParser.classify(500, "This video is private"))
        assertEquals(PipedFailureKind.SERVER, PipedParser.classify(500, "boom"))
    }

    @Test fun readsErrorMessage() {
        assertEquals("nope", PipedParser.errorMessage("""{"error":"x","message":"nope"}"""))
        assertNull(PipedParser.errorMessage("not json"))
        assertNull(PipedParser.errorMessage(null))
    }

    @Test fun normalizesInstanceUrl() {
        assertEquals("https://pipedapi.example.org", PipedApi.normalizeInstanceUrl(" https://pipedapi.example.org/ "))
        assertNull(PipedApi.normalizeInstanceUrl("http://pipedapi.example.org"))
        assertNull(PipedApi.normalizeInstanceUrl("pipedapi.example.org"))
        assertNull(PipedApi.normalizeInstanceUrl("https://"))
        assertNull(PipedApi.normalizeInstanceUrl("https://a.example/?x=1"))
    }
}
