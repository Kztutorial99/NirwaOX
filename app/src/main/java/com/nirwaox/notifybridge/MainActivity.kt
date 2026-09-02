package com.nirwaox.notifybridge

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val deviceId = Prefs.deviceId(this)
        findViewById<TextView>(R.id.tvDeviceId).text = deviceId

        val etToken = findViewById<EditText>(R.id.etToken)
        val etChatId = findViewById<EditText>(R.id.etChatId)
        etToken.setText(Prefs.botToken(this))
        etChatId.setText(Prefs.chatId(this))

        findViewById<Button>(R.id.btnSave).setOnClickListener {
            Prefs.setCredentials(this, etToken.text.toString().trim(), etChatId.text.toString().trim())
            Prefs.setOffset(this, 0L)
            TelegramPollingService.stop(this)
            TelegramPollingService.start(this)
            Toast.makeText(this, "Konfigurasi disimpan", Toast.LENGTH_SHORT).show()
            refresh()
        }

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
                val result = Telegram.sendMessageDetailed(this, "✅ Tes koneksi dari <b>$deviceId</b>")
                runOnUiThread {
                    Toast.makeText(this, result, Toast.LENGTH_LONG).show()
                    findViewById<TextView>(R.id.tvStatus).append("\n\nTes: $result")
                }
            }.start()
        }

        findViewById<Button>(R.id.btnCopy).setOnClickListener {
            val cm = getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            cm.setPrimaryClip(android.content.ClipData.newPlainText("device-id", deviceId))
            Toast.makeText(this, "ID disalin", Toast.LENGTH_SHORT).show()
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
        findViewById<TextView>(R.id.tvStatus).text = buildString {
            append(if (enabled) "Akses notifikasi: AKTIF ✅" else "Akses notifikasi: BELUM ⛔")
            append("\nForwarding: ")
            append(if (Prefs.isEnabled(this@MainActivity)) "AKTIF" else "NONAKTIF")
            append("\nBot token: ")
            append(if (token.isNotBlank()) "OK (${token.take(6)}…)" else "KOSONG ⛔")
            append("\nChat ID: ")
            append(Prefs.chatId(this@MainActivity).ifBlank { "KOSONG ⛔" })
        }
        if (enabled) TelegramPollingService.start(this)
    }
}
