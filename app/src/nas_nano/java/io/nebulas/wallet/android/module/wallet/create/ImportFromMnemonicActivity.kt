package io.nebulas.wallet.android.module.wallet.create

import android.app.Activity
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.text.Editable
import android.text.TextWatcher
import android.text.method.HideReturnsTransformationMethod
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import io.nebulas.wallet.android.R
import io.nebulas.wallet.android.base.BaseActivity
import io.nebulas.wallet.android.common.Constants
import io.nebulas.wallet.android.common.DataCenter
import io.nebulas.wallet.android.common.URLConstants
import io.nebulas.wallet.android.extensions.removeKeyBoard
import io.nebulas.wallet.android.module.html.HtmlActivity
import io.nebulas.wallet.android.module.wallet.create.model.Wallet
import io.nebulas.wallet.android.module.wallet.create.viewmodel.CreateWalletViewModel
import io.nebulas.wallet.android.util.Formatter
import io.nebulas.wallet.android.util.SecurityHelper
import io.nebulas.wallet.android.view.Curtain
import io.nebulas.wallet.android.view.JustShowFirstTransformationMethod
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.functions.Consumer
import kotlinx.android.synthetic.nas_nano.activity_import_from_mnemonic.*
import kotlinx.android.synthetic.nas_nano.app_bar_import_wallet.*
import org.jetbrains.anko.findOptional
import org.jetbrains.anko.startActivityForResult
import org.jetbrains.annotations.NotNull
import java.util.concurrent.TimeUnit
import kotlin.math.min


class ImportFromMnemonicActivity : BaseActivity() {
    companion object {
        const val IS_CHANGE_PWD = "isChangePwd"
        const val WALLET = "wallet"
        /**
         * 启动ImportFromMnemonicActivity
         *
         * @param context
         * @param requestCode
         * @param wallet
         */
        fun launch(@NotNull context: Context, @NotNull requestCode: Int, wallet: Wallet? = null) {
            if (wallet != null) {
                DataCenter.setData(WALLET, wallet)
            }
            (context as AppCompatActivity).startActivityForResult<ImportFromMnemonicActivity>(requestCode, ImportWalletActivity.FROM_TYPE to requestCode)
        }

    }

    private var wallet: Wallet? = null
    var mnemonicEditTexts = mutableListOf<AutoCompleteTextView>()

    private lateinit var createWalletViewModel: CreateWalletViewModel
    private lateinit var arrayAdapter: ArrayAdapter<String>

    private var currentFocusAutoCompleteTextView: AutoCompleteTextView? = null

