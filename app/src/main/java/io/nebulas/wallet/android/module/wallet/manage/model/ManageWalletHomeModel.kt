package io.nebulas.wallet.android.module.wallet.manage.model

import io.nebulas.wallet.android.base.BaseEntity
import io.nebulas.wallet.android.module.wallet.create.model.Wallet

/**
 * Created by Heinoc on 2018/3/21.
 */
data class ManageWalletHomeModel(
        var cateName: String? = null,
        var wallet: Wallet? = null,
        var hasFooter: Boolean = false
) : BaseEntity()