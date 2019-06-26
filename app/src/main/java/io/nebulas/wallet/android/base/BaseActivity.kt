package io.nebulas.wallet.android.base

import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.support.annotation.DrawableRes
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.widget.Toolbar
import android.util.DisplayMetrics
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import com.google.firebase.analytics.FirebaseAnalytics
import io.nebulas.wallet.android.R
import io.nebulas.wallet.android.app.WalletApplication
import io.nebulas.wallet.android.common.Constants
import io.nebulas.wallet.android.common.DataCenter
import io.nebulas.wallet.android.dialog.NasBottomDialog
import io.nebulas.wallet.android.dialog.NasBottomListDialog
import io.nebulas.wallet.android.extensions.removeKeyBoard
import io.nebulas.wallet.android.module.balance.BalanceFragment
import io.nebulas.wallet.android.module.launch.LaunchActivity
import io.nebulas.wallet.android.module.main.MainActivity
import io.nebulas.wallet.android.module.token.viewmodel.SupportTokenViewModel
import io.nebulas.wallet.android.module.wallet.create.model.Wallet
import io.nebulas.wallet.android.module.wallet.manage.MnemonicBackupCheckActivity
import io.nebulas.wallet.android.module.wallet.manage.WalletBackupActivity
import io.nebulas.wallet.android.network.exception.ApiException
import io.nebulas.wallet.android.util.NetWorkUtil
import io.nebulas.wallet.android.util.Util
import io.nebulas.wallet.android.view.research.CurtainResearch
import me.imid.swipebacklayout.lib.app.SwipeBackActivity
import java.util.*


/**
 * Created by Heinoc on 2018/1/31.
 */

abstract class BaseActivity : SwipeBackActivity() {

    companion object {
        /**
         * 申请权限的requestCode
         */
        const val REQUEST_CODE_PERMISSION = 0x00099
        /**
         * open System Setting request code
         */
        const val REQUEST_CODE_SETTING_PAGE = 2018
    }

    var firebaseAnalytics: FirebaseAnalytics? = null


