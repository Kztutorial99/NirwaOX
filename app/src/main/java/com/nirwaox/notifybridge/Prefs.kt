package com.nirwaox.notifybridge

import android.content.Context

object Prefs {
    private const val FILE = "nirwaox_prefs"
    private const val KEY_ENABLED = "forward_enabled"
    private const val KEY_MUTED = "muted_packages"
    private const val KEY_OFFSET = "telegram_offset"

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
}
