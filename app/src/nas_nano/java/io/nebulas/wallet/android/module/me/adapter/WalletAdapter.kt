package io.nebulas.wallet.android.module.me.adapter

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import io.nebulas.wallet.android.R
import io.nebulas.wallet.android.common.Constants
import io.nebulas.wallet.android.common.DataCenter
import io.nebulas.wallet.android.extensions.logD
import io.nebulas.wallet.android.module.wallet.create.model.Wallet
import io.nebulas.wallet.android.util.AConverter
import io.nebulas.wallet.android.util.Formatter
import io.nebulas.wallet.android.util.getWalletColorCircleDrawable
import io.nebulas.wallet.android.view.AutoFitTextView
import org.jetbrains.anko.find
import java.math.BigDecimal

class WalletAdapter(val callback: WalletAdapterCallback?, private var _isBalanceHidden: Boolean = false) : RecyclerView.Adapter<VH>(), View.OnClickListener {

    interface WalletAdapterCallback {
        fun onCreateWalletClicked()
        fun onBackupClicked(wallet: Wallet)
        fun onItemClicked(wallet: Wallet, position: Int)
    }

    companion object {
        private const val VIEW_TYPE_WALLET = 0
        private const val VIEW_TYPE_BUTTON = 1
    }

    private val dataSource: MutableList<Wallet> = mutableListOf()

    fun setDataSource(wallets: List<Wallet>) {
        dataSource.clear()
        dataSource.addAll(wallets)
        notifyDataSetChanged()
    }

    fun isBalanceHidden(hidden: Boolean) {
        _isBalanceHidden = hidden
        notifyDataSetChanged()
    }

    override fun onClick(v: View?) {
        if (v == null) {
            return
        }
        val index = v.tag as Int
        if (index < 0 || index >= dataSource.size) {
            return
        }
        val wallet = dataSource[index]
        when (v.id) {
            R.id.noticeLayout -> {
                callback?.onBackupClicked(wallet)
            }
            R.id.layoutRoot -> {
                callback?.onItemClicked(wallet, index)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        return when (viewType) {
            VIEW_TYPE_WALLET -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.item_wallet_list_wallet, parent, false)
                val holder = WalletHolder(view)
                holder.itemView.setOnClickListener(this)
                holder.noticeLayout.setOnClickListener(this)
                holder
            }
            else -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.item_wallet_list_button, parent, false)
                view.setOnClickListener {
                    callback?.onCreateWalletClicked()
                }
                ButtonHolder(view)
            }
        }
    }

    override fun getItemCount(): Int {
        return if (dataSource.isEmpty()) {
            0
        } else {
            dataSource.size + 1
        }
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        when (holder) {
            is WalletHolder -> {
                val wallet = dataSource[position]
                holder.noticeLayout.tag = position
                holder.itemView.tag = position
                holder.coinNameTV.text = wallet.walletName
                holder.walletTotalValueTV.text = if (_isBalanceHidden) {
                    "****"
                } else {
                    var totalValue = BigDecimal(0)
                    DataCenter.coins.forEach {
                        if (it.walletId == wallet.id) {
                            totalValue += BigDecimal(it.balanceValue)
                        }
                    }
                    "≈" + Constants.CURRENCY_SYMBOL + Formatter.priceFormat(totalValue)
                }
                holder.noticeLayout.visibility = if (wallet.isNeedBackup()) {
                    View.VISIBLE
                } else {
                    View.GONE
                }
                holder.walletIconTV.background = getWalletColorCircleDrawable(holder.walletIconTV.context, wallet.id)

                //判断第一个是否为表情
                if (wallet.walletName.first() == '\uD83D' || wallet.walletName.first() == '\uD83C' || wallet.walletName.first() == '\uD83E'){
                    holder.walletIconTV.text = wallet.walletName.substring(0, 2)
                } else {
                    holder.walletIconTV.text = wallet.walletName.first().toString().toUpperCase()
                }
                val tokenIcons = mutableListOf<String>()

                DataCenter.coins.filter {
                    it.walletId == wallet.id
                }.sortedBy {
                    it.displayed
                }.map {
                    it.logo
                }.toCollection(tokenIcons)
                AConverter.loadTokenIconsWithLayoutCache(holder.layoutTokenList, tokenIcons)
            }
            else -> {
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (position < dataSource.size) {
            VIEW_TYPE_WALLET
        } else {
            VIEW_TYPE_BUTTON
        }
    }
}

abstract class VH(itemView: View) : RecyclerView.ViewHolder(itemView)

class WalletHolder(itemView: View) : VH(itemView) {
    val noticeLayout = itemView.find<View>(R.id.noticeLayout)
    val coinNameTV = itemView.find<TextView>(R.id.coinNameTV)
    val walletTotalValueTV = itemView.find<TextView>(R.id.wallet_total_value_tv)
    val layoutTokenList = itemView.find<LinearLayout>(R.id.layoutTokenList)
    val walletIconTV = itemView.find<TextView>(R.id.walletIconTV)
}

class ButtonHolder(itemView: View) : VH(itemView)