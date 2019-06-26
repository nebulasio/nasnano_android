package io.nebulas.wallet.android.network.eth.model

import io.nebulas.wallet.android.base.BaseEntity

/**
 * Created by Heinoc on 2018/3/15.
 */
class ETHErrorResp : BaseEntity() {
    var code: Int = 0
    var message: String = ""
}