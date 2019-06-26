package io.nebulas.wallet.android.module.detail.fragment

import android.annotation.SuppressLint
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import io.nebulas.wallet.android.R
import io.nebulas.wallet.android.common.DataCenter
import io.nebulas.wallet.android.module.balance.model.Coin
import io.nebulas.wallet.android.module.wallet.create.model.Wallet
import io.nebulas.wallet.android.util.getWalletColorCircleDrawable
import io.nebulas.wallet.android.view.LineDecoration
import kotlinx.android.synthetic.nas_nano.fragment_wallet_list_for_token_detail.*
import org.jetbrains.anko.dip
import org.jetbrains.anko.find

/**
 * Token详情页面的钱包列表
 * Created by young on 2018/6/26.
 */
class WalletListFragment : Fragment() {

    companion object {
        private const val PARAM_TOKEN_ID = "tokenId"
        fun newInstance(tokenId: String): WalletListFragment {
            return WalletListFragment().apply {
                arguments = Bundle().apply {
                    putString(PARAM_TOKEN_ID, tokenId)
                }
            }
        }
    }

    data class ItemWrapper(val wallet: Wallet, val coin: Coin)

    private val items = mutableListOf<ItemWrapper>()
    private val adapter = WalletAdapter()
    private lateinit var tokenId: String

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_wallet_list_for_token_detail, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        tokenId = arguments?.getString(PARAM_TOKEN_ID) ?: ""
        recyclerViewForWallets.layoutManager = LinearLayoutManager(context)
        //recyclerViewForWallets.addItemDecoration(LineDecoration(resources.getColor(R.color.divider_line), context!!.dip(1), context!!.dip(18)))
        recyclerViewForWallets.adapter = adapter

        loadWallets()
    }

    private fun loadWallets() {
        val allWallets = DataCenter.wallets
        val walletMap = HashMap<String, Wallet>((allWallets.size / 0.75f).toInt())
        allWallets.forEach {
            walletMap.put(it.id.toString(), it)
        }
        DataCenter.coins.filter {
            it.tokenId == tokenId
        }.forEach { coin ->
            val wallet = walletMap[coin.walletId.toString()]
            wallet?.let {
                if (it.walletName.isNotEmpty()) {
                    items.add(ItemWrapper(it, coin))
                }
            }
        }
        if (items.size > 0) {
            adapter.notifyDataSetChanged()
        } else {
            recyclerViewForWallets.visibility = View.GONE
            layoutEmpty.visibility = View.VISIBLE
        }
    }

    private fun onItemSelected(index: Int) {
        val itemWrapper = items[index]
        val wallet = itemWrapper.wallet
        //跳转到钱包详情   本期不做点击跳转
//        WalletDetailActivity.launch(context!!, 0, wallet.id, wallet.walletName, index)
    }


    class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val icIcon = itemView.find<ImageView>(R.id.ivIcon)
        val tvWalletName = itemView.find<TextView>(R.id.tvWalletName)
        val tvValue = itemView.find<TextView>(R.id.tvValue)
    }

    inner class WalletAdapter : RecyclerView.Adapter<VH>(), View.OnClickListener {

        override fun onClick(v: View?) {
            if (v == null) {
                return
            }
            onItemSelected(v.tag as Int)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            return VH(layoutInflater.inflate(R.layout.item_wallet_list_for_token_detail, parent, false))
//                    .apply {
//                        setOnClickListener(this@WalletAdapter)
//                    }
        }

        @SuppressLint("SetTextI18n")
        override fun onBindViewHolder(holder: VH, position: Int) {
            val item = items[position]
            context?.apply {
                holder.icIcon.setImageDrawable(getWalletColorCircleDrawable(this, item.wallet.id, item.wallet.walletName[0]))
            }
            holder.tvWalletName.text = item.wallet.walletName
            holder.tvValue.text = "${item.coin.balanceString} ${item.coin.symbol}"
            holder.itemView.tag = position
        }

        override fun getItemCount(): Int = items.size
    }

}