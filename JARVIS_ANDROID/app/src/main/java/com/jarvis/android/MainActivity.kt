package com.jarvis.android

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.widget.Button
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity(), TextToSpeech.OnInitListener {
    private lateinit var tts: TextToSpeech
    private lateinit var state: TextView
    private lateinit var response: TextView
    private val brain by lazy { JarvisBrain(this) }
    private val cameraRequest = 1001
    private val microphoneRequest = 1002
    private val notificationRequest = 1003

    private val wakeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                JarvisWakeService.ACTION_COMMAND -> {
                    val command = intent.getStringExtra(JarvisWakeService.EXTRA_COMMAND).orEmpty()
                    state.text = "COMMAND RECEIVED"
                    response.text = command
                    processCommand(command)
                }
                JarvisWakeService.ACTION_WAKE -> {
                    state.text = "JARVIS ACTIVATED"
                    response.text = "Sí, te escucho."
                    speak("Sí, te escucho.")
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        state = findViewById(R.id.systemState)
        response = findViewById(R.id.responseText)
        tts = TextToSpeech(this, this)

        findViewById<Button>(R.id.listenButton).setOnClickListener { startListening() }
        findViewById<Button>(R.id.cameraButton).setOnClickListener { prepareCamera() }
        findViewById<Button>(R.id.settingsButton).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        registerWakeReceiver()
        prepareVoiceService()
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

    @Deprecated("Android speech activity result API retained for compatibility")
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
            "cómo estás" in clean || "como estas" in clean ->
                speak("Todos los sistemas funcionan correctamente.")
            "jarvis" == clean -> speak("A sus órdenes.")
            else -> askBrain(clean)
        }
    }

    private fun askBrain(prompt: String) {
        state.text = "AI PROCESSING..."
        response.text = "Procesando..."
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
            val answer = brain.ask(prompt)
            response.text = answer
            speak(answer)
            state.text = "SYSTEM ONLINE"
        }
    }

    private fun prepareCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), cameraRequest)
        } else {
            startActivity(Intent(this, CameraActivity::class.java))
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        results: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, results)
        if (requestCode == cameraRequest && results.firstOrNull() == PackageManager.PERMISSION_GRANTED) {
            startActivity(Intent(this, CameraActivity::class.java))
        } else if (requestCode == microphoneRequest && results.firstOrNull() == PackageManager.PERMISSION_GRANTED) {
            prepareVoiceService()
        }
    }

    override fun onInit(statusCode: Int) {
        if (statusCode == TextToSpeech.SUCCESS) {
            tts.language = Locale("es", "ES")
        }
    }

    private fun speak(text: String) {
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "jarvis")
    }

    private fun prepareVoiceService() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), microphoneRequest)
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), notificationRequest)
        }

        startWakeService()
    }

    private fun startWakeService() {
        val intent = Intent(this, JarvisWakeService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    private fun registerWakeReceiver() {
        val filter = IntentFilter().apply {
            addAction(JarvisWakeService.ACTION_WAKE)
            addAction(JarvisWakeService.ACTION_COMMAND)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(wakeReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("DEPRECATION")
            registerReceiver(wakeReceiver, filter)
        }
    }

    override fun onDestroy() {
        runCatching { unregisterReceiver(wakeReceiver) }
        tts.stop()
        tts.shutdown()
        super.onDestroy()
    }
}
