package io.nebulas.wallet.android.module.detail.adapter

import android.content.Context
import android.os.Bundle
import android.support.v7.widget.RecyclerView
import android.util.Base64
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.alibaba.fastjson.JSON
import io.nebulas.wallet.android.R
import io.nebulas.wallet.android.atp.AtpHolder
import io.nebulas.wallet.android.base.BaseBindingAdapter
import io.nebulas.wallet.android.base.BaseBindingViewHolder
import io.nebulas.wallet.android.common.Constants
import io.nebulas.wallet.android.common.firebaseAnalytics
import io.nebulas.wallet.android.databinding.ItemWalletDetailBinding
import io.nebulas.wallet.android.module.detail.model.WalletDetailModel
import org.jetbrains.anko.find
import org.jetbrains.anko.findOptional
import java.lang.Exception

/**
 * Created by alina on 2018/6/27.
 */
class WalletDetailAdapter(context: Context) : BaseBindingAdapter<WalletDetailModel, ItemWalletDetailBinding>(context) {

    class VH(itemView: View) : BaseBindingViewHolder(itemView) {
        val ivAtpAds = itemView.findOptional<ImageView>(R.id.ivAtpAds)
        val amount_tv = itemView.findOptional<TextView>(R.id.amount_tv)
        val ivVote = itemView.find<ImageView>(R.id.ivVote)
    }

    override fun getViewHolder(itemView: View, viewType: Int): BaseBindingViewHolder {
        return VH(itemView)
    }

    override fun getLayoutResId(viewType: Int): Int {
        return R.layout.item_wallet_detail
    }

    override fun onBindItem(binding: ItemWalletDetailBinding?, item: WalletDetailModel) {
        binding?.item = item
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        super.onBindViewHolder(holder, position)
        if (holder is VH) {
            val tx = items[position].transaction
            if (tx == null) {
                holder.ivAtpAds?.visibility = View.GONE
                return
            }
            if (AtpHolder.isRenderable(tx.txData)) {
                firebaseAnalytics.logEvent(Constants.TxRecord_ATPAds_Show, Bundle())
                holder.ivAtpAds?.visibility = View.VISIBLE
                holder.amount_tv?.visibility = View.GONE
            } else {
                holder.ivAtpAds?.visibility = View.GONE
                holder.amount_tv?.visibility = View.VISIBLE
            }
            holder.ivVote.visibility = View.GONE
            if (Constants.voteContracts.contains(tx.receiver)) {
                val data = tx.txData
                if (!data.isNullOrEmpty()) {
                    try {
                        val txData = String(Base64.decode(data.toByteArray(), Base64.DEFAULT))
                        val json = JSON.parseObject(txData)
                        val function = json.getString("Function")
                        if (function == "vote") {
                            holder.ivVote.visibility = View.VISIBLE
                        }
                    } catch (e: Exception) {

                    }
                }
            }
        }
    }
}