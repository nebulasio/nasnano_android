package io.nebulas.wallet.android.module.detail.model

import io.nebulas.wallet.android.base.BaseEntity
import io.nebulas.wallet.android.module.balance.model.Coin
import io.nebulas.wallet.android.module.transaction.model.Transaction
import io.nebulas.wallet.android.module.wallet.create.model.Address
import io.nebulas.wallet.android.module.wallet.create.model.Wallet

/**
 * Created by Heinoc on 2018/2/28.
 */
data class BalanceDetailModel(
        var wallet: Wallet? = null,
        var address: Address? = null,
        var coin: Coin? = null,
        var transaction: Transaction? = null,
        var categoryName: String? = null,
        var empty: Boolean = false,
        var loading: Boolean = false
) : BaseEntity()