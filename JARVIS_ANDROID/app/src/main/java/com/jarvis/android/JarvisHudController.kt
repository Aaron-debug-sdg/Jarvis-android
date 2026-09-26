package com.jarvis.android

import android.animation.ObjectAnimator
import android.view.View
import android.view.animation.LinearInterpolator

class JarvisHudController(private val ring: View) {
    private var animator: ObjectAnimator? = null

    fun start() {
        if (animator?.isRunning == true) return
        animator = ObjectAnimator.ofFloat(ring, View.ROTATION, 0f, 360f).apply {
            duration = 5000
            repeatCount = ObjectAnimator.INFINITE
            interpolator = LinearInterpolator()
            start()
        }
    }

    fun stop() {
        animator?.cancel()
        animator = null
        ring.rotation = 0f
    }

    fun pulse() {
        ring.animate().scaleX(1.08f).scaleY(1.08f).setDuration(220).withEndAction {
            ring.animate().scaleX(1f).scaleY(1f).setDuration(220).start()
        }.start()
    }
}
