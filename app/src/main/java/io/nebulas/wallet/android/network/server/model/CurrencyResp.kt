package io.nebulas.wallet.android.network.server.model

import io.nebulas.wallet.android.base.BaseEntity
import io.nebulas.wallet.android.module.wallet.create.model.SupportToken

/**
 * Created by Heinoc on 2018/3/15.
 */
class CurrencyResp : BaseEntity() {
    var groupList: MutableList<Currencys>? = null
}

class Currencys : BaseEntity() {
    var currencies: MutableList<SupportToken>? = null
    var mark: String? = null
    var markName: String? = null
}