    private val focusChangedListener = View.OnFocusChangeListener { v, focus ->
        if (v is AutoCompleteTextView) {
            if (focus) {
                v.transformationMethod = HideReturnsTransformationMethod.getInstance()
                v.apply {
                    val color = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        resources.getColor(R.color.color_323232, null)
                    } else {
                        resources.getColor(R.color.color_323232)
                    }
                    setTextColor(color)
                    hint = ""
                    if (this != currentFocusAutoCompleteTextView) {
                        Single.timer(50, TimeUnit.MILLISECONDS)
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe(Consumer {
                                    selectAll()
                                })
                    }
                }
                if (currentFocusAutoCompleteTextView != v) {
                    currentFocusAutoCompleteTextView = v
                }
            } else {
                val color = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    resources.getColor(R.color.color_666666, null)
                } else {
                    resources.getColor(R.color.color_666666)
                }
                v.hint = v.tag as String
                v.setTextColor(color)
                v.transformationMethod = JustShowFirstTransformationMethod
            }
        }
    }
    private val keyListener = View.OnKeyListener { v, keyCode, event ->
        if (v is AutoCompleteTextView) {
            if (v.length() == 0 && keyCode == KeyEvent.KEYCODE_DEL && event.action == KeyEvent.ACTION_DOWN) {
                val preView = findOptional<AutoCompleteTextView>(v.nextFocusLeftId)
                preView?.apply {
                    requestFocus()
                    selectAll()
                }
            }
        }
        return@OnKeyListener false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        createWalletViewModel = ViewModelProviders.of(this).get(CreateWalletViewModel::class.java)
        setContentView(R.layout.activity_import_from_mnemonic)
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

        mnemonicEditTexts.add(mnemonicET1)
        mnemonicEditTexts.add(mnemonicET2)
        mnemonicEditTexts.add(mnemonicET3)
        mnemonicEditTexts.add(mnemonicET4)
        mnemonicEditTexts.add(mnemonicET5)
        mnemonicEditTexts.add(mnemonicET6)
        mnemonicEditTexts.add(mnemonicET7)
        mnemonicEditTexts.add(mnemonicET8)
        mnemonicEditTexts.add(mnemonicET9)
        mnemonicEditTexts.add(mnemonicET10)
        mnemonicEditTexts.add(mnemonicET11)
        mnemonicEditTexts.add(mnemonicET12)
        mnemonicEditTexts.forEach {
            it.transformationMethod = JustShowFirstTransformationMethod
            it.onFocusChangeListener = focusChangedListener
            it.setOnKeyListener(keyListener)
        }
        arrayAdapter = ArrayAdapter(this, R.layout.mnemonic_text_layout)
        createWalletViewModel.loadAllMemorizeWords(this) {
            arrayAdapter.addAll(it)
            mnemonicEditTexts.forEach {
                it.setOnItemClickListener { parent, view, position, id ->
                    findOptional<View>(it.nextFocusForwardId)?.requestFocus()
                    if (it.imeOptions == EditorInfo.IME_ACTION_DONE) {
                        it.clearFocus()
                        removeKeyBoard()
                    }
                }
                it.setAdapter(arrayAdapter)
            }
        }

        privacyTV.setOnClickListener {
            HtmlActivity.launch(this, URLConstants.PRIVACY_URL, getString(R.string.privacy_title))
        }
        serviceProtocolTV.setOnClickListener {
            HtmlActivity.launch(this, URLConstants.TERMS_URL_EN, getString(R.string.terms_service_title))
        }

        agreeCB.setOnCheckedChangeListener { buttonView, isChecked ->
            importBtn.isEnabled = isChecked
        }

        importBtn.setOnClickListener {

            var sb = StringBuffer()
            mnemonicEditTexts.forEach {
                if (it.text.isEmpty()) {
//                    centerToast(R.string.need_all_mnemonic)
                    Curtain.create(this)
                            .withContentRes(R.string.need_all_mnemonic)
                            .withLevel(Curtain.CurtainLevel.ERROR)
                            .show()
                    return@setOnClickListener
                }
                sb.append(" ")
                sb.append(it.text.toString().trim())
            }
            if (sb.isNotEmpty())
                sb.deleteCharAt(0)

            if (!agreeCB.isChecked) {
//                centerToast(R.string.need_agree_protocol)
                Curtain.create(this)
                        .withContentRes(R.string.need_agree_protocol)
                        .withLevel(Curtain.CurtainLevel.ERROR)
                        .show()
                return@setOnClickListener
            }

            /**
             * 校验私钥，是否已存在
             */

            progressBar.visibility = View.VISIBLE

            createWalletViewModel.getPlainPrivateKeyFromMnemonic(sb.toString(),
                    Constants.NAS_MNEMONIC_PATH, { privateKey ->

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

                                    })
                        } else {
                            goToSetPassPhrase(sb.toString(), Constants.NAS_MNEMONIC_PATH)
                        }
                    } else {
                        if (wallet == null) {

                            /**
                             * 导入钱包时（非修改密码），导入的是已存在的钱包
                             */
                            showTipsDialog(getString(R.string.tips_title),
                                    getString(R.string.import_exist_wallet_tips),
                                    getString(R.string.cancel_import),
                                    { },
                                    getString(R.string.continue_import),
                                    {
                                        goToSetPassPhrase(sb.toString(), Constants.NAS_MNEMONIC_PATH)
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
                                        goToSetPassPhrase(sb.toString(), Constants.NAS_MNEMONIC_PATH)
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


            }, {
                progressBar.visibility = View.GONE

                toastErrorMessage(Formatter.formatWalletErrorMsg(this, it))

            })


        }

        initEditTextView()
    }

    private fun initEditTextView() {

        mnemonicEditTexts.forEach {
            it.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(p0: Editable?) {
                    val value = p0.toString()
                    if (value.trim().indexOf(" ") >= 0) {
                        val arr = value.trim().split(" ")
                        (0 until min(arr.size, mnemonicEditTexts.size)).forEach {
                            mnemonicEditTexts[it].setText(arr[it])
                        }
                        it.clearFocus()
                        removeKeyBoard()
                        return
                    }
                    if (value.endsWith(" ")) {
                        it.setText(value.replace(" ", ""))
                        it.setSelection(value.length - 1)
                        it.clearFocus()

                        val next = findOptional<AutoCompleteTextView>(it.nextFocusForwardId)
                        next?.apply {
                            requestFocus()
                        }
                        if (next == null) {
                            removeKeyBoard()
                        }

                    }

                }

                override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                }

                override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                }
            })
        }

    }

    private fun goToSetPassPhrase(mnemonic: String, path: String) {
        val list = ArrayList<String>().apply {
            Constants.PLATFORMS
        }
        SetPassPhraseActivity.launch(context = this,
                requestCode = CreateWalletActivity.IMPORT_WALLET_REQUEST_CODE,
                mnemonic = mnemonic,
                path = path,
                platforms = list,
                isChangePwd = wallet != null)
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            CreateWalletActivity.IMPORT_WALLET_REQUEST_CODE -> {
                val finalWallet = wallet
                if (finalWallet != null) {
                    SecurityHelper.walletCorrectPassword(finalWallet)
                }
                if (resultCode == Activity.RESULT_OK) {
                    setResult(Activity.RESULT_OK)
                    finish()
                }
            }
        }


    }


}
