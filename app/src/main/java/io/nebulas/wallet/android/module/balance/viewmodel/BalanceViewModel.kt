package io.nebulas.wallet.android.module.balance.viewmodel

import android.arch.lifecycle.*
import android.content.Context
import android.util.Base64
import com.alibaba.fastjson.JSON
import com.google.gson.Gson
import com.young.polling.PollingFutureTask
import com.young.polling.SyncManager
import io.nebulas.wallet.android.R
import io.nebulas.wallet.android.app.WalletApplication
import io.nebulas.wallet.android.atp.AtpHolder
import io.nebulas.wallet.android.common.Constants
import io.nebulas.wallet.android.common.DataCenter
import io.nebulas.wallet.android.common.TransactionPollingTaskInfo
import io.nebulas.wallet.android.common.URLConstants
import io.nebulas.wallet.android.configuration.Configuration
import io.nebulas.wallet.android.db.DBUtil
import io.nebulas.wallet.android.module.balance.ScreeningItemHelper
import io.nebulas.wallet.android.module.balance.model.BalanceListModel
import io.nebulas.wallet.android.module.balance.model.Coin
import io.nebulas.wallet.android.module.balance.model.SwapItem
import io.nebulas.wallet.android.module.swap.SwapHelper
import io.nebulas.wallet.android.module.transaction.model.Transaction
import io.nebulas.wallet.android.network.cache.CacheInterceptor
import io.nebulas.wallet.android.network.cache.CacheManager
import io.nebulas.wallet.android.network.callback.OnResultCallBack
import io.nebulas.wallet.android.network.server.HttpManager
import io.nebulas.wallet.android.network.server.model.*
import io.nebulas.wallet.android.network.subscriber.HttpSubscriber
import io.nebulas.wallet.android.util.Formatter
import io.nebulas.wallet.android.util.Util
import okhttp3.MediaType
import okhttp3.Request
import okhttp3.RequestBody
import org.jetbrains.anko.doAsync
import org.json.JSONObject
import java.lang.Exception
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.*

/**
 * Created by Heinoc on 2018/3/1.
 */
class BalanceViewModel : ViewModel(), PollingFutureTask.PollingCompleteCallback<Transaction> {

    var loading: MutableLiveData<Boolean>? = null

    var totalBalance: MutableLiveData<String>? = null

    var accountFloat: MutableLiveData<AccountFloat>? = null

    var coins: MutableLiveData<MutableList<Coin>>? = null
    private var coinsValue: MutableList<Coin>? = null

    var feedFlowDatas: MutableLiveData<MutableList<BalanceListModel>>? = null

    fun getLoading(): LiveData<Boolean> {
        if (null == loading) {
            loading = MutableLiveData<Boolean>()
        }

        return loading!!

    }

    fun setLoading(loading: Boolean) {
        if (null == this.loading) {
            this.loading = MutableLiveData<Boolean>()
        }
        this.loading?.value = loading
    }

    fun getAccountFloat(): LiveData<AccountFloat> {
        if (null == accountFloat) {
            accountFloat = MutableLiveData<AccountFloat>()
        }

        return accountFloat!!

    }

    fun setAccountFloat(accountFloat: AccountFloat) {
        if (null == this.accountFloat) {
            this.accountFloat = MutableLiveData<AccountFloat>()
        }
        this.accountFloat?.value = accountFloat
    }

    fun getCoins(): LiveData<MutableList<Coin>> {
        if (null == coins) {
            coins = MutableLiveData<MutableList<Coin>>()
        }

        return coins!!

    }

    fun setCoins(coins: MutableList<Coin>) {
        if (null == this.coins) {
            this.coins = MutableLiveData<MutableList<Coin>>()
        }
        this.coins?.value = coins
    }

    fun setTotalBalance(balance: String) {
        if (null == this.totalBalance) {
            this.totalBalance = MutableLiveData<String>()
        }
        this.totalBalance?.value = balance
    }

    fun getTotalBalance(): LiveData<String> {
        if (null == totalBalance) {
            totalBalance = MutableLiveData<String>()
        }

        return totalBalance!!

    }

    fun getFeedFlowDatas(): LiveData<MutableList<BalanceListModel>> {
        if (null == feedFlowDatas) {
            feedFlowDatas = MutableLiveData()
        }
        return feedFlowDatas!!
    }

    fun setFeedFlowDatas(value: MutableList<BalanceListModel>) {
        if (null == feedFlowDatas) {
            feedFlowDatas = MutableLiveData()
        }
        feedFlowDatas?.value = value
    }

    fun updateCoins(lifecycle: Lifecycle, onFinish: () -> Unit) {

        requestCoinBalance(lifecycle, onFinish)


    }

