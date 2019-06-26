package io.nebulas.wallet.android.network.server.model

import io.nebulas.wallet.android.base.BaseEntity
import java.io.Serializable

class SwapTransferResp : BaseEntity() {

    var eth_gas_price: String? = null
    var amount: String? = null
    var eth_gas_used: String? = null
    var eth_address: String? = null
    var send_erc_timestamp: Long? = null
    var send_nas_timestamp: Long? = null
    var nas_gass_used: String? = null
    var status: Int? = null
    var neb_timestamp: Long? = null
    var neb_tx_hash: String? = null
    var neb_address: String? = null
    var eth_tx_hash: String? = null
}