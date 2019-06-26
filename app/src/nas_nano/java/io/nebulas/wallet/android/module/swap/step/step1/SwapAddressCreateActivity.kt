package io.nebulas.wallet.android.module.swap.step.step1

import android.animation.Animator
import android.animation.ValueAnimator
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.annotation.WorkerThread
import android.text.Editable
import android.text.TextWatcher
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import io.nebulas.wallet.android.R
import io.nebulas.wallet.android.base.BaseActivity
import io.nebulas.wallet.android.common.Constants
import io.nebulas.wallet.android.common.URLConstants
import io.nebulas.wallet.android.extensions.removeKeyBoard
import io.nebulas.wallet.android.extensions.showKeyBoard
import io.nebulas.wallet.android.module.detail.fragment.transaction.ErrorHandler
import io.nebulas.wallet.android.module.html.HtmlActivity
import io.nebulas.wallet.android.module.swap.SwapHelper
import io.nebulas.wallet.android.module.token.viewmodel.SupportTokenViewModel
import io.nebulas.wallet.android.module.wallet.create.viewmodel.CreateWalletViewModel
import io.nebulas.wallet.android.module.wallet.create.viewmodel.PkPlatformStruct
import io.nebulas.wallet.android.module.wallet.create.viewmodel.WalletViewModel
import io.nebulas.wallet.android.network.server.HttpManager
import io.nebulas.wallet.android.network.server.api.ApiResponse
import io.nebulas.wallet.android.network.server.model.BindSwapAddressRequest
import io.nebulas.wallet.android.view.Curtain
import io.nebulas.wallet.android.view.PasswordInputDelegate
import kotlinx.android.synthetic.nas_nano.activity_swap_address_create.*
import kotlinx.android.synthetic.nas_nano.app_bar_main.*
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import walletcore.SignedData
import walletcore.Walletcore

class SwapAddressCreateActivity : BaseActivity() {

    companion object {
        private const val STEP_NEW_PASSWORD_INPUT = 0   //逻辑步骤 -- 新密码输入
        private const val STEP_NEW_PASSWORD_CONFIRM = 1 //逻辑步骤 -- 新密码确认

        private const val KEY_NAS_ADDRESS = "key_nas_address"

        fun launch(context: Context, nasAddress: String) {
            context.startActivity(Intent(context, SwapAddressCreateActivity::class.java).apply {
                putExtra(KEY_NAS_ADDRESS, nasAddress)
            })
        }
    }

    private lateinit var passwordInputDelegate: PasswordInputDelegate

    lateinit var createWalletViewModel: CreateWalletViewModel
    lateinit var supportTokenViewModel: SupportTokenViewModel
    lateinit var walletViewModel: WalletViewModel

