package com.jarvis.android

sealed class JarvisCommand {
    data object Time : JarvisCommand()
    data object Date : JarvisCommand()
    data object Camera : JarvisCommand()
    data object Vision : JarvisCommand()
    data class AskAI(val text: String) : JarvisCommand()
    data class Unknown(val text: String) : JarvisCommand()
}
