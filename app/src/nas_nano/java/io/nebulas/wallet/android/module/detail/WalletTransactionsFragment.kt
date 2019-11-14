package io.nebulas.wallet.android.module.detail

import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.alibaba.fastjson.JSON
import io.nebulas.wallet.android.R
import io.nebulas.wallet.android.atp.AtpHolder
import io.nebulas.wallet.android.base.BaseBindingAdapter
import io.nebulas.wallet.android.base.BaseFragment
import io.nebulas.wallet.android.common.Constants
import io.nebulas.wallet.android.common.DataCenter
import io.nebulas.wallet.android.common.firebaseAnalytics
import io.nebulas.wallet.android.module.balance.model.Coin
import io.nebulas.wallet.android.module.detail.adapter.WalletDetailAdapter
import io.nebulas.wallet.android.module.detail.model.WalletDetailModel
import io.nebulas.wallet.android.module.transaction.TxDetailActivity
import io.nebulas.wallet.android.module.transaction.model.Transaction
import io.nebulas.wallet.android.module.transaction.viewmodel.TxViewModel
import io.nebulas.wallet.android.network.server.model.WalletReq
import kotlinx.android.synthetic.nas_nano.wallet_detail_transaction_fragment.*
import org.jetbrains.anko.doAsync

/**
 * Created by alina on 2018/6/26.
 */
