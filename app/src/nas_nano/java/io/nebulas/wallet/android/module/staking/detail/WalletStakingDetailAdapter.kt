package io.nebulas.wallet.android.module.staking.detail

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import android.widget.TextView
import io.nebulas.wallet.android.R
import io.nebulas.wallet.android.module.staking.ProfitRecord
import io.nebulas.wallet.android.module.staking.StakingTools
import io.nebulas.wallet.android.util.Formatter
import org.jetbrains.anko.dip
import org.jetbrains.anko.find
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat

class WalletStakingDetailAdapter(val context: Context,
                                 val actionListener: ActionListener,
                                 val dataCenter: WalletStakingDetailDataCenter)
    : RecyclerView.Adapter<WalletStakingDetailAdapter.VH>() {

    interface ActionListener {
        fun actionCancelPledge()
    }

    private val dataSource: MutableList<ProfitRecord> = mutableListOf()

    private var isInitLoading = true

    public fun updateDataSource(list: List<ProfitRecord>) {
        dataSource.clear()
        appendDataSource(list)
    }

    public fun appendDataSource(list: List<ProfitRecord>) {
        isInitLoading = false
        dataSource.addAll(list)
        notifyDataSetChanged()
    }

    enum class ViewType {
        WalletStackingProfits, WalletPledgeInfo, WalletProfitsHeader, WalletProfit, NoProfits, LoadMore, None
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            ViewType.WalletStackingProfits.ordinal -> {
                WalletStackingProfitsVH(inflater.inflate(R.layout.layout_wallet_stacking_profit, parent, false))
            }
            ViewType.WalletPledgeInfo.ordinal -> {
                WalletPledgeInfoVH(inflater.inflate(R.layout.layout_wallet_pledge_info, parent, false))
            }
            ViewType.WalletProfitsHeader.ordinal -> {
                WalletProfitsHeaderVH(inflater.inflate(R.layout.layout_profits_list_header, parent, false))
            }
            ViewType.NoProfits.ordinal -> {
                NoProfitsVH(inflater.inflate(R.layout.layout_no_staking_profits, parent, false))
            }
            ViewType.None.ordinal -> {
                VH(View(context))
            }
            ViewType.LoadMore.ordinal -> {
                val layout = RelativeLayout(context)
                val vPadding = context.dip(12)
                val loadingView = LayoutInflater.from(context).inflate(R.layout.loading_view, layout, false)
                layout.setPadding(0, vPadding, 0, vPadding)
                layout.addView(loadingView, RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT).apply {
                    addRule(RelativeLayout.CENTER_IN_PARENT)
                })
                layout.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                LoadMoreVH(layout)
            }
            else -> {
                WalletProfitVH(inflater.inflate(R.layout.layout_stacking_profit_item, parent, false))
            }
        }
    }

    override fun getItemCount(): Int {
        if (isInitLoading) {
            return 0
        }
        val profitsInfo = dataCenter.addressProfits.value ?: return 0
        val footerCount = if (profitsInfo.total_page > profitsInfo.current_page) 1 else 0
        return dataSource.size + 3 + footerCount
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        when (holder) {
            is WalletProfitVH -> {
                val realIdx = position - 3
                if (realIdx < 0 || realIdx >= dataSource.size) {
                    return
                }
                val record = dataSource[realIdx]
                holder.tvProfit.text = "+${StakingTools.naxFormat(record.profit)} NAX"
                holder.tvProfitDate.text = Formatter.timeFormat("MMddHHmm", record.timestamp)
            }
            is WalletStackingProfitsVH -> holder.tvAddressTotalProfits.text = StakingTools.naxFormat(dataCenter.addressProfits.value?.total_profits)
            is WalletPledgeInfoVH -> {
                holder.tvPledgedNas.text = StakingTools.nasFormat2KM(dataCenter.pledgedNas)
                holder.tvPledgedAge.text = formatPledgeAge(dataCenter.pledgedAge)
                holder.tvTotalNas.text = StakingTools.nasFormat2KM(dataCenter.addressBalance)
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        val profitsInfo = dataCenter.addressProfits.value ?: return ViewType.None.ordinal
        return when (position) {
            0 -> ViewType.WalletStackingProfits.ordinal
            1 -> ViewType.WalletPledgeInfo.ordinal
            2 -> {
                if (profitsInfo.total_count<=0){
                    ViewType.NoProfits.ordinal
                } else {
                    ViewType.WalletProfitsHeader.ordinal
                }
            }
            else -> {
                val footerCount = if (profitsInfo.total_page > profitsInfo.current_page) 1 else 0
                if (position == itemCount - 1) {
                    if (footerCount > 0) {
                        ViewType.LoadMore.ordinal
                    } else {
                        ViewType.WalletProfit.ordinal
                    }
                } else {
                    ViewType.WalletProfit.ordinal
                }
            }
        }
    }

    private fun formatPledgeAge(age: String): String {
        return try {
            val df = DecimalFormat("###,##0.00")
            df.format(BigDecimal(age).divide(BigDecimal.ONE, 2, RoundingMode.FLOOR))
        } catch (e: Exception) {
            "0.00"
        }
    }

    open inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView)

    inner class WalletStackingProfitsVH(itemView: View) : VH(itemView) {
        private val tvCancelPledge = itemView.find<TextView>(R.id.tvCancelPledge)
        val tvAddressTotalProfits = itemView.find<TextView>(R.id.tvAddressTotalProfits)

        init {
            tvCancelPledge.setOnClickListener {
                actionListener.actionCancelPledge()
            }
        }
    }

    inner class WalletPledgeInfoVH(itemView: View) : VH(itemView) {
        val tvPledgedNas = itemView.find<TextView>(R.id.tvPledgedNas)
        val tvPledgedAge = itemView.find<TextView>(R.id.tvPledgedAge)
        val tvTotalNas = itemView.find<TextView>(R.id.tvTotalNas)
    }

    inner class WalletProfitsHeaderVH(itemView: View) : VH(itemView)
    inner class LoadMoreVH(itemView: View) : VH(itemView)
    inner class NoProfitsVH(itemView: View) : VH(itemView)

    inner class WalletProfitVH(itemView: View) : VH(itemView) {
        val tvProfit = itemView.find<TextView>(R.id.tvProfit)
        val tvProfitDate = itemView.find<TextView>(R.id.tvProfitDate)
    }
}