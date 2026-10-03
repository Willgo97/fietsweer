package nl.fietsweer.app.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.GZIPInputStream

object Net {

    // OpenStreetMap and Nominatim require an identifying User-Agent.
    private const val USER_AGENT =
        "Fietsweer/1.0 (Android; cycling weather app; " +
            "https://github.com/Willgo97/fietsweer)"

    suspend fun getText(url: String, timeoutMs: Int = 20_000): String =
        withContext(Dispatchers.IO) { blockingBytes(url, timeoutMs).toString(Charsets.UTF_8) }

    fun blockingBytes(url: String, timeoutMs: Int = 20_000): ByteArray {
        var connection: HttpURLConnection? = null
        try {
            connection = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = timeoutMs
                readTimeout = timeoutMs
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", USER_AGENT)
                setRequestProperty("Accept-Encoding", "gzip")
                setRequestProperty("Accept-Language", "nl,en;q=0.8")
            }
            val status = connection.responseCode
            if (status !in 200..299) {
                throw IOException("HTTP $status for $url")
            }
            val rawStream = connection.inputStream
            val stream =
                if (connection.contentEncoding.equals("gzip", true)) GZIPInputStream(rawStream) else rawStream
            val output = ByteArrayOutputStream(16 * 1024)
            val buffer = ByteArray(16 * 1024)
            stream.use {
                while (true) {
                    val bytesRead = it.read(buffer)
                    if (bytesRead < 0) break
                    output.write(buffer, 0, bytesRead)
                }
            }
            return output.toByteArray()
        } finally {
            connection?.disconnect()
        }
    }
}
