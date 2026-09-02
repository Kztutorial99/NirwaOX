package com.nirwaox.notifybridge

import android.content.Context
import org.json.JSONObject
import java.io.BufferedReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

object Telegram {

    private fun api(ctx: Context, method: String) =
        "https://api.telegram.org/bot${Prefs.botToken(ctx)}/$method"

    fun sendMessage(ctx: Context, text: String): Boolean {
        val token = Prefs.botToken(ctx)
        val chat = Prefs.chatId(ctx)
        if (token.isBlank() || chat.isBlank()) return false
        return try {
            val body = "chat_id=${URLEncoder.encode(chat, "UTF-8")}" +
                "&parse_mode=HTML&disable_web_page_preview=true" +
                "&text=${URLEncoder.encode(text.take(4000), "UTF-8")}"
            post(api(ctx, "sendMessage"), body) != null
        } catch (e: Exception) {
            false
        }
    }

    /** Long-poll updates. Returns raw JSON response or null. */
    fun getUpdates(ctx: Context, offset: Long, timeoutSec: Int = 30): JSONObject? {
        val token = Prefs.botToken(ctx)
        if (token.isBlank()) return null
        return try {
            val body = "offset=$offset&timeout=$timeoutSec&allowed_updates=%5B%22message%22%5D"
            post(api(ctx, "getUpdates"), body, readTimeoutMs = (timeoutSec + 15) * 1000)
        } catch (e: Exception) {
            null
        }
    }

    private fun post(urlStr: String, body: String, readTimeoutMs: Int = 20000): JSONObject? {
        val conn = URL(urlStr).openConnection() as HttpURLConnection
        return try {
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.connectTimeout = 15000
            conn.readTimeout = readTimeoutMs
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            conn.outputStream.use { it.write(body.toByteArray()) }
            val stream = if (conn.responseCode in 200..299) conn.inputStream else conn.errorStream
            val text = stream?.bufferedReader()?.use(BufferedReader::readText) ?: return null
            JSONObject(text)
        } catch (e: Exception) {
            null
        } finally {
            conn.disconnect()
        }
    }
}
