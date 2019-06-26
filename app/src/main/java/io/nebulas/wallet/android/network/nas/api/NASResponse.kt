package io.nebulas.wallet.android.network.nas.api

import io.nebulas.wallet.android.base.BaseEntity

/**
 * Created by Heinoc on 2018/3/2.
 */
open class NASResponse<T> : BaseEntity{
    //错误信息    {"error":"account: invalid account","code":2}

    var code: Int? = null
    var result: T? = null
    var error: String? = null
    constructor()

    constructor(code: Int?, result: T?, error: String?) {
        this.code = code
        this.result = result
        this.error = error
    }

    override fun toString(): String {
        val sb = StringBuffer()
        sb.append("code=$code error=$error\n result=$result")
        return sb.toString()
    }


}