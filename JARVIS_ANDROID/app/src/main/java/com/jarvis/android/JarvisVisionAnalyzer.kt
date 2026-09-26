package com.jarvis.android

import android.graphics.ImageFormat
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy

class JarvisVisionAnalyzer(
    private val onFrameInfo: (String) -> Unit
) : ImageAnalysis.Analyzer {

    override fun analyze(image: ImageProxy) {
        val width = image.width
        val height = image.height
        val format = image.format

        if (format == ImageFormat.YUV_420_888) {
            onFrameInfo("VISIÓN ACTIVA • " + width + "x" + height)
        }

        image.close()
    }
}
