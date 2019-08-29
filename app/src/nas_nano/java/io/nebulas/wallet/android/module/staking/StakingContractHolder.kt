package io.nebulas.wallet.android.module.staking

import io.nebulas.wallet.android.network.server.model.StakingContractsResponse

object StakingContractHolder {

    var stakingProxyContract: String? = null
    var dataContract: String? = null
    var startHeight: String? = null
    var stageStep: String? = null

    public fun holdStakingContractInfo(stakingContractsResponse: StakingContractsResponse) {
        this.stakingProxyContract = stakingContractsResponse.stakingProxy
        this.dataContract = stakingContractsResponse.data
        this.startHeight = stakingContractsResponse.startHeight
        this.stageStep = stakingContractsResponse.stageStep
    }
}