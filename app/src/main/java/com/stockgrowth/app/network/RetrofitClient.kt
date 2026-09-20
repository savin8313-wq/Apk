package com.stockgrowth.app.network

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    private var retrofit: Retrofit? = null
    private var currentBaseUrl: String? = null

    /**
     * چون آدرس سرور توسط کاربر در تنظیمات قابل تغییر است، هر بار که آدرس
     * عوض شود یک نمونه‌ی جدید از Retrofit ساخته می‌شود.
     */
    fun getApi(baseUrl: String): ApiService {
        val normalizedUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"

        if (retrofit == null || currentBaseUrl != normalizedUrl) {
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            }

            val client = OkHttpClient.Builder()
                .addInterceptor(logging)
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(15, TimeUnit.SECONDS)
                .build()

            retrofit = Retrofit.Builder()
                .baseUrl(normalizedUrl)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            currentBaseUrl = normalizedUrl
        }

        return retrofit!!.create(ApiService::class.java)
    }
}
