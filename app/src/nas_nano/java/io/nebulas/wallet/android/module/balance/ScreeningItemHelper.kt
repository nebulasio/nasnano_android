package io.nebulas.wallet.android.module.balance

import android.content.Context
import io.nebulas.wallet.android.module.transaction.model.Transaction
import io.nebulas.wallet.android.network.server.model.HomeFeedItem

/**
 * Created by young on 2018/7/3.
 */
object ScreeningItemHelper {

    private const val FILE_SCREENING_FEED_ITEM = "file_screening_feed_item"
    private const val FILE_SCREENING_TRANSACTION_ITEM = "file_screening_transaction_item"

    fun clear(context: Context) {
        val sp1 = context.getSharedPreferences(FILE_SCREENING_FEED_ITEM, Context.MODE_PRIVATE)
        val sp2 = context.getSharedPreferences(FILE_SCREENING_TRANSACTION_ITEM, Context.MODE_PRIVATE)
        sp1.edit().clear().apply()
        sp2.edit().clear().apply()
    }

    fun processRedundantFeedItemData(context: Context, list: List<HomeFeedItem>?) {
        if (list == null) {
            return
        }
        val feedItemsNow = mutableListOf<String>()
        list.forEach {
            feedItemsNow.add(it.id.toString())
        }
        val allScreeningFeedItemIds = getScreeningFeedItemId(context)
        allScreeningFeedItemIds.forEach {
            if (!feedItemsNow.contains(it)) {
                deleteScreeningFeedItem(context, it)
            }
        }
    }

    fun processRedundantTransactionData(context: Context) {
        val allScreeningTransactionHash = getScreeningTransactionHash(context)
        val timeNow = System.currentTimeMillis()
        for ((k, v) in allScreeningTransactionHash) {
            if (v !is Long) {
                deleteScreeningTransaction(context, k)
            } else {
                if (timeNow - v > 24L * 60 * 60 * 1000) {   //超过24小时
                    deleteScreeningTransaction(context, k)
                }
            }
        }
    }

    fun toScreenFeedItem(context: Context, item: HomeFeedItem) {
        val sp = context.getSharedPreferences(FILE_SCREENING_FEED_ITEM, Context.MODE_PRIVATE)
        sp.edit().putString(item.id.toString(), "").commit()
    }

    fun toScreenTransaction(context: Context, item: Transaction) {
        if (item.hash == null) {
            return
        }
        val sp = context.getSharedPreferences(FILE_SCREENING_TRANSACTION_ITEM, Context.MODE_PRIVATE)
        sp.edit().putLong(item.hash, item.sendTimestamp ?: 0L).commit()
    }

    fun getScreeningFeedItemId(context: Context): Set<String> {
        val sp = context.getSharedPreferences(FILE_SCREENING_FEED_ITEM, Context.MODE_PRIVATE)
        return sp.all.keys
    }

    fun getScreeningTransactionHash(context: Context): Map<String, Any?> {
        val sp = context.getSharedPreferences(FILE_SCREENING_TRANSACTION_ITEM, Context.MODE_PRIVATE)
        return sp.all
    }

    private fun deleteScreeningFeedItem(context: Context, itemId: String) {
        val sp = context.getSharedPreferences(FILE_SCREENING_FEED_ITEM, Context.MODE_PRIVATE)
        sp.edit().remove(itemId).apply()
    }

    private fun deleteScreeningTransaction(context: Context, hash: String) {
        val sp = context.getSharedPreferences(FILE_SCREENING_TRANSACTION_ITEM, Context.MODE_PRIVATE)
        sp.edit().remove(hash).apply()
    }

}