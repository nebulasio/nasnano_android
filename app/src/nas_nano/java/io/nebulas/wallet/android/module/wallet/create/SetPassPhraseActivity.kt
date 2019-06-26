package io.nebulas.wallet.android.module.wallet.create

import android.animation.Animator
import android.animation.ValueAnimator
import android.app.Activity
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.annotation.StringRes
import android.support.v7.app.AppCompatActivity
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import io.nebulas.wallet.android.R
import io.nebulas.wallet.android.base.BaseActivity
import io.nebulas.wallet.android.common.Constants
import io.nebulas.wallet.android.common.DataCenter
import io.nebulas.wallet.android.common.URLConstants
import io.nebulas.wallet.android.common.mainHandler
import io.nebulas.wallet.android.db.DBUtil
import io.nebulas.wallet.android.dialog.NasBottomDialog
import io.nebulas.wallet.android.extensions.removeKeyBoard
import io.nebulas.wallet.android.extensions.showKeyBoard
import io.nebulas.wallet.android.module.balance.model.Coin
import io.nebulas.wallet.android.module.html.HtmlActivity
import io.nebulas.wallet.android.module.token.viewmodel.SupportTokenViewModel
import io.nebulas.wallet.android.module.wallet.create.model.Wallet
import io.nebulas.wallet.android.module.wallet.create.viewmodel.CreateWalletViewModel
import io.nebulas.wallet.android.module.wallet.create.viewmodel.PkPlatformStruct
import io.nebulas.wallet.android.module.wallet.create.viewmodel.WalletViewModel
import io.nebulas.wallet.android.service.IntentServiceUpdateDeviceInfo
import io.nebulas.wallet.android.util.Formatter
import io.nebulas.wallet.android.util.SecurityHelper
import io.nebulas.wallet.android.view.Curtain
import io.nebulas.wallet.android.view.PasswordInputDelegate
import kotlinx.android.synthetic.nas_nano.activity_set_pass_phrase.*
import kotlinx.android.synthetic.nas_nano.app_bar_main.*
import org.jetbrains.anko.contentView
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.startActivityForResult
import org.jetbrains.anko.uiThread
import org.jetbrains.annotations.NotNull
import java.io.Serializable

class SetPassPhraseActivity : BaseActivity() {

    enum class SourceType(private val desc: String,
                          @StringRes val titleResId: Int) : Serializable {
        CREATE_OR_IMPORT("创建/导入钱包", R.string.set_pwd_title),
        REIMPORT("重新导入(修改密码)", R.string.set_pwd_title),
        CHANGE_PASSWORD("通过旧密码修改密码", R.string.change_pwd_title)
    }

    companion object {
        /**
         * 链
         */
        const val PLATFORMS = "platforms"
        /**
         * 明文私钥
         */
        const val PLAIN_PRIVATE_KEY = "plainPrivateKey"

        /**
         * 助记词字符串
         */
        const val MNEMONIC_WORDS = "mnemonicWords"
        /**
         * 助记词路径
         */
        const val MNEMONIC_PATH = "mnemonicPath"

        const val IS_CHANGE_PWD = "isChangePwd"

        const val SOURCE_TYPE = "sourceType"

        const val PASSWORD_TYPE = "passwordType"
        const val FOCUSED_WALLET_INDEX = "focusedWalletIndex"

        const val WALLET = "wallet"

        /**
         * 启动方法
         * @param activity Activity
         * @param requestCode   requestCode
         * @param sourceType SourceType 类型（创建/导入钱包；重新导入(修改密码)；通过旧密码修改密码）
         */
        fun launch(activity: Activity,
                   requestCode: Int,
                   sourceType: SourceType,
                   wallet: Wallet? = null,
                   isComplexPassword: Boolean = false) {
            if (wallet != null) {
                DataCenter.setData(WALLET, wallet)
            }
            activity.startActivityForResult<SetPassPhraseActivity>(requestCode,
                    SOURCE_TYPE to sourceType,
                    PASSWORD_TYPE to isComplexPassword)
        }


        /**
         * 启动SetPassPhraseActivity
         *
         * @param context
         * @param requestCode
         */
        fun launch(@NotNull context: Context, @NotNull requestCode: Int) {
            if (CreateWalletActivity.CREATE_CHECK_ALL_SUPPORT == requestCode) (context as BaseActivity).firebaseAnalytics?.logEvent(Constants.kACreateWalletBtnClick, Bundle())

            (context as AppCompatActivity).startActivityForResult<SetPassPhraseActivity>(requestCode, CreateWalletActivity.FROM_TYPE to requestCode)
        }

        /**
         * 启动SetPassPhraseActivity
         *
         * @param context
         * @param requestCode
         * @param plainPrivateKey
         * @param platform
         * @param isChangePwd
         */
        fun launch(@NotNull context: Context, requestCode: Int, plainPrivateKey: String, platforms: ArrayList<String>, isChangePwd: Boolean) {
            (context as AppCompatActivity).startActivityForResult<SetPassPhraseActivity>(requestCode,
                    PLAIN_PRIVATE_KEY to plainPrivateKey,
                    PLATFORMS to platforms,
                    IS_CHANGE_PWD to isChangePwd)
        }

        /**
         * 启动SetPassPhraseActivity
         *
         * @param context
         * @param requestCode
         * @param mnemonic
         * @param path
         * @param isChangePwd
         */
        fun launch(@NotNull context: Context, requestCode: Int, mnemonic: String, path: String, platforms: ArrayList<String>, isChangePwd: Boolean) {
            (context as AppCompatActivity).startActivityForResult<SetPassPhraseActivity>(requestCode,
                    MNEMONIC_WORDS to mnemonic,
                    MNEMONIC_PATH to path,
                    PLATFORMS to platforms,
                    IS_CHANGE_PWD to isChangePwd)
        }

        private const val STEP_OLD_PASSWORD_VERIFY = -1 //逻辑步骤 -- 旧密码验证
        private const val STEP_NEW_PASSWORD_INPUT = 0   //逻辑步骤 -- 新密码输入
        private const val STEP_NEW_PASSWORD_CONFIRM = 1 //逻辑步骤 -- 新密码确认

    }

