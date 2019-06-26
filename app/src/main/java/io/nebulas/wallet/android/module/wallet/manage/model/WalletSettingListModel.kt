package io.nebulas.wallet.android.module.wallet.manage.model

import io.nebulas.wallet.android.base.BaseEntity

/**
 * Created by Heinoc on 2018/3/21.
 */
data class WalletSettingListModel(
        var cateName: String? = null,
        var name: String? = null,
        var des: String = "",
        var highlight: Boolean = false,
        var hasFooter: Boolean = false,
        var separate: Boolean = false
) : BaseEntity()