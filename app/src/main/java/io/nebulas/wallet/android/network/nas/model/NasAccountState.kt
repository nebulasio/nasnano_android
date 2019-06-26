package io.nebulas.wallet.android.network.nas.model

import io.nebulas.wallet.android.base.BaseEntity

class NasAccountState : BaseEntity() {
    var balance: String = "0"
    var nonce: Long = 0
    var type: Int = 87
}