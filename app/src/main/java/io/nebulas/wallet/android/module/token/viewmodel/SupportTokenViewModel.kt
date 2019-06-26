package io.nebulas.wallet.android.module.token.viewmodel

import android.app.Activity
import android.arch.lifecycle.Lifecycle
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import android.os.Bundle
import android.text.TextUtils
import io.nebulas.wallet.android.R
import io.nebulas.wallet.android.app.WalletApplication
import io.nebulas.wallet.android.base.BaseActivity
import io.nebulas.wallet.android.common.Constants
import io.nebulas.wallet.android.common.DataCenter
import io.nebulas.wallet.android.db.DBUtil
import io.nebulas.wallet.android.module.balance.model.Coin
import io.nebulas.wallet.android.module.token.model.SupportTokenModel
import io.nebulas.wallet.android.module.wallet.create.model.SupportToken
import io.nebulas.wallet.android.network.callback.OnResultCallBack
import io.nebulas.wallet.android.network.eth.ETHHttpManager
import io.nebulas.wallet.android.network.nas.NASHttpManager
import io.nebulas.wallet.android.network.server.HttpManager
import io.nebulas.wallet.android.network.server.model.CurrencyPriceResp
import io.nebulas.wallet.android.network.server.model.CurrencyResp
import io.nebulas.wallet.android.network.server.model.CurrencyRpcHOSTResp
import io.nebulas.wallet.android.network.server.model.PriceList
import io.nebulas.wallet.android.network.subscriber.HttpSubscriber
import io.nebulas.wallet.android.util.Util
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import walletcore.Walletcore

/**
 * Created by Heinoc on 2018/3/8.
 */
class SupportTokenViewModel : ViewModel() {

    var tokens: MutableLiveData<MutableList<SupportToken>>? = null

    fun getSupportTokens(): LiveData<MutableList<SupportToken>> {
        if (null == tokens)
            tokens = MutableLiveData()

        return tokens!!

    }


    fun loadCoins(lifecycle: Lifecycle, onFinish: (MutableList<SupportToken>) -> Unit) {

        doAsync {
            var list = DBUtil.appDB.supportTokenDao().loadAllToken() as MutableList<SupportToken>
            uiThread {
                if (list.isEmpty()) {
                    getTokensFromServer(lifecycle, { onFinish(tokens?.value!!) })
                } else {
                    if (null == tokens)
                        tokens = MutableLiveData<MutableList<SupportToken>>()

                    tokens?.value = list

                    onFinish(tokens?.value!!)
                }


            }
        }


    }

    /**
     * 获取币rpc host
     */
    fun getCurrencyRpcHOST(lifecycle: Lifecycle, onFinish: () -> Unit) {
        HttpManager.getCurrencyRpcHOST(HttpSubscriber(object : OnResultCallBack<CurrencyRpcHOSTResp> {
            override fun onSuccess(t: CurrencyRpcHOSTResp) {
                if (null != t.coins && t.coins!!.isNotEmpty()) {
                    t.coins!!.forEach {
                        when (it.name) {
                            Walletcore.NAS -> {
                                NASHttpManager.setUrlHost(it.url!!)
                            }
                            Walletcore.ETH -> {
                                it.url += "/"
                                ETHHttpManager.setUrlHost(it.url!!)
                            }
                        }
                    }
                }

                onFinish()

            }

            override fun onError(code: Int, errorMsg: String) {
                super.onError(code, errorMsg)
                onFinish()
            }
        }, lifecycle))
    }

