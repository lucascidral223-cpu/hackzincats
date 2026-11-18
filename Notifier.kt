package com.lucas.catsbot
import java.net.HttpURLConnection
import java.net.URL

object Notifier {
    fun send(topic: String, title: String, msg: String) = Thread {
        try {
            val c = URL("https://ntfy.sh/$topic").openConnection() as HttpURLConnection
            c.requestMethod = "POST"; c.doOutput = true
            c.setRequestProperty("Title", title)
            c.outputStream.use { it.write(msg.toByteArray()) }
            c.responseCode; c.disconnect()
        } catch (_: Exception) {}
    }.start()
}
