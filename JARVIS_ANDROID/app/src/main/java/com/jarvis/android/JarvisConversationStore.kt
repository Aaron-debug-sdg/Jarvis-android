package com.jarvis.android

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class JarvisMessage(
    val role: String,
    val text: String
)

class JarvisConversationStore(context: Context) {
    private val prefs = context.getSharedPreferences("jarvis_conversation", Context.MODE_PRIVATE)
    private val key = "messages"
    private val maxMessages = 20

    fun add(role: String, text: String) {
        val messages = load().toMutableList()
        messages.add(JarvisMessage(role, text.trim()))
        while (messages.size > maxMessages) messages.removeAt(0)
        save(messages)
    }

    fun recent(): List<JarvisMessage> = load()

    fun clear() {
        prefs.edit().remove(key).apply()
    }

    private fun load(): List<JarvisMessage> {
        val raw = prefs.getString(key, null) ?: return emptyList()
        return runCatching {
            val json = JSONArray(raw)
            buildList {
                for (i in 0 until json.length()) {
                    val item = json.getJSONObject(i)
                    add(JarvisMessage(item.optString("role"), item.optString("text")))
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun save(messages: List<JarvisMessage>) {
        val json = JSONArray()
        messages.forEach {
            json.put(JSONObject().put("role", it.role).put("text", it.text))
        }
        prefs.edit().putString(key, json.toString()).apply()
    }
}
