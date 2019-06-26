package io.nebulas.wallet.android.module.wallet.create

import android.app.Activity
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.view.Menu
import android.view.MenuItem
import android.view.View
import io.nebulas.wallet.android.R
import io.nebulas.wallet.android.base.BaseActivity
import io.nebulas.wallet.android.common.Constants
import io.nebulas.wallet.android.common.DataCenter
import io.nebulas.wallet.android.common.URLConstants
import io.nebulas.wallet.android.dialog.NasBottomDialog
import io.nebulas.wallet.android.module.balance.model.Coin
import io.nebulas.wallet.android.module.html.HtmlActivity
import io.nebulas.wallet.android.module.qrscan.QRScanActivity
import io.nebulas.wallet.android.module.wallet.create.model.Wallet
import io.nebulas.wallet.android.module.wallet.create.viewmodel.CreateWalletViewModel
import io.nebulas.wallet.android.module.token.viewmodel.SupportTokenViewModel
import io.nebulas.wallet.android.service.IntentServiceUpdateDeviceInfo
import io.nebulas.wallet.android.util.Formatter
import io.nebulas.wallet.android.util.SecurityHelper
import io.nebulas.wallet.android.util.TextChange
import kotlinx.android.synthetic.nas_nano.activity_import_from_key_store.*
import kotlinx.android.synthetic.nas_nano.app_bar_import_wallet.*
import org.jetbrains.anko.startActivityForResult
import org.jetbrains.annotations.NotNull

/**
 * Created by Heinoc on 2018/2/3.
 */
class ImportFromKeyStoreActivity : BaseActivity() {

    companion object {

        /**
         * 当前导入的keystore文件所属的数字货币类型
         */
        const val PLATFORM = "platform"
        const val WALLET = "wallet"
        /**
         * open QRScanActivity request code
         */
        const val REQUEST_CODE_SCAN_QRCODE = 40001

        /**
         * 启动ImportFromKeyStoreActivity
         *
         * @param context
         * @param requestCode
         * @param platform
         * @param wallet
         */
        fun launch(@NotNull context: Context, @NotNull requestCode: Int, @NotNull platform: String, wallet: Wallet? = null) {
            if (wallet != null) {
                DataCenter.setData(WALLET, wallet)
            }
            (context as AppCompatActivity).startActivityForResult<ImportFromKeyStoreActivity>(requestCode,
                    PLATFORM to platform,
                    ImportWalletActivity.FROM_TYPE to requestCode)
        }

    }

    lateinit var coinList: MutableList<Coin>

    lateinit var platform: String
    private var wallet: Wallet? = null

