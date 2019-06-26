package io.nebulas.wallet.android.module.wallet.create

import android.app.Activity
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.text.Html
import android.text.method.HideReturnsTransformationMethod
import android.view.View
import io.nebulas.wallet.android.R
import io.nebulas.wallet.android.base.BaseActivity
import io.nebulas.wallet.android.common.DataCenter
import io.nebulas.wallet.android.common.URLConstants
import io.nebulas.wallet.android.module.html.HtmlActivity
import io.nebulas.wallet.android.module.wallet.create.model.Wallet
import io.nebulas.wallet.android.module.wallet.create.viewmodel.CreateWalletViewModel
import io.nebulas.wallet.android.util.Formatter
import io.nebulas.wallet.android.util.SecurityHelper
import io.nebulas.wallet.android.util.TextChange
import io.nebulas.wallet.android.view.HiddenTransformationMethod
import kotlinx.android.synthetic.nas_nano.activity_import_from_plain_priv.*
import kotlinx.android.synthetic.nas_nano.app_bar_main_no_underline.*
import org.jetbrains.anko.startActivityForResult
import org.jetbrains.annotations.NotNull

class ImportFromPlainPrivActivity : BaseActivity() {

    companion object {

        /**
         * 当前导入的keystore文件所属的数字货币类型
         */
        const val PLATFORM = "platform"
        const val WALLET = "wallet"

        /**
         * 启动ImportFromPlainPrivActivity
         *
         * @param context
         * @param requestCode
         * @param wallet
         */
        fun launch(@NotNull context: Context, @NotNull requestCode: Int, platform: String, wallet: Wallet? = null) {
            if (wallet != null) {
                DataCenter.setData(WALLET, wallet)
            }
            (context as AppCompatActivity).startActivityForResult<ImportFromPlainPrivActivity>(requestCode, PLATFORM to platform, ImportWalletActivity.FROM_TYPE to requestCode)
        }
    }

    var text = ""
    private var wallet: Wallet? = null

    var createWalletViewModel: CreateWalletViewModel? = null
    var openEyeDra: Drawable? = null
    var closeEyeDra: Drawable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_import_from_plain_priv)
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

        desTV.text = Html.fromHtml(getString(R.string.import_from_priv_title))

        privacyTV.setOnClickListener {
            HtmlActivity.launch(this, URLConstants.PRIVACY_URL, getString(R.string.privacy_title))
        }
        serviceProtocolTV.setOnClickListener {
            HtmlActivity.launch(this, URLConstants.TERMS_URL_EN, getString(R.string.terms_service_title))
        }

        initDrawable()
        importBtn.setOnClickListener {

            if (plainPrivET.text.length != 64) {
                toastErrorMessage(R.string.wrong_privatekey_content)
                return@setOnClickListener
            }


            /**
             * 校验私钥，是否已存在
             */
            if (null == createWalletViewModel) {
                createWalletViewModel = ViewModelProviders.of(this).get(CreateWalletViewModel::class.java)
            }
            progressBar.visibility = View.VISIBLE

            createWalletViewModel?.getAddressFromPlainPrivateKey(plainPrivET.text.toString(), "", { addresses ->
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

                                })
                    } else {
                        goToSetPassPhrase(plainPrivET.text.toString())
                    }
                } else {
                    if (wallet == null) {

                        /**
                         * 导入的是已存在的钱包
                         */
                        showTipsDialog(getString(R.string.tips_title),
                                getString(R.string.import_exist_wallet_tips),
                                getString(R.string.cancel_import),
                                { },
                                getString(R.string.continue_import),
                                {
                                    goToSetPassPhrase(plainPrivET.text.toString())
                                })
                    } else if (walletId == wallet?.id) {
                        /**
                         * 导入的是已存在的钱包，并且是修改密码
                         */
                        showTipsDialog(getString(R.string.tips_title),
                                getString(R.string.change_pwd_import_exist_wallet_tips),
                                getString(R.string.not_btn),
                                { },
                                getString(R.string.continue_btn),
                                {
                                    goToSetPassPhrase(plainPrivET.text.toString())
                                })
                    } else {
                        /**
                         * 修改密码时，导入已存在但非当前钱包
                         */
                        showTipsDialog(getString(R.string.tips_title),
                                getString(R.string.change_pwd_import_new_wallet),
                                getString(R.string.i_see),
                                {

                                })

                    }
                }

            }, {
                progressBar.visibility = View.GONE

                toastErrorMessage(Formatter.formatWalletErrorMsg(this, it))
            })

        }

        plainPrivET.addTextChangedListener(TextChange({ showImportBtn() }, {}, {}))
        plainPrivET.transformationMethod = HiddenTransformationMethod

        showPlainPrivET.setOnClickListener {
            if (plainPrivET.transformationMethod == HideReturnsTransformationMethod.getInstance()) {
                plainPrivET.transformationMethod = HiddenTransformationMethod
                showPlainPrivET.setCompoundDrawables(openEyeDra, null, null, null)
                showPlainPrivET.setText(R.string.show_plain_priv_key)
            } else {
                plainPrivET.transformationMethod = HideReturnsTransformationMethod.getInstance()
                showPlainPrivET.setCompoundDrawables(closeEyeDra, null, null, null)
                showPlainPrivET.setText(R.string.hide_plain_priv_key)
            }
            plainPrivET.setSelection(plainPrivET.text.length)
        }
        agreeCB.setOnClickListener {
            showImportBtn()
        }
    }

    private fun initDrawable() {
        openEyeDra = resources.getDrawable(R.mipmap.open_eye_yellow)
        openEyeDra?.bounds = Rect(0, 0, openEyeDra!!.minimumWidth, openEyeDra!!.minimumHeight)
        closeEyeDra = resources.getDrawable(R.mipmap.close_eye_yellow)
        closeEyeDra?.bounds = Rect(0, 0, closeEyeDra!!.minimumWidth, closeEyeDra!!.minimumHeight)
    }

    private fun showImportBtn() = if (this.plainPrivET.text.isNotEmpty() && agreeCB.isChecked) {
        importBtn.setBackgroundResource(R.drawable.btn_import_bg)
        importBtn.isClickable = true
    } else {
        importBtn.setBackgroundResource(R.drawable.btn_import_disable_bg)
        importBtn.isClickable = false
    }

    private fun goToSetPassPhrase(plainPrivateKey: String) {
        SetPassPhraseActivity.launch(context = this,
                requestCode = CreateWalletActivity.IMPORT_WALLET_REQUEST_CODE,
                plainPrivateKey = plainPrivateKey,
                platforms = arrayListOf(intent.getStringExtra(PLATFORM)),
                isChangePwd = wallet != null)
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            CreateWalletActivity.IMPORT_WALLET_REQUEST_CODE -> {
                if (resultCode == Activity.RESULT_OK) {
                    val finalWallet = wallet
                    if (finalWallet != null) {
                        SecurityHelper.walletCorrectPassword(finalWallet)
                    }
                    setResult(Activity.RESULT_OK)
                    finish()
                }
            }
        }


    }

}
