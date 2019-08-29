package io.nebulas.wallet.android.module.staking.dashboard

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import io.nebulas.wallet.android.R
import io.nebulas.wallet.android.common.DataCenter
import io.nebulas.wallet.android.module.staking.StakingSummary
import io.nebulas.wallet.android.module.staking.StakingTools
import io.nebulas.wallet.android.module.wallet.create.model.Wallet
import io.nebulas.wallet.android.util.getWalletColorCircleDrawable
import org.jetbrains.anko.find
import walletcore.Walletcore

class StakingDashboardAdapter(private val context: Context,
                              private val actionListener: ActionListener,
                              private val dataCenter: StakingDashboardDataCenter) : RecyclerView.Adapter<StakingDashboardAdapter.VH>() {


    interface ActionListener {
        fun actionPledge()
        fun actionToWalletProfitDetail(wallet: Wallet)
    }

    enum class ViewType {
        Summary, PledgeAction, Text, PledgedWallet, Empty
    }

    enum class WalletStatusType(desc:String){
        Normal("正常已质押状态"),
        InPledging("发起了质押的交易，交易状态未确认"),
        InCancel("发起了取消质押的交易，交易状态未确认")
    }

    class WalletWrapper(val wallet: Wallet, var status:WalletStatusType)

    private val pledgedWallets: MutableList<WalletWrapper> = mutableListOf()

    private var isInitLoading = true

    public fun updateDataSource(wallets: List<WalletWrapper>) {
        isInitLoading = false
        pledgedWallets.clear()
        pledgedWallets.addAll(wallets)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            ViewType.Summary.ordinal -> {
                SummaryVH(inflater.inflate(R.layout.layout_stacking_profits_info, parent, false))
            }
            ViewType.PledgeAction.ordinal -> {
                PledgeActionVH(inflater.inflate(R.layout.layout_btn_pledge, parent, false))
            }
            ViewType.Text.ordinal -> {
                TextVH(inflater.inflate(R.layout.layout_text_only, parent, false))
            }
            ViewType.Empty.ordinal -> {
                EmptyVH(inflater.inflate(R.layout.layout_no_pledged_wallet, parent, false))
            }
            else -> {
                PledgedWalletVH(inflater.inflate(R.layout.layout_pledged_wallet, parent, false))
            }
        }
    }

    override fun getItemCount(): Int {
        if (isInitLoading) {
            return 0
        }
        return pledgedWallets.size + 3
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        if (holder is PledgedWalletVH) {
            val realIndex = position - 3
            if (realIndex < pledgedWallets.size && realIndex >= 0) {
                holder.updateData(pledgedWallets[realIndex])
            }
        } else if (holder is SummaryVH) {
            val stakingSummary = dataCenter.stakingSummary.value
            holder.updateData(stakingSummary)
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (position) {
            0 -> ViewType.Summary.ordinal
            1 -> ViewType.PledgeAction.ordinal
            2 -> {
                if (pledgedWallets.isEmpty()) {
                    ViewType.Empty.ordinal
                } else {
                    ViewType.Text.ordinal
                }
            }
            else -> ViewType.PledgedWallet.ordinal
        }
    }


    open inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private var obj: Any? = null

        open fun updateData(obj: Any?) {
            this.obj = obj
        }

        fun getData(): Any? = obj
    }

    inner class SummaryVH(itemView: View) : VH(itemView) {
        private val tvTotalProfits = itemView.find<TextView>(R.id.tvTotalProfits)
        private val tvLastProfit = itemView.find<TextView>(R.id.tvLastProfit)
        private val tvPledgeRate = itemView.find<TextView>(R.id.tvPledgeRate)
        private val tvEstimateNax = itemView.find<TextView>(R.id.tvEstimateNax)
        private val tvLastDestroyedNax = itemView.find<TextView>(R.id.tvLastDestroyedNax)
        private val tvLastDistributedNax = itemView.find<TextView>(R.id.tvLastDistributedNax)

        override fun updateData(obj: Any?) {
            super.updateData(obj)
            val stakingSummary = (obj as StakingSummary?) ?: return
            val stakingInfo = stakingSummary.stage?:return
            val profitsInfo = stakingSummary.profits?:return
            tvTotalProfits.text = StakingTools.naxFormat(profitsInfo.total_profits)
            tvLastProfit.text = StakingTools.naxFormat(profitsInfo.last_total_profits)
            tvPledgeRate.text = StakingTools.calculatePledgeRate(stakingInfo.pledged_nas, stakingInfo.total_nas)
            tvEstimateNax.text = StakingTools.naxFormat(stakingInfo.estimate_nax)
            tvLastDestroyedNax.text = StakingTools.naxFormat(stakingInfo.last_destroyed_nax)
            tvLastDistributedNax.text = StakingTools.naxFormat(stakingInfo.last_distributed_nax)
        }
    }

    inner class PledgeActionVH(itemView: View) : VH(itemView) {
        init {
            itemView.setOnClickListener {
                actionListener.actionPledge()
            }
        }
    }

    inner class TextVH(itemView: View) : VH(itemView)

    inner class PledgedWalletVH(itemView: View) : VH(itemView) {
        private val ivWalletIcon = itemView.find<ImageView>(R.id.ivWalletIcon)
        private val tvWalletName = itemView.find<TextView>(R.id.tvWalletName)
        private val tvWalletAddress = itemView.find<TextView>(R.id.tvWalletAddress)
        private val tvLastProfit = itemView.find<TextView>(R.id.tvLastProfit)
        private val tvProfitDate = itemView.find<TextView>(R.id.tvProfitDate)

        init {
            itemView.setOnClickListener {
                val walletWrapper = getData() as WalletWrapper
                if (walletWrapper.status!=WalletStatusType.Normal){
                    return@setOnClickListener
                }
                actionListener.actionToWalletProfitDetail(walletWrapper.wallet)
            }
        }

        override fun updateData(obj: Any?) {
            super.updateData(obj)
            obj ?: return
            val walletWrapper = obj as WalletWrapper
            val wallet = walletWrapper.wallet
            val c = if (wallet.walletName.isEmpty()) {
                ' '
            } else {
                wallet.walletName[0]
            }
            ivWalletIcon.setImageDrawable(getWalletColorCircleDrawable(context, wallet.id, c))
            tvWalletName.text = wallet.walletName
            val address = DataCenter.addresses.find { it.walletId == wallet.id && it.platform == Walletcore.NAS }
            address ?: return
            tvWalletAddress.text = address.address
            when(walletWrapper.status){
                WalletStatusType.Normal->{
                    val lastProfits = dataCenter.stakingSummary.value?.profits?.last_profits ?: mapOf()
                    val profit = lastProfits[address.address] ?: "0"
                    tvLastProfit.text = "+${StakingTools.naxFormat(profit)} NAX"
                    tvProfitDate.visibility = View.VISIBLE
                }
                else -> {
                    tvLastProfit.text = "上链中"
                }
            }

        }

    }

    inner class EmptyVH(itemView: View) : VH(itemView)
}