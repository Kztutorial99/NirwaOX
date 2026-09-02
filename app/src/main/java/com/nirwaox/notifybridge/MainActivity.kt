package com.nirwaox.notifybridge

import android.content.Context
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

        val deviceId = Prefs.deviceId(this)
        findViewById<TextView>(R.id.tvDeviceId).text = deviceId

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
                val ok = Telegram.sendMessage(this, "✅ Tes koneksi dari <b>$deviceId</b>")
                runOnUiThread {
                    Toast.makeText(this, if (ok) "Terkirim ke Telegram" else "Gagal kirim — cek token/chat id", Toast.LENGTH_LONG).show()
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
        findViewById<TextView>(R.id.tvStatus).text = buildString {
            append(if (enabled) "Akses notifikasi: AKTIF ✅" else "Akses notifikasi: BELUM ⛔")
            append("\nForwarding: ")
            append(if (Prefs.isEnabled(this@MainActivity)) "AKTIF" else "NONAKTIF")
            append("\nBot: ")
            append(if (Prefs.botToken(this@MainActivity).isNotBlank()) "terkonfigurasi" else "kosong")
        }
        if (enabled) TelegramPollingService.start(this)
    }
}