    fun requestCoinBalance(lifecycle: Lifecycle, onFinish: () -> Unit) {
        /**
         * 请求网络数据
         */
        var balanceReq = BalanceReq()
        DataCenter.addresses.forEach { balanceReq.addresses.add(UploadReq(it.address, it.platform)) }

//        setLoading(true)
        HttpManager.getBalance(balanceReq, HttpSubscriber(object : OnResultCallBack<BalanceResp> {
            override fun onSuccess(t: BalanceResp) {

                if (null != t.accountFloat)
                    setAccountFloat(t.accountFloat)

//                setTotalBalance(Formatter.priceFormat(BigDecimal(t.totalBalanceValue)))

                var totalBalance = BigDecimal.ZERO
                t.currencyList.forEach { tokenBalanceList ->

                    DataCenter.coins.filter {
                        it.address == tokenBalanceList.address
                    }.forEach { coin ->
                        // 先清空当前coin的balance
                        coin.balance = "0"
                        coin.balanceValue = "0.00"

                        val currency = tokenBalanceList.currencies.find { it.currencyId == coin.tokenId }
                        if (null != currency) {
                            coin.logo = currency.logo
                            coin.currencyPrice = currency.currencyPrice
                            coin.balance = currency.balance
                            val token = coinsValue?.find { it.tokenId == coin.tokenId }
                            if (null != token && !token.noCurrencyPrice) {
                                val balanceValue = BigDecimal(currency.balance).multiply(BigDecimal(token.currencyPrice)).setScale(2, RoundingMode.DOWN)
                                coin.balanceValue = balanceValue.toPlainString()
                            } else {
                                coin.balanceValue = currency.balanceValue
                            }

                            // 计算totalBalance
                            totalBalance += BigDecimal(coin.balanceValueString)

                            coin.tokenDecimals = currency.tokenDecimals
                        }


                        //本地数据缓存
                        doAsync {
                            DBUtil.appDB.coinDao().updateCoin(coin)
                        }
                    }

                }

                setTotalBalance(Formatter.priceFormat(totalBalance))

                setCoins(DataCenter.coinsGroupByCoinSymbol)

                onFinish()
//                setLoading(false)

            }

            override fun onError(code: Int, errorMsg: String) {
                super.onError(code, errorMsg)
                //加载本地数据
                setCoins(DataCenter.coinsGroupByCoinSymbol)
                if (coins!!.value!!.size == 0) {
                    onFinish()
//                    setLoading(false)
                    return
                }

                var totalBalanceValue = BigDecimal(0)
                DataCenter.coinsGroupByCoinSymbol.forEach {
                    totalBalanceValue += BigDecimal(it.balanceValueString)
                }
                setTotalBalance(Formatter.priceFormat(totalBalanceValue))

                onFinish()
//                setLoading(false)
            }
        }, lifecycle))

    }


    fun getCoinsValue(tokenIds: MutableList<Coin> = DataCenter.coinsGroupByCoinSymbol, lifecycle: Lifecycle, onFinish: (priceList: List<PriceList>) -> Unit, onFailed: () -> Unit) {

        if (tokenIds.isEmpty()) {
            onFailed()
            return
        }

        var currencyIds = mutableListOf<String>()
        tokenIds.forEach {
            currencyIds.add(it.tokenId ?: "")
        }

//        setLoading(true)
        HttpManager.getCurrencyPrice(currencyIds, HttpSubscriber(object : OnResultCallBack<CurrencyPriceResp> {
            override fun onSuccess(t: CurrencyPriceResp) {

                coinsValue?.clear()
                t.priceList.forEach { priceItem ->
                    var coin = DataCenter.coins.find { it.tokenId == priceItem.currencyId }
                    if (null != coin) {
                        coin = coin.copy()
                        coin.currencyPrice = priceItem.price
                        coinsValue?.add(coin)
                    }
                }

                onFinish(t.priceList)
//                setLoading(false)

            }

            override fun onError(code: Int, errorMsg: String) {
                super.onError(code, errorMsg)
                onFailed()
//                setLoading(false)
            }

        }, lifecycle))
    }

