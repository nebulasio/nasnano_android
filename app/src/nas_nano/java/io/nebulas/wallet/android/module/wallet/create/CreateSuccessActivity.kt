package io.nebulas.wallet.android.module.wallet.create

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import io.nebulas.wallet.android.R
import io.nebulas.wallet.android.base.BaseActivity
import io.nebulas.wallet.android.common.Constants
import io.nebulas.wallet.android.common.DataCenter
import io.nebulas.wallet.android.module.wallet.create.model.Wallet
import io.nebulas.wallet.android.module.wallet.manage.MnemonicBackupCheckActivity
import io.nebulas.wallet.android.module.wallet.manage.WalletBackupActivity
import kotlinx.android.synthetic.nas_nano.activity_success.*
import kotlinx.android.synthetic.nas_nano.app_bar_main.*
import org.jetbrains.anko.startActivityForResult
import org.jetbrains.annotations.NotNull

class CreateSuccessActivity : BaseActivity() {
    companion object {

        const val WALLET = "wallet"
        fun launch(@NotNull context: Context, @NotNull requestCode: Int, wallet: Wallet? = null) {

            (context as AppCompatActivity).startActivityForResult<CreateSuccessActivity>(requestCode,
                    CreateWalletActivity.FROM_TYPE to requestCode, WALLET to wallet)
        }

    }

    var wallet: Wallet? = null
    var fromType = 0
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_success)
    }

    override fun initView() {
        showBackBtn(true, toolbar)
        titleTV.text = getString(R.string.success_create_title)
        fromType = intent.getIntExtra(CreateWalletActivity.FROM_TYPE, 0)
        wallet = intent.getSerializableExtra(WALLET) as Wallet
        if (fromType == CreateWalletActivity.CREATE_CHECK_ALL_SUPPORT) {
            confirmBtn.text = getString(R.string.backup_tip_confirm)
            backupTV.visibility = View.GONE
        } else {
            confirmBtn.text = getString(R.string.back_to_dapp)
            backupTV.visibility = View.VISIBLE
        }
        confirmBtn.setOnClickListener {
            if (fromType == CreateWalletActivity.CREATE_CHECK_ALL_SUPPORT) {
                DataCenter.setData(MnemonicBackupCheckActivity.CLICK_BACKUP_FROM,Constants.Backup_NewUser_Success)
                WalletBackupActivity.launch(this,
                        fromType,
                        getString(R.string.wallet_backup_mnemonic),
                        wallet!!)
            } else {
                onBack()
            }
        }
        backupTV.setOnClickListener {
            firebaseAnalytics?.logEvent(Constants.Backup_NewUser_Click, Bundle())
            DataCenter.setData(MnemonicBackupCheckActivity.CLICK_BACKUP_FROM,Constants.Backup_NewUser_Success)
            WalletBackupActivity.launch(this,
                    fromType,
                    getString(R.string.wallet_backup_mnemonic),
                    wallet!!)
        }
    }

    private fun onBack() {
        val data = Intent()
        data.putExtra(CreateWalletActivity.WALLET_RESULT, wallet)
        setResult(Activity.RESULT_OK, data)
        finish()
    }

    override fun onBackPressed() {
        onBack()
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when(requestCode){
            CreateWalletActivity.CREATE_CHECK_ALL_SUPPORT,CreateWalletActivity.CREATE_CHECK_FROM_WEB -> finish()
        }
    }
}