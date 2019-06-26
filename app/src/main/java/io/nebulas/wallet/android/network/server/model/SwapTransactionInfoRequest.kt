package io.nebulas.wallet.android.network.server.model

import io.nebulas.wallet.android.base.BaseEntity

data class SwapTransactionInfoRequest(
        val eth_address:String,
        val eth_tx_hash:String,
        val amount:String
) : BaseEntity()