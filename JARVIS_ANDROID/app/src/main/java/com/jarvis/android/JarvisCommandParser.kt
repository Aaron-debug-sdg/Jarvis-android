package com.jarvis.android

class JarvisCommandParser {
    fun parse(input: String): JarvisCommand {
        val command = input.trim().lowercase()
        if (command.isBlank()) return JarvisCommand.Unknown("")

        return when {
            "hora" in command -> JarvisCommand.Time
            "fecha" in command || "día" in command || "dia" in command -> JarvisCommand.Date
            "cámara" in command || "camara" in command -> JarvisCommand.Camera
            "visión" in command || "vision" in command -> JarvisCommand.Vision
            else -> JarvisCommand.Unknown(command)
        }
    }
}
