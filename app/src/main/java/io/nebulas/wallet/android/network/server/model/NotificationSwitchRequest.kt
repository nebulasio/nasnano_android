package io.nebulas.wallet.android.network.server.model

import io.nebulas.wallet.android.base.BaseEntity

data class NotificationSwitchRequest(var transactionPushOn:Int=1): BaseEntity()