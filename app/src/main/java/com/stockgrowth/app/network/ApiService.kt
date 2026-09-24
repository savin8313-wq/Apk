package com.stockgrowth.app.network

import com.stockgrowth.app.model.ScanListResponse
import com.stockgrowth.app.model.StatusResponse
import com.stockgrowth.app.model.StockResult
import com.stockgrowth.app.model.SignalTrackingStats
import com.stockgrowth.app.model.BacktestAutoResult
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {

    @GET("api/status")
    suspend fun getStatus(): Response<StatusResponse>

    @GET("api/growing")
    suspend fun getGrowing(@Query("limit") limit: Int = 100): Response<ScanListResponse>

    @GET("api/declining")
    suspend fun getDeclining(@Query("limit") limit: Int = 100): Response<ScanListResponse>

    @GET("api/early-decline")
    suspend fun getEarlyDecline(@Query("limit") limit: Int = 100): Response<ScanListResponse>

    @GET("api/early-growth")
    suspend fun getEarlyGrowth(@Query("limit") limit: Int = 100): Response<ScanListResponse>

    @GET("api/day-trading")
    suspend fun getDayTrading(@Query("limit") limit: Int = 100): Response<ScanListResponse>

    @GET("api/stock/{symbol}")
    suspend fun getStock(@Path("symbol") symbol: String): Response<StockResult>

    @GET("api/stock/{symbol}/history")
    suspend fun getStockHistory(
        @Path("symbol") symbol: String,
        @Query("days") days: Int = 40
    ): Response<com.stockgrowth.app.model.PriceHistoryResponse>

    @GET("api/signal-tracking")
    suspend fun getSignalTracking(): Response<SignalTrackingStats>

    @GET("api/backtest-auto")
    suspend fun getBacktestAuto(): Response<BacktestAutoResult>

    @POST("api/backtest-auto/run-now")
    suspend fun runBacktestNow(): Response<Map<String, String>>

    @POST("api/scan")
    suspend fun triggerScan(): Response<Map<String, String>>
}
