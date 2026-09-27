package com.stockgrowth.app.model

import com.google.gson.annotations.SerializedName

data class SignalEntry(
    val symbol: String,
    @SerializedName("signal_date") val signalDate: String,
    @SerializedName("entry_price") val entryPrice: Double,
    @SerializedName("stop_loss") val stopLoss: Double?,
    @SerializedName("take_profit") val takeProfit: Double?,
    val status: String,   // pending | hit_take_profit | hit_stop_loss | expired_neutral
    @SerializedName("outcome_date") val outcomeDate: String?,
    @SerializedName("outcome_return_pct") val outcomeReturnPct: Double?
)

data class SignalTrackingStats(
    @SerializedName("total_signals") val totalSignals: Int,
    val pending: Int,
    val resolved: Int,
    @SerializedName("win_rate") val winRate: Double?,
    @SerializedName("avg_return_pct") val avgReturnPct: Double?,
    val recent: List<SignalEntry>?
)

data class BacktestAutoResult(
    val available: Boolean,
    val message: String?,
    @SerializedName("run_at") val runAt: String?,
    @SerializedName("sample_size") val sampleSize: Int?,
    @SerializedName("hold_days") val holdDays: Int?,
    @SerializedName("avg_win_rate") val avgWinRate: Double?,
    @SerializedName("avg_return_pct") val avgReturnPct: Double?
)

data class PricePoint(
    val date: String,
    val close: Double,
    val ma20: Double?,
    val ma50: Double?
)

data class PriceHistoryResponse(
    val symbol: String,
    val points: List<PricePoint>
)
