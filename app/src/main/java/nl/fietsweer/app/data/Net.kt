package nl.fietsweer.app.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.GZIPInputStream

/**
 * Small blocking-IO HTTP helper. The app makes a handful of GET requests per
 * refresh, so a dedicated client library would be more machinery than value.
 */
object Net {

    /**
     * OpenStreetMap and Nominatim both require a real identifying User-Agent.
     */
    const val USER_AGENT =
        "Fietsweer/1.0 (Android; cycling weather app; " +
            "https://github.com/Willgo97/fietsweer)"

    suspend fun getText(url: String, timeoutMs: Int = 20_000): String =
        withContext(Dispatchers.IO) { blockingText(url, timeoutMs) }

    suspend fun getBytes(url: String, timeoutMs: Int = 20_000): ByteArray =
        withContext(Dispatchers.IO) { blockingBytes(url, timeoutMs) }

    fun blockingText(url: String, timeoutMs: Int = 20_000): String =
        blockingBytes(url, timeoutMs).toString(Charsets.UTF_8)

    fun blockingBytes(url: String, timeoutMs: Int = 20_000): ByteArray {
        var conn: HttpURLConnection? = null
        try {
            conn = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = timeoutMs
                readTimeout = timeoutMs
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", USER_AGENT)
                setRequestProperty("Accept-Encoding", "gzip")
                setRequestProperty("Accept-Language", "nl,en;q=0.8")
            }
            val code = conn.responseCode
            if (code !in 200..299) {
                throw HttpError(code, "HTTP $code for $url")
            }
            val raw = conn.inputStream
            val stream = if (conn.contentEncoding.equals("gzip", true)) GZIPInputStream(raw) else raw
            val out = ByteArrayOutputStream(16 * 1024)
            val buf = ByteArray(16 * 1024)
            stream.use {
                while (true) {
                    val n = it.read(buf)
                    if (n < 0) break
                    out.write(buf, 0, n)
                }
            }
            return out.toByteArray()
        } finally {
            conn?.disconnect()
        }
    }

    class HttpError(val code: Int, message: String) : RuntimeException(message)
}
