package com.stockgrowth.app.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.stockgrowth.app.R

object NotificationHelper {
    private const val CHANNEL_ID = "stock_signals"
    private var nextNotificationId = 1000

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(NotificationManager::class.java)
            val existing = manager.getNotificationChannel(CHANNEL_ID)
            if (existing == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "سیگنال‌های سهام",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "اطلاع‌رسانی وقتی سهم جدیدی وارد لیست رشد یا مستعد رشد می‌شه"
                }
                manager.createNotificationChannel(channel)
            }
        }
    }

    fun notifyNewSignal(context: Context, title: String, message: String) {
        ensureChannel(context)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(nextNotificationId++, notification)
        } catch (e: SecurityException) {
            // اجازه‌ی نوتیفیکیشن داده نشده؛ به‌آرامی نادیده گرفته می‌شه
        }
    }
}
