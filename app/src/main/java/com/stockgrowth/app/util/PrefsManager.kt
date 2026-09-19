package com.stockgrowth.app.util

import android.content.Context

object PrefsManager {
    private const val PREFS_NAME = "stock_growth_prefs"
    private const val KEY_SERVER_URL = "server_url"
    private const val KEY_REFRESH_INTERVAL = "refresh_interval_minutes"
    private const val KEY_DARK_MODE = "dark_mode"
    private const val KEY_NOTIFY_SIGNALS = "notify_signals"

    private const val DEFAULT_URL = "http://192.168.1.10:8000"
    private const val DEFAULT_REFRESH_INTERVAL = 15
    private const val DEFAULT_DARK_MODE = false
    private const val DEFAULT_NOTIFY_SIGNALS = false

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getServerUrl(context: Context): String =
        prefs(context).getString(KEY_SERVER_URL, DEFAULT_URL) ?: DEFAULT_URL

    fun setServerUrl(context: Context, url: String) {
        prefs(context).edit().putString(KEY_SERVER_URL, url.trim()).apply()
    }

    /** بازه‌ی بروزرسانی خودکار به دقیقه (فقط وقتی اپ باز و در جلوی صفحه‌ست اعمال می‌شه) */
    fun getRefreshIntervalMinutes(context: Context): Int =
        prefs(context).getInt(KEY_REFRESH_INTERVAL, DEFAULT_REFRESH_INTERVAL)

    fun setRefreshIntervalMinutes(context: Context, minutes: Int) {
        prefs(context).edit().putInt(KEY_REFRESH_INTERVAL, minutes).apply()
    }

    fun isDarkMode(context: Context): Boolean =
        prefs(context).getBoolean(KEY_DARK_MODE, DEFAULT_DARK_MODE)

    fun setDarkMode(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_DARK_MODE, enabled).apply()
    }

    fun isNotifySignalsEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_NOTIFY_SIGNALS, DEFAULT_NOTIFY_SIGNALS)

    fun setNotifySignalsEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_NOTIFY_SIGNALS, enabled).apply()
    }
}
