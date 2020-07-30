package io.nebulas.wallet.android.module.vote

import android.content.Intent
import com.young.binder.lifecycle.Data
import com.young.binder.lifecycle.DataCenterViewModel
import io.nebulas.wallet.android.module.wallet.create.model.Wallet

class VoteViewModel : DataCenterViewModel() {
    lateinit var contractAddress: String
    lateinit var amountNAT: String
    lateinit var function: String
    lateinit var args: String

    var currentWallet: Wallet? = null

    val gasPrice: Data<String> = Data("20000000000")
    val estimateGas: Data<String> = Data("800000")
    val payError: Data<String> = Data()

    fun handleIntent(intent: Intent) {
        contractAddress = intent.getStringExtra(VoteActivity.PARAM_CONTRACT_ADDRESS)
        amountNAT = intent.getStringExtra(VoteActivity.PARAM_AMOUNT_NAT)
        function = intent.getStringExtra(VoteActivity.PARAM_FUNCTION)
        args = intent.getStringExtra(VoteActivity.PARAM_ARGS)
        gasPrice.value = intent.getStringExtra(VoteActivity.PARAM_GASPRICE)
        estimateGas.value = intent.getStringExtra(VoteActivity.PARAM_GASLIMIT)
    }
}