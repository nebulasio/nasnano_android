package io.nebulas.wallet.android.network.server.model

import io.nebulas.wallet.android.base.BaseEntity

/**
 * Created by Heinoc on 2018/8/24.
 */
data class SwapEntranceResp(
        /**
         * 0：关闭；
         * 1：打开
         */
        var transfer_config: Int,
        var transfer_status: Int,
        var gas_fee_lower: String? = null,
        var gas_fee_upper: String? = null,
        var notice: String? = null
) : BaseEntity()