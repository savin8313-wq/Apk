package com.stockgrowth.app.network

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    private var retrofit: Retrofit? = null
    private var currentBaseUrl: String? = null
    private var currentApiKey: String? = null

    /**
     * چون آدرس سرور و کلید API توسط کاربر در تنظیمات قابل تغییرن، هر بار که
     * هر کدوم عوض بشن، یک نمونه‌ی جدید از Retrofit ساخته می‌شه.
     */
    fun getApi(baseUrl: String, apiKey: String = ""): ApiService {
        val normalizedUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"

        if (retrofit == null || currentBaseUrl != normalizedUrl || currentApiKey != apiKey) {
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            }

            val clientBuilder = OkHttpClient.Builder()
                .addInterceptor(logging)
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(15, TimeUnit.SECONDS)

            if (apiKey.isNotBlank()) {
                clientBuilder.addInterceptor { chain ->
                    val newRequest = chain.request().newBuilder()
                        .addHeader("X-API-Key", apiKey)
                        .build()
                    chain.proceed(newRequest)
                }
            }

            retrofit = Retrofit.Builder()
                .baseUrl(normalizedUrl)
                .client(clientBuilder.build())
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            currentBaseUrl = normalizedUrl
            currentApiKey = apiKey
        }

        return retrofit!!.create(ApiService::class.java)
    }
}
