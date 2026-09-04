package com.nirwaos.notifybridge

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.app.Service
import android.content.pm.ApplicationInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONObject

/**
 * Foreground service that long-polls Telegram getUpdates and executes commands
 * addressed to THIS device only (matched by its unique NIR-XXX-XXX-XXX id).
 */
class TelegramPollingService : Service() {

    private var job: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    companion object {
        private const val CHANNEL_ID = "nirwaos_service"
        private const val NOTIF_ID = 1001

        fun start(ctx: Context) {
            val intent = Intent(ctx, TelegramPollingService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ctx.startForegroundService(intent)
            } else {
                ctx.startService(intent)
            }
        }

        fun stop(ctx: Context) {
            ctx.stopService(Intent(ctx, TelegramPollingService::class.java))
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startForeground(NOTIF_ID, buildNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (job?.isActive != true) {
            job = scope.launch { pollLoop() }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        job?.cancel()
        super.onDestroy()
    }

    private fun buildNotification(): Notification {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(CHANNEL_ID, "NirwaOS Service", NotificationManager.IMPORTANCE_MIN)
            ch.setShowBadge(false)
            nm.createNotificationChannel(ch)
        }
        val pi = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            Notification.Builder(this, CHANNEL_ID) else Notification.Builder(this)
        return builder
            .setContentTitle("NirwaOS aktif")
            .setContentText(Prefs.deviceId(this))
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setOngoing(true)
            .setContentIntent(pi)
            .build()
    }

    private suspend fun pollLoop() {
        val ctx = applicationContext
        while (scope.isActive) {
            val res = Telegram.getUpdates(ctx, Prefs.offset(ctx))
            if (res == null || !res.optBoolean("ok", false)) {
                delay(5000)
                continue
            }
            val updates = res.optJSONArray("result") ?: continue
            for (i in 0 until updates.length()) {
                val upd = updates.optJSONObject(i) ?: continue
                Prefs.setOffset(ctx, upd.optLong("update_id") + 1)
                handleUpdate(upd)
            }
        }
    }

    private fun handleUpdate(upd: JSONObject) {
        val ctx = applicationContext
        val msg = upd.optJSONObject("message") ?: return
        val chatId = msg.optJSONObject("chat")?.optString("id") ?: return
        if (chatId != Prefs.chatId(ctx)) return

        val raw = msg.optString("text").trim()
        if (raw.isEmpty() || !raw.startsWith("/")) return

        val myId = Prefs.deviceId(ctx)
        // Scope routing: a command may target one device, e.g. "/status NIR-ABC-DEF-GHI"
        val targets = Regex("NIR-[A-Z0-9]{3}-[A-Z0-9]{3}-[A-Z0-9]{3}").findAll(raw.uppercase())
            .map { it.value }.toList()
        if (targets.isNotEmpty() && !targets.contains(myId)) return

        val cleaned = raw.replace(Regex("NIR-[A-Za-z0-9]{3}-[A-Za-z0-9]{3}-[A-Za-z0-9]{3}"), "").trim()
        val parts = cleaned.split(Regex("\\s+")).filter { it.isNotBlank() }
        val cmd = parts.firstOrNull()?.lowercase()?.substringBefore("@") ?: return
        val arg = parts.drop(1).joinToString(" ").trim()

        when (cmd) {
            "/start", "/help" -> reply(help(myId))
            "/id", "/ping" -> reply("🟢 <b>$myId</b> online.")
            "/status" -> reply(status(myId))
            "/on" -> { Prefs.setEnabled(ctx, true); reply("✅ <b>$myId</b> forwarding <b>AKTIF</b>.") }
            "/off" -> { Prefs.setEnabled(ctx, false); reply("⛔ <b>$myId</b> forwarding <b>NONAKTIF</b>.") }
            "/apps" -> reply(appList(myId))
            "/muted" -> {
                val m = Prefs.mutedPackages(ctx)
                reply("🔇 <b>$myId</b> muted (${m.size}):\n" + (if (m.isEmpty()) "-" else m.joinToString("\n") { "• <code>$it</code>" }))
            }
            "/mute" -> {
                if (arg.isBlank()) reply("Format: <code>/mute com.package.name $myId</code>")
                else { Prefs.mute(ctx, arg); reply("🔇 <b>$myId</b> mute <code>$arg</code>") }
            }
            "/unmute" -> {
                if (arg.isBlank()) reply("Format: <code>/unmute com.package.name $myId</code>")
                else { Prefs.unmute(ctx, arg); reply("🔔 <b>$myId</b> unmute <code>$arg</code>") }
            }
            "/muteall" -> {
                val all = installedApps().map { it.first }.toSet()
                Prefs.setMuted(ctx, all)
                reply("🔇 <b>$myId</b> semua app di-mute (${all.size}).")
            }
            "/unmuteall" -> { Prefs.setMuted(ctx, emptySet()); reply("🔔 <b>$myId</b> semua app di-unmute.") }
        }
    }

    private fun help(myId: String) = """
        <b>NirwaOS — $myId</b>
        Tambahkan ID device di akhir perintah agar hanya perangkat itu yang merespons.

        /status — status perangkat
        /ping — cek online
        /on — aktifkan forwarding
        /off — matikan forwarding
        /apps — daftar aplikasi + status
        /mute &lt;package&gt; — matikan notif app
        /unmute &lt;package&gt; — nyalakan notif app
        /muted — daftar app yang di-mute
        /muteall, /unmuteall
        /help — bantuan

        Contoh: <code>/mute com.whatsapp $myId</code>
    """.trimIndent()

    private fun status(myId: String): String {
        val ctx = applicationContext
        return """
            <b>📟 $myId</b>
            Forwarding: ${if (Prefs.isEnabled(ctx)) "AKTIF ✅" else "NONAKTIF ⛔"}
            Muted app: ${Prefs.mutedPackages(ctx).size}
            Device: ${Build.MANUFACTURER} ${Build.MODEL} (Android ${Build.VERSION.RELEASE})
        """.trimIndent()
    }

    private fun installedApps(): List<Pair<String, String>> {
        val pm = packageManager
        return pm.getInstalledApplications(0)
            .filter { it.packageName != packageName }
            .filter { (it.flags and ApplicationInfo.FLAG_SYSTEM) == 0 || pm.getLaunchIntentForPackage(it.packageName) != null }
            .map { it.packageName to pm.getApplicationLabel(it).toString() }
            .sortedBy { it.second.lowercase() }
    }

    private fun appList(myId: String): String {
        val muted = Prefs.mutedPackages(applicationContext)
        val apps = installedApps()
        val body = apps.take(60).joinToString("\n") { (pkg, label) ->
            val mark = if (muted.contains(pkg)) "🔇" else "🔔"
            "$mark <b>$label</b>\n<code>$pkg</code>"
        }
        return "<b>📱 $myId — ${apps.size} app</b>\n\n$body"
    }

    private fun reply(text: String) {
        scope.launch { Telegram.sendMessage(applicationContext, text) }
    }
}
