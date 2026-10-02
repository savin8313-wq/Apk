package com.stockgrowth.app.view

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import com.stockgrowth.app.model.PricePoint

/**
 * نمودار میله‌ای ساده‌ی حجم معاملات، هم‌تراز با محور افقی نمودار قیمت —
 * برای تشخیص اینکه حرکت قیمت با حجم تأیید شده یا نه.
 */
class VolumeChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private var points: List<PricePoint> = emptyList()

    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#93C5FD")
        style = Paint.Style.FILL
    }

    fun setData(newPoints: List<PricePoint>) {
        points = newPoints
        invalidate()
    }

    override fun onDraw(canvas: android.graphics.Canvas) {
        super.onDraw(canvas)
        if (points.size < 2) return

        val volumes = points.map { it.volume ?: 0.0 }
        val maxVol = volumes.maxOrNull()?.takeIf { it > 0 } ?: return

        val w = width.toFloat()
        val h = height.toFloat()
        val n = points.size
        val slotWidth = w / n
        val barWidth = (slotWidth * 0.7f).coerceAtLeast(2f)

        for (i in 0 until n) {
            val v = volumes[i]
            val barHeight = (v / maxVol).toFloat() * h
            val left = i * slotWidth + (slotWidth - barWidth) / 2f
            val top = h - barHeight
            canvas.drawRect(left, top, left + barWidth, h, barPaint)
        }
    }
}