    fun getHomeFeedFlow(lifecycleOwner: LifecycleOwner, context: Context?, onFinish: () -> Unit) {
        context ?: return
        val swapWalletInfo = SwapHelper.getSwapWalletInfo(context)
        val ethAddress = swapWalletInfo?.swapWalletAddress ?: ""
        val lastEthTransactionHash = SwapHelper.getLastSwapTransactionInfo(context)?.transactionHash
                ?: ""
        syncSwapEntrance(ethAddress, lastEthTransactionHash, lifecycleOwner.lifecycle)

        /**
         * 请求网络数据
         */
        var balanceReq = BalanceReq()
        DataCenter.addresses.forEach { balanceReq.addresses.add(UploadReq(it.address, it.platform)) }

//        setLoading(true)

        /**
         * 优先读取本地缓存，再同步网络请求
         */
        val requestBody = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), Gson().toJson(balanceReq))
        val request = Request.Builder()
                .url(HttpManager.mRetrofit.baseUrl().toString() + URLConstants.FEED_FLOW)
                .post(requestBody)
                .build()

        val cacheKey = CacheInterceptor().getCacheKeyOfRequest(request = request)
        val cacheStr = CacheManager.instance.getCache(cacheKey)
        if (null != cacheStr) {
            var result: FeedFlowResp = FeedFlowResp()
            result = Gson().fromJson<FeedFlowResp>(
                    JSONObject(cacheStr).optJSONObject("data").toString(),
                    result.javaClass
            )
            handleFeedFlowResponse(context, result, lifecycleOwner, onFinish)
        }


        HttpManager.getHomeFeedFlow(balanceReq, HttpSubscriber(object : OnResultCallBack<FeedFlowResp> {
            override fun onSuccess(t: FeedFlowResp) {
//                setLoading(false)
                handleFeedFlowResponse(context, t, lifecycleOwner, onFinish)
            }

            override fun onError(code: Int, errorMsg: String) {
                super.onError(code, errorMsg)

                setFeedFlowDatas(mutableListOf<BalanceListModel>())
                onFinish()
//                setLoading(false)
            }
        }, lifecycleOwner.lifecycle))

    }

    private fun handleFeedFlowResponse(context: Context?, t: FeedFlowResp, lifecycleOwner: LifecycleOwner, onFinish: () -> Unit) {

        if (context != null) {
            ScreeningItemHelper.processRedundantFeedItemData(context, t.flowList)
            ScreeningItemHelper.processRedundantTransactionData(context)
        }

        val screeningFeedItemIdSet = if (context != null) {
            ScreeningItemHelper.getScreeningFeedItemId(context)
        } else {
            setOf()
        }

        val screeningTransactionMap = if (context != null) {
            ScreeningItemHelper.getScreeningTransactionHash(context)
        } else {
            mapOf()
        }

        val items = mutableListOf<BalanceListModel>()

        val curList = getFeedFlowDatas().value
        if (null != curList && curList.any { null != it.swapItem }) {
            items.add(0, curList.first { null != it.swapItem })
        }

        t.configurations?.apply {
            Configuration.resetConfigurationsFromServer(this)
        }

        t.noticeList?.apply {
            if (this.isNotEmpty()) {
                // 只取最新一条notice
                t.noticeList?.sortByDescending {
                    it.createdAt
                }
                val noticeItem = t.noticeList?.first()
                if (null != noticeItem) {
                    items.add(BalanceListModel(noticeItem = noticeItem))
                }
            }
        }

        val tempItems = mutableListOf<BalanceListModel>()
        t.flowList?.apply {
            this.forEach {
                if (!screeningFeedItemIdSet.contains(it.id.toString())) {
                    tempItems.add(BalanceListModel(feedItem = it))
                }
            }
        }

        t.txList?.apply {
            if (isNotEmpty()) {
                DataCenter.addRecentlyTransactions(t.txList)
                val showBalanceDetail = if (context != null) {
                    !Util.getBalanceHidden(context)
                } else true
                t.txList?.forEach {
                    if (!screeningTransactionMap.containsKey(it.hash)) {
                        if (!it.confirmed && it.status != "fail") {
                            processPollingTransactions(it, lifecycleOwner)
                        }
                        tempItems.add(BalanceListModel(showBalanceDetail = showBalanceDetail, tx = it))
                    }

                    if (Constants.voteContracts.contains(it.receiver)) {
                        val data = it.txData
                        if (!data.isNullOrEmpty()) {
                            try {
                                val txData = String(Base64.decode(data.toByteArray(), Base64.DEFAULT))
                                val json = JSON.parseObject(txData)
                                val function = json.getString("Function")
                                val args = json.getString("Args")
                                if (function == "vote") {
                                    it.coinSymbol = "NAT"
                                    it.currencyId = "NAT"
                                    val jsonArray = JSON.parseArray(args)
                                    it.amount = jsonArray.last().toString()
                                }
                            } catch (e: Exception) {

                            }
                        }
                    }

                }
            }
        }

        tempItems.sortByDescending { it.getTimeStamp() }

        tempItems.sortWith(Comparator { t1, t2 ->
            val isAtpAds1 = AtpHolder.isRenderable(t1.tx?.txData)
            val isAtpAds2 = AtpHolder.isRenderable(t2.tx?.txData)
            if (isAtpAds1 && !isAtpAds2) {
                return@Comparator -1
            } else if (isAtpAds1 && isAtpAds2) {
                return@Comparator 0
            } else if (!isAtpAds1 && !isAtpAds2) {
                return@Comparator 0
            } else {
                return@Comparator 1
            }
        })

        items.addAll(tempItems)

        setFeedFlowDatas(items)

        onFinish()
    }

    override fun onCompleted(tId: String, lastPollingResult: Transaction?) {
        val dataSource = feedFlowDatas?.value
        if (dataSource == null || lastPollingResult == null) {
            return
        }
        var hasUpdate = false
        dataSource.forEach {
            val tx = it.tx
            if (tx != null && tx.hash == lastPollingResult.hash) {
                hasUpdate = true
                tx.confirmedCnt = lastPollingResult.confirmedCnt
                tx.status = lastPollingResult.status
                tx.confirmed = lastPollingResult.confirmed
            }
        }
        if (hasUpdate) {
            feedFlowDatas?.value = dataSource
        }
    }

    private fun processPollingTransactions(transaction: Transaction, lifecycleOwner: LifecycleOwner) {
        val taskInfo = TransactionPollingTaskInfo(transaction, lifecycleOwner, null, this)
        SyncManager.sync(taskInfo, lifecycleOwner)
    }

    /**
     * 首页换币入口
     */
    private fun syncSwapEntrance(ethAddress: String, lastEthTransactionHash: String, lifecycle: Lifecycle) {

        /**
         * 优先读取本地缓存，再同步网络请求
         */
        val requestBody = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), Gson().toJson(SwapEntranceReq(eth_address = ethAddress, eth_tx_hash = lastEthTransactionHash)))
        val request = Request.Builder()
                .url(HttpManager.mRetrofit.baseUrl().toString() + URLConstants.SWAP_ENTRANCE)
                .post(requestBody)
                .build()

        val cacheKey = CacheInterceptor().getCacheKeyOfRequest(request = request)
        val cacheStr = CacheManager.instance.getCache(cacheKey)
        if (null != cacheStr) {
            var result = SwapEntranceResp(0, 0)
            try {
                result = Gson().fromJson<SwapEntranceResp>(
                        JSONObject(cacheStr).optJSONObject("data").toString(),
                        result.javaClass
                )
            } catch (e: Exception) {
            }
            val localStatus = SwapHelper.getCurrentSwapStatus(WalletApplication.INSTANCE)
//            if (localStatus != SwapHelper.SWAP_STATUS_NONE) {
            result.transfer_status = localStatus
//            }
            handleSyncSwapEntrance(result)
        }


        HttpManager.swapEntrance(SwapEntranceReq(eth_address = ethAddress, eth_tx_hash = lastEthTransactionHash), HttpSubscriber(object : OnResultCallBack<SwapEntranceResp> {
            override fun onSuccess(t: SwapEntranceResp) {
                SwapHelper.updateSwapGasAndProcessDescription(WalletApplication.INSTANCE, t.gas_fee_lower, t.gas_fee_upper, t.notice)
                SwapHelper.setCurrentSwapStatus(WalletApplication.INSTANCE, t.transfer_status)
                handleSyncSwapEntrance(t)
            }
        }, lifecycle))
    }

    private fun handleSyncSwapEntrance(t: SwapEntranceResp) {
        var list = getFeedFlowDatas().value
        if (null == list) {
            list = mutableListOf()
        }
        if (t.transfer_status != SwapHelper.SWAP_STATUS_SUCCESS) {
            t.transfer_status = SwapHelper.getCurrentSwapStatus(WalletApplication.INSTANCE)
        }
        if (t.transfer_config == 1) {
            if (list.none { null != it.swapItem }) {
                /**
                 * 添加换币入口
                 */
                list.add(0, BalanceListModel(swapItem = SwapItem(
                        title = WalletApplication.INSTANCE.activity?.getString(R.string.home_swap_enter_des)
                                ?: "",
                        status = t.transfer_status)))
                setFeedFlowDatas(list)
            } else {
                /**
                 * 更新现有状态
                 */
                list.find { null != it.swapItem }.apply {
                    this?.swapItem?.status = t.transfer_status
                }
            }
            SwapHelper.setTransferConfig(WalletApplication.INSTANCE.applicationContext, true)
        } else {
            /**
             * 移除换币入口
             */
            list.removeAll { null != it.swapItem }
            SwapHelper.setTransferConfig(WalletApplication.INSTANCE.applicationContext, false)
        }
        setFeedFlowDatas(list)
    }


}