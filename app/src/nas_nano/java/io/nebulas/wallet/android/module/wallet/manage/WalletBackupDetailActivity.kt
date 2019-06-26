package io.nebulas.wallet.android.module.wallet.manage

import android.app.Activity
import android.arch.lifecycle.ViewModelProviders
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.text.method.ScrollingMovementMethod
import android.view.*
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import io.nebulas.wallet.android.R
import io.nebulas.wallet.android.base.BaseActivity
import io.nebulas.wallet.android.common.*
import io.nebulas.wallet.android.dialog.NasBottomDialog
import io.nebulas.wallet.android.dialog.VerifyPasswordDialog
import io.nebulas.wallet.android.image.ImageUtil
import io.nebulas.wallet.android.module.wallet.create.model.Wallet
import io.nebulas.wallet.android.module.wallet.create.viewmodel.WalletViewModel
import io.nebulas.wallet.android.module.wallet.manage.WalletBackupActivity.Companion.REQUEST_CODE_BACKUP_DETAIL
import io.nebulas.wallet.android.util.*
import kotlinx.android.synthetic.nas_nano.activity_wallet_backup_detail.*
import kotlinx.android.synthetic.nas_nano.app_bar_main.*
import kotlinx.android.synthetic.nas_nano.backup_keystore.*
import kotlinx.android.synthetic.nas_nano.backup_mnemonic_words.*
import kotlinx.android.synthetic.nas_nano.backup_private_key.*
import kotlinx.android.synthetic.nas_nano.dialog_wallet_passphrase.*
import org.jetbrains.anko.dip
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.startActivityForResult
import org.jetbrains.anko.uiThread
import org.jetbrains.annotations.NotNull
import java.io.File


class WalletBackupDetailActivity : BaseActivity() {

    companion object {
        /**
         * 当前备份类型，值在strings.xml中配置
         * R.string.wallet_backup_mnemonic
         * R.string.wallet_backup_keystore
         * R.string.wallet_backup_plain_priv
         */
        const val BACKUP_TYPE = "backupType"

        /**
         * 当前需要备份的钱包
         */
        const val WALLET = "wallet"

        /**
         * 当前需要备份的钱包里的币类型
         */
        const val ADDRESS_PLATFORM = "addressPlatform"

        /**
         * 当前需要备份的钱包里的币类型
         */
        const val WALLET_PASSPHRASE = "walletPassphrase"

        /**
         * 启动WalletBackupDetailActivity
         *
         * @param context
         * @param requestCode
         * @param backupType
         * @param wallet
         * @param addressPlatform
         * @param walletPassPhrase
         */
        fun launch(@NotNull context: Context,
                   @NotNull requestCode: Int,
                   @NotNull backupType: String,
                   @NotNull wallet: Wallet,
                   @NotNull addressPlatform: String,
                   @NotNull walletPassPhrase: String) {
            (context as AppCompatActivity).startActivityForResult<WalletBackupDetailActivity>(requestCode,
                    BACKUP_TYPE to backupType,
                    WALLET to wallet,
                    ADDRESS_PLATFORM to addressPlatform,
                    WALLET_PASSPHRASE to walletPassPhrase)
        }

    }

    lateinit var backupType: String

    lateinit var wallet: Wallet
    lateinit var addressPlatform: String
    lateinit var walletPassphrase: String

    var showBackupContentFlag = false

