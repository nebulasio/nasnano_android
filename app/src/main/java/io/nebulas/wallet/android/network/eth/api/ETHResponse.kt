package io.nebulas.wallet.android.network.eth.api

import io.nebulas.wallet.android.base.BaseEntity
import io.nebulas.wallet.android.network.eth.model.ETHErrorResp

/**
 * Created by Heinoc on 2018/3/1.
 */
class ETHResponse<T> : BaseEntity {
//  错误信息  {"jsonrpc":"2.0","error":{"code":-32601,"message":"{undefined} Method not found or unavailable","data":null}}

    var id: Int? = null
    var jsonrpc: String? = null
    var result: T? = null
    var error: ETHErrorResp? = null
    var data: T? = null

    constructor(id: Int?, jsonrpc: String?, result: T?, error: ETHErrorResp?, data: T?) {
        this.id = id
        this.jsonrpc = jsonrpc
        this.result = result
        this.error = error
        this.data = data
    }

    override fun toString(): String {
        val sb = StringBuffer()
        sb.append("id=$id jsonrpc=$jsonrpc result=$result\n error=$error data=$data")
        return sb.toString()
    }

}