    /**
     * 从服务器获取数据
     */
    fun getTokensFromServer(lifecycle: Lifecycle, onFinish: (MutableList<SupportToken>) -> Unit) {

        SupportToken.getSupportTokenList(HttpSubscriber(object : OnResultCallBack<CurrencyResp> {
            override fun onSuccess(t: CurrencyResp) {
                var list = mutableListOf<SupportToken>()

                t.groupList?.forEach { currencys ->

                    currencys.currencies?.forEach { supportToken ->
                        /**
                         * 全部默认显示
                         */
//                        supportToken.isSelected = currencys.mark == "CORE"
                        supportToken.isSelected = true
                        supportToken.markName = currencys.markName
                    }

                    list.addAll(currencys.currencies!!)

                }

                if (null == tokens)
                    tokens = MutableLiveData<MutableList<SupportToken>>()

                /**
                 * 获取币价
                 */
                val currencyIds = mutableListOf<String>()
                list.forEach {
                    currencyIds.add(it.id)
                }
                getCoinsValue(tokenIds = currencyIds,
                        lifecycle = lifecycle,
                        onFinish = {
                            it.forEach { priceItem ->
                                list.filter {
                                    it.id == priceItem.currencyId
                                }.forEach {
                                    it.currencyPrice = priceItem.price
                                }
                            }

                            /**
                             * 添加到全局变量
                             */
                            DataCenter.supportTokens.clear()
                            DataCenter.supportTokens.addAll(list)
                            tokens?.value = list


                            /**
                             * 检查当前钱包是否已创建过全部支持的币种
                             */
                            validateLocalCoins(list)

                            doAsync {
                                DBUtil.appDB.supportTokenDao().deleteAllData()
                                DBUtil.appDB.supportTokenDao().insertTokens(list)
                            }
                            onFinish(tokens?.value!!)

                        },
                        onFailed = {

                            /**
                             * 添加到全局变量
                             */
                            DataCenter.supportTokens.clear()
                            DataCenter.supportTokens.addAll(list)
                            tokens?.value = list

                            doAsync {
                                DBUtil.appDB.supportTokenDao().deleteAllData()
                                DBUtil.appDB.supportTokenDao().insertTokens(list)
                            }
                            onFinish(tokens?.value!!)
                        })

            }

            override fun onError(code: Int, errorMsg: String) {
                super.onError(code, errorMsg)
                if (null == tokens)
                    tokens = MutableLiveData<MutableList<SupportToken>>()
                val list = mutableListOf<SupportToken>()

                //添加默认token-NAS
                list.add(SupportToken(
                        id = "nebulas",
                        symbol = "NAS",
                        name = "Nebulas",
                        platform = "nebulas",
                        contractAddress = "",
                        logo = "https://walletapi.nebulas.io/img/NAS_MAINNET.png",
                        tokenDecimals = "18",
                        quotation = "",
                        isSelected = true,
                        displayed = 1,
                        type = 1,
                        mark = "CORE",
                        markName = "常用货币",
                        currencyPrice = "0.00",
                        hint = 1
                ))

                /**
                 * 添加到全局变量
                 */
                DataCenter.supportTokens.clear()
                DataCenter.supportTokens.addAll(list)
                tokens?.value = list

                doAsync {
                    DBUtil.appDB.supportTokenDao().deleteAllData()
                    DBUtil.appDB.supportTokenDao().insertTokens(list)
                }

                onFinish(tokens?.value!!)
            }
        }, lifecycle = lifecycle))
    }


    /**
     * 获取数字货币的法币汇率
     */
    private fun getCoinsValue(tokenIds: MutableList<String>, lifecycle: Lifecycle, onFinish: (priceList: List<PriceList>) -> Unit, onFailed: () -> Unit) {

        HttpManager.getCurrencyPrice(tokenIds, HttpSubscriber(object : OnResultCallBack<CurrencyPriceResp> {
            override fun onSuccess(t: CurrencyPriceResp) {
                onFinish(t.priceList)

            }

            override fun onError(code: Int, errorMsg: String) {
                super.onError(code, errorMsg)
                onFailed()
            }

        }, lifecycle))

    }

    /**
     * 检查当前钱包是否已创建过全部支持的币种
     */
    private fun validateLocalCoins(list: MutableList<SupportToken>) {

        doAsync {
            list.forEach { supportToken: SupportToken ->

                DataCenter.addresses.forEach { address ->
                    val hasToken = DataCenter.coins.find { it.addressId == address.id && it.tokenId == supportToken.id }

                    if (null != hasToken) {

                        // 更新现有币种数据
                        hasToken.logo = supportToken.logo
                        hasToken.symbol = supportToken.symbol
                        hasToken.name = supportToken.name
                        hasToken.displayed = supportToken.displayed
                        hasToken.contractAddress = supportToken.contractAddress
                        DBUtil.appDB.coinDao().updateCoin(hasToken)

                    } else {

                        // 为新增币种添加地址
                        if (address.platform == supportToken.platform) {
                            val coin = Coin(supportToken)
                            coin.walletId = address.walletId
                            coin.addressId = address.id
                            coin.address = address.address

                            //数据持久化，插入coin
                            coin.id = DBUtil.appDB.coinDao().insertCoin(coin)

                            DataCenter.coins.add(coin)
                        }
                    }

                }
            }
        }
    }

    fun showSupportTokens(context: Activity) {
        var tokenList: MutableList<Any>? = mutableListOf()
        DataCenter.supportTokens.forEach {
            tokenList?.add(SupportTokenModel(it.logo, it.symbol))
        }
        (context as BaseActivity).showTipsDialogWithIcon(title = context.getString(R.string.support_token_title),
                icon = R.drawable.token_support,
                message = context.getString(R.string.support_token_content),
                positiveTitle = context.getString(R.string.i_got),
                onConfirm = {

                },
                dataList = tokenList
        )
        context.firebaseAnalytics?.logEvent(Constants.receiveSupporttokensShow, Bundle())
    }

    fun isNeedPopShowTokens(): Boolean {

        var hasShowTokens: String = Util.getNeedShowTokens(WalletApplication.INSTANCE)
        val tokenIds: List<String> = hasShowTokens.split(",")
        var isNeedSHow = false

        DataCenter.supportTokens.forEach {
            if (it.hint != 0 && (!tokenIds.contains(it.id))) {
                if (TextUtils.isEmpty(hasShowTokens))
                    hasShowTokens = it.id
                else
                    hasShowTokens += ",".plus(it.id)
                isNeedSHow = true
            }
        }
        if (isNeedSHow)
            Util.setNeddShowTokens(WalletApplication.INSTANCE, hasShowTokens)
        return isNeedSHow
    }

}