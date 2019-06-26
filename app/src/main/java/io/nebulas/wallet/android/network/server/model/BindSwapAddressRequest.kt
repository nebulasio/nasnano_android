package io.nebulas.wallet.android.network.server.model

import io.nebulas.wallet.android.base.BaseEntity

class BindSwapAddressRequest : BaseEntity() {
    var eth_address: String? = null
    var nas_address: String? = null
    var signature: String? = null
}