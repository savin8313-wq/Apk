package com.stockgrowth.app.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.stockgrowth.app.R
import com.stockgrowth.app.model.PortfolioPosition

/** ترکیب یه پوزیشن ذخیره‌شده با آخرین قیمت زنده‌ای که از سرور گرفتیم. */
data class PortfolioDisplayItem(
    val position: PortfolioPosition,
    val livePrice: Double?
)

class PortfolioAdapter(
    private var items: List<PortfolioDisplayItem>,
    private val onItemLongClick: (PortfolioPosition) -> Unit
) : RecyclerView.Adapter<PortfolioAdapter.PortfolioViewHolder>() {

    fun updateData(newItems: List<PortfolioDisplayItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PortfolioViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_portfolio, parent, false)
        return PortfolioViewHolder(view)
    }

    override fun onBindViewHolder(holder: PortfolioViewHolder, position: Int) {
        holder.bind(items[position], onItemLongClick)
    }

    override fun getItemCount(): Int = items.size

    class PortfolioViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvSymbol: android.widget.TextView = itemView.findViewById(R.id.tvPfSymbol)
        private val tvPnlPercent: android.widget.TextView = itemView.findViewById(R.id.tvPfPnlPercent)
        private val tvPrices: android.widget.TextView = itemView.findViewById(R.id.tvPfPrices)
        private val tvPnlAmount: android.widget.TextView = itemView.findViewById(R.id.tvPfPnlAmount)
        private val tvAlert: android.widget.TextView = itemView.findViewById(R.id.tvPfAlert)

        fun bind(item: PortfolioDisplayItem, onLongClick: (PortfolioPosition) -> Unit) {
            val pos = item.position
            tvSymbol.text = pos.symbol

            val live = item.livePrice
            if (live == null) {
                tvPnlPercent.text = "—"
                tvPnlPercent.setTextColor(ContextCompat.getColor(itemView.context, R.color.neutral_gray))
                tvPrices.text = "خرید: %,.0f ریال   —   قیمت زنده در دسترس نیست".format(pos.buyPrice)
                tvPnlAmount.text = ""
                tvAlert.visibility = View.GONE
            } else {
                val pnlPercent = (live - pos.buyPrice) / pos.buyPrice * 100
                val pnlAmount = (live - pos.buyPrice) * pos.quantity
                val sign = if (pnlPercent >= 0) "+" else ""

                tvPnlPercent.text = "$sign%.1f٪".format(pnlPercent)
                val color = if (pnlPercent >= 0) R.color.green_growth else R.color.red_decline
                tvPnlPercent.setTextColor(ContextCompat.getColor(itemView.context, color))

                tvPrices.text = "خرید: %,.0f ریال   →   الان: %,.0f ریال".format(pos.buyPrice, live)
                tvPnlAmount.text = "سود/زیان: $sign%,.0f ریال   (تعداد: %,.0f)".format(pnlAmount, pos.quantity)

                when {
                    pos.takeProfit != null && live >= pos.takeProfit -> {
                        tvAlert.text = "🎯 به حد سود رسیده! (%,.0f ریال)".format(pos.takeProfit)
                        tvAlert.setTextColor(ContextCompat.getColor(itemView.context, R.color.green_growth))
                        tvAlert.visibility = View.VISIBLE
                    }
                    pos.stopLoss != null && live <= pos.stopLoss -> {
                        tvAlert.text = "🛑 به حد ضرر رسیده! (%,.0f ریال)".format(pos.stopLoss)
                        tvAlert.setTextColor(ContextCompat.getColor(itemView.context, R.color.red_decline))
                        tvAlert.visibility = View.VISIBLE
                    }
                    else -> tvAlert.visibility = View.GONE
                }
            }

            itemView.setOnLongClickListener {
                onLongClick(pos)
                true
            }
        }
    }
}
