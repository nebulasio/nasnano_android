package io.nebulas.wallet.android.module.swap.step.step1

import android.content.Context
import android.content.Intent
import android.os.Bundle
import io.nebulas.wallet.android.R
import io.nebulas.wallet.android.base.BaseActivity
import io.nebulas.wallet.android.common.DataCenter
import io.nebulas.wallet.android.module.swap.SwapHelper
import io.nebulas.wallet.android.module.wallet.create.model.Address
import io.nebulas.wallet.android.module.wallet.create.model.Wallet
import io.nebulas.wallet.android.module.wallet.manage.WalletBackupActivity
import kotlinx.android.synthetic.nas_nano.activity_backup_again.*
import kotlinx.android.synthetic.nas_nano.app_bar_main.*
import walletcore.Walletcore

class BackupAgainActivity : BaseActivity() {

    companion object {
        private const val REQUEST_CODE_BACKUP = 10001
        private const val KEY_SWAP_WALLET_INFO = "key_swap_wallet_info"

        fun launch(context: Context, swapWalletInfo: SwapHelper.SwapWalletInfo) {
            context.startActivity(Intent(context, BackupAgainActivity::class.java).apply {
                putExtra(KEY_SWAP_WALLET_INFO, swapWalletInfo)
            })
        }
    }

    private lateinit var swapWalletInfo: SwapHelper.SwapWalletInfo

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_backup_again)
    }

    override fun initView() {
        titleTV.text = getString(R.string.swap_title_backup_again)
        showBackBtn(true, toolbar)
        swapWalletInfo = intent.getSerializableExtra(KEY_SWAP_WALLET_INFO) as SwapHelper.SwapWalletInfo

        layout_backup_keystore.setOnClickListener {
            backup(resources.getString(R.string.wallet_backup_keystore))
        }

        layout_backup_private_key.setOnClickListener {
            backup(resources.getString(R.string.wallet_backup_plain_priv))
        }
    }

    private fun backup(backupTitle: String) {
        val wallet = Wallet("Swap-Wallet")
        wallet.id = -10001
        wallet.setMnemonic(swapWalletInfo.swapWalletWords)
        wallet.isComplexPwd = swapWalletInfo.isComplexPassword
        val address = Address(swapWalletInfo.swapWalletAddress, swapWalletInfo.swapWalletKeystore, Walletcore.ETH)
        DataCenter.swapAddress = address
        WalletBackupActivity.launch(this, REQUEST_CODE_BACKUP, backupTitle, wallet)
    }
}
