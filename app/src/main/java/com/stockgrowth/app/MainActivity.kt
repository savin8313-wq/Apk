package com.stockgrowth.app

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.tabs.TabLayout
import com.google.android.material.textfield.TextInputEditText
import com.stockgrowth.app.adapter.StockAdapter
import com.stockgrowth.app.model.StockResult
import com.stockgrowth.app.network.RetrofitClient
import com.stockgrowth.app.util.NotificationHelper
import com.stockgrowth.app.util.PrefsManager
import com.stockgrowth.app.util.UpdateChecker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

class MainActivity : AppCompatActivity() {

    // ایندکس تب فعلی: ۰=در حال رشد ، ۱=مستعد رشد ، ۲=شروع ریزش ، ۳=در حال ریزش ، ۴=مستعد نوسان‌گیری
    private var currentTab = 0

    private lateinit var recyclerView: RecyclerView
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var tvLastScan: TextView
    private lateinit var tvEmpty: TextView
    private lateinit var progressBar: android.widget.ProgressBar
    private lateinit var etSearchSymbol: TextInputEditText

    private lateinit var adapter: StockAdapter

    // کش محلی هر تب تا با سوییچ بین تب‌ها دوباره از سرور نگیریم
    private val tabDataCache = mutableMapOf<Int, List<StockResult>>()

    // برای تشخیص سیگنال‌های تازه (برای نوتیفیکیشن)
    private var knownPositiveSymbols = mutableSetOf<String>()
    private var hasBaselineForNotify = false
    private var hasWarnedStaleServer = false

    private val autoRefreshHandler = Handler(Looper.getMainLooper())
    private var autoRefreshRunnable: Runnable? = null

    private val notificationPermissionLauncher =
        registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        // اعمال حالت تیره/روشن قبل از ساخت View
        AppCompatDelegate.setDefaultNightMode(
            if (PrefsManager.isDarkMode(this)) AppCompatDelegate.MODE_NIGHT_YES
            else AppCompatDelegate.MODE_NIGHT_NO
        )
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val toolbar = findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        toolbar.title = ""
        toolbar.setOnMenuItemClickListener { onOptionsItemSelected(it) }

        recyclerView = findViewById(R.id.recyclerView)
        swipeRefresh = findViewById(R.id.swipeRefresh)
        tvLastScan = findViewById(R.id.tvLastScan)
        tvEmpty = findViewById(R.id.tvEmpty)
        progressBar = findViewById(R.id.progressBar)
        etSearchSymbol = findViewById(R.id.etSearchSymbol)

        adapter = StockAdapter(emptyList()) { item -> showStockDetailDialog(item) }
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        swipeRefresh.setOnRefreshListener { loadCurrentTab(forceRefresh = true) }

