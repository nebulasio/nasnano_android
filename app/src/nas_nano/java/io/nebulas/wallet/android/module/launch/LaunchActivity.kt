package io.nebulas.wallet.android.module.launch

import android.app.Activity
import android.arch.lifecycle.Lifecycle
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.content.Intent
import android.hardware.fingerprint.FingerprintManager
import android.os.*
import android.support.annotation.RequiresApi
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import com.young.adaptive.AdaptiveAssistant
import io.nebulas.wallet.android.R
import io.nebulas.wallet.android.base.BaseActivity
import io.nebulas.wallet.android.common.Constants
import io.nebulas.wallet.android.common.DataCenter
import io.nebulas.wallet.android.common.mainHandler
import io.nebulas.wallet.android.configuration.Configuration
import io.nebulas.wallet.android.extensions.*
import io.nebulas.wallet.android.invoke.InvokeError
import io.nebulas.wallet.android.invoke.InvokeHelper
import io.nebulas.wallet.android.module.main.MainActivity
import io.nebulas.wallet.android.module.verification.LaunchWalletPasswordVerifyActivity
import io.nebulas.wallet.android.module.vote.VoteActivity
import io.nebulas.wallet.android.module.wallet.create.viewmodel.WalletViewModel
import io.nebulas.wallet.android.push.message.KEY_PUSH_MESSAGE
import io.nebulas.wallet.android.push.message.Message
import io.nebulas.wallet.android.push.message.PushManager
import io.nebulas.wallet.android.service.IntentServiceUpdateDeviceInfo
import kotlinx.android.synthetic.nas_nano.activity_launch.*
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.startActivity
import org.jetbrains.anko.uiThread
import org.jetbrains.annotations.NotNull
import kotlin.Boolean
import kotlin.CharSequence
import kotlin.Exception
import kotlin.Int
import kotlin.apply
import kotlin.to

/**
 * Created by Heinoc on 2018/1/31.
 */

class LaunchActivity : BaseActivity(), FingerprintVerifyDialog.ActionListener {

    companion object {
        private const val GOTO_TRANSFER = "gotoTransfer"

        private const val REQUEST_CODE_PASSWORD_VERIFICATION = 1011

        /**
         * 启动LaunchActivity
         *
         * @param context
         * @param gotoTransfer 是否要跳转到转账界面
         */
        fun launch(@NotNull context: Context, gotoTransfer: Boolean = false) {
            context.startActivity<LaunchActivity>(GOTO_TRANSFER to gotoTransfer)
        }

    }


