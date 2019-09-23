package io.nebulas.wallet.android.module.staking.dashboard

import com.young.binder.lifecycle.Data
import com.young.binder.lifecycle.DataCenterViewModel
import io.nebulas.wallet.android.extensions.logD
import io.nebulas.wallet.android.module.staking.PledgeDetail
import io.nebulas.wallet.android.module.staking.StakingConfiguration
import io.nebulas.wallet.android.module.staking.StakingSummary

class StakingDashboardDataCenter : DataCenterViewModel() {

    companion object {
        const val EVENT_DATA_SOURCE_CHANGED = "event_data_source_changed"
    }

    val error: Data<String> = Data("")

    val isLoading: Data<Boolean> = Data(true)

    var pledgeDetailList: List<PledgeDetail> = emptyList()
        set(value) {
            field = value
            notifyDataChanged(EVENT_DATA_SOURCE_CHANGED)
        }

    val stakingSummary: Data<StakingSummary> = Data(null)

    val stakingFailed: Data<Boolean> = Data(false)
    val stakingSuccess: Data<Boolean> = Data(false)

    val stakingCancelFailed: Data<Boolean> = Data(false)
    val stakingCancelSuccess: Data<Boolean> = Data(false)

    var inOperationWallets: List<StakingConfiguration.PledgingWalletWrapper> = emptyList()
        set(value) {
            field = value
            notifyDataChanged(EVENT_DATA_SOURCE_CHANGED)
        }
}