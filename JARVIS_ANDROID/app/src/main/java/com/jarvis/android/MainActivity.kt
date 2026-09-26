package com.jarvis.android

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.Locale

class MainActivity : ComponentActivity(), TextToSpeech.OnInitListener {
    private lateinit var tts: TextToSpeech
    private lateinit var state: TextView
    private lateinit var response: TextView
    private val cameraRequest = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        tts = TextToSpeech(this, this)
        state = findViewById(R.id.systemState)
        response = findViewById(R.id.responseText)
        findViewById<Button>(R.id.listenButton).setOnClickListener { startListening() }
        findViewById<Button>(R.id.cameraButton).setOnClickListener { prepareCamera() }
    }

    private fun startListening() {
        state.text = "LISTENING..."
        response.text = "Te escucho."
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-ES")
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Te escucho")
        }
        startActivityForResult(intent, 2001)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != 2001) return
        val command = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
        if (command.isNullOrBlank()) {
            state.text = "SYSTEM ONLINE"
            response.text = "No he detectado ninguna orden."
            speak("No he detectado ninguna orden.")
            return
        }
        state.text = "COMMAND RECEIVED"
        response.text = command
        processCommand(command.lowercase(Locale.ROOT))
    }

    private fun processCommand(command: String) {
        when {
            "hola" in command -> speak("Hola. JARVIS operativo.")
            "cómo estás" in command || "como estas" in command -> speak("Todos los sistemas funcionan correctamente.")
            "cámara" in command || "camara" in command -> prepareCamera()
            "jarvis" in command -> speak("A sus órdenes.")
            else -> speak("He recibido la orden: $command")
        }
    }

    private fun prepareCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), cameraRequest)
        } else {
            startActivity(Intent(this, CameraActivity::class.java))
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, results: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, results)
        if (requestCode == cameraRequest && results.firstOrNull() == PackageManager.PERMISSION_GRANTED) {
            startActivity(Intent(this, CameraActivity::class.java))
        }
    }

    override fun onInit(statusCode: Int) {
        if (statusCode == TextToSpeech.SUCCESS) tts.language = Locale("es", "ES")
    }

    private fun speak(text: String) {
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "jarvis")
    }

    override fun onDestroy() {
        tts.stop()
        tts.shutdown()
        super.onDestroy()
    }
}