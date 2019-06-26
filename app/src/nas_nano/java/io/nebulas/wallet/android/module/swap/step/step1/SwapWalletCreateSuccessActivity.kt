package io.nebulas.wallet.android.module.swap.step.step1

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import io.nebulas.wallet.android.R
import io.nebulas.wallet.android.base.BaseActivity
import io.nebulas.wallet.android.common.Constants
import io.nebulas.wallet.android.common.DataCenter
import io.nebulas.wallet.android.module.swap.SwapHelper
import io.nebulas.wallet.android.module.wallet.create.model.Address
import io.nebulas.wallet.android.module.wallet.create.model.Wallet
import io.nebulas.wallet.android.module.wallet.manage.WalletBackupActivity
import kotlinx.android.synthetic.nas_nano.activity_swap_wallet_create_success.*
import kotlinx.android.synthetic.nas_nano.app_bar_main.*
import walletcore.Walletcore

class SwapWalletCreateSuccessActivity : BaseActivity() {

    companion object {

        private const val REQUEST_CODE_BACKUP = 10001
        private const val KEY_SWAP_WALLET_INFO = "key_swap_wallet_info"

        fun launch(context: Context, swapWalletInfo: SwapHelper.SwapWalletInfo) {
            context.startActivity(Intent(context, SwapWalletCreateSuccessActivity::class.java).apply {
                putExtra(KEY_SWAP_WALLET_INFO, swapWalletInfo)
            })
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_swap_wallet_create_success)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_BACKUP && resultCode == Activity.RESULT_OK) {
            SwapHelper.swapWalletBackupSuccess(this)
            finish()
        }
    }

    override fun initView() {
        titleTV.text = getString(R.string.swap_title_setup_successful)
        showBackBtn(true, toolbar)

        val swapWalletInfo = intent.getSerializableExtra(KEY_SWAP_WALLET_INFO) as SwapHelper.SwapWalletInfo

        tv_backup_later.setOnClickListener {
            finish()
        }

        tv_backup_now.setOnClickListener {
            firebaseAnalytics?.logEvent(Constants.Exchange_NewBackup_Success,Bundle())
            val wallet = Wallet("Swap-Wallet")
            wallet.id = -10001
            wallet.setMnemonic(swapWalletInfo.swapWalletWords)
            wallet.isComplexPwd = swapWalletInfo.isComplexPassword
            val address = Address(swapWalletInfo.swapWalletAddress, swapWalletInfo.swapWalletKeystore, Walletcore.ETH)
            DataCenter.swapAddress = address
            WalletBackupActivity.launch(this, REQUEST_CODE_BACKUP, resources.getString(R.string.wallet_backup_mnemonic), wallet)
        }
    }
}
