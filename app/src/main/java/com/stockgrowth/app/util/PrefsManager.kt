package com.stockgrowth.app.util

import android.content.Context
import com.stockgrowth.app.model.PortfolioPosition
import org.json.JSONArray
import org.json.JSONObject

object PrefsManager {
    private const val PREFS_NAME = "stock_growth_prefs"
    private const val KEY_SERVER_URL = "server_url"
    private const val KEY_REFRESH_INTERVAL = "refresh_interval_minutes"
    private const val KEY_DARK_MODE = "dark_mode"
    private const val KEY_NOTIFY_SIGNALS = "notify_signals"
    private const val KEY_API_KEY = "api_key"
    private const val KEY_WATCHLIST = "watchlist_symbols"
    private const val KEY_PORTFOLIO = "portfolio_positions_json"

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

    /** کلید API اختیاری؛ اگه سرور با STOCK_API_KEY بالا اومده باشه، باید همینجا هم ست بشه. */
    fun getApiKey(context: Context): String =
        prefs(context).getString(KEY_API_KEY, "") ?: ""

    fun setApiKey(context: Context, key: String) {
        prefs(context).edit().putString(KEY_API_KEY, key.trim()).apply()
    }

    // --- واچ‌لیست (علاقه‌مندی‌ها) ---

    fun getWatchlist(context: Context): Set<String> =
        prefs(context).getStringSet(KEY_WATCHLIST, emptySet())?.toSet() ?: emptySet()

    fun addToWatchlist(context: Context, symbol: String) {
        val current = getWatchlist(context).toMutableSet()
        current.add(symbol.trim())
        prefs(context).edit().putStringSet(KEY_WATCHLIST, current).apply()
    }

    fun removeFromWatchlist(context: Context, symbol: String) {
        val current = getWatchlist(context).toMutableSet()
        current.remove(symbol.trim())
        prefs(context).edit().putStringSet(KEY_WATCHLIST, current).apply()
    }

    fun isInWatchlist(context: Context, symbol: String): Boolean =
        getWatchlist(context).contains(symbol.trim())

    // --- پورتفولیو (خریدهای واقعی کاربر) ---
    // کاملاً محلی، هیچ‌وقت به سرور فرستاده نمی‌شه.

    fun getPortfolio(context: Context): List<PortfolioPosition> {
        val json = prefs(context).getString(KEY_PORTFOLIO, "[]") ?: "[]"
        val list = mutableListOf<PortfolioPosition>()
        try {
            val arr = JSONArray(json)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    PortfolioPosition(
                        symbol = obj.getString("symbol"),
                        buyPrice = obj.getDouble("buyPrice"),
                        quantity = obj.optDouble("quantity", 1.0),
                        stopLoss = if (obj.isNull("stopLoss")) null else obj.getDouble("stopLoss"),
                        takeProfit = if (obj.isNull("takeProfit")) null else obj.getDouble("takeProfit"),
                        addedAtMillis = obj.optLong("addedAtMillis", System.currentTimeMillis())
                    )
                )
            }
        } catch (e: Exception) {
            // اگه داده خراب بود، لیست خالی برمی‌گردونیم به‌جای کرش کردن اپ
        }
        return list
    }

    private fun savePortfolio(context: Context, positions: List<PortfolioPosition>) {
        val arr = JSONArray()
        for (p in positions) {
            val obj = JSONObject()
            obj.put("symbol", p.symbol)
            obj.put("buyPrice", p.buyPrice)
            obj.put("quantity", p.quantity)
            obj.put("stopLoss", p.stopLoss)
            obj.put("takeProfit", p.takeProfit)
            obj.put("addedAtMillis", p.addedAtMillis)
            arr.put(obj)
        }
        prefs(context).edit().putString(KEY_PORTFOLIO, arr.toString()).apply()
    }

    fun addPosition(context: Context, position: PortfolioPosition) {
        val current = getPortfolio(context).toMutableList()
        current.add(position)
        savePortfolio(context, current)
    }

    fun removePosition(context: Context, symbol: String, addedAtMillis: Long) {
        val current = getPortfolio(context).filterNot {
            it.symbol == symbol && it.addedAtMillis == addedAtMillis
        }
        savePortfolio(context, current)
    }
}
