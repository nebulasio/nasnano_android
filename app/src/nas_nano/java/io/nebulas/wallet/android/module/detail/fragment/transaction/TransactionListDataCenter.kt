package io.nebulas.wallet.android.module.detail.fragment.transaction

import com.young.binder.AbstractDataCenter
import io.nebulas.wallet.android.module.transaction.model.Transaction

/**
 * Created by young on 2018/6/27.
 */
class TransactionListDataCenter(val tokenId: String) : AbstractDataCenter() {

    companion object {
        const val PAGE_SIZE = 20
        const val LOADING_STATUS_NONE = 0
        const val LOADING_STATUS_REFRESH = 1
        const val LOADING_STATUS_LOAD_MORE = 2
    }

    val dataSource = mutableListOf<Transaction>()

    var hasMore: Boolean = false

    fun addData(transactions: List<Transaction>) {
        dataSource.addAll(transactions)
        notifyDataChanged(TransactionListEvents.event_data_source_updated)
    }

    fun addDataWithClear(transactions: List<Transaction>) {
        dataSource.clear()
        dataSource.addAll(transactions)
        notifyDataChanged(TransactionListEvents.event_data_source_updated)
    }

    fun replaceData(transaction: Transaction) {
        if (transaction.hash == null) {
            return
        }
        var hasUpdate = false
        dataSource.forEach { tx ->
            if (tx.hash == transaction.hash) {
                hasUpdate = true
                tx.confirmedCnt = transaction.confirmedCnt
                tx.status = transaction.status
                tx.confirmed = transaction.confirmed
            }
        }
        if (hasUpdate) {
            notifyDataChanged(TransactionListEvents.event_data_source_updated)
        }
    }

    var loadingStatus: Int = LOADING_STATUS_NONE
        set(value) {
            field = value
            notifyDataChanged(TransactionListEvents.event_loading_status_changed)
        }
}