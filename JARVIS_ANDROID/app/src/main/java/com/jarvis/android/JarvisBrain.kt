package com.jarvis.android

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class JarvisBrain(private val endpoint: String? = null) {

    suspend fun ask(prompt: String): String = withContext(Dispatchers.IO) {
        if (endpoint.isNullOrBlank()) {
            return@withContext "Todavía no tengo conectado un servicio de IA externo. Mi núcleo local está operativo."
        }

        runCatching {
            val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 10000
                readTimeout = 20000
                doOutput = true
                doInput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                setRequestProperty("Accept", "application/json")
            }

            val payload = JSONObject().put("message", prompt).toString()
            connection.outputStream.use { it.write(payload.toByteArray(Charsets.UTF_8)) }

            val stream = if (connection.responseCode in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream
            }

            val body = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            connection.disconnect()

            if (body.isBlank()) {
                "El servicio de IA no ha devuelto una respuesta."
            } else {
                body
            }
        }.getOrElse {
            "No puedo contactar con mi cerebro de IA ahora mismo."
        }
    }
}
