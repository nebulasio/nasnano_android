package io.nebulas.wallet.android.module.stacking

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.nebulas.wallet.android.R
import io.nebulas.wallet.android.module.wallet.create.model.Wallet

open class VH(itemView: View) : RecyclerView.ViewHolder(itemView)

class SummaryVH(itemView: View) : VH(itemView) {

}

class PledgeActionVH(itemView: View) : VH(itemView) {

}

class TextVH(itemView: View) : VH(itemView) {

}

class PledgedWalletVH(itemView: View) : VH(itemView) {

}

class EmptyVH(itemView: View) : VH(itemView) {

}

class StackingAdapter(private val pledgedWallets: List<Wallet>) : RecyclerView.Adapter<VH>() {

    enum class ViewType {
        Summary, PledgeAction, Text, PledgedWallet, Empty
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
        return pledgedWallets.size + 3
    }

    override fun onBindViewHolder(holder: VH, position: Int) {

    }

    override fun getItemViewType(position: Int): Int {
        return when (position) {
            0 -> ViewType.Summary.ordinal
            1 -> ViewType.PledgeAction.ordinal
            2 -> {
                if (pledgedWallets.isEmpty()){
                    ViewType.Empty.ordinal
                } else {
                    ViewType.Text.ordinal
                }
            }
            else -> ViewType.PledgedWallet.ordinal
        }
    }
}