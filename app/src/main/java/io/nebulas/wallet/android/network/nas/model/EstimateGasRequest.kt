package io.nebulas.wallet.android.network.nas.model

import io.nebulas.wallet.android.base.BaseEntity

class EstimateGasRequest: BaseEntity() {
    var from: String = ""
    var to: String = ""
    var value: String = "0"
    var gas_price: String = "1"
    var gas_limit: String = "20000"
    var type: String = "call"
    var contract: ContractCall? = null
}