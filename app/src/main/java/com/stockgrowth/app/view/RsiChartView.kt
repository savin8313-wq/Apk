package com.stockgrowth.app.view

import android.content.Context
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import com.stockgrowth.app.model.PricePoint

/**
 * نمودار RSI (همیشه بین ۰ تا ۱۰۰) با دو خط راهنمای افقی در ۳۰ و ۷۰ —
 * ناحیه‌ی بالای ۷۰ یعنی اشباع خرید، زیر ۳۰ یعنی اشباع فروش.
 */
class RsiChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private var points: List<PricePoint> = emptyList()

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#7C3AED")
        strokeWidth = 4f
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
    }
    private val guideLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#9CA3AF")
        strokeWidth = 2f
        style = Paint.Style.STROKE
        pathEffect = DashPathEffect(floatArrayOf(8f, 6f), 0f)
    }

    fun setData(newPoints: List<PricePoint>) {
        points = newPoints
        invalidate()
    }

    override fun onDraw(canvas: android.graphics.Canvas) {
        super.onDraw(canvas)
        if (points.size < 2) return

        val w = width.toFloat()
        val h = height.toFloat()

        fun yFor(value: Double): Float = h - (value / 100.0).toFloat() * h

        // خطوط راهنمای ۳۰ و ۷۰
        canvas.drawLine(0f, yFor(70.0), w, yFor(70.0), guideLinePaint)
        canvas.drawLine(0f, yFor(30.0), w, yFor(30.0), guideLinePaint)

        val path = android.graphics.Path()
        var started = false
        for (i in points.indices) {
            val rsi = points[i].rsi ?: continue
            val x = i.toFloat() / (points.size - 1).toFloat() * w
            val y = yFor(rsi)
            if (!started) {
                path.moveTo(x, y)
                started = true
            } else {
                path.lineTo(x, y)
            }
        }
        if (started) canvas.drawPath(path, linePaint)
    }
}
