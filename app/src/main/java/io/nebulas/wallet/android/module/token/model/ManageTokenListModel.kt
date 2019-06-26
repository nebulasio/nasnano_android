package io.nebulas.wallet.android.module.token.model

import io.nebulas.wallet.android.base.BaseEntity
import io.nebulas.wallet.android.module.balance.model.Coin

/**
 * Created by Heinoc on 2018/5/16.
 */
data class ManageTokenListModel(
        var category: String? = null,
        var currencyName: String? = null,
        var currencySymbol: String? = null,
        var coin: Coin? = null,
        var emptyView: Boolean? = false
) : BaseEntity()