package io.nebulas.wallet.android.module.transaction.viewmodel

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import io.nebulas.wallet.android.R
import io.nebulas.wallet.android.app.WalletApplication
import io.nebulas.wallet.android.common.DataCenter
import io.nebulas.wallet.android.db.DBUtil
import io.nebulas.wallet.android.module.transaction.model.ChooseTokenListModel
import io.nebulas.wallet.android.module.transaction.model.Transaction
import io.nebulas.wallet.android.module.wallet.create.model.Wallet
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread

/**
 * Created by Heinoc on 2018/6/21.
 */
class ChooseTokenViewModel : ViewModel() {

    private var loadingData: MutableLiveData<Boolean>? = null
    private var tokenList = mutableListOf<ChooseTokenListModel>()
    private var chooseTokenLists: MutableLiveData<MutableList<ChooseTokenListModel>>? = null

    fun setLoadingData(loadingData: Boolean) {
        if (null == this.loadingData)
            this.loadingData = MutableLiveData<Boolean>()

        this.loadingData?.value = loadingData
    }

    fun getLoadingData(): LiveData<Boolean> {
        if (null == this.loadingData)
            this.loadingData = MutableLiveData<Boolean>()

        return this.loadingData!!
    }

    fun setChooseTokenLists(list: MutableList<ChooseTokenListModel>) {
        if (null == this.chooseTokenLists)
            this.chooseTokenLists = MutableLiveData()

        this.chooseTokenLists?.value = list
    }

    fun getChooseTokenLists(): LiveData<MutableList<ChooseTokenListModel>> {
        if (null == this.chooseTokenLists)
            this.chooseTokenLists = MutableLiveData()

        return this.chooseTokenLists!!
    }

    /**
     * 加载所有币种
     */
    fun loadTokens(wallet: Wallet? = null, isReceive: Boolean = false) {
        if (tokenList.isNotEmpty()) {
            setChooseTokenLists(tokenList)
            return
        }

        setLoadingData(true)

        doAsync {
            val recentlyMap = if (isReceive) {
                DataCenter.getRecentlyReceivedTransactions()
            } else {
                val recentlyTx = DBUtil.appDB.transactionDao().getRecentlySentRecordWithDiffCoin(8)
                mutableMapOf<String, Transaction>().apply {
                    recentlyTx.forEach {
                        it.currencyId?.apply {
                            put(this, it)
                        }
                    }
                }
            }

            val recentlyCoins = mutableListOf<ChooseTokenListModel>()
            val allCoins = mutableListOf<ChooseTokenListModel>()
            DataCenter.coinsGroupByCoinSymbol.forEach {
                if (wallet != null) {
                    if (it.walletId == wallet.id) {
                        allCoins.add(ChooseTokenListModel(coin = it))
                    }
                } else {
                    allCoins.add(ChooseTokenListModel(coin = it))
                }
                if (recentlyMap.containsKey(it.tokenId)) {
                    recentlyCoins.add(ChooseTokenListModel(coin = it))
                }
            }
            if (recentlyMap.isNotEmpty() && recentlyCoins.isNotEmpty()) {
                val resId = if (isReceive) R.string.received_recently else R.string.sent_recently
                tokenList.add(ChooseTokenListModel(category = WalletApplication.INSTANCE.activity?.getString(resId)))
                tokenList.addAll(recentlyCoins)
            }

            tokenList.add(ChooseTokenListModel(category = WalletApplication.INSTANCE.activity?.getString(R.string.all_tokens)))
            tokenList.addAll(allCoins)
            uiThread {
                setChooseTokenLists(tokenList)
                setLoadingData(false)
            }
        }
    }

    fun searchToken(keyWord: String, wallet: Wallet? = null) {
        if (keyWord.isNullOrEmpty()) {
            setChooseTokenLists(tokenList)
            return
        }

        val list = mutableListOf<ChooseTokenListModel>()

        list.add(ChooseTokenListModel(category = WalletApplication.INSTANCE.activity?.getString(R.string.choose_token_search_result_title)))

        /**
         * 搜索指定wallet下的币
         */
        if (null != wallet) {
            DataCenter.coins.filter {
                it.symbol.contains(keyWord, true) && it.walletId == wallet.id
            }.forEach {
                list.add(ChooseTokenListModel(coin = it))
            }
        } else {
            /**
             * 搜索全部币种
             */

            DataCenter.coinsGroupByCoinSymbol.filter {
                it.symbol.contains(keyWord, true)
            }.forEach {
                list.add(ChooseTokenListModel(coin = it))
            }
        }

        setChooseTokenLists(list)
    }

}