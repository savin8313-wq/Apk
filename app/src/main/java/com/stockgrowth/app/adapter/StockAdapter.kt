package com.stockgrowth.app.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.stockgrowth.app.R
import com.stockgrowth.app.model.StockResult

class StockAdapter(
    private var items: List<StockResult>,
    private val onItemClick: (StockResult) -> Unit
) : RecyclerView.Adapter<StockAdapter.StockViewHolder>() {

    fun updateData(newItems: List<StockResult>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StockViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_stock, parent, false)
        return StockViewHolder(view)
    }

    override fun onBindViewHolder(holder: StockViewHolder, position: Int) {
        holder.bind(items[position], onItemClick)
    }

    override fun getItemCount(): Int = items.size

    class StockViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvSymbol: android.widget.TextView = itemView.findViewById(R.id.tvSymbol)
        private val tvPrice: android.widget.TextView = itemView.findViewById(R.id.tvPrice)
        private val tvChange: android.widget.TextView = itemView.findViewById(R.id.tvChange)
        private val tvVerdict: android.widget.TextView = itemView.findViewById(R.id.tvVerdict)
        private val tvConfidence: android.widget.TextView = itemView.findViewById(R.id.tvConfidence)
        private val tvAction: android.widget.TextView = itemView.findViewById(R.id.tvAction)
        private val tvActionPercent: android.widget.TextView = itemView.findViewById(R.id.tvActionPercent)
        private val layoutAction: android.widget.LinearLayout = itemView.findViewById(R.id.layoutAction)

        fun bind(item: StockResult, onItemClick: (StockResult) -> Unit) {
            tvSymbol.text = item.symbol
            tvPrice.text = "%,.0f ریال".format(item.lastPrice)

            val changeSign = if (item.changePercent >= 0) "+" else ""
            tvChange.text = "$changeSign%.2f%%".format(item.changePercent)
            tvChange.setTextColor(
                if (item.changePercent >= 0)
                    ContextCompat.getColor(itemView.context, R.color.green_growth)
                else
                    ContextCompat.getColor(itemView.context, R.color.red_decline)
            )

            tvVerdict.text = item.verdict
            val verdictColor = when (item.verdict) {
                "رشد" -> R.color.green_growth
                "مستعد رشد" -> R.color.teal_early_growth
                "شروع ریزش" -> R.color.amber_warning
                "ریزش" -> R.color.red_decline
                else -> R.color.neutral_gray
            }
            tvVerdict.setTextColor(ContextCompat.getColor(itemView.context, verdictColor))

            if (item.dayTrading?.qualifies == true) {
                tvConfidence.text =
                    "اطمینان: ${item.confidence} (امتیاز ${item.score})  •  نوسان‌گیری: ${item.dayTrading.score?.toInt() ?: 0}/100"
                tvConfidence.setTextColor(ContextCompat.getColor(itemView.context, R.color.purple_day_trading))
            } else {
                tvConfidence.text = "اطمینان: ${item.confidence}  (امتیاز ${item.score})"
                tvConfidence.setTextColor(ContextCompat.getColor(itemView.context, R.color.neutral_gray))
            }

            val buyPct = item.buyPercent ?: 50
            val sellPct = item.sellPercent ?: 50
            val isBuy = (item.action ?: "خرید") == "خرید"

            tvAction.text = item.action ?: "خرید"
            tvActionPercent.text = "خرید %d٪  •  فروش %d٪".format(buyPct, sellPct)

            if (isBuy) {
                tvAction.setTextColor(ContextCompat.getColor(itemView.context, R.color.green_growth))
                layoutAction.setBackgroundResource(R.drawable.bg_action_buy)
            } else {
                tvAction.setTextColor(ContextCompat.getColor(itemView.context, R.color.red_decline))
                layoutAction.setBackgroundResource(R.drawable.bg_action_sell)
            }

            itemView.setOnClickListener { onItemClick(item) }
        }
    }
}
