package io.nebulas.wallet.android.module.wallet.create

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import io.nebulas.wallet.android.BuildConfig
import io.nebulas.wallet.android.R
import io.nebulas.wallet.android.base.BaseActivity
import io.nebulas.wallet.android.common.DataCenter
import io.nebulas.wallet.android.module.wallet.create.model.Wallet
import io.nebulas.wallet.android.module.wallet.manage.WalletBackupActivity
import io.nebulas.wallet.android.module.wallet.manage.WalletBackupDetailActivity
import kotlinx.android.synthetic.nas_nano.activity_block_chain_type.*
import kotlinx.android.synthetic.nas_nano.app_bar_main.*
import org.jetbrains.anko.startActivityForResult
import org.jetbrains.annotations.NotNull
import walletcore.Walletcore
import java.io.Serializable

class BlockChainTypeActivity : BaseActivity() {

    enum class BlockChainTypeLaunchType : Serializable {
        IMPORT_WALLET,
        BACKUP_WALLET
    }

    companion object {
        const val LAUNCH_TYPE = "launchType"
        const val IMPORT_TYPE = "importType"
        const val WALLET = "wallet"
        const val TITLE = "title"
        /**
         * 当前备份类型，值在strings.xml中配置
         * R.string.wallet_backup_mnemonic
         * R.string.wallet_backup_keystore
         * R.string.wallet_backup_plain_priv
         */
        const val BACKUP_TYPE = "backupType"

        /**
         * 当前需要备份的钱包里的币类型
         */
        const val WALLET_PASSPHRASE = "walletPassphrase"

        /**
         * 启动BlockChainTypeActivity
         *
         * @param context
         * @param requestCode
         * @param launchType
         * @param wallet
         * @param title
         */
        fun launch(@NotNull context: Context, requestCode: Int,
                   @NotNull launchType: BlockChainTypeLaunchType,
                   importType: String,
                   wallet: Wallet? = null,
                   title: String = "") {
            if (wallet != null) {
                DataCenter.setData(WALLET, wallet)
            }
            (context as AppCompatActivity).startActivityForResult<BlockChainTypeActivity>(requestCode,
                    LAUNCH_TYPE to launchType,
                    IMPORT_TYPE to importType,
                    TITLE to title)
        }

        /**
         * 从钱包备份启动BlockChainTypeActivity
         *
         * @param context
         * @param requestCode
         * @param launchType
         * @param wallet
         * @param title
         */
        fun launch(@NotNull context: Context,
                   requestCode: Int,
                   @NotNull launchType: BlockChainTypeLaunchType,
                   @NotNull wallet: Wallet,
                   backupType: String,
                   walletPassPhrase: String,
                   title: String = "") {
            (context as AppCompatActivity).startActivityForResult<BlockChainTypeActivity>(requestCode,
                    LAUNCH_TYPE to launchType,
                    WALLET to wallet,
                    BACKUP_TYPE to backupType,
                    WALLET_PASSPHRASE to walletPassPhrase,
                    TITLE to title)
        }


    }

    private var wallet: Wallet? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_block_chain_type)
    }

    override fun initView() {

        showBackBtn(true, toolbar = toolbar)
        titleTV.text = intent.getStringExtra(TITLE)

        if (intent.hasExtra(LAUNCH_TYPE)) {
            when (intent.getSerializableExtra(LAUNCH_TYPE)) {
                BlockChainTypeLaunchType.IMPORT_WALLET -> {
                    if (DataCenter.containsData(WALLET)) {
                        wallet = DataCenter.getData(WALLET, true) as Wallet
                    }
                    nasLayout.visibility = View.GONE
                    ethLayout.visibility = View.GONE

                    BuildConfig.PLATFORMS.split(";").forEach {
                        when (it) {
                            Walletcore.NAS -> {
                                nasLayout.visibility = View.VISIBLE
                            }
                            Walletcore.ETH -> {
                                ethLayout.visibility = View.VISIBLE
                            }
                        }
                    }

                    nasLayout.setOnClickListener {
                        goToImport(Walletcore.NAS)
                    }

                    ethLayout.setOnClickListener {
                        goToImport(Walletcore.ETH)
                    }

                }
                BlockChainTypeLaunchType.BACKUP_WALLET -> {

                    wallet = intent.getSerializableExtra(WALLET) as Wallet

                    nasLayout.visibility = View.GONE
                    ethLayout.visibility = View.GONE

                    DataCenter.addresses.forEach {
                        if (it.walletId == wallet?.id) {
                            when (it.platform) {
                                Walletcore.NAS -> {
                                    nasLayout.visibility = View.VISIBLE
                                }
                                Walletcore.ETH -> {
                                    ethLayout.visibility = View.VISIBLE
                                }
                            }
                        }
                    }

                    nasLayout.setOnClickListener {
                        goToWalletBackUpDetail(Walletcore.NAS)
                    }

                    ethLayout.setOnClickListener {
                        goToWalletBackUpDetail(Walletcore.ETH)
                    }

                }
            }
        }

    }

    private fun goToImport(platform: String) {
        if (intent.hasExtra(IMPORT_TYPE)) {
            when (intent.getStringExtra(IMPORT_TYPE)) {
                ImportWalletActivity.KEY_JSON -> {

                    ImportFromKeyStoreActivity.launch(this,
                            CreateWalletActivity.IMPORT_WALLET_REQUEST_CODE,
                            platform,
                            wallet)

                }
                ImportWalletActivity.PRIVATEKEY -> {

                    ImportFromPlainPrivActivity.launch(this,
                            CreateWalletActivity.IMPORT_WALLET_REQUEST_CODE,
                            platform,
                            wallet)

                }
            }
        }
    }

    private fun goToWalletBackUpDetail(platform: String) {
        WalletBackupDetailActivity.launch(this,
                WalletBackupActivity.REQUEST_CODE_BACKUP_DETAIL,
                intent.getStringExtra(BACKUP_TYPE),
                wallet!!,
                platform,
                intent.getStringExtra(WALLET_PASSPHRASE))

    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (Activity.RESULT_OK == resultCode) {
            setResult(resultCode, data)
            finish()
        }
    }

}
