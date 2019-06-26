package io.nebulas.wallet.android.dialog

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.view.Gravity
import android.view.View
import android.view.ViewTreeObserver
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import io.nebulas.wallet.android.R
import io.nebulas.wallet.android.base.BaseActivity
import io.nebulas.wallet.android.common.*
import io.nebulas.wallet.android.extensions.getScreenHeight
import io.nebulas.wallet.android.extensions.removeKeyBoard
import io.nebulas.wallet.android.extensions.showKeyBoard
import io.nebulas.wallet.android.extensions.startValueAnimator
import io.nebulas.wallet.android.view.PasswordInputDelegate
import io.nebulas.wallet.android.view.research.CurtainResearch
import kotlinx.android.synthetic.main.dialog_verify_password.*

class VerifyPasswordDialog(val activity: Activity,
                           val title: String,
                           @PasswordType val passwordType: Int = PASSWORD_TYPE_SIMPLE,
                           var onNext: ((String) -> Unit)? = null,
                           var onDismiss:() -> Unit = {},
                           var onCancel: () -> Unit = {}) : Dialog(activity, R.style.AppDialog) {

    private lateinit var passwordInputDelegate: PasswordInputDelegate

    fun toastSuccessMessage(msg: String) {
        CurtainResearch.create(this)
                .withLevel(CurtainResearch.CurtainLevel.SUCCESS)
                .withContent(msg)
                .show()
    }

    fun toastErrorMessage(msg: String) {
        CurtainResearch.create(this)
                .withLevel(CurtainResearch.CurtainLevel.ERROR)
                .withContent(msg)
                .show()
    }

    fun reset() {
        clearPassword()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dialog_verify_password)

        tvTitle.text = title
        setOnCancelListener { onCancel.invoke() }
        ibClose.setOnClickListener {
            cancel()
        }

        if (passwordType == PASSWORD_TYPE_SIMPLE) {
            initSimplePasswordInputDelegate()
            showSimplePasswordLayout()
        } else {
            initComplexPasswordComponent()
            showComplexPasswordLayout()
        }
    }

    override fun show() {
        if (context != null && context is Activity) {
            val activity = context as Activity
            if (activity.isFinishing || activity.isDestroyed) {
                return
            }
        }
        if (activity is BaseActivity) {
            activity.firebaseAnalytics?.logEvent(Constants.KAInputPassword, Bundle())
        }
        super.show()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {//5.0 全透明实现
            window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_LAYOUT_STABLE)
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.statusBarColor = Color.TRANSPARENT
        } else {//4.4 全透明状态栏
            window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        }

        val attr = window.attributes
        attr.gravity = Gravity.BOTTOM
        attr.width = WindowManager.LayoutParams.MATCH_PARENT
        attr.height = WindowManager.LayoutParams.MATCH_PARENT
        window.decorView.setPadding(0, 0, 0, 0)
        window.attributes = attr

        if (passwordType == PASSWORD_TYPE_SIMPLE) {
            addViewObserver()
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)
            mainHandler.postDelayed({
                passwordInputDelegate.requestFocus()
            }, 100L)
        } else {
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
            activity.showKeyBoard(complexPwdET, 0L)
            complexPwdET.requestFocus()
        }

    }

    private fun showSimplePasswordLayout() {
        layoutSimplePassword.visibility = View.VISIBLE
        layoutComplexPassword.visibility = View.GONE
        hideConfirmButton()
    }

    private fun showComplexPasswordLayout() {
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
        showLoadingLayout()
        onNext?.invoke(pwd)
    }

    private fun clearPassword() {
        hideLoadingLayout()
        if (passwordType == PASSWORD_TYPE_COMPLEX) {
            showConfirmButton()
            complexPwdET.setText("")
        } else {
            passwordInputDelegate.clearPassword()
        }
    }


    /**
     * 隐藏键盘
     */
    private fun removeComplexETKeyBoard() {
        val imm = activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(complexPwdET.windowToken, 0) //强制隐藏键盘
        complexPwdET.clearFocus()
    }

    override fun dismiss() {
        directDismiss()
        onDismiss()
    }

    fun directDismiss() {
        super.dismiss()
        removeComplexETKeyBoard()
        window.decorView.viewTreeObserver.removeOnGlobalLayoutListener(viewTreeListener)
        reset()
        activity.removeKeyBoard()

    }

    override fun onBackPressed() {

        reset()

        super.onBackPressed()
    }

    private val viewTreeListener = ViewTreeObserver.OnGlobalLayoutListener {
        val decorView = window.decorView
        val rect = Rect()
        decorView.getWindowVisibleDisplayFrame(rect)
        val screenHeight = activity.getScreenHeight()
        val heightDifference = screenHeight - rect.bottom//计算软键盘占有的高度  = 屏幕高度 - 视图可见高度
        if (preHeightDifference == heightDifference) {
            return@OnGlobalLayoutListener
        }
        val start = layoutInputArea.translationY
        val end = -heightDifference
        doAnimator(start, end.toFloat())

        preHeightDifference = heightDifference
    }
    private var preHeightDifference = -1
    private fun addViewObserver() {
        window.decorView.viewTreeObserver.addOnGlobalLayoutListener(viewTreeListener)
    }

    private fun doAnimator(start: Float, end: Float) {
        layoutInputArea.startValueAnimator(start, end, 200L) {
            translationY = it
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
}