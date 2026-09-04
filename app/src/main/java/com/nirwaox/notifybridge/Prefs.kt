package com.nirwaox.notifybridge

import android.content.Context
import java.util.UUID

object Prefs {
    private const val FILE = "nirwaox_prefs"
    private const val KEY_ENABLED = "forward_enabled"
    private const val KEY_MUTED = "muted_packages"
    private const val KEY_OFFSET = "telegram_offset"
    private const val KEY_DEVICE_ID = "device_id"

    private fun sp(ctx: Context) = ctx.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun isEnabled(ctx: Context) = sp(ctx).getBoolean(KEY_ENABLED, true)
    fun setEnabled(ctx: Context, value: Boolean) = sp(ctx).edit().putBoolean(KEY_ENABLED, value).apply()

    fun mutedPackages(ctx: Context): MutableSet<String> =
        HashSet(sp(ctx).getStringSet(KEY_MUTED, emptySet()) ?: emptySet())

    fun setMuted(ctx: Context, packages: Set<String>) =
        sp(ctx).edit().putStringSet(KEY_MUTED, packages).apply()

    fun mute(ctx: Context, pkg: String) {
        val s = mutedPackages(ctx); s.add(pkg); setMuted(ctx, s)
    }

    fun unmute(ctx: Context, pkg: String) {
        val s = mutedPackages(ctx); s.remove(pkg); setMuted(ctx, s)
    }

    fun offset(ctx: Context) = sp(ctx).getLong(KEY_OFFSET, 0L)
    fun setOffset(ctx: Context, value: Long) = sp(ctx).edit().putLong(KEY_OFFSET, value).apply()

    fun botToken(@Suppress("UNUSED_PARAMETER") ctx: Context): String = BuildConfig.TELEGRAM_BOT_TOKEN
    fun chatId(@Suppress("UNUSED_PARAMETER") ctx: Context): String = BuildConfig.TELEGRAM_CHAT_ID

    // ID perangkat lokal (NIR-XXX-XXX-XXX) untuk routing perintah multi-device.
    // Dibuat sekali di perangkat, tidak bisa diubah dari UI.
    fun deviceId(ctx: Context): String {
        val p = sp(ctx)
        val existing = p.getString(KEY_DEVICE_ID, null)
        if (!existing.isNullOrBlank()) return existing
        val raw = UUID.randomUUID().toString().replace("-", "").uppercase()
        val id = "NIR-" + raw.substring(0, 3) + "-" + raw.substring(3, 6) + "-" + raw.substring(6, 9)
        p.edit().putString(KEY_DEVICE_ID, id).apply()
        return id
    }
}
