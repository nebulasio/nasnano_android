package io.nebulas.wallet.android.module.verification

import android.app.Activity
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.hardware.fingerprint.FingerprintManager
import android.os.*
import android.support.annotation.RequiresApi
import android.text.Editable
import android.text.TextWatcher
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import io.nebulas.wallet.android.R
import io.nebulas.wallet.android.base.BaseActivity
import io.nebulas.wallet.android.common.DataCenter
import io.nebulas.wallet.android.common.mainHandler
import io.nebulas.wallet.android.dialog.NasBottomListDialog
import io.nebulas.wallet.android.extensions.logD
import io.nebulas.wallet.android.extensions.showKeyBoard
import io.nebulas.wallet.android.module.launch.FingerprintVerifyDialog
import io.nebulas.wallet.android.module.wallet.create.CreateWalletActivity
import io.nebulas.wallet.android.module.wallet.create.model.Address
import io.nebulas.wallet.android.module.wallet.create.model.Wallet
import io.nebulas.wallet.android.module.wallet.create.viewmodel.WalletViewModel
import io.nebulas.wallet.android.util.SecurityHelper
import io.nebulas.wallet.android.util.Util
import io.nebulas.wallet.android.view.PasswordInputDelegate
import kotlinx.android.synthetic.nas_nano.activity_wallet_password_verification.*
import kotlinx.android.synthetic.nas_nano.app_bar_main.*

@RequiresApi(Build.VERSION_CODES.M)
class WalletPasswordVerificationActivity : BaseActivity(), FingerprintVerifyDialog.ActionListener {

    override fun initView() {}

    companion object {
        fun launch(activity: Activity, requestCode: Int = -1) {
            activity.startActivityForResult(Intent(activity, WalletPasswordVerificationActivity::class.java), requestCode)
        }
    }

