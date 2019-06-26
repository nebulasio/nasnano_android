package io.nebulas.wallet.android.module.setting

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.hardware.fingerprint.FingerprintManager
import android.os.Build
import android.os.Bundle
import android.support.annotation.RequiresApi
import android.support.v4.hardware.fingerprint.FingerprintManagerCompat
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.CompoundButton
import io.nebulas.wallet.android.R
import io.nebulas.wallet.android.base.BaseActivity
import io.nebulas.wallet.android.common.Constants
import io.nebulas.wallet.android.common.DataCenter
import io.nebulas.wallet.android.configuration.Configuration
import io.nebulas.wallet.android.dialog.NasBottomDialog
import io.nebulas.wallet.android.extensions.initCollapsingToolbar
import io.nebulas.wallet.android.module.feedback.FeedBackActivity
import io.nebulas.wallet.android.module.me.AboutActivity
import io.nebulas.wallet.android.module.me.ListSelectActivity
import io.nebulas.wallet.android.module.me.model.ListSelectModel
import io.nebulas.wallet.android.module.verification.WalletPasswordVerificationActivity
import kotlinx.android.synthetic.nas_nano.activity_setting.*
import org.jetbrains.anko.startActivity

class SettingActivity : BaseActivity() {
    companion object {
        const val REQUEST_CODE_CHANGE_LANGUAGE = 20001
        const val REQUEST_CODE_CHANGE_CURRENCY = 20002
        const val REQUEST_CODE_PAYMENT_WALLET = 20003
        const val REQUEST_CODE_RECEIVE_WALLET = 2004
        const val REQUEST_CODE_FINGERPRINT_VERIFY = 2005

        fun launch(context: Context) {
            (context as AppCompatActivity).startActivity<SettingActivity>()
        }
    }

    private val checkedListener = object : CompoundButton.OnCheckedChangeListener {
        @RequiresApi(Build.VERSION_CODES.M)
        override fun onCheckedChanged(compoundButton: CompoundButton, isChecked: Boolean) {
            if (isChecked) { //打开指纹验证
                compoundButton.setOnCheckedChangeListener(null)
                compoundButton.isChecked = false
                compoundButton.setOnCheckedChangeListener(this)
                val fingerprintManager = getSystemService(FingerprintManager::class.java)
                val isSupported = fingerprintManager.isHardwareDetected
                if (!isSupported) {
                    toastErrorMessage(R.string.tip_fingerprint_not_support)
                } else {
                    val hasFingerprint = fingerprintManager.hasEnrolledFingerprints()
                    if (!hasFingerprint) {
                        toastErrorMessage(R.string.tip_fingerprint_not_opened)
                    } else {
                        //验证钱包密码及指纹
                        WalletPasswordVerificationActivity.launch(this@SettingActivity, REQUEST_CODE_FINGERPRINT_VERIFY)
                    }
                }
            } else { //关闭指纹验证
                NasBottomDialog.Builder(this@SettingActivity)
                        .withIcon(R.drawable.icon_notice)
                        .withTitle(getString(R.string.tip_title_important))
                        .withContent(getString(R.string.tip_fingerprint_close_warning))
                        .withCanceledOnTouchOutsideEnable(false)
                        .withCancelButton(getString(R.string.cancel_btn)) { _, _ ->
                            compoundButton.setOnCheckedChangeListener(null)
                            compoundButton.isChecked = true
                            compoundButton.setOnCheckedChangeListener(this)
                        }
                        .withConfirmButton(getString(R.string.action_confirm_for_yes)) { _, dialog ->
                            Configuration.disableFingerprint(this@SettingActivity)
                            dialog.dismiss()
                        }
                        .build().show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setting)
        initView()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_CODE_FINGERPRINT_VERIFY -> {
                if (resultCode == Activity.RESULT_OK) {
                    toastSuccessMessage(R.string.tip_fingerprint_setup_success)
                    switchFingerprint.setOnCheckedChangeListener(null)
                    switchFingerprint.isChecked = true
                    switchFingerprint.setOnCheckedChangeListener(checkedListener)
                    Configuration.enableFingerprint(this)
                } else {
                    switchFingerprint.setOnCheckedChangeListener(null)
                    switchFingerprint.isChecked = false
                    switchFingerprint.setOnCheckedChangeListener(checkedListener)
                }
            }
        }
    }

    override fun initView() {
        initCollapsingToolbar(getString(R.string.setting)) { onBackPressed() }
        /**
         * 默认支付钱包设置
         */
        paymentWallet.setOnClickListener {
            DefaultWalletActivity.launch(this, REQUEST_CODE_PAYMENT_WALLET, R.string.payment_wallet)
        }
        /**
         * 默认收币钱包设置
         */
        receivingWallet.setOnClickListener {
            DefaultWalletActivity.launch(this, REQUEST_CODE_RECEIVE_WALLET, R.string.receiving_wallet)
        }
        /**
         * 语言设置
         */
        language.setOnClickListener {
            initLanguageData()
        }
        /**
         * 币种设置
         */
        currencyUnit.setOnClickListener {
            initCurrencyUnit()
        }
        notificationSetting.setOnClickListener {
            NotificationSettingActivity.launch(this)
        }
        /**
         * 关于我们
         */
        aboutUs.setOnClickListener {
            AboutActivity.launch(this)
        }

        feedback.setOnClickListener {
            FeedBackActivity.launch(this)
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            layoutFingerprint.visibility = View.GONE
        } else {
            switchFingerprint.isChecked = Configuration.isFingerprintEnabled(this)
            switchFingerprint.setOnCheckedChangeListener(checkedListener)
        }
    }

    private fun initLanguageData() {
        var list = mutableListOf<ListSelectModel>()
        list.add(ListSelectModel(getString(R.string.language_auto), "auto", false))
        list.add(ListSelectModel(getString(R.string.language_cn), "cn", false))
        list.add(ListSelectModel(getString(R.string.language_en), "en", false))
        list.add(ListSelectModel(getString(R.string.language_kr), "kr", false))

        list.forEach {
            it.selected = Constants.LANGUAGE == it.value
        }

        DataCenter.setData(ListSelectActivity.LIST_ITEMS, list)
        ListSelectActivity.launch(this, REQUEST_CODE_CHANGE_LANGUAGE, getString(R.string.language))
    }

    private fun initCurrencyUnit() {
        var list = mutableListOf<ListSelectModel>()
        list.add(ListSelectModel(getString(R.string.currency_usd), getString(R.string.currency_symbol_name_usd).toLowerCase(), false))
        list.add(ListSelectModel(getString(R.string.currency_cny), getString(R.string.currency_symbol_name_cny).toLowerCase(), false))

        list.forEach {
            it.selected = Constants.CURRENCY_SYMBOL_NAME == it.value
        }

        DataCenter.setData(ListSelectActivity.LIST_ITEMS, list)
        ListSelectActivity.launch(this, REQUEST_CODE_CHANGE_CURRENCY, getString(R.string.me_token_setting))
    }
}