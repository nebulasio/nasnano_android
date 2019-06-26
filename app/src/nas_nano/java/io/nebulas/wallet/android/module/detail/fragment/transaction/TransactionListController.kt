package io.nebulas.wallet.android.module.detail.fragment.transaction

import android.app.Activity
import android.arch.lifecycle.LifecycleOwner
import android.support.annotation.WorkerThread
import com.young.binder.lifecycle.LifecycleController
import com.young.polling.PollingFutureTask
import com.young.polling.SyncManager
import io.nebulas.wallet.android.app.WalletApplication
import io.nebulas.wallet.android.common.DataCenter
import io.nebulas.wallet.android.common.TransactionPollingTaskInfo
import io.nebulas.wallet.android.db.DBUtil
import io.nebulas.wallet.android.module.transaction.model.Transaction
import io.nebulas.wallet.android.network.exception.ApiException
import io.nebulas.wallet.android.network.server.HttpManager
import io.nebulas.wallet.android.util.NetWorkUtil
import org.jetbrains.anko.doAsync
import java.util.concurrent.Future

/**
 * Created by young on 2018/6/27.
 */
class TransactionListController(private val lifecycleOwner: LifecycleOwner,
                                val context: Activity,
                                private val dataCenter: TransactionListDataCenter) :
        LifecycleController(lifecycleOwner, context),
        PollingFutureTask.PollingCompleteCallback<Transaction> {

    override fun onCompleted(tId: String, lastPollingResult: Transaction?) {
        lastPollingResult?.apply {
            dataCenter.replaceData(this)
        }
    }

    private var loadingFuture: Future<Unit>? = null

    private var pageCount = 1
    private var pageSize = TransactionListDataCenter.PAGE_SIZE

    override fun onDestroyed() {
        loadingFuture?.apply {
            if (!isDone && !isCancelled) {
                cancel(false)
            }
        }
    }

    fun refresh() {
        if(!NetWorkUtil.instance.isNetWorkConnected()){
            WalletApplication.INSTANCE.activity?.toastErrorMessage(ApiException.CONNECT_EXCEPTION)
        }
        pageCount = 1
        doLoad(true)
    }

    fun loadData() {
        doLoad()
    }

    private fun doLoad(shouldClear: Boolean = false) {
        checkFuture(loadingFuture)
        loadingFuture = doAsync(ErrorHandler { _, _ ->
            dataCenter.loadingStatus = TransactionListDataCenter.LOADING_STATUS_NONE
        }.defaultHandler)
        {
            dataCenter.loadingStatus = if (shouldClear) {
                TransactionListDataCenter.LOADING_STATUS_REFRESH
            } else {
                TransactionListDataCenter.LOADING_STATUS_LOAD_MORE
            }
            val address = setupAddress()
            if (address.isEmpty()) {
                return@doAsync
            }
            val list = requestTransactionRecords(address, dataCenter.tokenId)
            DataCenter.addRecentlyTransactions(list)
            dataCenter.hasMore = !(list == null || list.size < pageSize)
            val secondaryProcessingResult = processingTransactions(list)
            secondaryProcessingResult?.apply {
                if (shouldClear) {
                    dataCenter.addDataWithClear(this)
                } else {
                    dataCenter.addData(this)
                }
            }
            dataCenter.loadingStatus = TransactionListDataCenter.LOADING_STATUS_NONE
        }
    }

    @WorkerThread
    private fun requestTransactionRecords(address: String, tokenId: String): List<Transaction>? {
        val call = HttpManager.getServerApi().getTxRecordsWithoutRX(HttpManager.getHeaderMap(), address, tokenId, pageCount++, pageSize)
        return call.execute().body()?.data?.txVoList
    }

    private fun setupAddress(): String {
        val builder = StringBuilder()
        DataCenter.coins.forEach {
            if (it.tokenId == dataCenter.tokenId) {
                builder.append(it.address).append(",")
            }
        }
        if (builder.isNotEmpty()) {
            builder.deleteCharAt(builder.length - 1)
        }
        return builder.toString()
    }

    /**
     * 对服务器返回的交易记录列表数据做二次加工
     */
    @WorkerThread
    private fun processingTransactions(list: List<Transaction>?): List<Transaction>? {
        if (list == null) {
            return null
        }
        val afterFilter = list.filter { !it.hash.isNullOrEmpty() }
        afterFilter.forEach {

            if (it.blockHeight == null || (!it.confirmed && it.status != "fail")) {
                val taskInfo = TransactionPollingTaskInfo(it, lifecycleOwner, null, this)
                SyncManager.sync(taskInfo, lifecycleOwner)
            }

            val fromDB: Transaction? = DBUtil.appDB.transactionDao().getTransactionByHash(it.hash!!)
            if (fromDB != null && !fromDB.confirmed && it.status != "fail") {
                it.id = fromDB.id
                it.remark = fromDB.remark
                it.name = fromDB.name
                it.coinSymbol = fromDB.coinSymbol
                it.platform = fromDB.platform
                if (it.txFee!!.isEmpty()) {
                    it.txFee = fromDB.txFee
                }
            }
            if (!it.confirmed) {
                if (it.status != "fail") {
                    //交易状态
                    it.status = if (it.confirmedCnt > 0) "pending" else "waiting"
                }
            }
        }
        return afterFilter
    }

    private fun checkFuture(future: Future<*>?) {
        if (future != null && !future.isDone && !future.isCancelled) {
            future.cancel(false)
        }
    }

}