    private lateinit var passwordInputDelegate: PasswordInputDelegate
    private lateinit var walletViewModel: WalletViewModel
    private lateinit var walletWrappers: MutableList<WalletWrapper>
    private var selectedWalletWrapper: WalletWrapper? = null
    private var selectWalletDialog: NasBottomListDialog<Wallet, WalletWrapper>? = null
    private var fingerprintVerifyDialog: FingerprintVerifyDialog? = null
    private var cancellationSignal: CancellationSignal? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wallet_password_verification)
        walletViewModel = ViewModelProviders.of(this).get(WalletViewModel::class.java)
        showBackBtn(true, toolbar)
        titleTV.text = getString(R.string.title_setup_fingerprint_verification)
    }

    override fun onResume() {
        super.onResume()
        if (DataCenter.wallets.isEmpty()) {
            layout_no_wallet.visibility = View.VISIBLE
            layout_verify.visibility = View.GONE
            tv_add_wallet.setOnClickListener {
                CreateWalletActivity.launch(this, 8888, true)
            }
        } else {
            layout_no_wallet.visibility = View.GONE
            layout_verify.visibility = View.VISIBLE
            initPasswordVerify()
        }
    }

    private fun initPasswordVerify() {
        if (selectedWalletWrapper != null) {
            return
        }
        val defaultWallet = getDefaultPaymentWallet()
        if (defaultWallet == null) {
            setResult(Activity.RESULT_CANCELED)
            finish()
            return
        }

        initSimplePasswordInputDelegate()
        initComplexPasswordComponent()

        if (DataCenter.wallets.size == 1) {
            layout_change_wallet.visibility = View.GONE
        } else {
            layout_change_wallet.visibility = View.VISIBLE
            layout_change_wallet.setOnClickListener {
                changeWallet()
            }
        }
        walletWrappers = MutableList(DataCenter.wallets.size) {
            val wallet = DataCenter.wallets[it]
            val wrapper = WalletWrapper(wallet, it)
            if (wallet.id == defaultWallet.id) {
                selectedWalletWrapper = wrapper
            }
            wrapper
        }
        updateWalletInfoView()
    }

    private fun getDefaultPaymentWallet(): Wallet? {
        if (DataCenter.wallets.size == 0) {
            return null
        }
        val defaultPaymentWalletId = Util.getDefaultPaymentWallet(this)
        if (defaultPaymentWalletId < 0) {
            return DataCenter.wallets[0]
        }
        for (wallet in DataCenter.wallets) {
            if (wallet.id == defaultPaymentWalletId) {
                return wallet
            }
        }
        return null
    }

    private fun updateWalletInfoView() {
        val selectedWalletWrapper = this.selectedWalletWrapper ?: return
        var address: Address? = null
        val selectedWallet = selectedWalletWrapper.wallet
        for (adr in DataCenter.addresses) {
            if (adr.walletId == selectedWallet.id) {
                address = adr
                break
            }
        }
        if (address == null) {
            setResult(Activity.RESULT_CANCELED)
            finish()
            return
        }
        tv_wallet_name.text = selectedWallet.walletName
        tv_wallet_address.text = address.address
        if (selectedWallet.isComplexPwd) {
            showComplexPasswordLayout()
        } else {
            showSimplePasswordLayout()
        }
    }

    private fun showSimplePasswordLayout() {
        passwordInputDelegate.requestFocus()
        layoutSimplePassword.visibility = View.VISIBLE
        layoutComplexPassword.visibility = View.GONE
        hideConfirmButton()
    }

    private fun showComplexPasswordLayout() {
        passwordInputDelegate.clearPassword()
        showKeyBoard(complexPwdET)
        layoutComplexPassword.visibility = View.VISIBLE
        layoutSimplePassword.visibility = View.GONE
        showConfirmButton()
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
                    //密码输入6位时，自动开始校验密码
                    verifyPassword(passwordInputDelegate.getPassword())
                }
            }
        }
    }

    private fun initComplexPasswordComponent() {
        tvConfirm.isEnabled = false
        complexPwdET.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                s?.apply {
                    tvConfirm.isEnabled = length >= 6
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

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

        tvConfirm.setOnClickListener {
            hideConfirmButton()
            verifyPassword(complexPwdET.text.toString())
        }
    }

    private fun verifyPassword(pwd: String) {
        val selectedWalletWrapper = this.selectedWalletWrapper ?: return
        if (SecurityHelper.isWalletLocked(selectedWalletWrapper.wallet)) {
            walletLocked()
            return
        }
        showLoadingLayout()
        walletViewModel.verifyWalletPassPhrase(selectedWalletWrapper.wallet, pwd, { isSuccess ->
            hideLoadingLayout()
            if (isSuccess) {
                SecurityHelper.walletCorrectPassword(selectedWalletWrapper.wallet)
                //密码验证成功，开始验证指纹
                    fingerprintVerify()
            } else {
                wrongPassword()
            }
        }, {
            hideLoadingLayout()
            wrongPassword()
        })
    }

    private fun walletLocked() {
        toastErrorMessage(R.string.tip_password_has_locked)
        clearPassword()
    }

    private fun wrongPassword() {
        val wrapper = selectedWalletWrapper ?: return
        if (SecurityHelper.walletWrongPassword(wrapper.wallet)) {
            toastErrorMessage(R.string.tip_password_error_to_lock)
        } else {
            toastErrorMessage(R.string.tip_password_error_with_retry)
        }
        clearPassword()
    }

    private fun clearPassword() {
        val selectedWalletWrapper = this.selectedWalletWrapper ?: return
        if (selectedWalletWrapper.wallet.isComplexPwd) {
            complexPwdET.setText("")
            showKeyBoard(complexPwdET)
            showConfirmButton()
        } else {
            passwordInputDelegate.clearPassword()
            passwordInputDelegate.requestFocus()
        }
    }

    /**
     * 显示确认按钮 -- 由于设置Visibility属性会引起重新layout，出现闪屏。
     * So，通过控制其alpha值达到同样的目的并避免重新layout
     */
    private fun showConfirmButton() {
        tvConfirm.alpha = 1f
    }

    /**
     * 隐藏确认按钮
     * @see showConfirmButton
     */
    private fun hideConfirmButton() {
        tvConfirm.alpha = 0f
    }

    /**
     * 显示LoadingLayout -- 由于设置Visibility属性会引起重新layout，出现闪屏。
     * So，通过在上层增加遮盖View，并控制其背景颜色达到同样的目的并避免重新layout
     */
    private fun showLoadingLayout() {
        layoutLoadingCover.setBackgroundColor(Color.TRANSPARENT)
    }

    /**
     * 隐藏LoadingLayout
     * @see showLoadingLayout
     */
    private fun hideLoadingLayout() {
        layoutLoadingCover.setBackgroundColor(Color.WHITE)
    }

    private fun changeWallet() {
        if (selectWalletDialog == null) {
            selectWalletDialog = NasBottomListDialog(
                    context = this,
                    title = getString(R.string.select_wallet_title),
                    dataSource = walletWrappers,
                    initSelectedItem = selectedWalletWrapper)
            selectWalletDialog?.onItemSelectedListener = object : NasBottomListDialog.OnItemSelectedListener<Wallet, WalletWrapper> {
                override fun onItemSelected(itemWrapper: WalletWrapper) {
                    selectedWalletWrapper = itemWrapper
                    updateWalletInfoView()
                }
            }
        }

        selectWalletDialog?.showWithSelectedItem(selectedWalletWrapper)
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun fingerprintVerify() {
        if (fingerprintVerifyDialog == null) {
            fingerprintVerifyDialog = FingerprintVerifyDialog(this, FingerprintVerifyDialog.Type.OPEN, this)
        }
        cancellationSignal = CancellationSignal()
        fingerprintVerifyDialog?.show()
        getSystemService(FingerprintManager::class.java).authenticate(null, cancellationSignal, 0, object : FingerprintManager.AuthenticationCallback() {
            override fun onAuthenticationError(errMsgId: Int, errString: CharSequence?) {
                super.onAuthenticationError(errMsgId, errString)
                //FingerprintManager.FINGERPRINT_ERROR_LOCKOUT //失败次数过多
                //FingerprintManager.FINGERPRINT_ERROR_CANCELED //取消
                logD("onAuthenticationError : $errMsgId - $errString")
                fingerprintVerifyDialog?.dismiss()
                if (errMsgId != FingerprintManager.FINGERPRINT_ERROR_CANCELED) {
                    val msg = errString?.toString() ?: "指纹验证出现错误"
                    toastErrorMessage(msg)
                    clearPassword()
                }
            }

            override fun onAuthenticationSucceeded(result: FingerprintManager.AuthenticationResult?) {
                super.onAuthenticationSucceeded(result)
                logD("onAuthenticationSucceeded")
                fingerprintVerifyDialog?.dismiss()
                fingerprintVerifySucceed()
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                logD("onAuthenticationFailed")
                val vibrator: Vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                if (vibrator.hasVibrator()) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
                    } else {
                        vibrator.vibrate(100)
                    }
                }
            }
        }, mainHandler)
    }

    override fun onCancelClicked() {
        fingerprintVerifyFailed()
    }

    override fun onPasswordClicked() {}

    private fun fingerprintVerifyFailed() {
        cancellationSignal?.apply {
            if (!isCanceled) {
                cancel()
            }
        }
        setResult(Activity.RESULT_CANCELED)
        finish()
    }

    private fun fingerprintVerifySucceed() {
        cancellationSignal?.apply {
            if (!isCanceled) {
                cancel()
            }
        }
        setResult(Activity.RESULT_OK)
        finish()
    }

    override fun finish() {
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)
        super.finish()
    }

    inner class WalletWrapper(val wallet: Wallet, val index: Int) : NasBottomListDialog.ItemWrapper<Wallet> {
        override fun getDisplayText(): String {
            val defaultPaymentWalletId = Util.getDefaultPaymentWallet(this@WalletPasswordVerificationActivity)
            var name = wallet.walletName
            if (defaultPaymentWalletId >= 0 && wallet.id == defaultPaymentWalletId) {
                name = name.plus(getString(R.string.confirm_transfer_default))
            }
            return name
        }

        override fun getOriginObject(): Wallet = wallet
        override fun isShow(): Boolean = false
    }
}
