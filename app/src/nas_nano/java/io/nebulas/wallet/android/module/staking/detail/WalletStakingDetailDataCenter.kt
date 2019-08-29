package io.nebulas.wallet.android.module.staking.detail

import android.content.Intent
import com.young.binder.lifecycle.Data
import com.young.binder.lifecycle.DataCenterViewModel
import io.nebulas.wallet.android.common.DataCenter
import io.nebulas.wallet.android.module.staking.AddressProfits
import io.nebulas.wallet.android.module.staking.ProfitRecord
import io.nebulas.wallet.android.module.staking.StakingTools
import walletcore.Walletcore

class WalletStakingDetailDataCenter : DataCenterViewModel() {

    companion object {
        const val PARAM_ADDRESS = "param_address"
        const val PARAM_WALLET_NAME = "param_wallet_name"
        const val PARAM_PLEDGED_NAS = "param_pledged_nas"
        const val PARAM_PLEDGED_AGE = "param_pledged_age"
    }

    val defaultEstimateGasFee = "0.004"

    val error: Data<String> = Data("")
    val walletName: Data<String> = Data("")
    val address: Data<String> = Data("")
    val addressProfits: Data<AddressProfits> = Data(null)
    val profitsRecords: Data<MutableList<ProfitRecord>> = Data(mutableListOf())
    val isSwipeRefresh: Data<Boolean> = Data(true)
    val isCenterLoading: Data<Boolean> = Data(false)
    val showCancelPledgeTipDialog:Data<Boolean> = Data(false)
    val cancelPledgeComplete:Data<Boolean> = Data(false)

    var isSwipeLoadingMore = false
    var currentPage: Int = 1

    var estimateGas:String = "20000"
    var gasPrice:String = "20000000000"
    var estimateGasFee:String = defaultEstimateGasFee

    lateinit var pledgedNas: String
    lateinit var pledgedAge: String
    lateinit var addressBalance: String

    fun handleIntent(intent: Intent) {
        val walletNameString = intent.getStringExtra(PARAM_WALLET_NAME)
        walletName.value = walletNameString
        val addressString = intent.getStringExtra(PARAM_ADDRESS)
        address.value = addressString

        pledgedNas = intent.getStringExtra(PARAM_PLEDGED_NAS)
        pledgedAge = intent.getStringExtra(PARAM_PLEDGED_AGE)
        val nasCoin = DataCenter.coins.find { it.address==addressString && it.platform==Walletcore.NAS }
        addressBalance=nasCoin?.balance?:"0"
    }

    fun hasMore(): Boolean {
        val profitsInfo = addressProfits.value ?: return false
        return profitsInfo.total_page - profitsInfo.current_page > 0
    }
}