package com.stockgrowth.app.util

import android.content.Context
import android.content.Intent

/**
 * وقتی اپ می‌فهمه سرور (روی همون گوشی، از طریق Termux) از دسترس خارج شده،
 * این کلاس یه دستور مستقیم به خودِ Termux می‌فرسته تا اسکریپت خودترمیم
 * (~/restart_server.sh) رو اجرا کنه — بدون نیاز به باز کردن دستی Termux.
 *
 * پیش‌نیازها (فقط یه‌بار، توی Termux):
 *   1) توی فایل ~/.termux/termux.properties این خط باشه:
 *        allow-external-apps=true
 *      (بعد از اضافه‌کردن، Termux رو کامل ببندید و دوباره باز کنید)
 *   2) اسکریپت ~/restart_server.sh وجود داشته باشه (طبق راهنمای README).
 */
object TermuxController {

    private const val TERMUX_PACKAGE = "com.termux"
    private const val RUN_COMMAND_SERVICE = "com.termux.app.RunCommandService"
    private const val RUN_COMMAND_ACTION = "com.termux.RUN_COMMAND"
    private const val RESTART_SCRIPT_PATH = "/data/data/com.termux/files/home/restart_server.sh"

    /**
     * true اگه دستور با موفقیت به Termux فرستاده شد (به این معنی نیست که
     * لزوماً موفق اجرا شد، فقط یعنی سرویس Termux پیام رو گرفته).
     */
    fun requestServerRestart(context: Context): Boolean {
        return try {
            val intent = Intent()
            intent.setClassName(TERMUX_PACKAGE, RUN_COMMAND_SERVICE)
            intent.action = RUN_COMMAND_ACTION
            intent.putExtra("com.termux.RUN_COMMAND_PATH", RESTART_SCRIPT_PATH)
            intent.putExtra("com.termux.RUN_COMMAND_WORKDIR", "/data/data/com.termux/files/home")
            intent.putExtra("com.termux.RUN_COMMAND_BACKGROUND", true)
            context.startService(intent)
            true
        } catch (e: Exception) {
            // یا Termux نصب نیست، یا allow-external-apps فعال نشده، یا هر دلیل دیگه
            false
        }
    }
}
