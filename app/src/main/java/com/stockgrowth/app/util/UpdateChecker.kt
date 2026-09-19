package com.stockgrowth.app.util

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.core.content.FileProvider
import com.stockgrowth.app.BuildConfig
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File

/**
 * چک‌کردن و نصب بروزرسانی اپ، بر پایه‌ی GitHub Releases.
 * ورک‌فلوی build-apk.yml بعد از هر بیلد موفق، یه Release جدید با تگ
 * "build-<شماره‌ی اجرا>" می‌سازه و فایل APK رو بهش پیوست می‌کنه. این کلاس
 * آخرین Release عمومی ریپازیتوری رو می‌گیره، شماره‌ش رو با نسخه‌ی نصب‌شده‌ی
 * فعلی (BuildConfig.VERSION_CODE) مقایسه می‌کنه، و در صورت جدیدتر بودن،
 * می‌تونه فایل رو دانلود و نصب‌کننده‌ی سیستم رو باز کنه.
 */
object UpdateChecker {

    // در صورت تغییر اسم مالک/ریپازیتوری گیت‌هاب، این دو خط رو عوض کنید.
    private const val GITHUB_OWNER = "savin8313-wq"
    private const val GITHUB_REPO = "Apk"

    private val client = OkHttpClient()

    data class UpdateInfo(
        val versionCode: Int,
        val versionLabel: String,
        val downloadUrl: String,
        val releaseNotes: String
    )

    /** آخرین Release رو از گیت‌هاب می‌گیره. در صورت نبود بروزرسانی یا بروز خطا، null برمی‌گردونه. */
    suspend fun checkForUpdate(): UpdateInfo? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("https://api.github.com/repos/$GITHUB_OWNER/$GITHUB_REPO/releases/latest")
                .header("Accept", "application/vnd.github+json")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val body = response.body?.string() ?: return@withContext null
                val json = JsonParser.parseString(body).asJsonObject

                val tagName = json.get("tag_name")?.asString ?: return@withContext null
                // تگ‌ها به فرم "build-N" هستن؛ عدد N همون versionCode ئه
                val remoteVersionCode = tagName.substringAfterLast("-").toIntOrNull() ?: return@withContext null

                if (remoteVersionCode <= BuildConfig.VERSION_CODE) return@withContext null

                val assets = json.getAsJsonArray("assets") ?: return@withContext null
                val apkAsset = assets.firstOrNull {
                    it.asJsonObject.get("name")?.asString?.endsWith(".apk") == true
                }?.asJsonObject ?: return@withContext null

                val downloadUrl = apkAsset.get("browser_download_url")?.asString ?: return@withContext null
                val notes = json.get("body")?.asString ?: ""
                val releaseName = json.get("name")?.asString ?: tagName

                UpdateInfo(
                    versionCode = remoteVersionCode,
                    versionLabel = releaseName,
                    downloadUrl = downloadUrl,
                    releaseNotes = notes
                )
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * دانلود APK با DownloadManager (که نوار پیشرفت سیستمی خودش رو داره) و بعد
     * از اتمام، خودکار نصب‌کننده‌ی سیستم رو باز می‌کنه.
     */
    fun downloadAndInstall(context: Context, info: UpdateInfo) {
        val fileName = "stock-growth-update-${info.versionCode}.apk"
        val destination = File(
            context.getExternalFilesDir("downloads"),
            fileName
        )
        if (destination.exists()) destination.delete()

        val request = DownloadManager.Request(Uri.parse(info.downloadUrl))
            .setTitle("بروزرسانی تحلیل سهام")
            .setDescription("در حال دانلود ${info.versionLabel}")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationUri(Uri.fromFile(destination))
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)

        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val downloadId = downloadManager.enqueue(request)

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                val completedId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                if (completedId == downloadId) {
                    context.unregisterReceiver(this)
                    installApk(context, destination)
                }
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.registerReceiver(
                receiver,
                IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
                Context.RECEIVER_NOT_EXPORTED
            )
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            context.registerReceiver(receiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
        }
    }

    private fun installApk(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(intent)
    }

    /** روی اندروید ۸ به بالا، کاربر باید صراحتاً اجازه‌ی «نصب از این منبع» رو بده. */
    fun canRequestPackageInstalls(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }
    }
}

private fun com.google.gson.JsonArray.firstOrNull(predicate: (com.google.gson.JsonElement) -> Boolean): com.google.gson.JsonElement? {
    for (element in this) {
        if (predicate(element)) return element
    }
    return null
}
