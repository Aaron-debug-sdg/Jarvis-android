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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity(), TextToSpeech.OnInitListener {
    private lateinit var tts: TextToSpeech
    private lateinit var state: TextView
    private lateinit var response: TextView
    private lateinit var hud: JarvisHudController
    private val brain by lazy { JarvisBrain(this) }
    private val memory by lazy { JarvisMemory(this) }
    private val conversation by lazy { JarvisConversationStore(this) }
    private val parser = JarvisCommandParser()
    private val deviceActions by lazy { JarvisDeviceActions(this) }
    private var aiJob: Job? = null

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
                    hud.pulse()
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
        hud = JarvisHudController(findViewById(R.id.arcCore))
        hud.start()
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
        hud.pulse()
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
        processCommand(command)
    }

    private fun processCommand(rawCommand: String) {
        val clean = rawCommand.trim().removePrefix("JARVIS").removePrefix("jarvis").trim()
        if (clean.isBlank()) {
            speak("A sus órdenes.")
            return
        }

        conversation.add("user", clean)
        when (val parsed = parser.parse(clean)) {
            JarvisCommand.Time -> {
                val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
                answer("Son las $time.")
            }
            JarvisCommand.Date -> {
                val date = SimpleDateFormat("EEEE d 'de' MMMM", Locale("es", "ES")).format(Date())
                answer("Hoy es $date.")
            }
            JarvisCommand.Camera, JarvisCommand.Vision -> {
                answer("Activando visión.")
                prepareCamera()
            }
            is JarvisCommand.Unknown -> processSpecialCommand(parsed.text)
            is JarvisCommand.AskAI -> askBrain(parsed.text)
        }
    }

    private fun processSpecialCommand(command: String) {
        when {
            command == "hola" -> answer("Hola. JARVIS operativo.")
            command == "cómo estás" || command == "como estas" ->
                answer("Todos los sistemas funcionan correctamente.")
            command == "abre ajustes" || command == "abre configuración" ->
                answer("Abriendo ajustes del dispositivo.") { deviceActions.openSettings() }
            command == "abre navegador" || command == "abre internet" ->
                answer("Abriendo navegador.") { deviceActions.openBrowser() }
            command == "sube el volumen" || command == "sube volumen" ->
                answer("Subiendo volumen.") { deviceActions.volumeUp() }
            command == "baja el volumen" || command == "baja volumen" ->
                answer("Bajando volumen.") { deviceActions.volumeDown() }
            command.startsWith("recuerda ") && command.contains(" es ") -> {
                val parts = command.removePrefix("recuerda ").split(" es ", limit = 2)
                memory.remember(parts[0], parts[1])
                answer("Lo recordaré.")
            }
            command.startsWith("recuerda que ") -> {
                memory.remember("nota_" + System.currentTimeMillis(), command.removePrefix("recuerda que "))
                answer("Guardado en mi memoria.")
            }
            command.startsWith("qué sabes de ") -> {
                val key = command.removePrefix("qué sabes de ").trim()
                val value = memory.recall(key)
                if (value.isNullOrBlank()) askBrain(command) else answer(value)
            }
            command == "qué recuerdas" || command == "que recuerdas" -> {
                val notes = memory.entries().entries.joinToString(". ") { "${it.key}: ${it.value}" }
                answer(if (notes.isBlank()) "Mi memoria local está vacía." else notes)
            }
            command == "borra memoria" || command == "olvida todo" -> {
                memory.clear()
                conversation.clear()
                answer("Memoria local borrada.")
            }
            else -> askBrain(command)
        }
    }

    private fun askBrain(prompt: String) {
        aiJob?.cancel()
        state.text = "AI PROCESSING..."
        response.text = "Procesando..."
        hud.pulse()
        aiJob = CoroutineScope(Dispatchers.Main).launch {
            val history = conversation.recent().takeLast(10)
            val contextPrompt = if (history.isEmpty()) {
                prompt
            } else {
                history.joinToString("\n") { "${it.role}: ${it.text}" } + "\nuser: " + prompt
            }
            val answer = brain.ask(contextPrompt)
            conversation.add("assistant", answer)
            response.text = answer
            speak(answer)
            state.text = "SYSTEM ONLINE"
        }
    }

    private fun answer(text: String, action: (() -> Unit)? = null) {
        response.text = text
        conversation.add("assistant", text)
        speak(text)
        action?.invoke()
        state.text = "SYSTEM ONLINE"
        hud.pulse()
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
        if (statusCode == TextToSpeech.SUCCESS) tts.language = Locale("es", "ES")
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
            @Suppress("DEPRECATION")
            registerReceiver(wakeReceiver, filter)
        }
    }

    override fun onDestroy() {
        aiJob?.cancel()
        runCatching { unregisterReceiver(wakeReceiver) }
        hud.stop()
        tts.stop()
        tts.shutdown()
        super.onDestroy()
    }
}
