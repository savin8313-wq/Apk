package com.stockgrowth.app.view

import android.content.Context
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import com.stockgrowth.app.model.PricePoint

/**
 * نمودار سبک قیمت (بدون هیچ کتابخونه‌ی خارجی — فقط Canvas خالص)، شامل:
 *   - خط قیمت پایانی (پررنگ)
 *   - خط MA20 و MA50 (کم‌رنگ‌تر، برای تشخیص روند)
 *   - خط چین حد ضرر (قرمز) و حد سود (سبز)، در صورت وجود
 * طراحی مینیمال و بدون محور اعداد — هدف نشون‌دادن «شکل روند» است، نه
 * خوندن دقیق هر عدد (برای اون، خود قیمت‌ها توی متن دیگه نشون داده می‌شن).
 */
class PriceChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private var points: List<PricePoint> = emptyList()
    private var stopLoss: Double? = null
    private var takeProfit: Double? = null

    private val closePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#2563EB")
        strokeWidth = 5f
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
    }
    private val ma20Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#F59E0B")
        strokeWidth = 3f
        style = Paint.Style.STROKE
    }
    private val ma50Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#8B5CF6")
        strokeWidth = 3f
        style = Paint.Style.STROKE
    }
    private val stopLossPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#D42E2E")
        strokeWidth = 2.5f
        style = Paint.Style.STROKE
        pathEffect = DashPathEffect(floatArrayOf(12f, 8f), 0f)
    }
    private val takeProfitPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1B873F")
        strokeWidth = 2.5f
        style = Paint.Style.STROKE
        pathEffect = DashPathEffect(floatArrayOf(12f, 8f), 0f)
    }

    fun setData(newPoints: List<PricePoint>, stopLossValue: Double?, takeProfitValue: Double?) {
        points = newPoints
        stopLoss = stopLossValue
        takeProfit = takeProfitValue
        invalidate()
    }

    override fun onDraw(canvas: android.graphics.Canvas) {
        super.onDraw(canvas)
        if (points.size < 2) return

        val paddingV = 16f
        val w = width.toFloat()
        val h = height.toFloat()
        val usableH = h - 2 * paddingV

        // محدوده‌ی قیمت برای مقیاس‌بندی عمودی (شامل حد ضرر/سود هم می‌شه تا خط‌ها بیرون از کادر نیفتن)
        val allValues = mutableListOf<Double>()
        points.forEach { p ->
            allValues.add(p.close)
            p.ma20?.let { allValues.add(it) }
            p.ma50?.let { allValues.add(it) }
        }
        stopLoss?.let { allValues.add(it) }
        takeProfit?.let { allValues.add(it) }
        if (allValues.isEmpty()) return

        val minVal = allValues.min()
        val maxVal = allValues.max()
        val range = (maxVal - minVal).takeIf { it > 0 } ?: 1.0

        fun yFor(value: Double): Float {
            val ratio = (value - minVal) / range
            return (h - paddingV - ratio.toFloat() * usableH)
        }

        fun xFor(index: Int): Float {
            return index.toFloat() / (points.size - 1).toFloat() * w
        }

        // خطوط راهنمای حد ضرر/سود (پشت همه‌چیز رسم می‌شن)
        stopLoss?.let { canvas.drawLine(0f, yFor(it), w, yFor(it), stopLossPaint) }
        takeProfit?.let { canvas.drawLine(0f, yFor(it), w, yFor(it), takeProfitPaint) }

        // MA50
        drawSeries(canvas, points.map { it.ma50 }, ::xFor, ::yFor, ma50Paint)
        // MA20
        drawSeries(canvas, points.map { it.ma20 }, ::xFor, ::yFor, ma20Paint)
        // قیمت پایانی (روی همه، پررنگ‌تر)
        drawSeries(canvas, points.map { it.close }, ::xFor, ::yFor, closePaint)
    }

    private fun drawSeries(
        canvas: android.graphics.Canvas,
        values: List<Double?>,
        xFor: (Int) -> Float,
        yFor: (Double) -> Float,
        paint: Paint
    ) {
        val path = android.graphics.Path()
        var started = false
        for (i in values.indices) {
            val v = values[i] ?: continue
            val x = xFor(i)
            val y = yFor(v)
            if (!started) {
                path.moveTo(x, y)
                started = true
            } else {
                path.lineTo(x, y)
            }
        }
        if (started) canvas.drawPath(path, paint)
    }
}
