package io.nebulas.wallet.android.module.transaction.viewmodel

import android.arch.lifecycle.Lifecycle
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import android.util.Base64
import com.alibaba.fastjson.JSON
import io.nebulas.wallet.android.common.Constants
import io.nebulas.wallet.android.common.DataCenter
import io.nebulas.wallet.android.db.DBUtil
import io.nebulas.wallet.android.module.balance.model.Coin
import io.nebulas.wallet.android.module.transaction.model.BlockStateModel
import io.nebulas.wallet.android.module.transaction.model.Transaction
import io.nebulas.wallet.android.network.callback.OnResultCallBack
import io.nebulas.wallet.android.network.server.HttpManager
import io.nebulas.wallet.android.network.server.model.TxResp
import io.nebulas.wallet.android.network.server.model.WalletReq
import io.nebulas.wallet.android.network.subscriber.HttpSubscriber
import io.nebulas.wallet.android.util.Formatter
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import walletcore.Walletcore

/**
 * Created by Heinoc on 2018/3/15.
 */
class TxViewModel : ViewModel() {

    /**
     * 当前页码
     */
    var page = 1
    /**
     * 分页大小
     */
    var pageSize = 20
    /**
     * 当前币
     */
    var coin: MutableLiveData<Coin>? = null

    /**
     * 交易记录列表
     */
    var txs: MutableLiveData<MutableList<Transaction>>? = null


    var addressBF: StringBuffer? = null

    /**
     * 交易详情
     */
    var tx: MutableLiveData<Transaction>? = null


    fun setCoin(coin: Coin) {

        if (null == this.coin) {
            this.coin = MutableLiveData<Coin>()
        }
        this.coin?.value = coin
    }

    fun getTxs(): LiveData<MutableList<Transaction>> {
        if (null == txs) {
            txs = MutableLiveData()
        }

        return txs!!
    }

    fun setTxs(txs: MutableList<Transaction>) {
        if (null == this.txs) {
            this.txs = MutableLiveData()
        }
        this.txs?.value?.clear()
        this.txs?.value = txs
    }

    fun getTx(): LiveData<Transaction> {
        if (null == tx) {
            tx = MutableLiveData()
        }

        return tx!!
    }

    fun setTx(tx: Transaction) {
        if (null == tx) {
            this.tx = MutableLiveData()
        }

        this.tx?.value = tx
    }

    fun getTxRecords(firstLoad: Boolean, lifecycle: Lifecycle, onComplete: (noMore: Boolean) -> Unit) {
        if (firstLoad)
            page = 1
        else
            page++

        if (null == addressBF) {
            addressBF = StringBuffer()
        }
        addressBF!!.setLength(0)
        DataCenter.coins.forEach {
            if (it.tokenId == coin?.value?.tokenId) {
                addressBF!!.append(",")
                addressBF!!.append(it.address)
            }
        }
        //地址为空，直接返回
        if (addressBF!!.isEmpty()) {
            this.txs?.value?.clear()
            onComplete(true)
            return
        }

        addressBF!!.deleteCharAt(0)

        HttpManager.getTxRecords(addressBF.toString(), coin?.value?.tokenId!!, page, pageSize, HttpSubscriber(object : OnResultCallBack<TxResp> {
            override fun onSuccess(t: TxResp) {

                t.txVoList?.forEach {
                    it.coinSymbol = coin?.value?.symbol
                    it.tokenDecimals = coin?.value?.tokenDecimals!!
                    it.platform = coin?.value?.platform!!
                }

                doAsync {

                    /**
                     * 查询数据库，匹配本地已保存的数据
                     * todo: it's ugly,why have so many db request,we need find a nice way to resolve this question
                     */
                    t.txVoList?.forEach {
                        var transaction = DBUtil.appDB.transactionDao().getTransactionByHash(it.hash!!)
                        if (null != transaction && !transaction.confirmed && it.status != "fail") {
                            it.id = transaction.id
                            it.remark = transaction.remark
                            it.name = transaction.name
                            it.coinSymbol = transaction.coinSymbol
                            it.platform = transaction.platform
                            if (it.txFee!!.isEmpty())
                                it.txFee = transaction.txFee
                        }
                        if (!it.confirmed) {
                            if (it.status != "fail") {
                                //交易状态
                                it.status = if (it.confirmedCnt > 0) "pending" else "waiting"
                            }
                        }
                    }


                    uiThread {
                        setTxs(t.txVoList!!)
                        onComplete(t.txVoList!!.size != pageSize)
                    }
                }
            }

            override fun onError(code: Int, errorMsg: String) {
                super.onError(code, errorMsg)
                onComplete(false)
            }

        }, lifecycle))
    }

