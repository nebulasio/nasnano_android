package io.nebulas.wallet.android.common

import io.nebulas.wallet.android.module.balance.model.Coin
import io.nebulas.wallet.android.module.transaction.model.Transaction
import io.nebulas.wallet.android.module.wallet.create.model.Address
import io.nebulas.wallet.android.module.wallet.create.model.SupportToken
import io.nebulas.wallet.android.module.wallet.create.model.Wallet
import java.math.BigDecimal

/**
 * Created by Heinoc on 2018/2/26.
 */
object DataCenter {

    private var dataMaps: MutableMap<String, Any> = mutableMapOf()
    private lateinit var result: Any

    var txRecords: MutableList<Transaction>? = null
    private val mRecentlyReceivedTransactions = mutableMapOf<String, Transaction>()

    var swapAddress: Address? = null

    var wallets: MutableList<Wallet> = mutableListOf()
    var addresses: MutableList<Address> = mutableListOf()
    var coins: MutableList<Coin> = mutableListOf()
    var coinsGroupByCoinSymbol: MutableList<Coin> = mutableListOf()
        get() {
            field.clear()
            for (coin in coins) {
                var insertFlag = false
                for (uniCoin in field) {
                    if (uniCoin.tokenId == coin.tokenId) {

                        if (uniCoin.currencyPrice.isNullOrEmpty() || uniCoin.currencyPrice == "0") {
                            uniCoin.currencyPrice = coin.currencyPrice
                        }

                        uniCoin.balance = (BigDecimal(coin.balance) + BigDecimal(uniCoin.balance)).stripTrailingZeros().toPlainString()
                        uniCoin.balanceValue = (BigDecimal(coin.balanceValue) + BigDecimal(uniCoin.balanceValue)).toPlainString()

                        insertFlag = true
                        break
                    }
                }
                if (!insertFlag) {
                    field.add(coin.copy())
                }
            }
            field.sortBy { it.displayed }
            return field
        }
    var supportTokens: MutableList<SupportToken> = mutableListOf()

    fun deleteWallet(wallet: Wallet){
        val iterator = wallets.iterator()
        while (iterator.hasNext()) {
            val w = iterator.next()
            if (w.id == wallet.id) {
                iterator.remove()
            }
        }
    }

    fun getRecentlyReceivedTransactions(): Map<String, Transaction> {
        return mRecentlyReceivedTransactions
    }

    @Synchronized
    fun addRecentlyTransactions(list: List<Transaction>?) {
        list?.forEach {
            if (!it.isSend) {
                val key = it.currencyId
                if (key != null) {
                    val exist = mRecentlyReceivedTransactions[key]
                    if (exist == null) {
                        if (mRecentlyReceivedTransactions.size >= 8) {
                            var lastTx: Transaction? = null
                            var earliestKey: String? = null
                            mRecentlyReceivedTransactions.keys.forEach {
                                if (lastTx == null) {
                                    earliestKey = it
                                    lastTx = mRecentlyReceivedTransactions[it]
                                } else {
                                    val temp = mRecentlyReceivedTransactions[it]
                                    val t1 = temp!!.sendTimestamp
                                    val t2 = lastTx!!.sendTimestamp
                                    if (t1 != null && t2 != null && t1 < t2) {
                                        earliestKey = it
                                    }
                                }
                            }
                            mRecentlyReceivedTransactions.remove(earliestKey)
                        }
                        mRecentlyReceivedTransactions[key] = it
                    } else {
                        val t1 = it.sendTimestamp
                        val t2 = exist.sendTimestamp
                        if (t1 != null && t2 != null && t1 > t2) {
                            mRecentlyReceivedTransactions[key] = it
                        }
                    }
                }
            }
        }
    }

    /**
     * set data into the DataCenter
     *
     * @param key key
     * @param value value
     */
    fun setData(key: String, value: Any) {
        dataMaps[key] = value
    }

    /**
     * check data is or not in the DataCenter
     *
     * @param key key
     * @return Boolean
     */
    fun containsData(key: String): Boolean {
        return dataMaps.containsKey(key)
    }

    /**
     * get the seted data from the DataCenter
     *
     * @param key key
     *
     * @return if DataCenter have the given key,return the value; if DataCenter not have the given key,return ""
     *
     */
    fun getData(key: String): Any {
        return getData(key, false)
    }

    /**
     * get the seted data from the DataCenter
     *
     * @param key key
     * @param autoDelete auto delete the value after get the value
     *
     * @return if DataCenter have the given key,return the value; if DataCenter not have the given key,return ""
     *
     */
    fun getData(key: String, autoDelete: Boolean): Any {

        return if (dataMaps.containsKey(key)) {
            result = dataMaps[key]!!
            if (autoDelete) {
                dataMaps.remove(key)
            }
            result
        } else
            ""
    }

    /**
     * remove the value of the given key
     */
    fun removeData(key: String) {
        if (dataMaps.containsKey(key))
            dataMaps.remove(key)
    }

}