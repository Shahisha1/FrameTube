package app.frame.video.core.provider.newpipe

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * Small platform downloader for NewPipe Extractor.
 *
 * It honours extractor request headers and never stores cookies or account state.
 * Extraction calls run on Dispatchers.IO in NewPipeClient.
 */
internal class FrameNewPipeDownloader : Downloader() {
    override fun execute(request: Request): Response {
        var connection: HttpURLConnection? = null
        try {
            connection = (URL(request.url()).openConnection() as HttpURLConnection).apply {
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
                instanceFollowRedirects = true
                requestMethod = request.httpMethod()
                setRequestProperty("User-Agent", DEFAULT_USER_AGENT)
                setRequestProperty("Accept-Language", "en-US,en;q=0.9")
                request.headers()?.forEach { (name, values) ->
                    if (values.isNotEmpty()) setRequestProperty(name, values.joinToString(","))
                }
                request.dataToSend()?.let { body ->
                    doOutput = true
                    setFixedLengthStreamingMode(body.size)
                    outputStream.use { it.write(body) }
                }
            }

            val status = connection.responseCode
            val stream = if (status in 200..399) connection.inputStream else connection.errorStream
            val body = stream?.use(::readBody).orEmpty()
            val headers = connection.headerFields
                .filterKeys { it != null }
                .mapKeys { it.key!! }
                .mapValues { (_, values) -> values.orEmpty() }

            return Response(
                status,
                connection.responseMessage.orEmpty(),
                headers,
                body,
                connection.url.toString()
            )
        } catch (error: IOException) {
            throw error
        } catch (error: Exception) {
            throw IOException("NewPipe request failed", error)
        } finally {
            connection?.disconnect()
        }
    }

    private fun readBody(input: java.io.InputStream): String {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(8 * 1024)
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            output.write(buffer, 0, count)
            if (output.size() > MAX_RESPONSE_BYTES) throw IOException("Extractor response was too large")
        }
        return output.toString(Charsets.UTF_8.name())
    }

    private companion object {
        const val CONNECT_TIMEOUT_MS = 15_000
        const val READ_TIMEOUT_MS = 25_000
        const val MAX_RESPONSE_BYTES = 12 * 1024 * 1024
        const val DEFAULT_USER_AGENT = "Frame/0.1 (Android)"
    }
}
