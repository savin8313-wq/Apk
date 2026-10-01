package com.stockgrowth.app.model

/**
 * یه پوزیشن (خرید واقعی) توی پورتفولیوی شخصی کاربر. برخلاف StockResult که
 * تحلیل زنده‌ی سروره، این کاملاً محلی (روی خود گوشی) ذخیره می‌شه — سرور
 * هیچی درباره‌ی خریدهای واقعی کاربر نمی‌دونه و نباید بدونه.
 */
data class PortfolioPosition(
    val symbol: String,
    val buyPrice: Double,
    val quantity: Double,
    val stopLoss: Double?,      // حد ضرر برنامه‌ریزی‌شده موقع خرید (ثابت می‌مونه، دنبال قیمت زنده نمی‌ره)
    val takeProfit: Double?,    // حد سود برنامه‌ریزی‌شده موقع خرید
    val addedAtMillis: Long
)
