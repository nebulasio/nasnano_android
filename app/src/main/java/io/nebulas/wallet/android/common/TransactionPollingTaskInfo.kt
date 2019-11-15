package io.nebulas.wallet.android.common

import android.arch.lifecycle.LifecycleOwner
import android.util.Base64
import com.alibaba.fastjson.JSON
import com.young.polling.PollingFutureTask
import com.young.polling.SyncManager
import io.nebulas.wallet.android.common.Constants.voteContracts
import io.nebulas.wallet.android.db.DBUtil
import io.nebulas.wallet.android.module.transaction.model.Transaction
import io.nebulas.wallet.android.network.nas.NASHttpManager
import io.nebulas.wallet.android.network.server.HttpManager
import io.nebulas.wallet.android.network.server.api.ServerApi
import walletcore.Walletcore

/**
 * Created by young on 2018/7/3.
 */
class TransactionPollingTaskInfo(var transaction: Transaction,
                                 private val _lifecycleOwner: LifecycleOwner,
                                 private val resultCallback: PollingFutureTask.PollingResultCallback<Transaction>?,
                                 private val completeCallback: PollingFutureTask.PollingCompleteCallback<Transaction>?
) : SyncManager.TaskInfo<Transaction> {

    private var blockHeight: String? = transaction.blockHeight

    private var blockHeightPollingCount = 0

    private val detailTask: DetailTask by lazy {
        DetailTask()
    }

    private val api: ServerApi by lazy {
        HttpManager.getServerApi()
    }

    override fun continuePolling(taskId: String): Boolean {
        if (transaction.hash == null || transaction.currencyId == null) {
            return false
        }
        if (blockHeight.isNullOrEmpty()) {
            if (blockHeightPollingCount >= 5) { //如果请求5次，blockHeight依然是null，则放弃本次轮询任务。避免无限轮询
                return false
            }
            return true
        }

        if (!transaction.confirmed) {
            return true
        }
        return false
    }

    override fun doInTask(): Transaction {
        if (blockHeight.isNullOrEmpty()) {
            blockHeightPollingCount++
            transaction = detailTask.doInTask()
            if (transaction.blockHeight.isNullOrEmpty()) {  //为空则等待15秒继续查询blockHeight，不为空则直接发起状态查询
                return transaction
            }
        }

        if (transaction.confirmed) {
            return detailTask.doInTask()
        }

        val call = NASHttpManager.getNebStateWithoutRX()
        val response = call.execute()
        val apiResponse = response.body()
        if (apiResponse != null) {
            val model = apiResponse.result
            if (model?.height != null) {
                val heightNow = model.height!!
                transaction.confirmedCnt = heightNow.toInt() - transaction.blockHeight!!.toInt()
                val isConfirmed = transaction.confirmedCnt >= transaction.maxConfirmCnt
                val isVoteTransaction = isVoteTransaction()
                if (isVoteTransaction){
                    val token = Constants.voteContractsMap[transaction.receiver]
                    val coin = DataCenter.coins.find { it.tokenId==token }
                    transaction.tokenDecimals = coin?.tokenDecimals?:"18"
                }
                transaction.confirmed = isConfirmed

                if (transaction.status != "fail") {
                    //交易状态
                    transaction.status = if (transaction.confirmedCnt > 0) "pending" else "waiting"
                }

                if (transaction.confirmed) {

                    transaction.status = "success"
                }

                if (transaction.confirmed || transaction.status == "fail") {
                    if (transaction.txData.isNullOrEmpty()) {
                        transaction.txData = ""
                    }
                    DBUtil.appDB.transactionDao().insertTransaction(transaction)
                }
                if (transaction.confirmed) {
                    return detailTask.doInTask()
                }
            }

            return transaction
        }

        return transaction
    }

    override fun getLifecycleOwner(): LifecycleOwner {
        return _lifecycleOwner
    }

    override fun getPollingCompleteCallback(): PollingFutureTask.PollingCompleteCallback<Transaction>? {
        return completeCallback
    }

    override fun getPollingCount(): Int {
        return SyncManager.POLLING_NEVER_STOP
    }

    override fun getPollingResultCallback(): PollingFutureTask.PollingResultCallback<Transaction>? {
        return resultCallback
    }

    override fun getTaskId(): String {
        return transaction.hash!!
    }

    override fun getTimeInterval(): Long {
        return 15000L
    }

    private fun isVoteTransaction(): Boolean {
        if (!voteContracts.contains(transaction.receiver)) {
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

    inner class DetailTask {

        private fun isVoteTransaction(): Boolean {
            if (!voteContracts.contains(transaction.receiver)) {
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

        fun doInTask(): Transaction {
            var currencyId = transaction.currencyId!!
            val isVoteTransaction = isVoteTransaction()
            if (isVoteTransaction) {
                currencyId = Walletcore.NAS
            }
            val call = api.getTxDetailWithoutRX(HttpManager.getHeaderMap(), transaction.hash!!, currencyId)
            val response = call.execute()
            val apiResponse = response.body()
            if (apiResponse != null) {
                val txFromServer = apiResponse.data
                if (txFromServer != null) {
                    run breakPoint@{
                        DataCenter.coins.forEach {
                            if (it.tokenId == txFromServer.currencyId) {
                                txFromServer.coinSymbol = it.symbol
                                txFromServer.tokenDecimals = it.tokenDecimals
                                txFromServer.platform = it.platform
                                return@breakPoint
                            }
                        }
                    }
                    val transaction = DBUtil.appDB.transactionDao().getTransactionByHash(txFromServer.hash!!)
                    if (null != transaction && !transaction.confirmed) {
                        txFromServer.id = transaction.id
                        txFromServer.remark = transaction.remark
                        txFromServer.name = transaction.name
                        if (null == txFromServer.txFee || txFromServer.txFee!!.isEmpty())
                            txFromServer.txFee = transaction.txFee
                    }
                    if (!txFromServer.confirmed) {
                        if (txFromServer.status != "fail") {
                            //交易状态
                            txFromServer.status = if (txFromServer.confirmedCnt > 0) "pending" else "waiting"
                        }
                    }

                    if (isVoteTransaction) {
                        val token = Constants.voteContractsMap[txFromServer.receiver]
                        val coin = DataCenter.coins.find { it.tokenId==token }
                        txFromServer.payload = this@TransactionPollingTaskInfo.transaction.payload
                        txFromServer.amount = this@TransactionPollingTaskInfo.transaction.amount
                        txFromServer.coinSymbol = this@TransactionPollingTaskInfo.transaction.coinSymbol
                        txFromServer.tokenDecimals = coin?.tokenDecimals?:"18"
                    }

                    blockHeight = txFromServer.blockHeight
                    this@TransactionPollingTaskInfo.transaction = txFromServer
                }
            }
            return transaction
        }

    }

}