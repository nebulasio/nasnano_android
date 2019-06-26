package io.nebulas.wallet.android.network.server.model

import io.nebulas.wallet.android.base.BaseEntity
import io.nebulas.wallet.android.util.Util

data class DeviceInfoRequest(
        var deviceId: String = "",
        var addresses: List<String> = emptyList(),
        var pushToken: String = "",
        var pushChannel: String = "",
        var language: String = "",
        var transactionPushOn: Int = 1) : BaseEntity()