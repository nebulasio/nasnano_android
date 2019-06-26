package io.nebulas.wallet.android.module.wallet.create.model

import io.nebulas.wallet.android.base.BaseEntity
import io.nebulas.wallet.android.module.balance.model.Coin

/**
 * Created by Heinoc on 2018/3/7.
 */
data class CoinListModel(
        var coin: Coin,
        var selected:Boolean = false
) : BaseEntity()