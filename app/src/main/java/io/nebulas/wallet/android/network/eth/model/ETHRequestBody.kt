package io.nebulas.wallet.android.network.eth.model

import io.nebulas.wallet.android.base.BaseEntity
import java.util.*

/**
 * Created by Heinoc on 2018/3/1.
 */
data class ETHRequestBody (

    /**
     * rpc version
     */
    var jsonrpc: String? = null,
    /**
     * rpc method
     */
    var method: String? = null,
    /**
     * params
     */
    var params: Array<Any>? = arrayOf(),
    /**
     * chainID
     */
    var id: Int? = 1
) : BaseEntity() {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ETHRequestBody

        if (jsonrpc != other.jsonrpc) return false
        if (method != other.method) return false
        if (!Arrays.equals(params, other.params)) return false
        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        var result = jsonrpc?.hashCode() ?: 0
        result = 31 * result + (method?.hashCode() ?: 0)
        result = 31 * result + (params?.let { Arrays.hashCode(it) } ?: 0)
        result = 31 * result + (id ?: 0)
        return result
    }
}