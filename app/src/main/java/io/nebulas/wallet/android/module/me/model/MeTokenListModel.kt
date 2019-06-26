package io.nebulas.wallet.android.module.me.model

import io.nebulas.wallet.android.base.BaseEntity
import io.nebulas.wallet.android.common.DataCenter
import io.nebulas.wallet.android.module.balance.model.Coin
import io.nebulas.wallet.android.util.Formatter
import java.math.BigDecimal

/**
 * Created by Heinoc on 2018/5/14.
 */
data class MeTokenListModel(
        var coin: Coin? = null,
        var showBalanceDetail: Boolean = true,
        var currencyName: String? = null,
        var currencySymbol: String? = null,
        var hasHideAssets: Boolean = false,
        var hasManageAssetsBtn: Boolean = false,
        var emptyView: Boolean = false
) : BaseEntity() {

    fun getHideAssetsValue(): String {

        var totalValue = BigDecimal(0)

        DataCenter.coinsGroupByCoinSymbol.filter {
            !it.isShow
        }.forEach {
            totalValue += BigDecimal(it.balanceValue)
        }

        return Formatter.priceFormat(totalValue)
    }

    fun getTokenIcons(): MutableList<String> {
        val tokenIcons = mutableListOf<String>()

        DataCenter.coinsGroupByCoinSymbol.filter {
            !it.isShow
        }.forEach {
            tokenIcons.add(it.logo)
        }

        return tokenIcons
    }

    fun getBalanceValueString(): String {
        return "≈${this.currencySymbol}${this.coin?.formattedBalanceValueString}"
    }

}