        val tabLayout = findViewById<TabLayout>(R.id.tabLayout)
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                currentTab = tab.position
                loadCurrentTab(forceRefresh = false)
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })

        findViewById<com.google.android.material.button.MaterialButton>(R.id.btnSearch)
            .setOnClickListener { performSearch() }

        etSearchSymbol.setOnEditorActionListener { _, _, _ ->
            performSearch()
            true
        }

        loadCurrentTab(forceRefresh = true)

        // بررسی بی‌صدای آپدیت هنگام باز شدن اپ؛ فقط اگه نسخه‌ی جدیدی بود چیزی نشون می‌ده
        checkAndPromptUpdate(showIfNoUpdate = false)
    }

    override fun onResume() {
        super.onResume()
        scheduleAutoRefresh()
    }

    override fun onPause() {
        super.onPause()
        autoRefreshRunnable?.let { autoRefreshHandler.removeCallbacks(it) }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_refresh -> {
                loadCurrentTab(forceRefresh = true)
                true
            }
            R.id.action_settings -> {
                showSettingsDialog()
                true
            }
            R.id.action_check_update -> {
                checkAndPromptUpdate(showIfNoUpdate = true)
                true
            }
            R.id.action_export_csv -> {
                exportCsv()
                true
            }
            R.id.action_help -> {
                showHelpDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showHelpDialog() {
        val message = """
            📈 در حال رشد: سهامی با سیگنال صعودی قوی
            🌱 مستعد رشد: هنوز رشد قطعی نشده ولی نشونه‌ی برگشت از نزول داره
            ⚠️ شروع ریزش: هنوز ریزش قطعی نشده ولی نشونه‌ی ضعیف‌شدن روند صعودی داره
            📉 در حال ریزش: سهامی با سیگنال نزولی قوی
            ⚡ مستعد نوسان‌گیری: مستقل از روند، به اندازه‌ی کافی نقدشونده و پرنوسانه

            هر سهم یه نشان «خرید» یا «فروش» با درصد اطمینان داره — این درصد
            از ترکیب چند اندیکاتور (RSI، MACD، میانگین متحرک، حجم، بولینگر)
            به‌دست میاد و صرفاً یه ابزار کمکیه، نه توصیه‌ی مالی قطعی.

            ⭐ روی ستاره‌ی هر سهم بزن تا به تب «علاقه‌مندی‌ها» اضافه بشه.

            ⚙️ از تنظیمات می‌تونی آدرس سرور، بازه‌ی بروزرسانی خودکار، حالت
            تیره، و اطلاع‌رسانی سیگنال‌های جدید رو تنظیم کنی.

            ⚠️ این اپ مشاوره‌ی مالی نیست؛ فقط یه ابزار کمکیه. قبل از هر
            تصمیم، خودتون تحقیق و بررسی کنید.
        """.trimIndent()

        AlertDialog.Builder(this)
            .setTitle("راهنمای اپ")
            .setMessage(message)
            .setPositiveButton("متوجه شدم", null)
            .show()
    }

    private fun exportCsv() {
        val baseUrl = PrefsManager.getServerUrl(this).trimEnd('/')
        val apiKey = PrefsManager.getApiKey(this)
        val url = "$baseUrl/api/export/csv"
        val fileName = "stock_export_${System.currentTimeMillis()}.csv"

        try {
            val request = android.app.DownloadManager.Request(Uri.parse(url))
                .setTitle("خروجی CSV تحلیل سهام")
                .setNotificationVisibility(android.app.DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalPublicDir(android.os.Environment.DIRECTORY_DOWNLOADS, fileName)

            if (apiKey.isNotBlank()) {
                request.addRequestHeader("X-API-Key", apiKey)
            }

            val downloadManager = getSystemService(Context.DOWNLOAD_SERVICE) as android.app.DownloadManager
            downloadManager.enqueue(request)
            Toast.makeText(this, "دانلود CSV شروع شد (توی پوشه‌ی Download)", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "دانلود CSV ممکن نشد: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // =========================================================================
    // بررسی و نصب بروزرسانی اپ (از GitHub Releases)
    // =========================================================================

    private fun checkAndPromptUpdate(showIfNoUpdate: Boolean) {
        if (showIfNoUpdate) {
            Toast.makeText(this, "در حال بررسی بروزرسانی...", Toast.LENGTH_SHORT).show()
        }

        CoroutineScope(Dispatchers.Main).launch {
            val info = UpdateChecker.checkForUpdate()

            if (info == null) {
                if (showIfNoUpdate) {
                    Toast.makeText(this@MainActivity, "شما آخرین نسخه رو دارید", Toast.LENGTH_SHORT).show()
                }
                return@launch
            }

            AlertDialog.Builder(this@MainActivity)
                .setTitle("بروزرسانی جدید موجوده")
                .setMessage("${info.versionLabel}\n\n${info.releaseNotes.ifBlank { "بهبودها و رفع مشکلات" }}")
                .setPositiveButton("بروزرسانی الان") { _, _ -> startUpdateDownload(info) }
                .setNegativeButton("بعداً", null)
                .show()
        }
    }

    private fun startUpdateDownload(info: UpdateChecker.UpdateInfo) {
        if (!UpdateChecker.canRequestPackageInstalls(this)) {
            Toast.makeText(
                this,
                "اجازه‌ی «نصب از این منبع» رو بده و دوباره امتحان کن",
                Toast.LENGTH_LONG
            ).show()
            val intent = Intent(
                android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
            return
        }

        Toast.makeText(this, "دانلود بروزرسانی شروع شد...", Toast.LENGTH_SHORT).show()
        UpdateChecker.downloadAndInstall(this, info)
    }

    // =========================================================================
    // بروزرسانی خودکار دوره‌ای (فقط وقتی اپ باز و در جلوی صفحه‌ست)
    // =========================================================================

    private fun scheduleAutoRefresh() {
        autoRefreshRunnable?.let { autoRefreshHandler.removeCallbacks(it) }

        val intervalMinutes = PrefsManager.getRefreshIntervalMinutes(this).coerceAtLeast(1)
        val intervalMs = intervalMinutes * 60_000L

        val runnable = object : Runnable {
            override fun run() {
                checkForNewSignalsAndRefresh()
                autoRefreshHandler.postDelayed(this, intervalMs)
            }
        }
        autoRefreshRunnable = runnable
        autoRefreshHandler.postDelayed(runnable, intervalMs)
    }

    private fun checkForNewSignalsAndRefresh() {
        tabDataCache.clear()
        loadCurrentTab(forceRefresh = true)
        checkServerHealth()

        if (!PrefsManager.isNotifySignalsEnabled(this)) return

        val baseUrl = PrefsManager.getServerUrl(this)
        val api = RetrofitClient.getApi(baseUrl, PrefsManager.getApiKey(this))

        CoroutineScope(Dispatchers.Main).launch {
            try {
                val growingResp = api.getGrowing()
                val earlyGrowthResp = api.getEarlyGrowth()

                val currentSymbols = mutableSetOf<String>()
                growingResp.body()?.results?.forEach { currentSymbols.add(it.symbol) }
                earlyGrowthResp.body()?.results?.forEach { currentSymbols.add(it.symbol) }

                if (hasBaselineForNotify) {
                    val newOnes = currentSymbols - knownPositiveSymbols
                    if (newOnes.isNotEmpty()) {
                        val preview = newOnes.take(5).joinToString("، ")
                        NotificationHelper.notifyNewSignal(
                            this@MainActivity,
                            "سیگنال جدید",
                            "نمادهای جدید در لیست رشد/مستعد رشد: $preview"
                        )
                    }
                }
                knownPositiveSymbols = currentSymbols
                hasBaselineForNotify = true
            } catch (e: Exception) {
                // شکست بی‌صدا؛ بروزرسانی معمولی تب جاری از قبل انجام شده
            }
        }
    }

    /**
     * بررسی سلامت سرور: اگه سرور مدت زیادی (بیشتر از ۳ برابر بازه‌ی اسکن خودش)
     * بروزرسانی نکرده، یعنی احتمالاً کرش کرده یا گوشیِ سرور خاموش/بی‌اینترنته —
     * یه هشدار یک‌باره نشون می‌ده (تا خودش دوباره سالم بشه، دوباره هشدار نمی‌ده).
     */
    private fun checkServerHealth() {
        val baseUrl = PrefsManager.getServerUrl(this)
        val api = RetrofitClient.getApi(baseUrl, PrefsManager.getApiKey(this))

        CoroutineScope(Dispatchers.Main).launch {
            try {
                val response = api.getStatus()
                val status = response.body() ?: return@launch
                val lastScanStr = status.lastScan ?: return@launch

                val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US)
                val lastScanTime = sdf.parse(lastScanStr) ?: return@launch
                val ageMinutes = (System.currentTimeMillis() - lastScanTime.time) / 60_000
                val threshold = (status.scanIntervalMinutes * 3).coerceAtLeast(30)

                if (ageMinutes > threshold) {
                    if (!hasWarnedStaleServer) {
                        hasWarnedStaleServer = true
                        NotificationHelper.notifyNewSignal(
                            this@MainActivity,
                            "⚠️ سرور بروزرسانی نمی‌شه",
                            "آخرین اسکن ${ageMinutes} دقیقه پیش بوده. بررسی کن گوشیِ سرور روشن و متصله."
                        )
                    }
                } else {
                    hasWarnedStaleServer = false
                }
            } catch (e: Exception) {
                // اگه اصلاً وصل نشد هم چیزی نشون نمی‌دیم؛ خطای اتصال جای دیگه‌ای قبلاً نشون داده می‌شه
            }
        }
    }

    // =========================================================================
    // بارگذاری لیست تب فعلی
    // =========================================================================

    private fun loadCurrentTab(forceRefresh: Boolean) {
        if (!forceRefresh && tabDataCache.containsKey(currentTab)) {
            renderList(tabDataCache[currentTab] ?: emptyList())
            return
        }

        if (currentTab == 5) {
            loadWatchlist()
            return
        }

        tvEmpty.text = getString(R.string.no_data)
        setLoading(true)
        val baseUrl = PrefsManager.getServerUrl(this)
        val api = RetrofitClient.getApi(baseUrl, PrefsManager.getApiKey(this))

        CoroutineScope(Dispatchers.Main).launch {
            try {
                val response = when (currentTab) {
                    0 -> api.getGrowing()
                    1 -> api.getEarlyGrowth()
                    2 -> api.getEarlyDecline()
                    3 -> api.getDeclining()
                    else -> api.getDayTrading()
                }

                swipeRefresh.isRefreshing = false
                setLoading(false)

                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    tabDataCache[currentTab] = body.results
                    renderList(body.results)
                    updateLastScanText(body.lastScan)
                } else {
                    showError(getString(R.string.error_connection))
                }
            } catch (e: IOException) {
                swipeRefresh.isRefreshing = false
                setLoading(false)
                showError(getString(R.string.error_connection))
            } catch (e: HttpException) {
                swipeRefresh.isRefreshing = false
                setLoading(false)
                showError(getString(R.string.error_connection))
            }
        }
    }

    /**
     * تب «علاقه‌مندی‌ها» یه اندپوینت جدا توی سرور نداره — برای هر نمادی که
     * کاربر ذخیره کرده، جدا از /api/stock/{symbol} تحلیل زنده می‌گیریم.
     */
    private fun loadWatchlist() {
        val symbols = PrefsManager.getWatchlist(this)
        if (symbols.isEmpty()) {
            renderList(emptyList())
            tvEmpty.text = getString(R.string.watchlist_empty)
            updateLastScanText(null)
            return
        }
        tvEmpty.text = getString(R.string.no_data)

        setLoading(true)
        val baseUrl = PrefsManager.getServerUrl(this)
        val api = RetrofitClient.getApi(baseUrl, PrefsManager.getApiKey(this))

        CoroutineScope(Dispatchers.Main).launch {
            val results = mutableListOf<StockResult>()
            for (symbol in symbols) {
                try {
                    val response = api.getStock(symbol)
                    response.body()?.let { results.add(it) }
                } catch (e: Exception) {
                    // اگه یکی از نمادها خطا داد، بقیه رو ادامه بده
                }
            }

            swipeRefresh.isRefreshing = false
            setLoading(false)
            tabDataCache[currentTab] = results
            renderList(results)
            updateLastScanText("زنده")
        }
    }

    private fun renderList(items: List<StockResult>) {
        adapter.updateData(items)
        tvEmpty.visibility = if (items.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
    }

    private fun updateLastScanText(lastScan: String?) {
        tvLastScan.text = getString(R.string.last_scan_prefix) + (lastScan ?: "—")
    }

    private fun setLoading(loading: Boolean) {
        if (loading && !swipeRefresh.isRefreshing) {
            progressBar.visibility = android.view.View.VISIBLE
        } else {
            progressBar.visibility = android.view.View.GONE
        }
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    // =========================================================================
    // جستجوی یک نماد خاص
    // =========================================================================

    private fun performSearch() {
        val rawSymbol = etSearchSymbol.text?.toString()?.trim().orEmpty()
        if (rawSymbol.isEmpty()) {
            Toast.makeText(this, getString(R.string.search_hint), Toast.LENGTH_SHORT).show()
            return
        }
        // نرمال‌سازی حروف عربی رایج (ي، ك) به معادل فارسی، تا اختلاف کیبورد باعث «پیدا نشد» نشه
        val symbol = rawSymbol.replace("ي", "ی").replace("ك", "ک")

        setLoading(true)
        val baseUrl = PrefsManager.getServerUrl(this)
        val api = RetrofitClient.getApi(baseUrl, PrefsManager.getApiKey(this))

        CoroutineScope(Dispatchers.Main).launch {
            try {
                val response = api.getStock(symbol)
                setLoading(false)

                if (response.isSuccessful && response.body() != null) {
                    showStockDetailDialog(response.body()!!)
                } else if (response.code() == 404) {
                    showError(getString(R.string.error_not_found))
                } else {
                    showError(getString(R.string.error_connection))
                }
            } catch (e: IOException) {
                setLoading(false)
                showError(getString(R.string.error_connection))
            } catch (e: HttpException) {
                setLoading(false)
                showError(getString(R.string.error_connection))
            }
        }
    }

    // =========================================================================
    // دیالوگ جزئیات یک سهم
    // =========================================================================

    private fun showStockDetailDialog(item: StockResult) {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_stock_detail, null)

        view.findViewById<TextView>(R.id.tvDetailSymbol).text = item.symbol

        val btnWatchlist = view.findViewById<android.widget.ImageButton>(R.id.btnToggleWatchlist)
        fun updateStarIcon() {
            val isFav = PrefsManager.isInWatchlist(this, item.symbol)
            btnWatchlist.setImageResource(
                if (isFav) android.R.drawable.btn_star_big_on else android.R.drawable.btn_star_big_off
            )
        }
        updateStarIcon()
        btnWatchlist.setOnClickListener {
            if (PrefsManager.isInWatchlist(this, item.symbol)) {
                PrefsManager.removeFromWatchlist(this, item.symbol)
                Toast.makeText(this, "از علاقه‌مندی‌ها حذف شد", Toast.LENGTH_SHORT).show()
            } else {
                PrefsManager.addToWatchlist(this, item.symbol)
                Toast.makeText(this, "به علاقه‌مندی‌ها اضافه شد", Toast.LENGTH_SHORT).show()
            }
            updateStarIcon()
            tabDataCache.remove(5)
        }

        val changeSign = if (item.changePercent >= 0) "+" else ""
        view.findViewById<TextView>(R.id.tvDetailPrice).text =
            "%,.0f ریال   (%s%.2f%%)".format(item.lastPrice, changeSign, item.changePercent)

        view.findViewById<TextView>(R.id.tvDetailVerdict).text =
            "وضعیت: ${item.verdict}  —  اطمینان: ${item.confidence}  (امتیاز ${item.score})"

        val buyPct = item.buyPercent ?: 50
        val sellPct = item.sellPercent ?: 50
        val isBuy = (item.action ?: "خرید") == "خرید"
        val layoutAction = view.findViewById<android.widget.LinearLayout>(R.id.layoutDetailAction)
        val tvDetailAction = view.findViewById<TextView>(R.id.tvDetailAction)
        tvDetailAction.text = "سیگنال: ${item.action ?: "-"}"
        view.findViewById<TextView>(R.id.tvDetailActionPercent).text =
            "احتمال خرید %d٪   •   احتمال فروش %d٪".format(buyPct, sellPct)
        if (isBuy) {
            tvDetailAction.setTextColor(ContextCompat.getColor(this, R.color.green_growth))
            layoutAction.setBackgroundResource(R.drawable.bg_action_buy)
        } else {
            tvDetailAction.setTextColor(ContextCompat.getColor(this, R.color.red_decline))
            layoutAction.setBackgroundResource(R.drawable.bg_action_sell)
        }

        view.findViewById<TextView>(R.id.tvDetailReasons).text =
            item.reasons.joinToString("\n") { "• $it" }

        val ind = item.indicators
        view.findViewById<TextView>(R.id.tvDetailIndicators).text = if (ind != null) {
            "RSI: ${ind.rsi ?: "-"}   |   MACD Hist: ${ind.macdHist ?: "-"}\n" +
                "MA20: ${ind.ma20 ?: "-"}   |   MA50: ${ind.ma50 ?: "-"}\n" +
                "نسبت حجم به میانگین: ${ind.volumeRatio ?: "-"}"
        } else ""

        val dt = item.dayTrading
        val tvDayTrading = view.findViewById<TextView>(R.id.tvDetailDayTrading)
        if (dt != null && dt.score != null) {
            val qualifiesText = if (dt.qualifies == true) "بله ✅" else "خیر"
            tvDayTrading.text =
                "مناسب نوسان‌گیری روزانه: $qualifiesText  (امتیاز ${dt.score.toInt()}/100)\n" +
                    "میانگین دامنه‌ی نوسان روزانه: ${dt.avgDailyRangePct ?: "-"}٪\n" +
                    "میانگین ارزش معاملات روزانه: ${formatToman(dt.avgValueTraded)}\n" +
                    "پایداری این ویژگی: ${((dt.consistencyRatio ?: 0.0) * 100).toInt()}٪"
            tvDayTrading.visibility = android.view.View.VISIBLE
        } else {
            tvDayTrading.visibility = android.view.View.GONE
        }

        AlertDialog.Builder(this)
            .setView(view)
            .setPositiveButton("بستن", null)
            .show()
    }

    private fun formatToman(rialValue: Double?): String {
        if (rialValue == null) return "-"
        val billion = rialValue / 1_000_000_000.0
        return "%.1f میلیارد ریال".format(billion)
    }

    // =========================================================================
    // دیالوگ تنظیمات اپ
    // =========================================================================

    private fun showSettingsDialog() {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_settings, null)
        val etUrl = view.findViewById<TextInputEditText>(R.id.etServerUrl)
        val etApiKey = view.findViewById<TextInputEditText>(R.id.etApiKey)
        val etInterval = view.findViewById<TextInputEditText>(R.id.etRefreshInterval)
        val switchDark = view.findViewById<SwitchMaterial>(R.id.switchDarkMode)
        val switchNotify = view.findViewById<SwitchMaterial>(R.id.switchNotify)

        etUrl.setText(PrefsManager.getServerUrl(this))
        etApiKey.setText(PrefsManager.getApiKey(this))
        etInterval.setText(PrefsManager.getRefreshIntervalMinutes(this).toString())
        switchDark.isChecked = PrefsManager.isDarkMode(this)
        switchNotify.isChecked = PrefsManager.isNotifySignalsEnabled(this)

        AlertDialog.Builder(this)
            .setTitle(R.string.settings_title)
            .setView(view)
            .setPositiveButton(R.string.save) { _, _ ->
                val newUrl = etUrl.text?.toString()?.trim().orEmpty()
                if (newUrl.isNotEmpty()) {
                    PrefsManager.setServerUrl(this, newUrl)
                }

                PrefsManager.setApiKey(this, etApiKey.text?.toString().orEmpty())

                val newInterval = etInterval.text?.toString()?.trim()?.toIntOrNull()
                if (newInterval != null && newInterval > 0) {
                    PrefsManager.setRefreshIntervalMinutes(this, newInterval)
                }

                val wasDark = PrefsManager.isDarkMode(this)
                val nowDark = switchDark.isChecked
                PrefsManager.setDarkMode(this, nowDark)

                val notifyEnabled = switchNotify.isChecked
                PrefsManager.setNotifySignalsEnabled(this, notifyEnabled)
                if (notifyEnabled) {
                    requestNotificationPermissionIfNeeded()
                }

                tabDataCache.clear()

                if (wasDark != nowDark) {
                    // تغییر تم نیاز به بازسازی اکتیویتی داره
                    recreate()
                } else {
                    loadCurrentTab(forceRefresh = true)
                    scheduleAutoRefresh()
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            if (!granted) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}
