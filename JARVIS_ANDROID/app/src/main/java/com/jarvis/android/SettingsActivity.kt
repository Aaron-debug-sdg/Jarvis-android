package com.jarvis.android

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.ComponentActivity

class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val endpoint = findViewById<EditText>(R.id.aiEndpoint)
        val serverKey = findViewById<EditText>(R.id.aiServerKey)
        val prefs = getSharedPreferences("jarvis_settings", MODE_PRIVATE)

        endpoint.setText(prefs.getString("ai_endpoint", ""))
        serverKey.setText(prefs.getString("ai_server_key", ""))

        findViewById<Button>(R.id.saveSettings).setOnClickListener {
            prefs.edit()
                .putString("ai_endpoint", endpoint.text.toString().trim())
                .putString("ai_server_key", serverKey.text.toString())
                .apply()
            Toast.makeText(this, "Configuración de IA guardada", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}
