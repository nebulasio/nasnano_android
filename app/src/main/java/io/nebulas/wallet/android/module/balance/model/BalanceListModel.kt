package io.nebulas.wallet.android.module.balance.model

import android.content.Context
import android.support.annotation.ColorRes
import io.nebulas.wallet.android.R
import io.nebulas.wallet.android.app.WalletApplication
import io.nebulas.wallet.android.base.BaseEntity
import io.nebulas.wallet.android.module.swap.SwapHelper
import io.nebulas.wallet.android.module.transaction.model.Transaction
import io.nebulas.wallet.android.network.server.model.HomeFeedItem

/**
 * Created by Heinoc on 2018/3/6.
 */
data class BalanceListModel(
        var coin: Coin? = null,
        var isHeader: Boolean = false,
        var showBackUpTips: Boolean = false,
        var isFooter: Boolean = false,
        var showBalanceDetail: Boolean = true,
        var currencyName: String? = null,
        var swapItem: SwapItem? = null,
        var noticeItem: HomeFeedItem? = null,
        var categoryName: String? = null,
        var feedItem: HomeFeedItem? = null,
        var tx: Transaction? = null,
        var emptyWallet: Boolean = false,
        var isStacking: Boolean = false

) : BaseEntity() {
    fun getTimeStamp(): Long {
        if (null != this.feedItem) {
            return this.feedItem?.createdAt ?: 0
        }
        if (null != tx) {
            return this.tx?.sendTimestamp ?: 0
        }
        return 0
    }

    @ColorRes
    fun getTxAmountColor(): Int {
        return if (null == tx) {
            WalletApplication.INSTANCE.resources.getColor(R.color.transparent)
        } else {
            if (tx?.status == "fail") {
                WalletApplication.INSTANCE.resources.getColor(R.color.color_FF5552)
            } else {
                if (tx?.confirmed == true) {
                    if (tx?.isSend == true) {
                        WalletApplication.INSTANCE.resources.getColor(R.color.color_202020)
                    } else {
                        WalletApplication.INSTANCE.resources.getColor(R.color.color_038AFB)
                    }
                } else {
                    WalletApplication.INSTANCE.resources.getColor(R.color.color_FF8F00)
                }
            }
        }
    }

    fun getTxAddress(): String {
        return if (null == tx) {
            ""
        } else if (tx?.status == "fail") {
            WalletApplication.INSTANCE.activity?.getString(R.string.home_tx_failed_des) ?: ""
        } else if (tx?.isSend == true) {
            tx?.receiver ?: ""
        } else {
            tx?.sender ?: ""
        }
    }

    fun getTxAddressColor(): Int {
        return if (tx?.status == "fail") {
            WalletApplication.INSTANCE.resources.getColor(R.color.color_FF5552)
        } else {
            WalletApplication.INSTANCE.resources.getColor(R.color.color_8F8F8F)
        }
    }

}

class SwapItem(
        var title: String,
        /**
         * 0：换币失败；
         * 1：换币成功；
         * 2：换币中；
         * 3：没有换币
         */
        var status: Int = 3
) {
    fun getSwapStatus(): String {
        val realStatus = if (status == 1) {
            status
        } else {
            SwapHelper.getCurrentSwapStatus(WalletApplication.INSTANCE)
        }
        val resId = when (realStatus) {
            0 -> {
                R.string.swap_status_failed
            }
            1 -> {
                R.string.swap_status_successful
            }
            2 -> {
                R.string.swap_status_in_process
            }
            else -> {
                -1
            }
        }
        return if (resId > 0) {
            WalletApplication.INSTANCE.activity?.getString(resId) ?: ""
        } else {
            ""
        }

    }
}