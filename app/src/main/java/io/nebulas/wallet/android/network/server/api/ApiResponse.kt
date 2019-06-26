package io.nebulas.wallet.android.network.server.api

import io.nebulas.wallet.android.base.BaseEntity

/**
 * Created by Heinoc on 2018/2/6.
 */

class ApiResponse<T>() : BaseEntity() {
    var code: Int? = null
    var msg: String? = null
    var data: T? = null

    constructor(status: Int?, msg: String?, data: T?) : this() {
        this.code = status
        this.msg = msg
        this.data = data
    }

    override fun toString(): String {
        val sb = StringBuffer()
        sb.append("status=$code msg=$msg")
        if (null != data) {
            sb.append(" data:" + data.toString())
        }
        return sb.toString()
    }
}