package io.nebulas.wallet.android.module.transaction.model

import io.nebulas.wallet.android.base.BaseEntity
import io.nebulas.wallet.android.module.balance.model.Coin

/**
 * Created by Heinoc on 2018/6/21.
 */
data class ChooseTokenListModel(
        var category: String? = null,
        var coin: Coin? = null,
        var seperator: Boolean = false
) : BaseEntity()