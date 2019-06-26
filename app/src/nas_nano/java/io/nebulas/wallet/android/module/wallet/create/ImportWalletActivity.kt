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
import io.nebulas.wallet.android.extensions.initCollapsingToolbar
import io.nebulas.wallet.android.module.html.HtmlActivity
import io.nebulas.wallet.android.module.wallet.create.model.Wallet
import kotlinx.android.synthetic.nas_nano.activity_import_wallet.*
import org.jetbrains.anko.startActivityForResult
import org.jetbrains.annotations.NotNull
import walletcore.Walletcore

/**
 * Created by Heinoc on 2018/2/2.
 */

class ImportWalletActivity : BaseActivity() {

    companion object {

        /**
         * 导入钱包的方式
         */
        const val MNEMONIC = "mnemonic"
        const val KEY_JSON = "keyJson"
        const val PRIVATEKEY = "privateKey"

        const val WALLET = "wallet"
        const val FROM_TYPE = "fromType"
        /**
         * 启动ImportWalletActivity
         *
         * @param context
         * @param requestCode
         * @param wallet
         */
        fun launch(@NotNull context: Context, @NotNull requestCode: Int, wallet: Wallet? = null) {
            if (wallet != null) {
                DataCenter.setData(WALLET, wallet)
            }
            (context as BaseActivity).firebaseAnalytics?.logEvent(Constants.kAImportWalletBtnClick, Bundle())
            (context as AppCompatActivity).startActivityForResult<ImportWalletActivity>(requestCode)
        }
    }

    var wallet: Wallet? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_import_wallet)
    }

    override fun initView() {

        initCollapsingToolbar(getString(R.string.select_import_type), { onBackPressed() })

        if (DataCenter.containsData(WALLET)) {
            wallet = DataCenter.getData(WALLET, true) as Wallet
        }

        importFromMnemonicLayout.setOnClickListener {
            firebaseAnalytics?.logEvent(Constants.kAImportWalletMnemonicClick, Bundle())

            ImportFromMnemonicActivity.launch(this,
                    CreateWalletActivity.IMPORT_WALLET_REQUEST_CODE,
                    wallet)

        }

        importFromKeystoreLayout.setOnClickListener {
            firebaseAnalytics?.logEvent(Constants.kAImportWalletKeystoreClick, Bundle())

            /*BlockChainTypeActivity.launch(this,
                    CreateWalletActivity.IMPORT_WALLET_REQUEST_CODE,
                    BlockChainTypeActivity.BlockChainTypeLaunchType.IMPORT_WALLET,
                    KEY_JSON,
                    walletIndex,
                    getString(R.string.import_wallet_title))*/

            ImportFromKeyStoreActivity.launch(this,
                    CreateWalletActivity.IMPORT_WALLET_REQUEST_CODE,
                    Walletcore.NAS,
                    wallet)


//            startActivityForResult<KeyStoreCoinListActivity>(CreateAccountActivity.IMPORT_WALLET_REQUEST_CODE)
        }

        importFromPlainPrivLayout.setOnClickListener {
            firebaseAnalytics?.logEvent(Constants.kAImportWalletPrivateKeyClick, Bundle())

            /*BlockChainTypeActivity.launch(this,
                    CreateWalletActivity.IMPORT_WALLET_REQUEST_CODE,
                    BlockChainTypeActivity.BlockChainTypeLaunchType.IMPORT_WALLET,
                    PRIVATEKEY,
                    walletIndex,
                    getString(R.string.import_wallet_title))*/

            ImportFromPlainPrivActivity.launch(this,
                    CreateWalletActivity.IMPORT_WALLET_REQUEST_CODE,
                    Walletcore.NAS,
                    wallet)
        }

        importHelpLayout.setOnClickListener {
            HtmlActivity.launch(this, getString(R.string.import_wallet_guide), getString(R.string.import_wallet_help))
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

