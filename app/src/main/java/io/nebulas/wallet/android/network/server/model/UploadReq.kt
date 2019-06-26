package io.nebulas.wallet.android.network.server.model

import io.nebulas.wallet.android.base.BaseEntity

/**
 * Created by Heinoc on 2018/3/15.
 */
data class UploadReq(
        var address: String? = null,
        var platform: String? = null

) : BaseEntity()