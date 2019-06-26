package io.nebulas.wallet.android.module.detail

import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.nebulas.wallet.android.R
import io.nebulas.wallet.android.base.BaseFragment
import io.nebulas.wallet.android.module.balance.viewmodel.BalanceViewModel
import io.nebulas.wallet.android.module.detail.adapter.WalletDetailAdapter
import io.nebulas.wallet.android.module.detail.model.WalletDetailModel
import kotlinx.android.synthetic.nas_nano.wallet_detail_token_fragment.*

/**
 * Created by alina on 2018/6/26.
 */
class WalletTokenFragment : BaseFragment() {

    private lateinit var balanceViewModel: BalanceViewModel
    private lateinit var walletAdapter: WalletDetailAdapter
    private lateinit var list: MutableList<WalletDetailModel>
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.wallet_detail_token_fragment, container, false)
    }

    override fun onResume() {
        super.onResume()
        doLoad()
    }


    override fun initView() {
        balanceViewModel = ViewModelProviders.of(activity@ this).get(BalanceViewModel::class.java)
        var linearLayoutManager = LinearLayoutManager(this@WalletTokenFragment.context!!, LinearLayoutManager.VERTICAL, false)
        walletAdapter = WalletDetailAdapter(this@WalletTokenFragment.context!!)
        list = mutableListOf()
        walletDetailTokeRecyclerView.layoutManager = linearLayoutManager
        walletDetailTokeRecyclerView.adapter = walletAdapter
    }

    private fun doLoad() {
        loadData()
        setAdapterData(list)
    }

    private fun loadData() {
        list.clear()
        val currentWalletCoins = (activity as WalletDetailActivity).currentWalletCoins
        currentWalletCoins.sortBy { it.displayed }
        currentWalletCoins.forEach {
            list.add(WalletDetailModel(it, null))
        }
    }


    private fun setAdapterData(list: MutableList<WalletDetailModel>) {
        if (list.isEmpty()) {
            list.add(WalletDetailModel(null, null, isEmptyView = true))
        }
        walletAdapter.items.clear()
        walletAdapter.items.addAll(list)
        //walletAdapter.notifyDataSetChanged()
    }
}