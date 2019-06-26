package io.nebulas.wallet.android.network.nas.api

import io.nebulas.wallet.android.base.BaseEntity

/**
 * Created by Heinoc on 2018/3/2.
 */
class NASRequestBody : BaseEntity {

    /**
     * rpc version
     */
    var jsonrpc: String? = null
    /**
     * rpc mnasod
     */
    var mnasod: String? = null
    /**
     * params
     */
    var params: Array<String>? = null
    /**
     * chainID
     */
    var id: Int? = 1

    constructor(jsonrpc: String?, mnasod: String?, params: Array<String>?, id: Int?) {
        this.jsonrpc = jsonrpc
        this.mnasod = mnasod
        this.params = params
        this.id = id
    }
}