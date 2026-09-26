package com.jarvis.android

import android.content.Context

class JarvisMemory(context: Context) {
    private val prefs = context.getSharedPreferences("jarvis_memory", Context.MODE_PRIVATE)

    fun remember(key: String, value: String) {
        prefs.edit().putString(key.trim().lowercase(), value.trim()).apply()
    }

    fun recall(key: String): String? =
        prefs.getString(key.trim().lowercase(), null)

    fun clear() {
        prefs.edit().clear().apply()
    }

    fun entries(): Map<String, *> = prefs.all
}
