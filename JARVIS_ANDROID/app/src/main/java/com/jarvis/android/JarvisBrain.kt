package com.jarvis.android

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class JarvisBrain(context: Context) {
    private val preferences = context.getSharedPreferences("jarvis_settings", Context.MODE_PRIVATE)

    suspend fun ask(prompt: String): String = withContext(Dispatchers.IO) {
        val endpoint = preferences.getString("ai_endpoint", null)
        val serverKey = preferences.getString("ai_server_key", "").orEmpty()

        if (endpoint.isNullOrBlank()) {
            return@withContext "Mi núcleo local está operativo. Configura un servidor de IA en Ajustes para activar la conversación inteligente."
        }

        runCatching {
            val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 10000
                readTimeout = 60000
                doOutput = true
                doInput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                setRequestProperty("Accept", "application/json")
                if (serverKey.isNotBlank()) setRequestProperty("X-Jarvis-Key", serverKey)
            }

            val payload = JSONObject().put("message", prompt).toString()
            connection.outputStream.use { it.write(payload.toByteArray(Charsets.UTF_8)) }

            val stream = if (connection.responseCode in 200..299) connection.inputStream else connection.errorStream
            val body = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            connection.disconnect()

            if (body.isBlank()) "El servicio de IA no ha devuelto una respuesta." else parseResponse(body)
        }.getOrElse {
            "No puedo contactar con mi cerebro de IA ahora mismo."
        }
    }

    private fun parseResponse(body: String): String {
        return runCatching {
            val json = JSONObject(body)
            listOf("reply", "response", "message", "text")
                .firstNotNullOfOrNull { key -> json.optString(key, null)?.takeIf { it.isNotBlank() } }
                ?: body
        }.getOrElse { body }
    }
}