    var isRunningForeground = true
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState != null) {
            restart()
            return
        }
        if (!screenshotEnabled()) {
            window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {

            val decorView = window.decorView

            val option = (View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN

                    or View.SYSTEM_UI_FLAG_LAYOUT_STABLE)

            decorView.systemUiVisibility = option

            window.statusBarColor = Color.TRANSPARENT

            if (window.decorView.systemUiVisibility != View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR) {
                //状态栏图标和文字颜色为暗色
                window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            }

        }


        firebaseAnalytics = FirebaseAnalytics.getInstance(this)


    }

    open fun screenshotEnabled(): Boolean = true

    override fun setContentView(layoutResID: Int) {
        super.setContentView(layoutResID)

        initView()
    }

    fun restart() {
        val intent = Intent(this, LaunchActivity::class.java).apply {
            action = "android.intent.action.MAIN"
            addCategory("android.intent.category.LAUNCHER")
            flags = Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }

    abstract fun initView()

    override fun onRestart() {
        super.onRestart()
    }

    override fun onResume() {
        if (this is LaunchActivity) {
        } else {
            if (Constants.LANGUAGE == "auto") {
                Util.isLanguageChanged(this, LaunchActivity::class.java)
            }
        }

        super.onResume()

        WalletApplication.INSTANCE.activity = this

    }

    override fun onPause() {
        super.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    /**
     * 多语言支持
     */
    override fun attachBaseContext(newBase: Context?) {

        val newContext = if (Constants.LANGUAGE != "auto") {
            val locale: Locale? = when (Constants.LANGUAGE) {
                "cn" -> {
                    Locale.SIMPLIFIED_CHINESE
                }
                "en" -> {
                    Locale.ENGLISH
                }
                "kr" -> {
                    Locale.KOREA
                }
                else -> {
                    Locale.ENGLISH
                }
            }

            val res = newBase!!.resources
            val config = res.configuration
            config.setLocale(locale)
            newBase.createConfigurationContext(config)
        } else {
            newBase
        }

        super.attachBaseContext(newContext)

    }

    /**
     * 请求权限
     */

    fun requestPermission(permissions: Array<String>, requestCode: Int = REQUEST_CODE_PERMISSION) {
        val needRequestPermissionList = getDeniedPermissions(permissions)
        if (needRequestPermissionList.isNotEmpty()) {
            val ps = needRequestPermissionList.toTypedArray()
            ActivityCompat.requestPermissions(this, ps, requestCode)
        }

    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == REQUEST_CODE_PERMISSION) {
            if (verifyPermissions(grantResults)) {
                //权限开启成功
            } else {
                //权限开启失败
            }
        }

    }

    /**
     * 检测所有的权限是否都已授权
     *
     * @param permissions
     * @return
     */
    protected fun checkPermissions(permissions: Array<String>): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true
        }

        permissions.forEach { permission ->
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false
            }
        }

        return true
    }

    /**
     * 获取权限集中需要申请权限的列表
     *
     * @param permissions
     * @return
     */
    fun getDeniedPermissions(permissions: Array<String>): List<String> {
        return permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED || ActivityCompat.shouldShowRequestPermissionRationale(this, it)
        }
    }

    /**
     * 确认所有的权限是否都已授权
     *
     * @param grantResults
     * @return
     */
    fun verifyPermissions(grantResults: IntArray): Boolean {
        return grantResults.none { it != PackageManager.PERMISSION_GRANTED }
    }

    /**
     * 需要权限的弹窗提示
     */
    fun showPermissionTips() {
        showTipsDialog(getString(R.string.alert_tips), getString(R.string.need_permissions),
                getString(R.string.alert_negative_title),
                { finish() },
                getString(R.string.alert_positive_title),
                {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    intent.data = Uri.parse("package:" + packageName)
                    startActivityForResult(intent, REQUEST_CODE_SETTING_PAGE)

                })

    }

    /**
     * 显示返回上一页按钮
     */
    fun showBackBtn(blackBack: Boolean = false, toolbar: Toolbar? = null) {
        if (null == toolbar)
            return

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        if (blackBack) {
            toolbar.setNavigationIcon(R.drawable.backarrow_black)

            if (window.decorView.systemUiVisibility != View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR) {
                //状态栏图标和文字颜色为暗色
                window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            }
        } else {
            toolbar.setNavigationIcon(R.drawable.backarrow_white)

            if (window.decorView.systemUiVisibility != View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_STABLE) {
                //状态栏图标和文字颜色为浅色
                window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            }
        }
    }

    protected open fun autoRemoveKeyboardOnDispatchTouchEvent(): Boolean = true

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        if (autoRemoveKeyboardOnDispatchTouchEvent()) {
            removeKeyBoard()
        }
        return super.dispatchTouchEvent(ev)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {

        if (this is MainActivity) {
            return this.mCurrentFragment!!.onOptionsItemSelected(item)
        }

        when (item?.itemId) {
            android.R.id.home -> {
                finish()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }


    /**
     * 获取当前设备statusBar的高度
     */
    fun getStatusBarHeight(context: Context): Int {
        var c: Class<*>? = null
        var obj: Any? = null
        var field: java.lang.reflect.Field? = null
        var x = 0
        var statusBarHeight = 0
        try {
            c = Class.forName("com.android.internal.R\$dimen")
            obj = c!!.newInstance()
            field = c.getField("status_bar_height")
            x = Integer.parseInt(field!!.get(obj).toString())
            statusBarHeight = context.resources.getDimensionPixelSize(x)
            return statusBarHeight
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return statusBarHeight
    }


    /**
     * 获取屏幕宽度单位px
     *
     * @return
     */
    fun getScreenWidth(): Int {
        val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val outMetrics = DisplayMetrics()
        wm.defaultDisplay.getMetrics(outMetrics)
        return outMetrics.widthPixels
    }


    /**
     * 显示提示对话框
     */
    fun showTipsDialog(title: String, message: String, positiveTitle: String, onConfirm: () -> Unit) {
        NasBottomDialog.Builder(this)
                .withTitle(title)
                .withContent(message)
                .withConfirmButton(positiveTitle, { _, dialog ->
                    dialog.dismiss()
                    onConfirm()
                })
                .build()
                .show()
    }


    /**
     * 显示提示对话框
     */
    fun showTipsDialog(title: String, message: String, negativeTitle: String, onCancel: () -> Unit, positiveTitle: String, onConfirm: () -> Unit, onCustomBackPressed: (() -> Unit)? = null) {

        NasBottomDialog.Builder(this)
                .withTitle(title)
                .withContent(message)
                .withCancelButton(buttonText = negativeTitle, block = { _, dialog ->
                    onCancel()
                    dialog.dismiss()
                })
                .withConfirmButton(positiveTitle, { _, dialog ->
                    dialog.dismiss()
                    onConfirm()
                })
                .withOnCustomBackPressed(onCustomBackPressed)
                .build()
                .show()

    }


    /**
     * 显示提示对话框
     */
    fun showTipsDialogWithIcon(title: String, @DrawableRes icon: Int, message: String, positiveTitle: String, onConfirm: () -> Unit, dataList: MutableList<Any>? = null) {
        NasBottomDialog.Builder(this)
                .withTitle(title)
                .withIcon(icon)
                .withContent(message)
                .withConfirmButton(positiveTitle, { _, dialog ->
                    dialog.dismiss()
                    onConfirm()
                })
                .withOnGridDataList(dataList)
                .build()
                .show()
    }

    /**
     * 显示提示对话框
     */
    fun showTipsDialogWithIcon(title: String, @DrawableRes icon: Int, message: String, negativeTitle: String, onCancel: () -> Unit, positiveTitle: String, onConfirm: () -> Unit, onCustomBackPressed: (() -> Unit)? = null) {

        NasBottomDialog.Builder(this)
                .withTitle(title)
                .withIcon(icon)
                .withContent(message)
                .withCancelButton(buttonText = negativeTitle, block = { _, dialog ->
                    onCancel()
                    dialog.dismiss()
                })
                .withConfirmButton(positiveTitle, { _, dialog ->
                    dialog.dismiss()
                    onConfirm()
                })
                .withOnCustomBackPressed(onCustomBackPressed)
                .build()
                .show()

    }

    /**
     * toast普通信息
     */
    fun toastMessage(msg: String) {
        CurtainResearch.create(this)
                .withLevel(CurtainResearch.CurtainLevel.NORMAL)
                .withContent(msg)
                .show()
    }

    /**
     * toast成功信息
     */
    fun toastSuccessMessage(msg: String) {
        CurtainResearch.create(this)
                .withLevel(CurtainResearch.CurtainLevel.SUCCESS)
                .withContent(msg)
                .show()
    }

    /**
     * toast错误信息
     */
    fun toastErrorMessage(msg: String) {
        CurtainResearch.create(this)
                .withLevel(CurtainResearch.CurtainLevel.ERROR)
                .withContent(msg)
                .show()
    }

    fun toastErrorMessage(msg: String,onFinished: ()->Unit){
        CurtainResearch.create(this)
                .withLevel(CurtainResearch.CurtainLevel.ERROR)
                .withContent(msg)
                .withDismissBlock(onFinished)
                .show()
    }
    /**
     * toast普通信息
     */
    fun toastMessage(resID: Int) {
        CurtainResearch.create(this)
                .withLevel(CurtainResearch.CurtainLevel.NORMAL)
                .withContent(getString(resID))
                .show()
    }

    /**
     * toast成功信息
     */
    fun toastSuccessMessage(resID: Int) {
        CurtainResearch.create(this)
                .withLevel(CurtainResearch.CurtainLevel.SUCCESS)
                .withContent(getString(resID))
                .show()
    }

    /**
     * toast错误信息
     */
    fun toastErrorMessage(resID: Int) {
        CurtainResearch.create(this)
                .withLevel(CurtainResearch.CurtainLevel.ERROR)
                .withContent(getString(resID))
                .show()
    }

    override fun onStart() {
        super.onStart()

        if (!isRunningForeground) {
            ViewModelProviders.of(this).get(SupportTokenViewModel::class.java).getTokensFromServer(lifecycle) {}
        }
    }

    override fun onStop() {
        super.onStop()
        isRunningForeground = Util.isRunForeground(this)
    }


    fun isHasNetWork(): Boolean {
        if (!NetWorkUtil.instance.isNetWorkConnected()) {
            toastErrorMessage(ApiException.CONNECT_EXCEPTION)
            return false
        }
        return true
    }

    fun showBackupList(context: Context,
                       firebaseKeyShow:String?,
                       firebaseKeyClick: String?,
                       fireBaseKeyBackupSuccess: String?): Boolean {
        if (DataCenter.wallets.size == 0) {
            return false
        }
        var allNeedBackup = true
        run breakpoint@{
            DataCenter.wallets.forEach {
                if (!it.isNeedBackup()) {
                    allNeedBackup = false
                    return@breakpoint
                }
            }
        }
        if (allNeedBackup) {
            if (DataCenter.wallets.size > 1) {
                var selectWalletDialog: NasBottomListDialog<Wallet, WalletWrapper>? = null
                val wrappers = mutableListOf<WalletWrapper>()
                val walletCount = DataCenter.wallets.size
                (0 until walletCount).forEach {
                    val wrapper = WalletWrapper(DataCenter.wallets[it], it)
                    wrappers.add(wrapper)
                }

                selectWalletDialog = NasBottomListDialog(
                        context = this,
                        title = getString(R.string.select_wallet_title),
                        dataSource = wrappers)
                        .also {
                            it.onItemSelectedListener = object : NasBottomListDialog.OnItemSelectedListener<Wallet, WalletWrapper> {
                                override fun onItemSelected(itemWrapper: WalletWrapper) {
                                    firebaseKeyClick?.apply {
                                        firebaseAnalytics?.logEvent(firebaseKeyClick, Bundle())
                                    }
                                    fireBaseKeyBackupSuccess?.apply {
                                        DataCenter.setData(MnemonicBackupCheckActivity.CLICK_BACKUP_FROM, this)
                                    }
                                    WalletBackupActivity.launch((context as BaseActivity),
                                            BalanceFragment.REQUEST_CODE_BACKUP_WALLET,
                                            context.getString(R.string.wallet_backup_mnemonic),
                                            itemWrapper.wallet)
                                    selectWalletDialog?.dismiss()
                                }
                            }
                        }
                selectWalletDialog?.show()
            } else {
                firebaseKeyShow?.apply {
                    firebaseAnalytics?.logEvent(this, Bundle())
                }

                showTipsDialogWithIcon(title = getString(R.string.swap_title_important_tip),
                        icon = R.drawable.icon_notice,
                        message = getString(R.string.backup_for_tran),
                        negativeTitle = getString(R.string.backup_tip_cancel),
                        onCancel = {},
                        positiveTitle = getString(R.string.backup_tip_confirm),
                        onConfirm = {
                            firebaseKeyClick?.apply {
                                firebaseAnalytics?.logEvent(firebaseKeyClick, Bundle())
                            }
                            fireBaseKeyBackupSuccess?.apply {
                                DataCenter.setData(MnemonicBackupCheckActivity.CLICK_BACKUP_FROM, this)
                            }
                            WalletBackupActivity.launch(this,
                                    WalletBackupActivity.REQUEST_CODE_BACKUP_DETAIL,
                                    getString(R.string.wallet_backup_mnemonic),
                                    DataCenter.wallets[0])
                        },
                        onCustomBackPressed = {}
                )
            }
            return true
        } else
            return false
    }

    private class WalletWrapper(val wallet: Wallet, val index: Int) : NasBottomListDialog.ItemWrapper<Wallet> {
        override fun getDisplayText(): String = wallet.walletName
        override fun getOriginObject(): Wallet = wallet
        override fun isShow(): Boolean = false

    }
}
