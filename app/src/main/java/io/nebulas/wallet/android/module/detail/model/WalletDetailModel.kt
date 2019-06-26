package io.nebulas.wallet.android.module.detail.model

import android.support.annotation.ColorRes
import io.nebulas.wallet.android.R
import io.nebulas.wallet.android.app.WalletApplication
import io.nebulas.wallet.android.base.BaseEntity
import io.nebulas.wallet.android.common.Constants
import io.nebulas.wallet.android.common.DataCenter
import io.nebulas.wallet.android.module.balance.model.Coin
import io.nebulas.wallet.android.module.transaction.model.Transaction

/**
 * Created by alina on 2018/6/26.
 */
class WalletDetailModel(
        var coin: Coin? = null,
        var transaction: Transaction? = null,
        var isEmptyView: Boolean = false,
        var hasLoadingMore: Boolean = false
) : BaseEntity() {
    fun getCurrencySymble(): String {
        return Constants.CURRENCY_SYMBOL
    }

    fun getLogo(): String {
        var logoUrl = ""
        if (this.transaction == null) {
            return logoUrl
        }

        run breakpoint@{
            DataCenter.coins.forEach {
                if (it.tokenId == transaction!!.currencyId) {
                    logoUrl = it.logo
                    return@breakpoint
                }
            }
        }

        return logoUrl
    }

    /**
     * 返回值1：#FF8F00 等待状态，2：#038AFB，收币  3：#202020 转账  4：#FF5552 失败状态
     */
    @ColorRes
    fun getTxAmountColor(): Int {
        return if (this.transaction == null) {
            0
        }else {
            if (transaction?.status == "fail") {
                //转账失败
                WalletApplication.INSTANCE.resources.getColor(R.color.color_FF5552)
            } else {
                if (transaction?.confirmed == true) {
                    //已确认
                    if (transaction?.isSend == true){
                        //转出
                        WalletApplication.INSTANCE.resources.getColor(R.color.color_202020)
                    }else{
                        //收款
                        WalletApplication.INSTANCE.resources.getColor(R.color.color_038AFB)
                    }
                }else{
                    //转账中
                    WalletApplication.INSTANCE.resources.getColor(R.color.color_FF8F00)
                }
            }
        }
    }


    fun getTxAddress(): String {
        return if (null == transaction) {
            ""
        } else if (transaction?.status == "fail") {
            WalletApplication.INSTANCE.activity?.getString(R.string.home_tx_failed_des) ?: ""
        } else if (transaction?.isSend == true) {
            transaction?.receiver ?: ""
        } else {
            transaction?.sender ?: ""
        }
    }

}

