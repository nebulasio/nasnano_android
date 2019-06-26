package io.nebulas.wallet.android.module.me.model

import io.nebulas.wallet.android.base.BaseEntity
import io.nebulas.wallet.android.common.DataCenter
import io.nebulas.wallet.android.module.balance.model.Coin
import io.nebulas.wallet.android.module.wallet.create.model.Wallet
import io.nebulas.wallet.android.util.Formatter
import java.math.BigDecimal

/**
 * Created by Heinoc on 2018/5/14.
 */
data class MeWalletListModel(
        var wallet: Wallet? = null,
        var coin: Coin? = null,
        var showBalanceDetail: Boolean = true,
        var currencyName: String? = null,
        var currencySymbol: String? = null,
        var hasAddWalletBtn: Boolean = false,
        var emptyView: Boolean = false
) : BaseEntity() {
    fun getWalletNameFirstChar(): String{
        return if (null != this.wallet) this.wallet!!.walletName[0].toString().toUpperCase() else ""
    }

    /**
     * 获取当前钱包的总资产
     */
    fun getWalletTotalValue(): String{
        var totalValue = BigDecimal(0)

        DataCenter.coins.forEach {
            if (it.walletId == this.wallet?.id){
                totalValue += BigDecimal(it.balanceValue)
            }
        }

        return Formatter.priceFormat(totalValue)

    }

    fun isNeedBackUp(): Boolean {
        return if (null != this.wallet) this.wallet!!.getPlainMnemonic().isNotEmpty() else false
    }

    fun getTokenIcons(): MutableList<String> {
        val tokenIcons = mutableListOf<String>()
        DataCenter.coins.filter {
            it.walletId == this.wallet?.id
        }.sortedByDescending {
            BigDecimal(it.balanceValue)
        }.forEach {
            if (it.walletId == this.wallet?.id){
                tokenIcons.add(it.logo)
            }
        }

        return tokenIcons
    }

}