package io.nebulas.wallet.android.module.transaction.model

import io.nebulas.wallet.android.base.BaseEntity

/**
 * Created by Heinoc on 2018/3/13.
 */
data class TransferTargetListModel(
        var category: String? = null,
        var targetType: String? = null,
        var transaction: Transaction? = null,
        var image: Int,
        var imageBg: Int
) : BaseEntity()