package io.nebulas.wallet.android.network.server.model

import io.nebulas.wallet.android.base.BaseEntity

/**
 * Created by Heinoc on 2018/3/28.
 */
class CurrencyPriceResp : BaseEntity() {
    var priceList: List<PriceList> = listOf()
}

class PriceList : BaseEntity() {
    var currencyId: String = ""
    var price : String = ""
}