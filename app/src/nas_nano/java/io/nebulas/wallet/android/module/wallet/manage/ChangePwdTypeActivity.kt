package io.nebulas.wallet.android.module.wallet.manage

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import io.nebulas.wallet.android.R
import io.nebulas.wallet.android.base.BaseActivity
import io.nebulas.wallet.android.common.DataCenter
import io.nebulas.wallet.android.module.wallet.create.ReImportWalletActivity
import io.nebulas.wallet.android.module.wallet.create.SetPassPhraseActivity
import io.nebulas.wallet.android.module.wallet.create.model.Wallet
import io.nebulas.wallet.android.util.SecurityHelper
import kotlinx.android.synthetic.nas_nano.activity_change_pwd_type.*
import kotlinx.android.synthetic.nas_nano.app_bar_main.*
import org.jetbrains.anko.startActivityForResult
import org.jetbrains.annotations.NotNull

class ChangePwdTypeActivity : BaseActivity() {

    companion object {

        const val WALLET = "wallet"
        /**
         * reset pwd request code
         */
        const val REQUEST_CODE_RESET_PWD = 10031
        /**
         * import wallet request code
         */
        const val REQUEST_CODE_RE_IMPORT = 10032

        /**
         * import wallet result code
         */
        const val RESULT_CODE_RE_IMPORT = 10033

        /**
         * 启动ChangePwdTypeActivity
         *
         * @param context
         * @param requestCode
         * @param wallet
         */
        fun launch(@NotNull context: Context, @NotNull requestCode: Int, wallet:Wallet) {
            DataCenter.setData(WALLET, wallet)
            (context as AppCompatActivity).startActivityForResult<ChangePwdTypeActivity>(requestCode)
        }
    }

    private lateinit var wallet:Wallet

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_change_pwd_type)
    }

    override fun initView() {
        showBackBtn(true, toolbar)
        titleTV.setText(R.string.change_pwd_title)

        wallet = if (DataCenter.containsData(WALLET)) {
            DataCenter.getData(WALLET, true) as Wallet
        } else {
            finish()
            return
        }

        rememberPwdLayout.setOnClickListener {
            //            ResetPwdActivity.launch(this, REQUEST_CODE_RESET_PWD, walletIndex)
            if (SecurityHelper.isWalletLocked(wallet)) {
                toastErrorMessage(R.string.tip_password_has_locked)
                return@setOnClickListener
            }
            SetPassPhraseActivity.launch(this,
                    REQUEST_CODE_RESET_PWD,
                    SetPassPhraseActivity.SourceType.CHANGE_PASSWORD,
                    wallet)
        }

        forgotPwdLayout.setOnClickListener {
            ReImportWalletActivity.launch(this, REQUEST_CODE_RE_IMPORT, wallet)
        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            REQUEST_CODE_RESET_PWD -> {
                if (resultCode == Activity.RESULT_OK) {
                    setResult(Activity.RESULT_OK)
                    finish()
                }
            }
            REQUEST_CODE_RE_IMPORT -> {
                if (resultCode == Activity.RESULT_OK) {
                    setResult(RESULT_CODE_RE_IMPORT)
                    finish()
                }
            }
        }

    }

}
