package io.nebulas.wallet.android.module.me

import android.arch.lifecycle.Observer
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.nebulas.wallet.android.R
import io.nebulas.wallet.android.base.BaseBindingAdapter
import io.nebulas.wallet.android.base.BaseFragment
import io.nebulas.wallet.android.common.Constants
import io.nebulas.wallet.android.common.Constants.TOKEN_WHITE_LIST
import io.nebulas.wallet.android.common.DataCenter
import io.nebulas.wallet.android.module.detail.BalanceDetailActivity
import io.nebulas.wallet.android.module.detail.HideAssetsActivity
import io.nebulas.wallet.android.module.main.MainActivity
import io.nebulas.wallet.android.module.me.adapter.TokenRecyclerViewAdapter
import io.nebulas.wallet.android.module.me.model.MeTokenListModel
import io.nebulas.wallet.android.util.Util
import kotlinx.android.synthetic.nas_nano.fragment_token.*
import java.math.BigDecimal

/**
 * Created by Heinoc on 2018/5/10.
 */
class TokenFragment : BaseFragment() {

    lateinit var adapter: TokenRecyclerViewAdapter

    var balanceHidden: Boolean = false

    private var initialized = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_token, container, false)
    }

    override fun initView() {
        balanceHidden = Util.getBalanceHidden(context!!)
        tokenRecyclerView.layoutManager = LinearLayoutManager(this.context)

        adapter = TokenRecyclerViewAdapter(this.context!!)

        tokenRecyclerView.adapter = adapter


        adapter.setOnClickListener(object : BaseBindingAdapter.OnItemClickListener {
            override fun onItemClick(view: View, position: Int) {

                if (null != adapter.items[position].coin && !adapter.items[position].hasHideAssets) {
                    BalanceDetailActivity.launch(activity!!, adapter.items[position].coin!!)
                    return
                }
                if (adapter.items[position].hasHideAssets) {
                    HideAssetsActivity.launch(activity!!)
                    return
                }

            }

            override fun onItemLongClick(view: View, position: Int) {

            }
        })

        (context as MainActivity).balanceViewModel().getCoins().observe(this, Observer {
            //            if (adapter.itemCount == 1) {
//                if (DataCenter.coins.size > 0) {
            loadData()
//                }
//            } else {
//
//            }
        })

        initialized = true

        loadData()
    }

    fun refreshBalanceHidden(isHidden: Boolean) {
        balanceHidden = isHidden
        if (!initialized) {
            return
        }
        adapter.items.forEach {
            it.showBalanceDetail = !isHidden
        }
        adapter.notifyDataSetChanged()
    }

    override fun onResume() {
        super.onResume()

        loadData()

    }

    private fun loadData() {

        if (adapter.items.isEmpty() || (adapter.itemCount == 1 && null == adapter.items[0].coin && DataCenter.coins.size > 0)) {

            /**
             * 初始化list
             */
            DataCenter.coinsGroupByCoinSymbol.filter {
                it.isShow
            }.forEach {
                var shown = false
                for (token in TOKEN_WHITE_LIST) {
                    if (token.equals(it.symbol, true)) {
                        shown = true
                        break
                    }
                }
                if (!shown) {
                    shown = BigDecimal(it.balance).toDouble() > 0
                }
                if (shown) {
                    adapter.items.add(MeTokenListModel(
                            coin = it,
                            showBalanceDetail = !balanceHidden,
                            currencyName = Constants.CURRENCY_SYMBOL_NAME,
                            currencySymbol = Constants.CURRENCY_SYMBOL)
                    )
                }
            }

        } else {

            /**
             * 更新资产
             */
            if (DataCenter.wallets.isNotEmpty()) {

                //删除空视图
                adapter.items.removeAll(adapter.items.filter { it.emptyView })

                var isContainToken = false
                DataCenter.coinsGroupByCoinSymbol.forEach { coin ->
                    isContainToken = false
                    var shown = false
                    for (token in TOKEN_WHITE_LIST) {
                        if (token.equals(coin.symbol, true)) {
                            shown = true
                            break
                        }
                    }
                    if (!shown) {
                        shown = BigDecimal(coin.balance).toDouble() > 0
                    }
                    if (!shown){
                        return@forEach
                    }
                    var coinListCount = 0

                    run breakPoint@{
                        adapter.items.forEach {

                            if (null != it.coin) {
                                coinListCount++
                                if (coin.tokenId == it.coin?.tokenId) {

                                    /**
                                     * 更新隐藏币种
                                     */
                                    if (!coin.isShow) {
                                        adapter.items.remove(it)

                                        return@breakPoint
                                    }


                                    isContainToken = true

                                    /**
                                     * 更新资产
                                     */
                                    if (it.currencySymbol != Constants.CURRENCY_SYMBOL || it.coin?.currencyPrice != coin.currencyPrice || it.coin?.balance != coin.balance || it.coin?.balanceValue != coin.balanceValue) {
                                        it.coin?.currencyPrice = coin.currencyPrice
                                        it.coin?.balance = coin.balance
                                        it.coin?.balanceValue = coin.balanceValue
                                        it.currencyName = Constants.CURRENCY_SYMBOL_NAME
                                        it.currencySymbol = Constants.CURRENCY_SYMBOL

                                        adapter.notifyItemChanged(adapter.items.indexOf(it))
                                    }

                                    return@breakPoint

                                }
                            }
                        }
                    }

                    /**
                     * 添加新增加的币种
                     */
                    if (!isContainToken && coin.isShow) {
                        adapter.items.add(coinListCount, MeTokenListModel(
                                coin = coin,
                                showBalanceDetail = !balanceHidden,
                                currencyName = Constants.CURRENCY_SYMBOL_NAME,
                                currencySymbol = Constants.CURRENCY_SYMBOL
                        ))
                    }

                }
            } else {
                /**
                 * 钱包已清空，删除所有item
                 */
                adapter.items.clear()
            }

        }


        if (adapter.items.isEmpty()) {
            /**
             * 添加空视图
             */
            adapter.items.add(MeTokenListModel(emptyView = true))
        } else {

            if (DataCenter.wallets.size > 0) {

                /**
                 * 添加“管理资产”item
                 */
                if (!adapter.items[adapter.itemCount - 1].hasManageAssetsBtn) {
                    adapter.items.add(MeTokenListModel(hasManageAssetsBtn = true))
                }

                /**
                 * 添加“隐藏资产”item
                 */
                if (DataCenter.coins.any { null != it.isShow && !it.isShow } && adapter.items.none { it.hasHideAssets }) {
                    adapter.items.add(adapter.itemCount - 1,
                            MeTokenListModel(
                                    showBalanceDetail = !balanceHidden,
                                    currencyName = Constants.CURRENCY_SYMBOL_NAME,
                                    currencySymbol = Constants.CURRENCY_SYMBOL,
                                    hasHideAssets = true)
                    )
                }

                if (DataCenter.coins.none { null != it.isShow && !it.isShow }) {
                    //删除“隐藏资产”item
                    adapter.items.removeAll(adapter.items.filter { it.hasHideAssets })
                }


                //删除空视图
                adapter.items.removeAll(adapter.items.filter { it.emptyView })

            }

        }

        adapter.notifyDataSetChanged()

    }

}