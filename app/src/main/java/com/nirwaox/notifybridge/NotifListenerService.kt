package com.nirwaox.notifybridge

import android.app.Notification
import android.content.pm.PackageManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NotifListenerService : NotificationListenerService() {

    private val scope = CoroutineScope(Dispatchers.IO)

    override fun onListenerConnected() {
        super.onListenerConnected()
        TelegramPollingService.start(this)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val ctx = applicationContext
        if (!Prefs.isEnabled(ctx)) return

        val pkg = sbn.packageName
        if (pkg == packageName) return
        if (Prefs.mutedPackages(ctx).contains(pkg)) return

        val extras = sbn.notification?.extras ?: return
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
            ?: extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString().orEmpty()
        if (title.isBlank() && text.isBlank()) return

        val appName = appLabel(pkg)
        val time = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date(sbn.postTime))
        val deviceId = Prefs.deviceId(ctx)

        val msg = buildString {
            append("<b>🔔 ").append(esc(appName)).append("</b>\n")
            if (title.isNotBlank()) append("<b>").append(esc(title)).append("</b>\n")
            if (text.isNotBlank()) append(esc(text)).append("\n")
            append("\n<code>").append(deviceId).append("</code> • ").append(esc(time))
            append("\n<i>").append(esc(pkg)).append("</i>")
        }

        scope.launch { Telegram.sendMessage(ctx, msg) }
    }

    private fun appLabel(pkg: String): String = try {
        val pm: PackageManager = packageManager
        pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
    } catch (e: Exception) {
        pkg
    }

    private fun esc(s: String) = s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
}
