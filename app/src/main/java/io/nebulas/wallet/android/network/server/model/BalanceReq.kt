package io.nebulas.wallet.android.network.server.model

import io.nebulas.wallet.android.base.BaseEntity

/**
 * Created by Heinoc on 2018/3/12.
 */
class BalanceReq : BaseEntity(){
    var addresses: MutableList<UploadReq> = mutableListOf()
    var containChange: Boolean = true
}