    private lateinit var viewModel: WalletViewModel
    private var hasVerified = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AdaptiveAssistant.setContentView(this, R.layout.activity_launch)
        intent?.extras?.keySet()?.forEach {
            logD("PUSH : INTENT : $it -> ${intent.extras[it]}")
        }
        PushManager.interceptPushMessage(intent)
        initView()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_PASSWORD_VERIFICATION) {
            if (resultCode == Activity.RESULT_OK) {
                hasVerified = true
                realDispatch()
            }
        }
    }

    override fun onRestart() {
        super.onRestart()
        if (!hasVerified && Configuration.isFingerprintEnabled(this)) {
            mainHandler.postDelayed({
                dispatch()
            }, 600)
        }
    }


    override fun initView() {
        showBackBtn(true)

        tv_action_password_verify.setOnClickListener {
            verifyWalletPassword()
        }

        layout_fingerprint.setOnClickListener {
            dispatch()
        }

//        启动页，取消滑动返回手势功能
        swipeBackLayout.setEnableGesture(false)

        viewModel = ViewModelProviders.of(this).get(WalletViewModel::class.java)

        firebaseAnalytics?.logEvent(Constants.kAUserSignIn, Bundle())

        prepareData()
    }

    private fun prepareData() {
        doAsync {
            val wallets = viewModel.getWallets()
            if (!wallets.isEmpty()) {

                //将查询到的wallet赋值给DataCenter缓存
                DataCenter.wallets.clear()
                wallets.forEach {
                    if (it.id >= 0) {
                        DataCenter.wallets.add(it)
                    }
                }
                DataCenter.wallets.sortWith(kotlin.Comparator { w1, w2 ->
                    return@Comparator (w1.id - w2.id).toInt()
                })

                DataCenter.addresses.clear()
                DataCenter.addresses.addAll(viewModel.getAddresses())

                DataCenter.coins.clear()
                DataCenter.coins.addAll(viewModel.getCoins())

                //更新设备信息
                IntentServiceUpdateDeviceInfo.startActionUpdateToken(this@LaunchActivity, PushManager.getCurrentToken())
                IntentServiceUpdateDeviceInfo.startActionUpdateAddresses(this@LaunchActivity)
            }
            uiThread {
                startAnim()
            }
        }
    }

    private fun startAnim() {
        val isFingerprintEnabled = Configuration.isFingerprintEnabled(this)

        if (isFingerprintEnabled) {
            tv_nebulas_io.withValueAnimator(1f, 0f, 2000L) {
                alpha = it
            }.start()
        }

        val originScale = 1f
        val targetScale = if (isFingerprintEnabled) 0.2f else 0.3f

        val originTop = 0f
        iv_logo.pivotX = AdaptiveAssistant.adaptiveWidth(this, 384).toFloat()
        val targetTop = if (isFingerprintEnabled) {
            iv_logo.pivotY = 0f
            AdaptiveAssistant.adaptiveHeight(this, 225).toFloat()
        } else {
            val lp = iv_logo.layoutParams as ViewGroup.MarginLayoutParams
            lp.topMargin = AdaptiveAssistant.adaptiveWidth(this, 155)
            iv_logo.layoutParams = lp
            iv_logo.pivotY = AdaptiveAssistant.adaptiveWidth(this, 384).toFloat()
            0f
        }
        animatorTogether(
                iv_logo.withValueAnimator(0f, 1f, 1200L) {
                    alpha = it
                },
                iv_logo.withValueAnimator(originScale, targetScale, 1200L) {
                    scaleX = it
                    scaleY = it
                },
                iv_logo.withValueAnimator(originTop, targetTop, 1200L) {
                    translationY = it
                }
        ).withInterpolator(AccelerateDecelerateInterpolator()).start()


        val originNanoScale = 1f
        val targetNanoScale = if (isFingerprintEnabled) 0.7f else 0.8f

        val originNanoTranslationY = 0f
        val targetNanoTranslationY = if (isFingerprintEnabled) {
            0 - AdaptiveAssistant.adaptiveWidth(this, (768 * 0.8f).toInt()).toFloat() + AdaptiveAssistant.adaptiveHeight(this, 225)
        } else {
            0 - AdaptiveAssistant.adaptiveWidth(this, (768 * 0.35f).toInt()).toFloat()
        }

        animatorTogether(
                iv_logo_nano.withValueAnimator(0f, 1f, 1000L) {
                    alpha = it
                },
                iv_logo_nano.withValueAnimator(originNanoScale, targetNanoScale, 1000L) {
                    scaleX = it
                    scaleY = it
                },
                iv_logo_nano.withValueAnimator(originNanoTranslationY, targetNanoTranslationY, 1000L) {
                    translationY = it
                })
                .delay(1200L)
                .withInterpolator(AccelerateDecelerateInterpolator())
                .doOnEnd {
                    dispatch()
                }
                .start()
    }

    override fun onCancelClicked() {}

    override fun onPasswordClicked() {
        verifyWalletPassword()
    }

    private fun verifyWalletPassword() {
        LaunchWalletPasswordVerifyActivity.launch(this, REQUEST_CODE_PASSWORD_VERIFICATION)
    }

    private var fingerprintVerifyDialog: FingerprintVerifyDialog? = null
    private var cancellationSignal: CancellationSignal? = null
    private fun dispatch() {
        if (Configuration.isFingerprintEnabled(this)) {
            if (layout_fingerprint.visibility != View.VISIBLE) {
                layout_fingerprint.visibility = View.VISIBLE
                layout_fingerprint.withValueAnimator(0f, 1f, 600L) { alpha = it }
                        .withInterpolator(AccelerateDecelerateInterpolator())
                        .doOnEnd { showFingerprintVerifyDialog() }
                        .start()
                tv_action_password_verify.visibility = View.VISIBLE
                tv_action_password_verify.withValueAnimator(0f, 1f, 600L) { alpha = it }
                        .withInterpolator(AccelerateDecelerateInterpolator())
                        .start()
            } else {
                showFingerprintVerifyDialog()
            }

        } else {
            realDispatch()
        }
    }

    private fun showFingerprintVerifyDialog() {
        if (fingerprintVerifyDialog != null && fingerprintVerifyDialog?.isShowing == true) {
            return
        } else {
            fingerprintVerifyDialog?.reset()
        }
        if (fingerprintVerifyDialog == null) {
            fingerprintVerifyDialog = FingerprintVerifyDialog(this, FingerprintVerifyDialog.Type.UNLOCK, this)
        }
        cancellationSignal = CancellationSignal()
        fingerprintVerifyDialog?.setOnDismissListener {
            cancellationSignal?.apply {
                if (!isCanceled) {
                    try {
                        cancel()
                    } catch (e: Exception) {
                    }
                }
            }
        }
        if (lifecycle.currentState == Lifecycle.State.RESUMED) {
            fingerprintVerifyDialog?.show()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                fingerprintVerify()
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun fingerprintVerify() {
        getSystemService(FingerprintManager::class.java).authenticate(null, cancellationSignal, 0, object : FingerprintManager.AuthenticationCallback() {
            override fun onAuthenticationError(errMsgId: Int, errString: CharSequence?) {
                super.onAuthenticationError(errMsgId, errString)
                //FingerprintManager.FINGERPRINT_ERROR_LOCKOUT //失败次数过多
                //FingerprintManager.FINGERPRINT_ERROR_CANCELED //取消
                logD("onAuthenticationError : $errMsgId - $errString")
                var errorMessage = errString
                if (errMsgId == FingerprintManager.FINGERPRINT_ERROR_HW_UNAVAILABLE) {
                    errorMessage = getString(R.string.tip_fingerprint_not_opened)
                }
                if (errMsgId != FingerprintManager.FINGERPRINT_ERROR_CANCELED) {
                    errorMessage?.apply {
                        toastErrorMessage(this.toString())
                    }
                }
                fingerprintVerifyDialog?.dismiss()
            }

            override fun onAuthenticationSucceeded(result: FingerprintManager.AuthenticationResult?) {
                super.onAuthenticationSucceeded(result)
                layout_fingerprint.isClickable = false
                fingerprintVerifyDialog?.dismiss()
                hasVerified = true
                realDispatch()
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                val vibrator: Vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                if (vibrator.hasVibrator()) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
                    } else {
                        vibrator.vibrate(100)
                    }
                }
                fingerprintVerifyDialog?.fingerVerifyError()
            }
        }, mainHandler)
    }

    private fun realDispatch() {
        if (intent.hasExtra("category")) {
            if ((intent.flags and Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY) != 0) {    //多任务（任务列表）进入
                gotoHome()
                return
            }
            val extras = intent.extras
            intent.replaceExtras(Bundle())
            InvokeHelper.setupPackage(extras)
            if (DataCenter.wallets.isEmpty()) {
                InvokeHelper.error(this, InvokeError.errorNoWallet)
                return
            }
            InvokeHelper.dispatch(this, extras)
        } else {
            gotoHome()
        }
    }

    private fun gotoHome() {
        val message:Message? = intent.getSerializableExtra(KEY_PUSH_MESSAGE) as? Message
        MainActivity.launch(
                context = this,
                gotoTransfer = intent.getBooleanExtra(GOTO_TRANSFER, false),
                pushMessage= message)
//        VoteActivity.launch(this)
        finish()
    }

}
