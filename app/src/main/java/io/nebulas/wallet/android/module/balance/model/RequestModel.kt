package io.nebulas.wallet.android.module.balance.model

import io.nebulas.wallet.android.base.BaseEntity

/**
 * Created by Heinoc on 2018/3/2.
 */
class RequestModel() : BaseEntity() {
    var requesting: Boolean = false
    var coin: Coin? = null

    constructor(coin: Coin?): this() {
        this.coin = coin
    }
}