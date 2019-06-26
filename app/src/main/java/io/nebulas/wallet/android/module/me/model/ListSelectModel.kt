package io.nebulas.wallet.android.module.me.model

import io.nebulas.wallet.android.base.BaseEntity

/**
 * Created by Heinoc on 2018/4/7.
 */
data class ListSelectModel(
        var cateName: String? = null,
        var value: String? = null,
        var selected: Boolean = false
) : BaseEntity()