    lateinit var coinList: MutableList<Coin>

    lateinit var passPhrase: String

    lateinit var createWalletViewModel: CreateWalletViewModel
    lateinit var supportTokenViewModel: SupportTokenViewModel
    lateinit var walletViewModel: WalletViewModel

    /**
     * 是否是复杂密码
     */
    var curPwdComplex: Boolean = false

    var plainPrivateKey: String? = null
    var mnemonic: String? = null
    var path: String? = null
    var platforms: ArrayList<String>? = null

    /**
     * 简单密码代理组件
     */
    private lateinit var passwordInputDelegate: PasswordInputDelegate
    private var sourceType: SourceType = SourceType.CREATE_OR_IMPORT
    private var step: Int = STEP_NEW_PASSWORD_INPUT
    private var cachedPassword: String = ""
    private var focusedWallet: Wallet? = null
    private var originPassword: String? = null
    private val animDuration = 300L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (intent.hasExtra(SOURCE_TYPE)) {
            sourceType = intent.getSerializableExtra(SOURCE_TYPE) as SourceType
        }
        if (DataCenter.containsData(WALLET)) {
            focusedWallet = DataCenter.getData(WALLET, true) as Wallet
            curPwdComplex = focusedWallet!!.isComplexPwd
        } else {
            curPwdComplex = intent.getBooleanExtra(PASSWORD_TYPE, false)
        }
        if (sourceType == SourceType.CHANGE_PASSWORD) {
            step = STEP_OLD_PASSWORD_VERIFY
        }
        setContentView(R.layout.activity_set_pass_phrase)
        setupViews()
    }

    override fun initView() {}

    private fun setupViews() {
        showBackBtn(true, toolbar)
        titleTV.setText(sourceType.titleResId)

        initSimplePasswordInputDelegate()

        agreeCB.setOnCheckedChangeListener { _, isChecked ->
            if (curPwdComplex) {
                submitBtn.isEnabled = (isChecked && complexPwdET.text.length >= 6)
            } else {
                submitBtn.isEnabled = (isChecked && passwordInputDelegate.getPassword().length == 6)
            }
        }

        plainPrivateKey = intent.getStringExtra(PLAIN_PRIVATE_KEY)
        mnemonic = intent.getStringExtra(MNEMONIC_WORDS)
        path = intent.getStringExtra(MNEMONIC_PATH)
        platforms = intent.getStringArrayListExtra(PLATFORMS)

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
                STEP_OLD_PASSWORD_VERIFY -> verifyOldPassword()
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

        complexPwdET.setOnEditorActionListener { textView, actionId, event ->
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

        if (step == STEP_OLD_PASSWORD_VERIFY) {
            agreeCB.isChecked = true    //修改密码时checkBox不可见，所以直接设置为选中状态
            mainTitleTV.setText(R.string.change_pwd_old_pwd_title)
            subTitleTV.visibility = View.GONE
            useComplexPassPhraseTV.visibility = View.INVISIBLE
            layoutProtocolConfirm.visibility = View.INVISIBLE
            if (!curPwdComplex) {
                showSimplePasswordInput(false)
                submitBtn.visibility = View.GONE
            } else {
                showComplexPasswordInput(false)
                passwordStrengthCheckView.visibility = View.GONE
            }
        }

    }

    private fun stepToInputNewPassword() {
        if (curPwdComplex) {
            complexPwdET.setText("")
        } else {
            passwordInputDelegate.clearPassword()
        }
        step = STEP_NEW_PASSWORD_INPUT
        mainTitleTV.setText(R.string.change_pwd_new_pwd_title)
        subTitleTV.visibility = View.GONE
        useComplexPassPhraseTV.visibility = View.VISIBLE
        submitBtn.visibility = View.VISIBLE
        if (!curPwdComplex) {
            showSimplePasswordInput(false)
        } else {
            showComplexPasswordInput(false)
            passwordStrengthCheckView.visibility = View.VISIBLE
        }
    }

    private fun verifyOldPassword() {
        val inputtedPassword = if (curPwdComplex) {
            complexPwdET.text.toString()
        } else {
            passwordInputDelegate.getPassword()
        }
        progressBar.visibility = View.VISIBLE
        walletViewModel.verifyWalletPassPhrase(
                wallet = focusedWallet!!,
                passPhrase = inputtedPassword,
                onComplete = {
                    SecurityHelper.walletCorrectPassword(focusedWallet!!)
                    progressBar.visibility = View.GONE
                    originPassword = inputtedPassword
                    stepToInputNewPassword()
                },
                onFailed = {
                    if (SecurityHelper.walletWrongPassword(focusedWallet!!)) {
                        progressBar.visibility = View.GONE
                        toastErrorMessage(R.string.tip_password_error_to_lock)
                        mainHandler.postDelayed({
                            contentView?.isEnabled = false
                            finish()
                        }, 1000)
                        return@verifyWalletPassPhrase
                    }
                    progressBar.visibility = View.GONE
                    Curtain.create(this)
                            .withContentRes(R.string.wrong_pwd)
                            .withLevel(Curtain.CurtainLevel.ERROR)
                            .show()
                    if (curPwdComplex) {
                        complexPwdET.setText("")
                        showKeyBoard(complexPwdET)
                    } else {
                        passwordInputDelegate.clearPassword()
                        passwordInputDelegate.requestFocus()
                    }
                })
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

    private fun confirmPassword() {
        submitBtn.isClickable = false

        val confirmedPassword = if (curPwdComplex) {
            complexPwdET.text.toString()
        } else {
            passwordInputDelegate.getPassword()
        }

        if (confirmedPassword == cachedPassword) {
            if (sourceType == SourceType.CHANGE_PASSWORD) {   //通过修改密码
                changePassword(originPassword!!, cachedPassword)
            } else {
                generateWallet(cachedPassword)
            }
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

    private fun changePassword(oldPassword: String, newPassword: String) {
        progressBar.visibility = View.VISIBLE
        walletViewModel.changePassPhrase(
                wallet = focusedWallet!!,
                oldPassPhrase = oldPassword,
                newPassPhrase = newPassword,
                onComplete = {
                    doAsync {
                        focusedWallet!!.isComplexPwd = curPwdComplex
                        DBUtil.appDB.walletDao().insertWallet(focusedWallet!!)
                        uiThread {
                            submitBtn.isClickable = true
                            progressBar.visibility = View.GONE
                            NasBottomDialog.Builder(this@SetPassPhraseActivity)
                                    .withIcon(R.drawable.icon_success)
                                    .withContent(getString(R.string.change_wallet_pwd_success))
                                    .withConfirmButton(getString(R.string.generate_wallet_success_ok)) { _, dialog ->
                                        dialog.dismiss()
                                        setResult(Activity.RESULT_OK)
                                        finish()
                                    }
                                    .build()
                                    .show()
                        }
                    }
                },
                onFailed = {
                    submitBtn.isClickable = true
                    progressBar.visibility = View.GONE
                    Curtain.create(this)
                            .withContent(it)
                            .withLevel(Curtain.CurtainLevel.ERROR)
                            .show()
                })
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
                    if (step == STEP_OLD_PASSWORD_VERIFY) {
                        //如果是修改密码，则密码输入6位时，自动验证密码是否正确
                        verifyOldPassword()
                    }
                }
                if (step != STEP_OLD_PASSWORD_VERIFY) {
                    submitBtn.isEnabled = (passwordInputDelegate.getPassword().length == 6 && agreeCB.isChecked)
                }
            }
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

    /**
     * 生成钱包
     */
    private fun generateWallet(passPhrase: String) {

        if (curPwdComplex) {
            this.firebaseAnalytics?.logEvent(Constants.kACreateWalletUseComplexPwd, Bundle())
        } else {
            this.firebaseAnalytics?.logEvent(Constants.kACreateWalletUseSimplePwd, Bundle())
        }

        progressBar.visibility = View.VISIBLE
        supportTokenViewModel.getTokensFromServer(lifecycle, {
            coinList = mutableListOf()

            if (platforms?.isEmpty() != false) {
                platforms = arrayListOf()
                platforms?.addAll(Constants.PLATFORMS)
            }
            it.forEach {
                // 筛选指定平台的币
                if (it.isSelected && platforms?.contains(it.platform) != false)
                    coinList.add(Coin(it))
            }

            val GA_KEY = if (null != plainPrivateKey && plainPrivateKey!!.isNotEmpty())
                Constants.kAImportWalletSuccessByPK
            else if (null != mnemonic && mnemonic!!.isNotEmpty())
                Constants.kAImportWalletSuccessByMnemonic
            else
                ""

            /**
             * 创建钱包的回调方法
             */
            val onComplete: (wallet: Wallet?) -> Unit = { wallet ->
                progressBar.visibility = View.GONE
                submitBtn.isClickable = true
                //更新设备信息
                IntentServiceUpdateDeviceInfo.startActionUpdateAddresses(this)
                if (null != wallet) {

                    if (!intent.getBooleanExtra(IS_CHANGE_PWD, false) && GA_KEY.isNotEmpty()) {
                        /**
                         * GA
                         */
                        firebaseAnalytics?.logEvent(GA_KEY, Bundle())
                    }

                    var msg: String? = null
                    if ((null != plainPrivateKey && plainPrivateKey!!.isNotEmpty()) || (null != mnemonic && mnemonic!!.isNotEmpty())) {
                        msg = getString(R.string.import_wallet_success)
                        firebaseAnalytics?.logEvent(Constants.kACreateWalletSuccess, Bundle())
                    } else {
                        CreateSuccessActivity.launch(this, intent.getIntExtra(CreateWalletActivity.FROM_TYPE, 0), wallet)
                        //msg = getString(R.string.generate_wallet_success)
                        firebaseAnalytics?.logEvent(Constants.kACreateWalletSuccess, Bundle())

                    }

                    if (intent.getBooleanExtra(IS_CHANGE_PWD, false))
                        msg = getString(R.string.change_wallet_pwd_success)
                    else
                        firebaseAnalytics?.logEvent(Constants.kAAddWalletSuccess, Bundle())

                    if (!TextUtils.isEmpty(msg)) {
                        NasBottomDialog.Builder(this)
                                .withIcon(R.drawable.icon_success)
                                .withTitle(getString(R.string.success_text))
                                .withContent(msg)
                                .withConfirmButton(getString(R.string.generate_wallet_success_ok), { _, dialog ->
                                    dialog.dismiss()
                                    val data = Intent()
                                    data.putExtra(CreateWalletActivity.WALLET_RESULT, wallet)
                                    setResult(Activity.RESULT_OK, data)
                                    finish()
                                })
                                .build()
                                .show()
                    }
                }
            }

            var onFailed: (errMsg: String) -> Unit = {
                Curtain.create(this)
                        .withContent(Formatter.formatWalletErrorMsg(this, it))
                        .withLevel(Curtain.CurtainLevel.ERROR)
                        .show()
                progressBar.visibility = View.GONE
                submitBtn.isClickable = true
            }


            if (null != plainPrivateKey && plainPrivateKey!!.isNotEmpty()) {
                /**
                 * 明文私钥导入
                 */
                val pkPlatformStructs = arrayListOf<PkPlatformStruct>()
                // 如果是明文私钥导入，则platforms一定不为空，并且只有一个item，否则业务逻辑崩了！！！
                if (null != platforms && platforms!!.size > 0) {
                    pkPlatformStructs.add(PkPlatformStruct(plainPrivateKey!!, platforms!![0]))
                }
                createWalletViewModel.generateWalletFromPrivateKey(getString(R.string.wallet_name), pkPlatformStructs, passPhrase, curPwdComplex, coinList, onComplete, onFailed)

            } else if (null != mnemonic && mnemonic!!.isNotEmpty()) {
                /**
                 * 助记词导入
                 */
                createWalletViewModel.generateWalletFromMnemonic(getString(R.string.wallet_name), mnemonic!!, path!!, passPhrase, curPwdComplex, coinList, platforms
                        ?: arrayListOf(), onComplete, onFailed)


            } else {
                /**
                 * 新创建钱包
                 */
                createWalletViewModel.generateWalletFromMnemonic(getString(R.string.wallet_name), "", Constants.NAS_MNEMONIC_PATH, passPhrase, curPwdComplex, coinList, platforms
                        ?: arrayListOf(), onComplete, onFailed)
            }

        })

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            CreateWalletActivity.CREATE_CHECK_FROM_WEB, CreateWalletActivity.CREATE_CHECK_ALL_SUPPORT -> {
                setResult(Activity.RESULT_OK, data)
                finish()

            }
        }
    }
}
