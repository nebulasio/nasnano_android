package io.nebulas.wallet.android.module.transaction.model

import io.nebulas.wallet.android.base.BaseEntity

/**
 * Created by Heinoc on 2018/3/31.
 */
data class BlockStateModel (

    var chain_id: Int = 0,
    var tail: String? = null,
    var lib: String? = null,
    var height: String? = null,
    var protocol_version: String? = null,
    var synchronized: Boolean = false,
    var version: String? = null
) : BaseEntity()