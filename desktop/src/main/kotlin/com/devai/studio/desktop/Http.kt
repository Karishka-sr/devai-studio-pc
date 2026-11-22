package com.devai.studio.desktop

import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class HttpException(val code: Int, val body: String) : Exception("HTTP $code: $body")

object Http {
    private const val USER_AGENT = "DevAI-Studio-Desktop/2.12"

    fun get(url: String, headers: Map<String, String> = emptyMap(), timeoutSec: Int = 30): String =
        request(url, "GET", headers, null, timeoutSec)

    fun postJson(url: String, json: JSONObject, headers: Map<String, String> = emptyMap(), timeoutSec: Int = 120): String =
        request(url, "POST", headers + mapOf("Content-Type" to "application/json"), json.toString(), timeoutSec)

    fun request(
        url: String,
        method: String,
        headers: Map<String, String>,
        body: String?,
        timeoutSec: Int,
    ): String {
        val conn = open(url, method, headers, timeoutSec)
        try {
            if (body != null) {
                conn.doOutput = true
                conn.outputStream.use { it.write(body.toByteArray(StandardCharsets.UTF_8)) }
            }
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = stream?.use { readAll(it) }.orEmpty()
            if (code !in 200..299) throw HttpException(code, text)
            return text
        } finally {
            conn.disconnect()
        }
    }

    /**
     * Opens an SSE connection and gives every line to [onLine].
     * The caller must close the returned connection.
     */
    fun stream(
        url: String,
        method: String,
        headers: Map<String, String>,
        body: String?,
        timeoutSec: Int,
        onLine: (String) -> Unit,
    ): Int {
        val conn = open(url, method, headers, timeoutSec)
        try {
            if (body != null) {
                conn.doOutput = true
                conn.outputStream.use { it.write(body.toByteArray(StandardCharsets.UTF_8)) }
            }
            val code = conn.responseCode
            if (code !in 200..299) {
                val error = conn.errorStream?.use { readAll(it) }.orEmpty()
                throw HttpException(code, error)
            }
            BufferedReader(InputStreamReader(conn.inputStream, StandardCharsets.UTF_8)).use { reader ->
                while (true) {
                    val line = reader.readLine() ?: break
                    onLine(line)
                }
            }
            return code
        } finally {
            conn.disconnect()
        }
    }

    fun encode(value: String): String = URLEncoder.encode(value, "UTF-8")

    private fun open(url: String, method: String, headers: Map<String, String>, timeoutSec: Int): HttpURLConnection {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.requestMethod = method
        conn.connectTimeout = 20_000
        conn.readTimeout = timeoutSec.coerceIn(15, 600) * 1000
        conn.instanceFollowRedirects = true
        conn.setRequestProperty("User-Agent", USER_AGENT)
        headers.forEach { (k, v) -> conn.setRequestProperty(k, v) }
        return conn
    }

    private fun readAll(input: InputStream): String =
        input.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
}

fun JSONObject.text(key: String): String {
    if (!has(key) || isNull(key)) return ""
    return optString(key, "")
}