    lateinit var createWalletViewModel: CreateWalletViewModel
    lateinit var supportTokenViewModel: SupportTokenViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_import_from_key_store)
    }

    override fun screenshotEnabled(): Boolean {
        return false
    }

    override fun initView() {
        showBackBtn(true, toolbar)

        val fromType = intent.getIntExtra(ImportWalletActivity.FROM_TYPE, -1)
        if (fromType == CreateWalletActivity.IMPORT_WALLET_REQUEST_CODE) titleTV.setText(R.string.import_wallet_title) else titleTV.setText(R.string.verify_account)

        if (DataCenter.containsData(WALLET)) {
            wallet = DataCenter.getData(WALLET, true) as Wallet
        }

        platform = intent.getStringExtra(PLATFORM)

        createWalletViewModel = ViewModelProviders.of(this).get(CreateWalletViewModel::class.java)
        supportTokenViewModel = ViewModelProviders.of(this).get(SupportTokenViewModel::class.java)

        privacyTV.setOnClickListener {
            HtmlActivity.launch(this, URLConstants.PRIVACY_URL, getString(R.string.privacy_title))
        }
        serviceProtocolTV.setOnClickListener {
            HtmlActivity.launch(this, URLConstants.TERMS_URL_EN, getString(R.string.terms_service_title))
        }

        changeEyePwd.setOnClickListener {
            if (passPhraseEt.transformationMethod == HideReturnsTransformationMethod.getInstance()) {
                changeEyePwd.setImageResource(R.mipmap.open_eye)
                passPhraseEt.transformationMethod = PasswordTransformationMethod.getInstance()
            } else {
                changeEyePwd.setImageResource(R.mipmap.close_eye)
                passPhraseEt.transformationMethod = HideReturnsTransformationMethod.getInstance()
            }
        }
        importBtn.setOnClickListener {

            var keyJson = keystoreContentEt.text.toString().replace("\n", "").trim()
            var passPhrase = passPhraseEt.text.toString()
            if (keyJson.isEmpty() || passPhrase.isEmpty() || !agreeCB.isChecked) {
                return@setOnClickListener
            }


            importBtn.isEnabled = false
            importBtn.isClickable = false

            /**
             * 校验私钥，是否已存在
             */
            progressBar.visibility = View.VISIBLE
            createWalletViewModel.getPlainPrivateKeyFromKeyJson(platform,
                    keyJson,
                    passPhrase,
                    { privateKey ->
                        createWalletViewModel.getAddressFromPlainPrivateKey(privateKey, "", { addresses ->
                            progressBar.visibility = View.GONE
                            var isNewWallet = true
                            var walletId: Long? = null
                            run breakpoint@{
                                DataCenter.addresses.forEach {
                                    if (addresses.contains(it.address)) {
                                        isNewWallet = false
                                        walletId = it.walletId
                                        return@breakpoint
                                    }
                                }
                            }
                            if (isNewWallet) {
                                if (wallet != null) {
                                    /**
                                     * 修改密码导入了新钱包
                                     */
                                    showTipsDialog(getString(R.string.tips_title),
                                            getString(R.string.change_pwd_import_new_wallet),
                                            getString(R.string.i_see),
                                            {
                                                importBtn.isEnabled = true
                                                importBtn.isClickable = true
                                            })
                                } else {
                                    generateWallet(keyJson, passPhrase)
                                }
                            } else {
                                if (wallet == null) {
                                    /**
                                     * 导入的是已存在的钱包
                                     */
                                    showTipsDialog(getString(R.string.tips_title),
                                            getString(R.string.import_exist_wallet_tips),
                                            getString(R.string.cancel_import),
                                            {
                                                importBtn.isEnabled = true
                                                importBtn.isClickable = true
                                            },
                                            getString(R.string.continue_import),
                                            {
                                                generateWallet(keyJson, passPhrase)
                                            })
                                } else if (walletId == wallet?.id) {
                                    showTipsDialog(getString(R.string.tips_title),
                                            getString(R.string.change_pwd_import_exist_wallet_tips),
                                            getString(R.string.not_btn),
                                            {
                                                importBtn.isEnabled = true
                                                importBtn.isClickable = true
                                            },
                                            getString(R.string.continue_btn),
                                            {
                                                generateWallet(keyJson, passPhrase)
                                            })
                                } else {
                                    /**
                                     * 修改密码时，导入已存在但非当前钱包
                                     */
                                    showTipsDialog(getString(R.string.tips_title),
                                            getString(R.string.change_pwd_import_new_wallet),
                                            getString(R.string.i_see),
                                            {
                                                importBtn.isEnabled = true
                                                importBtn.isClickable = true
                                            })
                                }
                            }


                        }, {
                            progressBar.visibility = View.GONE
                            importBtn.isEnabled = true
                            importBtn.isClickable = true

                            toastErrorMessage(Formatter.formatWalletErrorMsg(this, it))
                        })


                    }, {
                progressBar.visibility = View.GONE
                importBtn.isEnabled = true
                importBtn.isClickable = true

                toastErrorMessage(Formatter.formatWalletErrorMsg(this, it))

            })


        }
        keystoreContentEt.addTextChangedListener(TextChange({ changeImportBtn() }, {}, {}))
        passPhraseEt.addTextChangedListener(TextChange({ changeImportBtn() }, {}, {}))
        agreeCB.setOnClickListener {
            changeImportBtn()
        }
    }


    private fun generateWallet(keyJson: String, passPhrase: String) {

        progressBar.visibility = View.VISIBLE

        supportTokenViewModel.getTokensFromServer(lifecycle) {

            coinList = mutableListOf()
            it.forEach {
                // 通过keyJson创建钱包时，只创建keyJson所属链的钱包、地址
                if (it.isSelected && it.platform == platform)
                    coinList.add(Coin(it))
            }

            createWalletViewModel.generateWallet(
                    getString(R.string.wallet_name),
                    platform,
                    keyJson,
                    passPhrase,
                    true,
                    coinList,
                    { wallet ->
                        progressBar.visibility = View.GONE
                        importBtn.isEnabled = true
                        importBtn.isClickable = true
                        //更新设备信息
                        IntentServiceUpdateDeviceInfo.startActionUpdateAddresses(this)

                        if (null != wallet) {
                            if (this.wallet != null) {
                                firebaseAnalytics?.logEvent(Constants.kAImportWalletSuccess, Bundle())
                                firebaseAnalytics?.logEvent(Constants.kAAddWalletSuccess, Bundle())
                                firebaseAnalytics?.logEvent(Constants.kAImportWalletSuccessByKeystore, Bundle())
                            }
                            val finalWallet = this.wallet
                            if (finalWallet!=null){
                                SecurityHelper.walletCorrectPassword(finalWallet)
                            }
                            NasBottomDialog.Builder(this)
                                    .withIcon(R.drawable.icon_success)
                                    .withContent(if (this.wallet != null) getString(R.string.change_wallet_pwd_success) else getString(R.string.import_wallet_success))
                                    .withConfirmButton(getString(R.string.generate_wallet_success_ok)) { _, dialog ->
                                        dialog.dismiss()
                                        val data = Intent()
                                        data.putExtra(CreateWalletActivity.WALLET_RESULT, wallet)
                                        setResult(Activity.RESULT_OK, data)
                                        finish()
                                    }
                                    .build()
                                    .show()
                        }
                    },
                    {
                        toastErrorMessage(Formatter.formatWalletErrorMsg(this, it))
                    }
            )

        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            REQUEST_CODE_SCAN_QRCODE -> {
                if (resultCode == Activity.RESULT_OK) {
                    keystoreContentEt.setText(data?.getStringExtra(QRScanActivity.SCAN_QRCODE_RESULT_TEXT))
                }
            }
        }

    }


    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.qr_scan_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.actionScanQR -> {
                QRScanActivity.launch(context = this,
                        requestCode = REQUEST_CODE_SCAN_QRCODE,
                        galleryEnabled = false)
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun changeImportBtn() =
            if (keystoreContentEt.length() > 0 && passPhraseEt.length() > 0 && agreeCB.isChecked) {
                importBtn.setBackgroundResource(R.drawable.btn_import_bg)
                importBtn.isClickable = true
            } else {
                importBtn.setBackgroundResource(R.drawable.btn_import_disable_bg)
                importBtn.isClickable = false
            }
}
