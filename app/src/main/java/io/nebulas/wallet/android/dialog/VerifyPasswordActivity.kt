package io.nebulas.wallet.android.dialog

import android.app.Activity
import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.support.annotation.WorkerThread
import android.text.Editable
import android.text.TextWatcher
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import io.nebulas.wallet.android.R
import io.nebulas.wallet.android.base.BaseActivity
import io.nebulas.wallet.android.common.PASSWORD_TYPE_COMPLEX
import io.nebulas.wallet.android.common.PASSWORD_TYPE_SIMPLE
import io.nebulas.wallet.android.common.PasswordType
import io.nebulas.wallet.android.extensions.*
import io.nebulas.wallet.android.view.Curtain
import io.nebulas.wallet.android.view.PasswordInputDelegate
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.dialog_verify_password.*
import java.util.*

@Deprecated("Use #VerifyPasswordDialog instead")
class VerifyPasswordActivity : BaseActivity() {

    companion object {
        const val REQUEST_CODE: Int = 10011
        private const val PARAM_PASSWORD_TYPE = "password_type"

        fun show(activity: Activity, @PasswordType passwordType: Int) {
            val intent = Intent(activity, VerifyPasswordActivity::class.java)
            intent.putExtra(PARAM_PASSWORD_TYPE, passwordType)
            activity.startActivityForResult(intent, REQUEST_CODE)
        }
    }

    private val mainHandler = Handler(Looper.getMainLooper())
    private lateinit var passwordInputDelegate: PasswordInputDelegate

    @PasswordType
    private var passwordType: Int = PASSWORD_TYPE_SIMPLE

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dialog_verify_password)
        window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        val attr = window.attributes
        attr.gravity = Gravity.BOTTOM
        attr.width = WindowManager.LayoutParams.MATCH_PARENT
        attr.height = WindowManager.LayoutParams.MATCH_PARENT
        window.decorView.setPadding(0, 0, 0, 0)
        window.attributes = attr
        passwordType = intent.getIntExtra(PARAM_PASSWORD_TYPE, PASSWORD_TYPE_SIMPLE)

        ibClose.setOnClickListener {
            setResult(Activity.RESULT_CANCELED)
            finish()
        }

        if (passwordType == PASSWORD_TYPE_SIMPLE) {
            addViewObserver()
            initSimplePasswordInputDelegate()
            showSimplePasswordLayout()
        } else {
            initComplexPasswordComponent()
            showComplexPasswordLayout()
        }
    }

    override fun autoRemoveKeyboardOnDispatchTouchEvent(): Boolean = false

    private fun showSimplePasswordLayout() {
        layoutSimplePassword.visibility = View.VISIBLE
        layoutComplexPassword.visibility = View.GONE
        tvConfirm.visibility = View.INVISIBLE
        layoutLoading.visibility = View.INVISIBLE
    }

    private fun showComplexPasswordLayout() {
        layoutComplexPassword.visibility = View.VISIBLE
        layoutSimplePassword.visibility = View.GONE
        tvConfirm.visibility = View.VISIBLE
        layoutLoading.visibility = View.INVISIBLE
    }


    override fun initView() {}

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
            tvConfirm.withValueAnimator(1f, 0f) {
                scaleX = it
            }.doOnEnd {
                tvConfirm.visibility = View.GONE
                layoutLoading.visibility = View.VISIBLE
                layoutLoading.withValueAnimator(0f, 1f) {
                    scaleX = it
                }.doOnEnd {
                    verifyPassword(complexPwdET.text.toString())
                }.start()
            }.start()
        }
        showKeyBoard(complexPwdET)
    }

    var d: Disposable? = null
    private fun verifyPassword(pwd: String) {
        layoutLoading.visibility = View.VISIBLE
        d = Single.create<Boolean> {
            it.onSuccess(verify(pwd))
        }.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ it ->
                    if (it) {
                        mainHandler.postDelayed({
                            layoutLoading.visibility = View.INVISIBLE
                            setResult(Activity.RESULT_OK)
                            finish()
                        }, 500)
                    } else {
                        layoutLoading.visibility = View.INVISIBLE
                        Curtain.create(this)
                                .withLevel(Curtain.CurtainLevel.ERROR)
                                .withContentRes(R.string.wrong_pwd)
                                .show()
                        if (passwordType == PASSWORD_TYPE_COMPLEX) {
                            tvConfirm.visibility = View.VISIBLE
                            complexPwdET.setText("")
                            tvConfirm.startValueAnimator(0f, 1f) {
                                scaleX = it
                            }
                        } else {
                            passwordInputDelegate.clearPassword()
                        }
                    }

                }, {
                    println(it)
                })
    }

    override fun onDestroy() {
        super.onDestroy()
        d?.apply {
            if (!isDisposed) {
                dispose()
            }
        }
    }

    val r = Random()
    @WorkerThread
    private fun verify(pwd: String): Boolean {
        var result = false
        Thread.sleep(1500)
        result = r.nextBoolean()
        if (result) {
            return true
        }
        return false
    }

    class PasswordErrorException : Throwable()

    private var preHeightDifference = -1
    private fun addViewObserver() {
        val decorView = window.decorView
        decorView.viewTreeObserver.addOnGlobalLayoutListener {
            val rect = Rect()
            decorView.getWindowVisibleDisplayFrame(rect)
            val screenHeight = getScreenHeight()
            val heightDifference = screenHeight - rect.bottom//计算软键盘占有的高度  = 屏幕高度 - 视图可见高度
            if (preHeightDifference == heightDifference) {
                return@addOnGlobalLayoutListener
            }

            val start = layoutInputArea.translationY
            val end = -heightDifference
            doAnimator(start, end.toFloat())

            preHeightDifference = heightDifference
        }
    }

    private fun doAnimator(start: Float, end: Float) {
        layoutInputArea.startValueAnimator(start, end, 200L) {
            translationY = it
        }
    }

//    private fun getScreenHeight(): Int {
//        val manager = this.windowManager
//        val outMetrics = DisplayMetrics()
//        manager.defaultDisplay.getMetrics(outMetrics)
//        val height = outMetrics.heightPixels
//        return height
//    }
//
//    fun showKeyBoard(editText: EditText) {
//        editText.requestFocus()
//        val im: InputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
//        im.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT)
//    }
}