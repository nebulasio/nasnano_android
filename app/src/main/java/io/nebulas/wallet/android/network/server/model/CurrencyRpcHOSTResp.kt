package io.nebulas.wallet.android.network.server.model

import io.nebulas.wallet.android.base.BaseEntity

/**
 * Created by Heinoc on 2018/3/15.
 */
class CurrencyRpcHOSTResp : BaseEntity() {
    var coins: MutableList<RpcHOST>? = null
}

class RpcHOST : BaseEntity() {
    /**
     * platform
     */
    var name: String? = null
    /**
     * rpc host
     */
    var url: String? = null
}