    private lateinit var nasAddress: String
    private val animDuration = 300L
    private var curPwdComplex: Boolean = false
    private var step: Int = STEP_NEW_PASSWORD_INPUT
    private var cachedPassword: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_swap_address_create)
        nasAddress = intent.getStringExtra(KEY_NAS_ADDRESS)
        setupViews()
    }

    override fun initView() {}

    private fun setupViews() {
        showBackBtn(true, toolbar)
        titleTV.setText(R.string.set_pwd_title)

        initSimplePasswordInputDelegate()

        agreeCB.setOnCheckedChangeListener { _, isChecked ->
            if (curPwdComplex) {
                submitBtn.isEnabled = (isChecked && complexPwdET.text.length >= 6)
            } else {
                submitBtn.isEnabled = (isChecked && passwordInputDelegate.getPassword().length == 6)
            }
        }

        createWalletViewModel = ViewModelProviders.of(this).get(CreateWalletViewModel::class.java)
        supportTokenViewModel = ViewModelProviders.of(this).get(SupportTokenViewModel::class.java)
        walletViewModel = ViewModelProviders.of(this).get(WalletViewModel::class.java)

        useComplexPassPhraseTV.text = if (curPwdComplex) {
            getString(R.string.use_simple_pwd)
        } else {
            getString(R.string.use_complex_pwd)
        }
        useComplexPassPhraseTV.setOnClickListener {
            useComplexPassPhraseTV.isClickable = false
            useComplexPassPhraseTV.postDelayed({
                useComplexPassPhraseTV.isClickable = true
            }, animDuration)
            curPwdComplex = !curPwdComplex

            passwordInputDelegate.clearPassword()
            complexPwdET.setText("")

            if (curPwdComplex) {
                showComplexPasswordInput()
            } else {
                showSimplePasswordInput()
            }
        }

        ibShowOrHidePassword.setOnClickListener {
            if (complexPwdET.transformationMethod is PasswordTransformationMethod) {
                complexPwdET.transformationMethod = HideReturnsTransformationMethod.getInstance()
                complexPwdET.setSelection(complexPwdET.text.length)
                ibShowOrHidePassword.setImageResource(R.mipmap.ic_pwd_to_hide)
            } else {
                complexPwdET.transformationMethod = PasswordTransformationMethod.getInstance()
                complexPwdET.setSelection(complexPwdET.text.length)
                ibShowOrHidePassword.setImageResource(R.mipmap.ic_pwd_to_show)
            }
        }

        privacyTV.setOnClickListener {
            HtmlActivity.launch(this, URLConstants.PRIVACY_URL, getString(R.string.privacy_title))
        }
        serviceProtocolTV.setOnClickListener {
            HtmlActivity.launch(this, URLConstants.TERMS_URL_EN, getString(R.string.terms_service_title))
        }

        submitBtn.setOnClickListener {
            when (step) {
                STEP_NEW_PASSWORD_INPUT -> stepToConfirmPassword()
                STEP_NEW_PASSWORD_CONFIRM -> confirmPassword()
            }
        }

        complexPwdET.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                s?.apply {
                    submitBtn.isEnabled = length >= 6 && agreeCB.isChecked
                    passwordStrengthCheckView.check(s.toString())
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        complexPwdET.imeOptions = if (step == STEP_NEW_PASSWORD_INPUT) {
            EditorInfo.IME_ACTION_NEXT
        } else {
            EditorInfo.IME_ACTION_DONE
        }

        complexPwdET.setOnEditorActionListener { _, actionId, _ ->
            when (actionId) {
                EditorInfo.IME_ACTION_NEXT -> {
                    removeKeyBoard()
                    if (step == STEP_NEW_PASSWORD_INPUT) {
                        stepToConfirmPassword()
                    } else {
                        removeKeyBoard()
                    }
                    true
                }
                EditorInfo.IME_ACTION_DONE -> {
                    removeKeyBoard()
                    true
                }
                else -> false
            }
        }
    }

    private fun initSimplePasswordInputDelegate() {
        passwordInputDelegate = PasswordInputDelegate(this)
        val count = layoutSimplePassword.childCount
        (0 until count).forEach {
            val child = layoutSimplePassword.getChildAt(it)
            if (child is TextView) {
                passwordInputDelegate.addDisplayDelegate(child)
            }
        }
        passwordInputDelegate.callback = object : PasswordInputDelegate.DelegateCallback {
            override fun onDelegateTextChanged(displayDelegate: TextView, value: String) {
                displayDelegate.isSelected = value.isNotEmpty()
                if (passwordInputDelegate.getPassword().length == 6) {
                    //密码输入6位时，自动隐藏键盘
                    removeKeyBoard()
                }
                submitBtn.isEnabled = (passwordInputDelegate.getPassword().length == 6 && agreeCB.isChecked)
            }
        }
    }

    private val showComplexPasswordAnimListener = object : Animator.AnimatorListener {
        fun end() {
            showKeyBoard(complexPwdET)
            layoutSimplePassword.visibility = View.GONE
        }

        override fun onAnimationStart(animation: Animator?) {}
        override fun onAnimationRepeat(animation: Animator?) {}

        override fun onAnimationEnd(animation: Animator?) {
            end()
        }

        override fun onAnimationCancel(animation: Animator?) {
            end()
        }
    }

    private fun showComplexPasswordInput(anim: Boolean = true) {
        useComplexPassPhraseTV.text = getString(R.string.use_simple_pwd)
        layoutComplexPassword.visibility = View.VISIBLE
        passwordStrengthCheckView.visibility = View.VISIBLE
        if (anim) {
            val w = getScreenWidth().toFloat()
            val anim = ValueAnimator.ofFloat(w, 0f)
            anim.duration = animDuration
            anim.addUpdateListener {
                layoutComplexPassword.translationX = 0f - (it.animatedValue as Float)
                layoutSimplePassword.translationX = w - (it.animatedValue as Float)
                passwordStrengthCheckView.alpha = (w - (it.animatedValue as Float)) / w
            }
            anim.addListener(showComplexPasswordAnimListener)
            anim.start()
        } else {
            showComplexPasswordAnimListener.end()
        }
    }

    private val showSimplePasswordAnimListener = object : Animator.AnimatorListener {
        fun end() {
            passwordInputDelegate.requestFocus()
            layoutComplexPassword.visibility = View.GONE
            passwordStrengthCheckView.visibility = View.GONE
        }

        override fun onAnimationStart(animation: Animator?) {}
        override fun onAnimationRepeat(animation: Animator?) {}

        override fun onAnimationEnd(animation: Animator?) {
            end()
        }

        override fun onAnimationCancel(animation: Animator?) {
            end()
        }
    }

    private fun showSimplePasswordInput(anim: Boolean = true) {
        useComplexPassPhraseTV.text = getString(R.string.use_complex_pwd)
        layoutSimplePassword.visibility = View.VISIBLE

        if (anim) {
            val w = getScreenWidth().toFloat()
            val anim = ValueAnimator.ofFloat(0f, w)
            anim.duration = animDuration
            anim.addUpdateListener {
                layoutComplexPassword.translationX = 0f - (it.animatedValue as Float)
                layoutSimplePassword.translationX = w - (it.animatedValue as Float)
                passwordStrengthCheckView.alpha = (w - (it.animatedValue as Float)) / w
            }
            anim.addListener(showSimplePasswordAnimListener)
            anim.start()
        } else {
            showSimplePasswordAnimListener.end()
        }
    }

    private fun stepToConfirmPassword() {
        if (!agreeCB.isChecked) {
            toastErrorMessage(R.string.need_agree_protocol)
            return
        }
        cachedPassword = if (curPwdComplex) {
            complexPwdET.text.toString()
        } else {
            passwordInputDelegate.getPassword()
        }
        if (cachedPassword.length < 6) {
            toastErrorMessage(R.string.pwd_need_upper_six)
            return
        }
        if (curPwdComplex) {
            complexPwdET.setText("")
            complexPwdET.imeOptions = EditorInfo.IME_ACTION_DONE
            complexPwdET.transformationMethod = PasswordTransformationMethod.getInstance()
            ibShowOrHidePassword.setImageResource(R.mipmap.ic_pwd_to_show)
        } else {
            passwordInputDelegate.clearPassword()
        }
        step = STEP_NEW_PASSWORD_CONFIRM
        titleTV.setText(R.string.title_confirm_password)
        mainTitleTV.setText(R.string.reset_pwd_main_title)
        subTitleTV.setText(R.string.reset_pwd_sub_title)
        submitBtn.setText(R.string.confirm_btn)
        passwordStrengthCheckView.visibility = View.GONE
        useComplexPassPhraseTV.visibility = View.INVISIBLE
        layoutProtocolConfirm.visibility = View.INVISIBLE
        if (curPwdComplex) {
            showKeyBoard(complexPwdET)
        } else {
            passwordInputDelegate.requestFocus()
        }
    }

    private fun confirmPassword() {
        submitBtn.isClickable = false

        val confirmedPassword = if (curPwdComplex) {
            complexPwdET.text.toString()
        } else {
            passwordInputDelegate.getPassword()
        }

        if (confirmedPassword == cachedPassword) {
            generateWallet(cachedPassword)
        } else {
            Curtain.create(this)
                    .withContentRes(R.string.pwd_not_match)
                    .withLevel(Curtain.CurtainLevel.ERROR)
                    .show()
            if (curPwdComplex) {
                complexPwdET.setText("")
                showKeyBoard(complexPwdET)
            } else {
                passwordInputDelegate.clearPassword()
                passwordInputDelegate.requestFocus()
            }
            submitBtn.isClickable = true
        }
    }

    /**
     * 生成钱包并进行绑定，具体步骤如下
     * Step 1 - 根据助记词生成私钥
     * Step 2 - 通过私钥生成keyStore和address
     * Step 3 - 调用服务端绑定接口之前的参数进行签名
     * Step 4 - 调用接口，绑定NAS主网钱包和换币钱包
     */
    private fun generateWallet(passPhrase: String) {
        progressBar.visibility = View.VISIBLE
        doAsync(ErrorHandler { _, errorMsg ->
            runOnUiThread {
                toastErrorMessage(errorMsg)
                progressBar.visibility = View.GONE
                submitBtn.isClickable = true
            }
        }.defaultHandler) {
            //** Step 1 - 根据助记词生成私钥
            val response = Walletcore.getPlainPrivateKeyFromMnemonic("", Constants.ETH_MNEMONIC_PATH)
            if (response.errorCode != 0L) {
                //创建钱包失败
                return@doAsync
            }
            //Walletcore.getPlainPrivateKeyFromMnemonic 调用之后返回的response可以拿到助记词、明文私钥（address、keyStore不一定有）
            val words = response.mnemonic

            val pkPlatformItem = PkPlatformStruct(response.plainPrivateKey, Walletcore.ETH)

            //** Step 2 - 通过私钥生成keyStore和address
            val responseForImport = Walletcore.importWalletFromPrivateKey(pkPlatformItem.platform, pkPlatformItem.plainPrivateKey, passPhrase)

            if (responseForImport.errorCode != 0L) {
                //创建钱包失败
                uiThread {
                    bindError(responseForImport.errorMsg)
                }
                return@doAsync
            }
            //Walletcore.importWalletFromPrivateKey 调用之后返回的response可以拿到address、keyStore（助记词、明文私钥不一定有）
            val address = responseForImport.address
            val keystore = responseForImport.keyJson


            val info = SwapHelper.SwapWalletInfo(nasAddress, words, keystore, address, curPwdComplex)

            //** Step 3 - 调用服务端绑定接口之前的参数进行签名
            val signedData = getSignedData(info, passPhrase)
            if (signedData.errorCode != 0L) {
                uiThread {
                    bindError(signedData.errorMsg)
                }
                return@doAsync
            }
            //** Step 4 - 调用接口，绑定NAS主网钱包和换币钱包
            val apiResponse = bindSwapAddress(info, signedData.signature)
            if (apiResponse == null || apiResponse.code ?: -1 != 0) {
                uiThread {
                    bindError(apiResponse?.msg ?: getString(R.string.network_connect_exception))
                }
                return@doAsync
            }

            SwapHelper.saveSwapWalletInfo(this@SwapAddressCreateActivity, info)

            uiThread {
                walletCreateSuccess(info)
            }
        }
    }

    private fun bindError(errorMessage: String) {
        progressBar.visibility = View.GONE
        toastErrorMessage(errorMessage)
        submitBtn.isClickable = true
    }

    private fun walletCreateSuccess(swapWalletInfo: SwapHelper.SwapWalletInfo) {
        progressBar.visibility = View.GONE
        firebaseAnalytics?.logEvent(Constants.Exchange_AddressSuccess_Show,Bundle())
        SwapWalletCreateSuccessActivity.launch(this, swapWalletInfo)
        finish()
    }

    @WorkerThread
    private fun getSignedData(swapWalletInfo: SwapHelper.SwapWalletInfo, password: String): SignedData {
        return Walletcore.signData(Walletcore.ETH, swapWalletInfo.swapWalletKeystore, password, swapWalletInfo.nasWalletAddress)
    }

    @WorkerThread
    private fun bindSwapAddress(swapWalletInfo: SwapHelper.SwapWalletInfo, signature: String): ApiResponse<String>? {
        val body = BindSwapAddressRequest().apply {
            eth_address = swapWalletInfo.swapWalletAddress
            nas_address = swapWalletInfo.nasWalletAddress
            this.signature = signature
        }
        val call = HttpManager.getServerApi().bindSwapAddressWithoutRX(HttpManager.getHeaderMap(), body)
        return call.execute()?.body()
    }
}
