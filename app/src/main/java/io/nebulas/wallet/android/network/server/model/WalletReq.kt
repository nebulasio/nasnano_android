package io.nebulas.wallet.android.network.server.model

import io.nebulas.wallet.android.base.BaseEntity

/**
 * Created by alina on 2018/6/27.
 */
class WalletReq : BaseEntity() {
    var addressList: MutableList<AdressInfo>? = null
    var page: Int = 0
    var pageSize: Int = 0

    class AdressInfo : BaseEntity() {
        var address: String? = null
        var currencyId: ArrayList<String>? = null
        var platform: String? = null

    }
}