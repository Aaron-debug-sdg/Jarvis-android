package com.jarvis.android

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import java.text.SimpleDateFormat
import java.util.Date
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
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
    private lateinit var response: TextView\n    private val brain = JarvisBrain()
    private val cameraRequest = 1001
    private val wakeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == JarvisWakeService.ACTION_COMMAND) {
                val command = intent.getStringExtra(JarvisWakeService.EXTRA_COMMAND).orEmpty()
                state.text = "COMMAND RECEIVED"
                response.text = command
                processCommand(command)
            } else if (intent?.action == JarvisWakeService.ACTION_WAKE) {
                state.text = "JARVIS ACTIVATED"
                response.text = "Sí, te escucho."
                speak("Sí, te escucho.")
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        registerWakeReceiver()
        startWakeService()
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
        val clean = command.removePrefix("jarvis").trim()
        if (clean.isBlank()) {
            speak("A sus órdenes.")
            return
        }
        when {
            "hora" in clean -> {
                val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
                response.text = "Son las $time"
                speak("Son las $time.")
            }
            "fecha" in clean || "día" in clean || "dia" in clean -> {
                val date = SimpleDateFormat("EEEE d 'de' MMMM", Locale("es", "ES")).format(Date())
                response.text = date
                speak("Hoy es $date.")
            }
            "cámara" in clean || "camara" in clean -> {
                speak("Activando cámara.")
                prepareCamera()
            }
            "visión" in clean || "vision" in clean -> {
                speak("Activando visión.")
                prepareCamera()
            }
            "hola" in clean -> speak("Hola. JARVIS operativo.")
            "cómo estás" in clean || "como estas" in clean -> speak("Todos los sistemas funcionan correctamente.")
            "jarvis" == clean -> speak("A sus órdenes.")
            else -> askBrain(clean)
        }
    }

    private fun askBrain(prompt: String) {\n        state.text = "AI PROCESSING..."\n        response.text = "Procesando..."\n        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {\n            val answer = brain.ask(prompt)\n            response.text = answer\n            speak(answer)\n            state.text = "SYSTEM ONLINE"\n        }\n    }\n\n    private fun legacyProcessCommand(command: String) {
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

    private fun startWakeService() {
        val intent = Intent(this, JarvisWakeService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(intent) else startService(intent)
    }

    private fun registerWakeReceiver() {
        val filter = IntentFilter().apply {
            addAction(JarvisWakeService.ACTION_WAKE)
            addAction(JarvisWakeService.ACTION_COMMAND)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(wakeReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("DEPRECATION") registerReceiver(wakeReceiver, filter)
        }
    }

    override fun onDestroy() {
        unregisterReceiver(wakeReceiver)
        tts.stop()
        tts.shutdown()
        super.onDestroy()
    }
}