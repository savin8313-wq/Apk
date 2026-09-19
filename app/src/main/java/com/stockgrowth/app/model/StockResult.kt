package com.stockgrowth.app.model

import com.google.gson.annotations.SerializedName

data class Indicators(
    val rsi: Double?,
    @SerializedName("macd_hist") val macdHist: Double?,
    val ma20: Double?,
    val ma50: Double?,
    @SerializedName("volume_ratio") val volumeRatio: Double?
)

data class DayTrading(
    val qualifies: Boolean?,
    val score: Double?,
    @SerializedName("avg_daily_range_pct") val avgDailyRangePct: Double?,
    @SerializedName("avg_value_traded") val avgValueTraded: Double?,
    @SerializedName("consistency_ratio") val consistencyRatio: Double?,
    val reasons: List<String>?
)

data class StockResult(
    val symbol: String,
    @SerializedName("last_price") val lastPrice: Double,
    @SerializedName("change_percent") val changePercent: Double,
    val score: Int,
    val verdict: String,          // "رشد" / "مستعد رشد" / "شروع ریزش" / "ریزش" / "خنثی"
    val confidence: String,       // "قوی" / "متوسط" / "ضعیف"
    val reasons: List<String>,
    val indicators: Indicators?,
    @SerializedName("day_trading") val dayTrading: DayTrading?,
    @SerializedName("checked_at") val checkedAt: String?
)

data class ScanListResponse(
    @SerializedName("last_scan") val lastScan: String?,
    val results: List<StockResult>
)

data class StatusResponse(
    @SerializedName("last_scan") val lastScan: String?,
    @SerializedName("growing_count") val growingCount: Int,
    @SerializedName("declining_count") val decliningCount: Int,
    @SerializedName("early_decline_count") val earlyDeclineCount: Int,
    @SerializedName("early_growth_count") val earlyGrowthCount: Int,
    @SerializedName("day_trading_count") val dayTradingCount: Int,
    @SerializedName("is_scanning") val isScanning: Boolean,
    @SerializedName("scan_interval_minutes") val scanIntervalMinutes: Int
)
