package io.nebulas.wallet.android.module.staking.pledge

import com.young.binder.lifecycle.Data
import com.young.binder.lifecycle.DataCenterViewModel
import io.nebulas.wallet.android.module.staking.PledgeDetail
import io.nebulas.wallet.android.module.wallet.create.model.Wallet
import java.math.BigDecimal

class PledgeDataCenter: DataCenterViewModel() {

    enum class ButtonStatus{
        Disabled, Enabled, UploadingToChain
    }

    val defaultEstimateGasFee = "0.004"

    val error:Data<String> = Data(null)
    val isLoading:Data<Boolean> = Data(false)
    val buttonStatus:Data<ButtonStatus> = Data(ButtonStatus.Disabled)
    val estimateGasFee:Data<String> = Data("0.004")
    val pledgedInfo:Data<PledgeDetail> = Data(null)
    val walletData:Data<Wallet> = Data()
    val pledgeResult:Data<Boolean> = Data(false)
    val currentBalance:Data<BigDecimal> = Data(BigDecimal.ZERO)
    val insufficientBalance:Data<Boolean> = Data(false)

    var estimateGas:String = "20000"
    var gasPrice:String = "20000000000"
}