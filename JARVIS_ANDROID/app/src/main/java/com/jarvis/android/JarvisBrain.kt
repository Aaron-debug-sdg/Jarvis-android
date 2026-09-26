package com.jarvis.android

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

class JarvisBrain(private val endpoint: String? = null) {

    suspend fun ask(prompt: String): String = withContext(Dispatchers.IO) {
        if (endpoint.isNullOrBlank()) {
            return@withContext "Puedo procesar esta orden cuando conectemos el servicio de inteligencia de JARVIS."
        }

        runCatching {
            val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 10000
                readTimeout = 20000
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
            }

            val safePrompt = prompt.replace("\\", "\\\\").replace(""", "\"")
            connection.outputStream.use {
                it.write("""{"message":"$safePrompt"}""".toByteArray())
            }

            val body = connection.inputStream.bufferedReader().use { it.readText() }
            connection.disconnect()
            body
        }.getOrElse {
            "No puedo contactar con mi cerebro de IA ahora mismo."
        }
    }
}
