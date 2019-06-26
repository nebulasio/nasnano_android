package io.nebulas.wallet.android.network.server.model

import io.nebulas.wallet.android.base.BaseEntity
import io.nebulas.wallet.android.module.transaction.model.Transaction

/**
 * Created by Heinoc on 2018/3/16.
 */
class TxResp : BaseEntity() {
    var txVoList: MutableList<Transaction>? = null
}