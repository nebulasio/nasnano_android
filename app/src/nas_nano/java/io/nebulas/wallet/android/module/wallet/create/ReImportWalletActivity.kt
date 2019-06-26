package io.nebulas.wallet.android.module.wallet.create

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import io.nebulas.wallet.android.R
import io.nebulas.wallet.android.base.BaseActivity
import io.nebulas.wallet.android.common.Constants
import io.nebulas.wallet.android.common.DataCenter
import io.nebulas.wallet.android.module.wallet.create.model.Wallet
import io.nebulas.wallet.android.module.wallet.manage.ChangePwdTypeActivity
import kotlinx.android.synthetic.nas_nano.activity_reimport_wallet.*
import kotlinx.android.synthetic.nas_nano.app_bar_main_no_underline.*
import org.jetbrains.anko.startActivityForResult
import org.jetbrains.annotations.NotNull


class ReImportWalletActivity : BaseActivity() {

    companion object {

        /**
         * 导入钱包的方式
         */
        const val KEY_JSON = "keyJson"
        const val PRIVATEKEY = "privateKey"

        const val WALLET = "wallet"
        /**
         * 启动ImportWalletActivity
         *
         * @param context
         * @param requestCode
         * @param wallet
         */
        fun launch(@NotNull context: Context, @NotNull requestCode: Int, wallet: Wallet) {
            DataCenter.setData(WALLET, wallet)
            (context as AppCompatActivity).startActivityForResult<ReImportWalletActivity>(requestCode)
        }
    }

    private var wallet: Wallet? = null
    var platform: String? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reimport_wallet)
    }

    override fun initView() {

        titleTV.setText(R.string.change_pwd_title)
        showBackBtn(true, toolbar)

        wallet = if (DataCenter.containsData(WALLET)) {
            DataCenter.getData(WALLET, true) as Wallet
        } else {
            finish()
            return
        }

        setImportType()

        importFromMnemonicLayout.setOnClickListener {
            ImportFromMnemonicActivity.launch(this,
                    ChangePwdTypeActivity.REQUEST_CODE_RE_IMPORT,
                    wallet)

        }

        keystoreTv.setOnClickListener {


            ImportFromKeyStoreActivity.launch(this,
                    ChangePwdTypeActivity.REQUEST_CODE_RE_IMPORT,
                    platform!!,
                    wallet)

//            startActivityForResult<KeyStoreCoinListActivity>(CreateAccountActivity.IMPORT_WALLET_REQUEST_CODE)
        }

        plainPrivTv.setOnClickListener {

            ImportFromPlainPrivActivity.launch(this,
                    ChangePwdTypeActivity.REQUEST_CODE_RE_IMPORT,
                    platform!!,
                    wallet)

        }

    }

    private fun setImportType() {
        if (DataCenter.wallets.size == 0 || wallet == null) {
            return
        }
        val finalWallet = wallet ?: return
        /*var des: String? = null
        if (wallet.createdByMnemonic) {
            importFromKeyORPriLayout.visibility = View.GONE
            importFromMnemonicLayout.visibility = View.VISIBLE
            des = getString(R.string.reimport_from_mnemonic)
        } else {
            importFromMnemonicLayout.visibility = View.GONE
            importFromKeyORPriLayout.visibility = View.VISIBLE
            des = getString(R.string.keystore_privatekey)
        }
        descriptionTop.text = String.format(getString(R.string.reimport_des), des, des)*/
        run breakpoint@{
            DataCenter.coins.forEach {
                if (finalWallet.id == it.walletId) {
                    platform = it.platform
                    return@breakpoint
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()

        /**
         * GA
         */
        firebaseAnalytics?.logEvent(Constants.kAImportWalletBtnClick, Bundle())
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (Activity.RESULT_OK == resultCode) {
            setResult(resultCode, data)
            finish()
        }
    }

}