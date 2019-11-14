package io.nebulas.wallet.android.module.vote

import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.content.Intent
import android.os.Bundle
import io.nebulas.wallet.android.R
import io.nebulas.wallet.android.base.BaseActivity
import io.nebulas.wallet.android.common.*
import io.nebulas.wallet.android.db.DBUtil
import io.nebulas.wallet.android.dialog.VerifyPasswordDialog
import io.nebulas.wallet.android.module.transaction.TxDetailActivity
import io.nebulas.wallet.android.module.transaction.model.Transaction
import io.nebulas.wallet.android.module.wallet.create.model.Address
import io.nebulas.wallet.android.module.wallet.create.model.Wallet
import io.nebulas.wallet.android.network.callback.OnResultCallBack
import io.nebulas.wallet.android.network.nas.model.NASTransactionModel
import io.nebulas.wallet.android.network.subscriber.HttpSubscriber
import io.nebulas.wallet.android.util.SecurityHelper
import io.nebulas.wallet.android.view.research.CurtainResearch
import kotlinx.android.synthetic.nas_nano.activity_vote.*
import kotlinx.android.synthetic.nas_nano.app_bar_main_no_underline.*
import org.jetbrains.anko.doAsync
import walletcore.Payload
import walletcore.Walletcore
import java.math.BigDecimal

class VoteActivity : BaseActivity(), TransferConfirmDialog.OnConfirmListener {

    companion object {
        const val PARAM_CONTRACT_ADDRESS = "param_contract_address"
        const val PARAM_AMOUNT_NAT = "param_amount_nat"
        const val PARAM_FUNCTION = "param_function"
        const val PARAM_ARGS = "param_args"

        fun launch(context: Context,
                   contractAddress: String,
                   amountNAT: String,
                   function: String,
                   args: String) {
            context.startActivity(Intent(context, VoteActivity::class.java).apply {
                putExtra(PARAM_CONTRACT_ADDRESS, contractAddress)
                putExtra(PARAM_AMOUNT_NAT, amountNAT)
                putExtra(PARAM_FUNCTION, function)
                putExtra(PARAM_ARGS, args)
            })
        }
    }

    private lateinit var viewModel: VoteViewModel

    private lateinit var controller: VoteController

    private var verifyPWDDialog: VerifyPasswordDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProviders.of(this).get(VoteViewModel::class.java)
        controller = VoteController(viewModel, this, this)
        handleIntent()
        setContentView(R.layout.activity_vote)
        showBackBtn(true, toolbar)
        titleTV.text = "投票"
        tvTokenSymbol.text = Constants.voteContractsMap[viewModel.contractAddress]
        controller.getGas()
    }

    private fun handleIntent() {
        viewModel.handleIntent(intent)
    }

    override fun initView() {
        tvContractAddress.text = viewModel.contractAddress
        tvAmount.text = viewModel.amountNAT

        viewModel.gasPrice.observe(this) {
            gasFeeET.text = "${controller.calculateGasFee().toPlainString()} NAS"
        }
        viewModel.estimateGas.observe(this) {
            gasFeeET.text = "${controller.calculateGasFee().toPlainString()} NAS"
        }
        viewModel.payError.observe(this) {
            it?.apply {
                if (isNotEmpty()) {
                    if (getString(R.string.wrong_pwd) == this) {
                        val wallet = viewModel.currentWallet?:return@apply
                        if (SecurityHelper.walletWrongPassword(wallet)){
                            verifyPWDDialog?.toastErrorMessage(getString(R.string.tip_password_error_to_lock))
                            mainHandler.postDelayed({
                                verifyPWDDialog?.dismiss()
                            }, 1800)
                        } else {
                            verifyPWDDialog?.toastErrorMessage(this)
                        }
                    } else {
                        verifyPWDDialog?.toastErrorMessage(this)
                    }
                }
                verifyPWDDialog?.reset()
            }
        }

        nextStepBtn.setOnClickListener {
            confirmTransaction()
        }
    }

    override fun onConfirmed(wallet: Wallet) {
        if (SecurityHelper.isWalletLocked(wallet)) {
            CurtainResearch.create(this)
                    .withLevel(CurtainResearch.CurtainLevel.ERROR)
                    .withContent(getString(R.string.tip_password_has_locked))
                    .show()
            return
        }
        viewModel.currentWallet = wallet
        verifyPWDDialog = VerifyPasswordDialog(activity = this,
                title = getString(R.string.payment_password_text),
                passwordType = if (wallet.isComplexPwd) PASSWORD_TYPE_COMPLEX else PASSWORD_TYPE_SIMPLE,
                onNext = { passPhrase ->
                    controller.doTransfer(wallet, passPhrase)
                })
        verifyPWDDialog?.show()
    }

    private fun confirmTransaction() {
        val token = Constants.voteContractsMap[viewModel.contractAddress]?:""
        val confirmDialog = TransferConfirmDialog(
                context = this,
                title = getString(R.string.title_vote),
                targetAddress = viewModel.contractAddress,
                amount = viewModel.amountNAT,
                amountSymbol = token,
                amountTokenId = token,
                gasFee = controller.calculateGasFee().toPlainString(),
                onConfirmListener = this
        )
        confirmDialog.show()
    }
}
