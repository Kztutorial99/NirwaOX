package com.nirwaox.notifybridge

import android.content.Context
import java.security.SecureRandom

object Prefs {
    private const val FILE = "nirwaox_prefs"
    private const val KEY_DEVICE_ID = "device_id"
    private const val KEY_ENABLED = "forward_enabled"
    private const val KEY_MUTED = "muted_packages"
    private const val KEY_OFFSET = "telegram_offset"
    private const val KEY_BOT_TOKEN = "bot_token"
    private const val KEY_CHAT_ID = "chat_id"

    private fun sp(ctx: Context) = ctx.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun deviceId(ctx: Context): String {
        val p = sp(ctx)
        p.getString(KEY_DEVICE_ID, null)?.let { return it }
        val id = generateDeviceId()
        p.edit().putString(KEY_DEVICE_ID, id).apply()
        return id
    }

    private fun generateDeviceId(): String {
        val alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        val rnd = SecureRandom()
        fun block() = (1..3).map { alphabet[rnd.nextInt(alphabet.length)] }.joinToString("")
        return "NIR-${block()}-${block()}-${block()}"
    }

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

    fun botToken(ctx: Context): String {
        val stored = sp(ctx).getString(KEY_BOT_TOKEN, "") ?: ""
        return if (stored.isNotBlank()) stored else BuildConfig.TELEGRAM_BOT_TOKEN
    }

    fun chatId(ctx: Context): String {
        val stored = sp(ctx).getString(KEY_CHAT_ID, "") ?: ""
        return if (stored.isNotBlank()) stored else BuildConfig.TELEGRAM_CHAT_ID
    }

    fun setCredentials(ctx: Context, token: String, chatId: String) =
        sp(ctx).edit().putString(KEY_BOT_TOKEN, token).putString(KEY_CHAT_ID, chatId).apply()
}
