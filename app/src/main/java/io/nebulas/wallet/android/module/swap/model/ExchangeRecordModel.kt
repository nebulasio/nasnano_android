package io.nebulas.wallet.android.module.swap.model

import android.support.annotation.ColorRes
import io.nebulas.wallet.android.R
import io.nebulas.wallet.android.app.WalletApplication
import io.nebulas.wallet.android.base.BaseEntity

class ExchangeRecordModel(val time: String? = null, val status: Int? = null, val amount: String? = null, val gasFee: String? = null, val address: String? = null, val emptyView: Boolean = false, val hasMore: Boolean = false) : BaseEntity() {

    @ColorRes
    fun getExchangeStateColor(): Int {
        return when {
            this.status == 0 -> //失败
                WalletApplication.INSTANCE.resources.getColor(R.color.color_FF5552)
            this.status == 1 -> //成功
                WalletApplication.INSTANCE.resources.getColor(R.color.color_00CB91)
            else -> //等待中
                WalletApplication.INSTANCE.resources.getColor(R.color.color_FF8F00)
        }

    }

    fun getStatusDes(): Int {

        return when {
            this.status == 0 -> //失败
                R.string.result_fail
            this.status == 1 -> //成功
                R.string.result_success
            else -> //等待中
                R.string.result_wait
        }
    }
}