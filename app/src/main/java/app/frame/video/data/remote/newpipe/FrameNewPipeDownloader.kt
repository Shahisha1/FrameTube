package app.frame.video.data.remote.newpipe

import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/** Android HTTP transport for NewPipe Extractor. No cookies or account state are persisted. */
internal class FrameNewPipeDownloader : Downloader() {
    override fun execute(request: Request): Response {
        var connection: HttpURLConnection? = null
        try {
            connection = (URL(request.url()).openConnection() as HttpURLConnection).apply {
                connectTimeout = 15_000
                readTimeout = 30_000
                instanceFollowRedirects = true
                requestMethod = request.httpMethod()
                setRequestProperty("User-Agent", USER_AGENT)
                setRequestProperty("Accept-Language", "en-US,en;q=0.9")
                setRequestProperty("Accept", "*/*")
                request.headers()?.forEach { (name, values) -> if (values.isNotEmpty()) setRequestProperty(name, values.joinToString(",")) }
                request.dataToSend()?.let { body ->
                    doOutput = true
                    setFixedLengthStreamingMode(body.size)
                    outputStream.use { it.write(body) }
                }
            }
            val status = connection.responseCode
            val stream = if (status in 200..399) connection.inputStream else connection.errorStream
            val body = stream?.use(::readBody).orEmpty()
            val headers = connection.headerFields.filterKeys { it != null }.mapKeys { it.key!! }.mapValues { (_, values) -> values.orEmpty() }
            return Response(status, connection.responseMessage.orEmpty(), headers, body, connection.url.toString())
        } catch (error: IOException) {
            throw error
        } catch (error: Exception) {
            throw IOException("NewPipe HTTP request failed", error)
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
            if (output.size() > 12 * 1024 * 1024) throw IOException("Extractor response exceeded the size limit")
        }
        return output.toString(Charsets.UTF_8.name())
    }

    private companion object {
        const val USER_AGENT = "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36"
    }
}
