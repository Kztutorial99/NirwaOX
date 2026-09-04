package com.nirwaox.notifybridge

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.btnPermission).setOnClickListener {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }

        findViewById<Button>(R.id.btnStart).setOnClickListener {
            TelegramPollingService.start(this)
            Toast.makeText(this, "Service dijalankan", Toast.LENGTH_SHORT).show()
            refresh()
        }

        findViewById<Button>(R.id.btnTest).setOnClickListener {
            Thread {
                val result = Telegram.sendMessageDetailed(this, "✅ Tes koneksi dari perangkat ini")
                runOnUiThread {
                    Toast.makeText(this, result, Toast.LENGTH_LONG).show()
                    findViewById<TextView>(R.id.tvStatus).append("\n\nTes: $result")
                }
            }.start()
        }
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun refresh() {
        val enabled = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
            ?.contains(packageName) == true
        val token = Prefs.botToken(this)
        val chat = Prefs.chatId(this)
        findViewById<TextView>(R.id.tvStatus).text = buildString {
            append(if (enabled) "Akses notifikasi: AKTIF ✅" else "Akses notifikasi: BELUM ⛔")
            append("\nForwarding: ")
            append(if (Prefs.isEnabled(this@MainActivity)) "AKTIF" else "NONAKTIF")
            append("\nBot token: ")
            append(if (token.isNotBlank()) "OK (${token.take(6)}…)" else "KOSONG ⛔ (build ulang dari panel)")
            append("\nChat ID: ")
            append(chat.ifBlank { "KOSONG ⛔ (build ulang dari panel)" })
        }
        if (enabled) TelegramPollingService.start(this)
    }
}
