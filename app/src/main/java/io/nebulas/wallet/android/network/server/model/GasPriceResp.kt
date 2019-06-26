package io.nebulas.wallet.android.network.server.model

import io.nebulas.wallet.android.base.BaseEntity

/**
 * Created by Heinoc on 2018/3/14.
 */
data class GasPriceResp(
        var gasPriceMin: String? = null,
        var estimateGas: String? = null,
        var gasPriceMax: String? = null,
        var nonce: String? = "1"
) : BaseEntity()