    var walletViewModel: WalletViewModel? = null
    var openEyeDra: Drawable? = null
    var closeEyeDra: Drawable? = null
    var isFirst = true
    private var verifyPWDDialog: VerifyPasswordDialog? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wallet_backup_detail)
    }

    override fun screenshotEnabled(): Boolean {
        return false
    }

    override fun initView() {

        backupType = intent.getStringExtra(BACKUP_TYPE)
        wallet = intent.getSerializableExtra(WALLET) as Wallet
        addressPlatform = intent.getStringExtra(ADDRESS_PLATFORM)
        walletPassphrase = intent.getStringExtra(WALLET_PASSPHRASE)

        showBackBtn(true, toolbar)
        titleTV.text = getString(R.string.wallet_backup) + backupType

        contentSwitchBtn.text = getString(R.string.wallet_backup_detail_show) + backupType

        /**
         * edittext 不可编辑
         */
//        contentET.keyListener = null
        when (backupType) {
            getString(R.string.wallet_backup_mnemonic) -> {
                initMnemonicWords()
            }
            getString(R.string.wallet_backup_keystore) -> {
                initKeyStore()

            }
            getString(R.string.wallet_backup_plain_priv) -> {
                initPrivateKey()

            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        if (backupType == getString(R.string.wallet_backup_keystore)) {
            menuInflater.inflate(R.menu.menu_show_qr_code, menu)
        }
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        val finalItem = item ?: return false
        when (finalItem.itemId) {
            R.id.actionShowQrCode -> {
                showQrCode()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private var qrCodeImageView: ImageView? = null
    private fun showQrCode() {
        val content = contentET.text.toString()

        doAsync {
            val w = dip(200)
            val qrCodeFilePath = FileTools.getDiskCacheDir(
                    this@WalletBackupDetailActivity,
                    Constants.KEYSTORE_QRCODE_CACHE_DIR).absolutePath + File.separator + "qr_" + wallet.walletName + "_keystore_${System.currentTimeMillis()}" + ".jpg"
            QRCodeUtil.createQRImage(content, w, w, null, qrCodeFilePath, true)
            val padding6dp = dip(6)
            uiThread {
                val showingBg = getCornerDrawable(dip(6).toFloat(), Color.WHITE, dip(2), resources.getColor(R.color.color_E8E8E8))
                if (qrCodeImageView == null) {
                    qrCodeImageView = ImageView(this@WalletBackupDetailActivity)
                    qrCodeImageView!!.background = showingBg
                    qrCodeImageView!!.setPadding(padding6dp, padding6dp, padding6dp, padding6dp)
                    qrCodeImageView!!.layoutParams = ViewGroup.LayoutParams(w - padding6dp, w - padding6dp)
                }
                val dialog = NasBottomDialog.Builder(this@WalletBackupDetailActivity)
                        .withIcon(R.drawable.icon_notice)
                        .withTitle(getString(R.string.text_qr_code))
                        .withContent(getString(R.string.wallet_backup_detail_safe_tips_k_qr))
                        .withCustomView(qrCodeImageView)
                        .withConfirmButton(getString(R.string.text_close), { _, dialog ->
                            dialog.dismiss()
                        })
                        .build()
                dialog.setOnDismissListener {
                    //关闭对话框的同时，删除缓存的二维码图片
                    try {
                        val file = File(qrCodeFilePath)
                        if (file.exists()) {
                            file.delete()
                        }
                    } catch (e: Exception) {
                    }
                }
                dialog.show()
                ImageUtil.load(this@WalletBackupDetailActivity, qrCodeImageView!!, qrCodeFilePath)
            }
        }
    }

    private fun initMnemonicWords() {
        subDesTV.text = getString(R.string.wallet_backup_detail_safe_tips_m)
        desTV.setText(R.string.wallet_backup_detail_des_mnemonic)
        mnemonicWordsContainer.visibility = View.VISIBLE
        mnemonicLayout.visibility = View.VISIBLE
        doSomethingBtn.isEnabled = true
        doSomethingBtn.isClickable = true
//                doSomethingBtn.setBackgroundResource(R.drawable.custom_button_bg)
        doSomethingBtn.setText(R.string.wallet_backup_detail_mnemonic_next)
        layoutShowOrHide.setOnClickListener {
            showMnemonicWords()
            if (showBackupContentFlag) {
                showDialog()
            }
        }
        doSomethingBtn.setOnClickListener {
            MnemonicBackupCheckActivity.launch(this, REQUEST_CODE_BACKUP_DETAIL, wallet)
        }
        firebaseAnalytics?.logEvent(Constants.kABackupMnemonicClick, Bundle())
    }

    private fun showMnemonicWords() {
        showBackupContentFlag = !showBackupContentFlag
        if (showBackupContentFlag) {
            safeTipsLayout.visibility = View.GONE
            contentET.visibility = View.INVISIBLE
            if ("kr" == Util.getCurLanguage())
                contentSwitchBtn.text = backupType + getString(R.string.wallet_backup_detail_hide)
            else
                contentSwitchBtn.text = getString(R.string.wallet_backup_detail_hide) + backupType
            ivShowingStatus.setImageResource(R.drawable.ic_eye_close_ff8f00)
        } else {
            safeTipsLayout.visibility = View.VISIBLE
            contentET.visibility = View.VISIBLE
            if ("kr" == Util.getCurLanguage())
                contentSwitchBtn.text = backupType + getString(R.string.wallet_backup_detail_show)
            else
                contentSwitchBtn.text = getString(R.string.wallet_backup_detail_show) + backupType
            ivShowingStatus.setImageResource(R.drawable.ic_eye_open_ff8f00)
        }

    }

    private fun initKeyStore() {

        subDesTV.text = getString(R.string.wallet_backup_detail_safe_tips_k_s)
        val temp = DataCenter.coins.filter {
            it.walletId == wallet.id && it.type == 1
        }
        val symbol = if (temp.isNotEmpty())
            temp[0].symbol
        else
            addressPlatform

        desTV.text = String.format(getString(R.string.wallet_backup_detail_des_keystore), symbol)
        keystoreLayout.visibility = View.VISIBLE
        keystoreShowOrHide.text = getString(R.string.wallet_backup_detail_show) + backupType
        contentET.movementMethod = ScrollingMovementMethod.getInstance()

        contentET.setOnLongClickListener {
            (getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).primaryClip = ClipData.newPlainText("keystore", contentET.text.toString())
            showTipsDialog(getString(R.string.tips_title), getString(R.string.warning_keystore_copied), getString(R.string.i_see), {})
            true
        }
        keystoreShowOrHide.setOnClickListener {
            showKeystore()
            if (showBackupContentFlag) {
                showDialog()
            }
        }
        copyKeystore.setOnClickListener {
            (getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).primaryClip = ClipData.newPlainText("keystore", contentET.text.toString())
            showTipsDialog(getString(R.string.tips_title), getString(R.string.warning_keystore_copied), getString(R.string.i_see), {})

        }
        firebaseAnalytics?.logEvent(Constants.kABackupKeystoreClick, Bundle())
    }

    private fun showKeystore() {
        showBackupContentFlag = !showBackupContentFlag
        if (showBackupContentFlag) {
            safeTipsLayout.visibility = View.GONE
            keystoreShowOrHide.text = getString(R.string.wallet_backup_detail_hide) + backupType
        } else {
            safeTipsLayout.visibility = View.VISIBLE
            keystoreShowOrHide.text = getString(R.string.wallet_backup_detail_show) + backupType
        }
    }

    private fun initPrivateKey() {
        subDesTV.text = getString(R.string.wallet_backup_detail_safe_tips_k_p)
        desTV.setText(R.string.wallet_backup_detail_des_plain_priv)
        privateKeyShowOrHide.text = getString(R.string.wallet_backup_detail_show) + backupType
        privateKeyLayout.visibility = View.VISIBLE
        ivShowingStatus.setImageResource(R.drawable.ic_eye_open_ff8f00)
        initDrawable()
        contentET.setOnLongClickListener {
            (getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).primaryClip = ClipData.newPlainText("privateKey", contentET.text.toString())
            showTipsDialog(getString(R.string.tips_title), getString(R.string.warning_private_key_copied), getString(R.string.i_see), {})
            true
        }
        privateKeyShowOrHide.setOnClickListener {
            showPrivateKey()
            if (showBackupContentFlag) {
                showDialog()
            }
        }
        firebaseAnalytics?.logEvent(Constants.kABackupPrivateKeyClick, Bundle())

    }

    private fun showPrivateKey() {
        showBackupContentFlag = !showBackupContentFlag
        if (showBackupContentFlag) {
            safeTipsLayout.visibility = View.GONE
            privateKeyShowOrHide.text = getActionHideText()
            ivShowingStatus.setImageResource(R.drawable.ic_eye_close_ff8f00)
            privateKeyShowOrHide.setCompoundDrawables(closeEyeDra, null, null, null)
        } else {
            safeTipsLayout.visibility = View.VISIBLE
            privateKeyShowOrHide.text = getActionShowText()
            ivShowingStatus.setImageResource(R.drawable.ic_eye_open_ff8f00)
            privateKeyShowOrHide.setCompoundDrawables(openEyeDra, null, null, null)
        }
    }

    private fun getActionShowText():String{
        return when (backupType) {
            getString(R.string.wallet_backup_mnemonic) -> {
                getString(R.string.action_show_words)
            }
            getString(R.string.wallet_backup_keystore) -> {
                getString(R.string.action_show_keystore)
            }
            getString(R.string.wallet_backup_plain_priv) -> {
                getString(R.string.action_show_private_key)
            }
            else -> ""
        }
    }

    private fun getActionHideText():String{
        return when (backupType) {
            getString(R.string.wallet_backup_mnemonic) -> {
                getString(R.string.action_hide_words)
            }
            getString(R.string.wallet_backup_keystore) -> {
                getString(R.string.action_hide_keystore)
            }
            getString(R.string.wallet_backup_plain_priv) -> {
                getString(R.string.action_hide_private_key)
            }
            else -> ""
        }
    }

    private fun initDrawable() {
        openEyeDra = resources.getDrawable(R.drawable.ic_eye_open_ff8f00)
        openEyeDra!!.bounds = Rect(0, 0, openEyeDra!!.minimumWidth, openEyeDra!!.minimumHeight)
        closeEyeDra = resources.getDrawable(R.drawable.ic_eye_close_ff8f00)
        closeEyeDra!!.bounds = Rect(0, 0, closeEyeDra!!.minimumWidth, closeEyeDra!!.minimumHeight)
    }

    override fun onResume() {
        super.onResume()
        if (null == walletViewModel)
            walletViewModel = ViewModelProviders.of(this).get(WalletViewModel::class.java)
        when (backupType) {
            getString(R.string.wallet_backup_mnemonic) -> {
                if (mnemonicWordsContainer.childCount == 0)
                    loadMnemonicWordsView()
            }
            getString(R.string.wallet_backup_keystore) -> {
                run breakPoint@{
                    if (wallet.id < 0) {
                        val address = DataCenter.swapAddress
                        contentET.text = address?.getKeyStore()
                    } else {
                        DataCenter.addresses.forEach {
                            if (it.walletId == wallet.id && it.platform == addressPlatform) {
                                contentET.text = it.getKeyStore()
                                return@breakPoint
                            }
                        }
                    }
                }
            }
            getString(R.string.wallet_backup_plain_priv) -> {

                run breakPoint@{
                    if (wallet.id < 0) {
                        val address = DataCenter.swapAddress
                        walletViewModel!!.getPlainPrivateKey(address ?: return, walletPassphrase, {
                            contentET.text = it
                        }, {
                            toastErrorMessage(Formatter.formatWalletErrorMsg(this, it))
                        })
                    } else {
                        DataCenter.addresses.forEach {
                            if (it.walletId == wallet.id && it.platform == addressPlatform) {
                                walletViewModel!!.getPlainPrivateKey(it, walletPassphrase, {
                                    contentET.text = it
                                }, {
                                    toastErrorMessage(Formatter.formatWalletErrorMsg(this, it))
                                })
                                return@breakPoint
                            }
                        }
                    }
                }


            }
        }


    }

    private fun loadMnemonicWordsView() {
        val mnemonicWords = wallet.getPlainMnemonic().split(" ")

        for (lines in 0 until mnemonicWords.size step 3) {
            val linearlayout = LinearLayout(this)
            linearlayout.dividerDrawable = resources.getDrawable(R.drawable.divider_9dp_for_linear_layout)
            linearlayout.showDividers = LinearLayout.SHOW_DIVIDER_MIDDLE or LinearLayout.SHOW_DIVIDER_BEGINNING or LinearLayout.SHOW_DIVIDER_END
            val linearlayoutLayoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1F)
            linearlayout.layoutParams = linearlayoutLayoutParams
            linearlayout.orientation = LinearLayout.HORIZONTAL
            mnemonicWordsContainer.addView(linearlayout)

            for (index in lines until lines + 3) {
                val layout = RelativeLayout(this)
                layout.setBackgroundResource(R.drawable.shape_round_corner_6dp_ffffff)
                val textView = TextView(this)
                textView.gravity = Gravity.CENTER
                textView.setTextColor(ContextCompat.getColor(this, R.color.color_202020))
                textView.textSize = 15f
                textView.text = mnemonicWords[index]
                layout.addView(textView,
                        RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT).apply {
                            addRule(RelativeLayout.CENTER_IN_PARENT)
                        })

                val indexView = TextView(this)
                indexView.textSize = 13f
                indexView.setTextColor(resources.getColor(R.color.color_C5C5C5))
                indexView.text = (index + 1).toString()
                layout.addView(indexView,
                        RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT).apply {
                            addRule(RelativeLayout.ALIGN_PARENT_END)
                            addRule(RelativeLayout.ALIGN_PARENT_BOTTOM)
                            marginEnd = dip(3)
                            bottomMargin = dip(3)
                        })

                linearlayout.addView(layout, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1F))
            }

        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            REQUEST_CODE_BACKUP_DETAIL -> {
                if (resultCode == Activity.RESULT_OK) {
                    setResult(Activity.RESULT_OK)
                    finish()
                }
            }
        }

    }

    fun showDialog() {
        if (isFirst) {
            when (backupType) {
                getString(R.string.wallet_backup_mnemonic) -> {
                    showTipsDialog(getString(R.string.tips_title), getString(R.string.warning_backup_mnemonic), getString(R.string.i_see), {})
                }
                getString(R.string.wallet_backup_keystore) -> {
                    showTipsDialog(getString(R.string.tips_title), getString(R.string.warning_backup_keystore), getString(R.string.i_see), {})
                }
                getString(R.string.wallet_backup_plain_priv) -> {
                    showTipsDialog(getString(R.string.tips_title), getString(R.string.warning_backup_private_key), getString(R.string.i_see), {})
                }
            }

            isFirst = false
        }
    }

    override fun onStart() {
        super.onStart()
        if (!isRunningForeground) {
            showVeriDialog()
        }
    }

    override fun onStop() {
        onSwitch(true)
        super.onStop()
    }

    fun showVeriDialog() {
        if (verifyPWDDialog?.isShowing == true) {
            verifyPWDDialog?.dismiss()
        }
        verifyPWDDialog = VerifyPasswordDialog(activity = this,
                title = getString(R.string.wallet_backup_pwd_title),
                passwordType = if (wallet.isComplexPwd) PASSWORD_TYPE_COMPLEX else PASSWORD_TYPE_SIMPLE,
                onNext = { passPhrase ->
                    walletViewModel?.verifyWalletPassPhrase(wallet, passPhrase, {
                        if (it) {
                            SecurityHelper.walletCorrectPassword(wallet)
                            verifyPWDDialog?.dismiss()
                            onSwitch(false)
                        } else {
                            verifyPWDDialog?.walletPassPhraseET?.setText("")
                            verifyPWDDialog?.reset()
                        }
                    }, {
                        if (it.isNotEmpty()) {
                            if (SecurityHelper.walletWrongPassword(wallet)) {
                                verifyPWDDialog?.toastErrorMessage(getString(R.string.tip_password_error_to_lock))
                                mainHandler.postDelayed({
                                    verifyPWDDialog?.dismiss()
                                    finish()
                                }, 2300L)

                            } else {
                                verifyPWDDialog?.toastErrorMessage(Formatter.formatWalletErrorMsg(this, it))
                                verifyPWDDialog?.reset()
                            }
                        }
                    })
                }, onCancel = { finish() }
        )
        verifyPWDDialog?.show()
    }

    fun onSwitch(isExit: Boolean) {
        if (isExit) {
            if (verifyPWDDialog?.isShowing==true){
                return
            }
            showBackupContentFlag = !showBackupContentFlag
            safeTipsLayout.visibility = View.VISIBLE
            return
        }
        when (backupType) {
            getString(R.string.wallet_backup_mnemonic) -> {
                showMnemonicWords()
            }
            getString(R.string.wallet_backup_keystore) -> {
                showKeystore()
            }
            getString(R.string.wallet_backup_plain_priv) -> {
                showPrivateKey()
            }
        }

    }
}