    private fun isVoteTransaction(transaction: Transaction): Boolean {
        if (!Constants.voteContracts.contains(transaction.receiver)) {
            return false
        }
        if (transaction.payload != null) {
            val payload = transaction.payload!!
            return payload.nasFunction == "vote"
        } else {
            try {
                val data = transaction.txData
                if (!data.isNullOrEmpty()) {
                    val txData = String(Base64.decode(data.toByteArray(), Base64.DEFAULT))
                    val json = JSON.parseObject(txData)
                    val function = json.getString("Function")
                    return function == "vote"
                }
            } catch (e: Exception) {
                return false
            }
        }
        return false
    }

    fun getTxDetails(tx: Transaction = this.tx?.value!!, lifecycle: Lifecycle, onComplete: (tx: Transaction?) -> Unit) {
        tx.getTxDetail(HttpSubscriber(object : OnResultCallBack<Transaction> {
            override fun onSuccess(t: Transaction) {
                val isVoteTransaction = isVoteTransaction(tx)
                if (isVoteTransaction) {
                    t.payload = tx.payload
                    t.amount = tx.amount
                    t.currencyId = "NAT"
                } else {
                    run breakPoint@{
                        DataCenter.coins.forEach {
                            if (it.tokenId == t.currencyId) {
                                t.coinSymbol = it.symbol
                                t.tokenDecimals = it.tokenDecimals
                                t.platform = it.platform
                                return@breakPoint
                            }
                        }
                    }
                }

                doAsync {

                    /**
                     * 查询数据库，匹配本地已保存的数据
                     */
                    val transaction = DBUtil.appDB.transactionDao().getTransactionByHash(t.hash!!)
                    if (null != transaction && !transaction.confirmed) {

                        t.id = transaction.id
                        t.remark = transaction.remark
                        t.name = transaction.name
                        if (null == t.txFee || t.txFee!!.isEmpty())
                            t.txFee = transaction.txFee
                    }
                    if (!t.confirmed) {
                        if (t.status != "fail") {
                            //交易状态
                            t.status = if (t.confirmedCnt > 0) "pending" else "waiting"
                        }
                    }

                    uiThread {
                        setTx(t)
                        onComplete(t)
                    }
                }

            }

            override fun onError(code: Int, errorMsg: String) {
                if (!errorMsg.contains("transaction does not exist")) {
                    super.onError(code, errorMsg)
                }
                onComplete(null)
            }

        }, lifecycle))
    }

    /**
     * 获取当前交易的交易状态
     */
    fun getTxStatus(transaction: Transaction, lifecycle: Lifecycle, onComplete: (blockStateModel: BlockStateModel?) -> Unit) {
        when (transaction.platform) {
            Walletcore.NAS -> {
                transaction.getNasNebStatus(HttpSubscriber(object : OnResultCallBack<BlockStateModel> {
                    override fun onSuccess(t: BlockStateModel) {
                        onComplete(t)
                    }

                    override fun onError(code: Int, errorMsg: String) {
                        super.onError(code, errorMsg)
                        onComplete(null)
                    }

                }, lifecycle))
            }
            Walletcore.ETH -> {
                transaction.getEthTxStatus(HttpSubscriber(object : OnResultCallBack<String> {
                    override fun onSuccess(t: String) {
                        onComplete(BlockStateModel(height = Formatter.hexToString(t)))

                    }

                    override fun onError(code: Int, errorMsg: String) {
                        super.onError(code, errorMsg)
                        onComplete(null)
                    }
                }, lifecycle))
            }
        }
    }

    fun getTxRecordsByWallet(firstLoad: Boolean, walletReq: WalletReq, lifecycle: Lifecycle, onComplete: (MutableList<Transaction>) -> Unit, onFail: (String) -> Unit) {
        if (firstLoad) {
            page = 1
        } else {
            page++
        }
        walletReq.page = page
        HttpManager.getTxRecordsByWallet(walletReq, HttpSubscriber(object : OnResultCallBack<MutableList<Transaction>> {
            override fun onSuccess(t: MutableList<Transaction>) {
                onComplete(t)
            }

            override fun onError(code: Int, errorMsg: String) {
                //super.onError(code, errorMsg)
                onFail(errorMsg)
            }
        }, lifecycle))

    }

}