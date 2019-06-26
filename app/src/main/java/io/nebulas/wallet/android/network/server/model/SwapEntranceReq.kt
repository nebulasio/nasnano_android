package io.nebulas.wallet.android.network.server.model

import io.nebulas.wallet.android.base.BaseEntity

/**
 * Created by Heinoc on 2018/8/24.
 */
data class SwapEntranceReq(
        var eth_address: String,
        var eth_tx_hash: String = ""
) : BaseEntity()