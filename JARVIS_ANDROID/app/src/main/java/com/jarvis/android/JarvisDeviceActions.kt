package com.jarvis.android

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.provider.Settings

class JarvisDeviceActions(private val context: Context) {
    fun openSettings() {
        context.startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    fun openBrowser() {
        context.startActivity(Intent(Intent.ACTION_VIEW).apply {
            data = android.net.Uri.parse("https://www.google.com")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }

    fun volumeUp() {
        val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audio.adjustVolume(AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI)
    }

    fun volumeDown() {
        val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audio.adjustVolume(AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI)
    }
}
