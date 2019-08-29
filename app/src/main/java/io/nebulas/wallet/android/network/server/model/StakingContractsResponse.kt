package io.nebulas.wallet.android.network.server.model

import io.nebulas.wallet.android.app.WalletApplication
import io.nebulas.wallet.android.base.BaseEntity
import io.nebulas.wallet.android.util.DigestUtils


data class StakingContractsResponse(var stakingProxy: String? = null,
                                    var data: String? = null,
                                    var startHeight: String? = null,
                                    var stageStep: String? = null,
                                    var verification: String? = null) : BaseEntity() {
    public fun verify(): Boolean {
        if (stakingProxy == null || data == null || startHeight == null || stageStep == null) {
            return false
        }
        val sortedSet = sortedSetOf(stakingProxy, data, startHeight, stageStep)
        val builder = StringBuilder()
        sortedSet.forEach {
            builder.append(it)
        }
        builder.append(WalletApplication.INSTANCE.uuid)
        val md5 = DigestUtils.md5Encode(builder.toString())
        return verification == md5
    }
}