class WalletTransactionsFragment : BaseFragment() {
    private lateinit var txViewModel: TxViewModel
    private lateinit var walletAdapter: WalletDetailAdapter
    private lateinit var walletReq: WalletReq
    private var isFirstLoad = true
    var isLoading = false
    var isRefreshing = false
    var noMore = true
    var lastVisibleItem = 0
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.wallet_detail_transaction_fragment, container, false)
    }

    override fun initView() {
        txViewModel = ViewModelProviders.of(this).get(TxViewModel::class.java)
        val linearLayoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        walletDetailTransactionRecyclerView.layoutManager = linearLayoutManager
        walletAdapter = WalletDetailAdapter(context!!)
        walletDetailTransactionRecyclerView.adapter = walletAdapter

        /**
         * 下拉刷新
         */
        swipeRefreshLayout.setOnRefreshListener {
            if (isRefreshing) {
                return@setOnRefreshListener
            }
            swipeRefreshLayout.isRefreshing = true
            isRefreshing = true
            this.noMore = true
            isFirstLoad = true
            getWalletReq()
            loadData {
                isLoading = false
                isRefreshing = false

                swipeRefreshLayout.isRefreshing = false
            }
        }
        /**
         * 加载更多
         */
        walletDetailTransactionRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView?, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                lastVisibleItem = linearLayoutManager.findLastVisibleItemPosition()
            }

            override fun onScrollStateChanged(recyclerView: RecyclerView?, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)

                if ((!noMore) && (lastVisibleItem == walletAdapter.itemCount - 1) && !isLoading) {
                    isFirstLoad = false
                    isLoading = true
                    doLoad()
                }
            }
        })
        walletAdapter.setOnClickListener(object : BaseBindingAdapter.OnItemClickListener {
            override fun onItemClick(view: View, position: Int) {
                val tx = walletAdapter.items[position].transaction ?: return
                if (AtpHolder.isRenderable(tx.txData)) {
                    val address = if (tx.isSend) {
                        tx.sender ?: ""
                    } else {
                        tx.receiver ?: ""
                    }
                    firebaseAnalytics.logEvent(Constants.TxRecord_ATPAds_Click, Bundle())
                    AtpHolder.route(requireActivity(), tx.txData, address)
                    return
                }
                gotoTransactions(position)
            }

            override fun onItemLongClick(view: View, position: Int) {
            }
        })

    }

    private fun gotoTransactions(position: Int) {
        var coin: Coin? = null
        val transaction = walletAdapter.items[position].transaction ?: return

        run breakPoint@{
            DataCenter.coins.forEach {
                if (it.tokenId == transaction.currencyId) {
                    coin = it
                    return@breakPoint
                }
            }

        }
        if (null != coin)
            TxDetailActivity.launch(context!!, coin!!, transaction)
    }

    override fun onResume() {
        super.onResume()
        swipeRefreshLayout.isRefreshing = true
        isFirstLoad = true
        getWalletReq()
        doLoad()
    }

    private fun doLoad() {
        loadData {
            isLoading = false

            swipeRefreshLayout.isRefreshing = false
        }
    }

    private fun loadData(onFinish: () -> Unit) {
        isHasNetWork()
        txViewModel.getTxRecordsByWallet(isFirstLoad, walletReq, lifecycle, { transactionList ->
            if (transactionList.isNotEmpty()) {
                this.noMore = transactionList.size != txViewModel.pageSize
            } else {
                this.noMore = true
            }
            transactionList.forEach {
                if (Constants.voteContracts.contains(it.receiver)) {
                    val tokenSymbol = Constants.voteContractsMap[it.receiver]?:""
                    val data = it.txData
                    if (!data.isNullOrEmpty()) {
                        try {
                            val txData = String(Base64.decode(data.toByteArray(), Base64.DEFAULT))
                            val json = JSON.parseObject(txData)
                            val function = json.getString("Function")
                            val args = json.getString("Args")
                            if (function == "vote") {
                                it.coinSymbol = tokenSymbol
                                it.currencyId = tokenSymbol
                                val jsonArray = JSON.parseArray(args)
                                it.amount = jsonArray.last().toString()
                            }
                        } catch (e: Exception) {

                        }
                    }
                }
            }
            doAsync {
                DataCenter.addRecentlyTransactions(transactionList)
            }
            setAdapter(transactionList)
            onFinish()
        }, {
            if (isFirstLoad) {
                setAdapter(mutableListOf<Transaction>())
            }
            onFinish()
        })
    }

    private fun setAdapter(transactionList: MutableList<Transaction>) {
        if (transactionList.isEmpty()) {
//            this.noMore = true
            if (isFirstLoad) {
                walletAdapter.items.clear()
                walletAdapter.items.add(WalletDetailModel(null, null, isEmptyView = true))
//                walletAdapter.notifyDataSetChanged()
            }
        } else {
            walletAdapter.items.removeAll { it.hasLoadingMore }
            val list = mutableListOf<WalletDetailModel>()
            transactionList.forEach {
                list.add(WalletDetailModel(null, it))
            }
            if (isFirstLoad) {
                walletAdapter.changeDataSource(list)
            } else {
                walletAdapter.items.addAll(list)
            }
//            walletAdapter.notifyDataSetChanged()
        }

        if (!noMore) {
            walletAdapter.items.add(WalletDetailModel(hasLoadingMore = true))
        } else {
            walletAdapter.items.removeAll { it.hasLoadingMore }
        }

    }

    private fun getWalletReq() {
        val currentWalletCoins = (activity as WalletDetailActivity).currentWalletCoins

        walletReq = WalletReq()
        walletReq.pageSize = txViewModel.pageSize

        val hashMap = HashMap<String, Int>()
        val addressList: MutableList<WalletReq.AdressInfo> = mutableListOf()
        var addressInfo: WalletReq.AdressInfo
        var i = 0
        currentWalletCoins.forEach {
            if (hashMap.containsKey(it.address)) {
                val mAddressInfo = addressList[hashMap[it.address!!]!!]
                mAddressInfo.currencyId!!.add(it.tokenId!!)
            } else {
                addressInfo = WalletReq.AdressInfo()
                addressInfo.address = it.address
                addressInfo.platform = it.platform
                addressInfo.currencyId = ArrayList()
                addressInfo.currencyId!!.add(it.tokenId!!)
                addressList.add(addressInfo)
                hashMap[addressInfo.address!!] = i++
            }
        }
        walletReq.addressList = addressList

    }
}