package io.nebulas.wallet.android.module.main

import android.app.Activity
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.design.widget.BottomNavigationView
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentTransaction
import android.view.View
import com.google.gson.Gson
import io.nebulas.wallet.android.R
import io.nebulas.wallet.android.atp.AtpHolder
import io.nebulas.wallet.android.base.BaseActivity
import io.nebulas.wallet.android.common.Constants
import io.nebulas.wallet.android.common.DataCenter
import io.nebulas.wallet.android.db.DBUtil
import io.nebulas.wallet.android.extensions.logE
import io.nebulas.wallet.android.extensions.logI
import io.nebulas.wallet.android.module.balance.BalanceFragment
import io.nebulas.wallet.android.module.balance.model.Coin
import io.nebulas.wallet.android.module.balance.viewmodel.BalanceViewModel
import io.nebulas.wallet.android.module.discover.DiscoverFragment
import io.nebulas.wallet.android.module.html.HtmlActivity
import io.nebulas.wallet.android.module.me.MeFragment
import io.nebulas.wallet.android.module.swap.SwapHashUploadIntentService
import io.nebulas.wallet.android.module.token.viewmodel.SupportTokenViewModel
import io.nebulas.wallet.android.module.transaction.TxDetailActivity
import io.nebulas.wallet.android.module.transaction.model.Transaction
import io.nebulas.wallet.android.module.transaction.transfer.TransferActivity
import io.nebulas.wallet.android.push.message.KEY_PUSH_MESSAGE
import io.nebulas.wallet.android.push.message.MESSAGE_TYPE_NOTICE
import io.nebulas.wallet.android.push.message.MESSAGE_TYPE_TRANSACTION
import io.nebulas.wallet.android.push.message.Message
import io.nebulas.wallet.android.update.AppUpdateService
import io.nebulas.wallet.android.util.CheckDeviceUtil
import io.nebulas.wallet.android.util.NetWorkUtil
import io.nebulas.wallet.android.util.RootUtil
import kotlinx.android.synthetic.nas_nano.activity_main.*
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import org.jetbrains.annotations.NotNull
import org.json.JSONObject

/**
 * Created by Heinoc on 2018/1/31.
 */

class MainActivity : BaseActivity() {

    companion object {
        private const val GOTO_TRANSFER = "gotoTransfer"

        private const val REQUEST_CODE_TRANSFER = 65533

        /**
         * 启动MainActivity
         *
         * @param context
         */
        fun launch(@NotNull context: Context, gotoTransfer: Boolean = false, pushMessage: Message? = null) {
            val intent = Intent(context, MainActivity::class.java)
            intent.putExtra(GOTO_TRANSFER, gotoTransfer)
            if (pushMessage != null) {
                intent.putExtra(KEY_PUSH_MESSAGE, pushMessage)
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            context.startActivity(intent)
//            context.startActivity<MainActivity>(GOTO_TRANSFER to gotoTransfer)
        }
    }

    var mCurrentFragment: Fragment? = null
    private var balanceFragment: BalanceFragment? = null
    private var discoverFragment: DiscoverFragment? = null
    private var meFragment: MeFragment? = null

    lateinit var balanceViewModel: BalanceViewModel
    lateinit var supportTokenViewModel: SupportTokenViewModel

    private var lastChangeTime = System.currentTimeMillis()

    private val gson: Gson by lazy {
        Gson()
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //启动换币交易hash上传服务，用于处理hash上传失败后的重试问题
        SwapHashUploadIntentService.startAction(this)

        setContentView(R.layout.activity_main)

        if (NetWorkUtil.instance.isNetWorkConnected()) {
            AppUpdateService.startActionUpdate(this)
        }
        checkRuntimeEnvironment()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        dispatch()
    }

    override fun initView() {

//        首页，取消滑动返回手势功能
        swipeBackLayout.setEnableGesture(false)


        balanceViewModel = ViewModelProviders.of(this).get(BalanceViewModel::class.java)
        supportTokenViewModel = ViewModelProviders.of(this).get(SupportTokenViewModel::class.java)
        updateSupportTokens()

        //取消BottomNavigationView的图标默认填充效果
        navigation.itemIconTintList = null
        //初始默认显示
        navigation.menu.findItem(R.id.navBalance).setIcon(R.drawable.home_tab_home)

        navigation.setOnNavigationItemSelectedListener(BottomNavigationView.OnNavigationItemSelectedListener { item ->
            val now = System.currentTimeMillis()
            if (now - lastChangeTime < 100) {
                return@OnNavigationItemSelectedListener false
            }
            lastChangeTime = now
            resetNavBtnBG()
            when (item.itemId) {
                R.id.navBalance -> {
                    item.setIcon(R.drawable.home_tab_home)
                    showBalanceFragment()
                    return@OnNavigationItemSelectedListener true
                }
                R.id.navDiscover -> {
                    item.setIcon(R.drawable.home_tab_dapp)
                    showDiscoverFragment()
                    return@OnNavigationItemSelectedListener true
                }
                R.id.navMe -> {
                    item.setIcon(R.drawable.home_tab_me)
                    showMeFragment()
                    return@OnNavigationItemSelectedListener true
                }
            }
            false
        })


        /**
         * tab 初始非选中动效
         */
//        tabHomeLAV.setAnimation("tab_home.json")
//        tabHomeLAV.reverseAnimationSpeed()
//        tabHomeLAV.playAnimation()
//        tabDiscoverLAV.setAnimation("tab_discover.json")
////        tabDiscoverLAV.reverseAnimationSpeed()
//        tabDiscoverLAV.playAnimation()
//        tabMeLAV.setAnimation("tab_me.json")
//        tabMeLAV.reverseAnimationSpeed()
//        tabMeLAV.playAnimation()

        showBalanceFragment()

        dispatch()
    }

    private fun dispatch() {
        try {
            val finalIntent: Intent = intent ?: return
            if (finalIntent.getBooleanExtra(GOTO_TRANSFER, false)) {
                TransferActivity.launch(this,
                        REQUEST_CODE_TRANSFER,
                        true,
                        MainActivity::class.java.name,
                        "",
                        true)
                return
            }
            if (finalIntent.extras.containsKey(KEY_PUSH_MESSAGE)) {
                val message: Message = finalIntent.getSerializableExtra(KEY_PUSH_MESSAGE) as Message
                when (message.type) {
                    MESSAGE_TYPE_TRANSACTION -> {
                        goToTransactionDetail(message)
                    }
                    MESSAGE_TYPE_NOTICE -> {
                        goToNotice(message)
                    }
                    else -> {
                        logI("Process dispatch : 暂不支持的通知类型")
                    }
                }
            }
        } catch (e: Exception) {
            logE("Process dispatch error : ${e.message ?: e.toString()}")
        }
    }

    private fun goToTransactionDetail(message: Message) {
        val data = message.data
        val transaction: Transaction = gson.fromJson(data, Transaction::class.java) ?: return

        if (AtpHolder.isRenderable(transaction.txData)) {
            val address = if (transaction.isSend) {
                transaction.sender ?: ""
            } else {
                transaction.receiver ?: ""
            }
            AtpHolder.route(this, transaction.txData, address)
            return
        }
        var coin: Coin? = null
        run breakPoint@{
            DataCenter.coins.forEach {
                if (it.tokenId == transaction.currencyId) {
                    coin = it
                    return@breakPoint
                }
            }
        }
        if (null != coin) {
            val hash = transaction.hash ?: return
            doAsync {
                val localTx: Transaction? = DBUtil.appDB.transactionDao().getTransactionByHash(hash)
                localTx?.also {
                    transaction.remark = it.remark   //从本地数据库读取备注信息
                    if (transaction.txFee.isNullOrEmpty()) {
                        transaction.txFee = it.txFee
                    }
                }
                uiThread {
                    TxDetailActivity.launch(this@MainActivity, coin ?: return@uiThread, transaction)
                }
            }
        }
    }

    private fun goToNotice(message: Message) {
        val data = message.data
        val dataJson = JSONObject(data)
        val url = dataJson.optString("actionUrl")
        val title = dataJson.optString("title") ?: ""
        if (url.isNullOrEmpty()) {
            return
        }
        HtmlActivity.launch(this, url, title)
    }

    private fun resetNavBtnBG() {
        navigation.menu.findItem(R.id.navBalance).setIcon(R.drawable.home_tab_home_normal)
        navigation.menu.findItem(R.id.navDiscover).setIcon(R.drawable.home_tab_dapp_normal)
        navigation.menu.findItem(R.id.navMe).setIcon(R.drawable.home_tab_me_normal)
    }

    fun showFragment(fragment: Fragment) {
        if (fragment === mCurrentFragment) {
            return
        }

        /**
         * 页面激活动效
         */
//        when (fragment.javaClass.name) {
//            balanceFragment?.javaClass?.name -> {
//                tabHomeLAV.setAnimation("tab_home.json")
//                tabHomeLAV.reverseAnimationSpeed()
//                tabHomeLAV.playAnimation()
//            }
//
//            discoverFragment?.javaClass?.name -> {
//                tabDiscoverLAV.setAnimation("tab_discover.json")
//                tabDiscoverLAV.reverseAnimationSpeed()
//                tabDiscoverLAV.playAnimation()
//            }
//
//            meFragment?.javaClass?.name -> {
//                tabMeLAV.setAnimation("tab_me.json")
//                tabMeLAV.reverseAnimationSpeed()
//                tabMeLAV.playAnimation()
//            }
//
//        }
        /**
         * 页面隐藏动效
         */
//        when (mCurrentFragment?.javaClass?.name) {
//            balanceFragment?.javaClass?.name -> {
//                tabHomeLAV.setAnimation("tab_home.json")
//                tabHomeLAV.reverseAnimationSpeed()
//                tabHomeLAV.playAnimation()
//            }
//
//            discoverFragment?.javaClass?.name -> {
//                tabDiscoverLAV.setAnimation("tab_discover.json")
//                tabDiscoverLAV.reverseAnimationSpeed()
//                tabDiscoverLAV.playAnimation()
//            }
//
//            meFragment?.javaClass?.name -> {
//                tabMeLAV.setAnimation("tab_me.json")
//                tabMeLAV.reverseAnimationSpeed()
//                tabMeLAV.playAnimation()
//            }
//
//        }

        val transaction = supportFragmentManager.beginTransaction()

        if (mCurrentFragment !== null && mCurrentFragment!!.isAdded) {
            transaction.hide(mCurrentFragment!!)
        }

        if (fragment.isHidden)
            transaction.show(fragment)
        else {
            if (!fragment.isAdded) {
                transaction.add(R.id.fragmentContainer, fragment)
            }

        }
        transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
        //transaction.commit();-->出现了这个错误 IllegalStateException: Can not perform this action after onSaveInstanceState
        transaction.commitAllowingStateLoss()

        mCurrentFragment = fragment


    }

    fun showBalanceFragment() {

        if (balanceFragment === null) {
            balanceFragment = BalanceFragment()
            //balanceFragment?.balanceViewModel = balanceViewModel
        }


        if (window.decorView.systemUiVisibility != View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_STABLE) {
            //状态栏图标和文字颜色为浅色
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        }

        showFragment(balanceFragment!!)

    }

    fun showDiscoverFragment() {

        if (discoverFragment === null)
            discoverFragment = DiscoverFragment()

        if (window.decorView.systemUiVisibility != View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR) {
            //状态栏图标和文字颜色为暗色
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        }

        /**
         * GA
         */
        if (discoverFragment != mCurrentFragment) {
            firebaseAnalytics?.logEvent(Constants.kADiscoverShowed, Bundle())
        }

        showFragment(discoverFragment!!)

    }

    fun showMeFragment() {

        if (meFragment === null) {
            meFragment = MeFragment()
            //meFragment?.balanceViewModel = balanceViewModel
        }

        if (window.decorView.systemUiVisibility != View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_STABLE) {
            //状态栏图标和文字颜色为浅色
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        }

        showFragment(meFragment!!)


    }

    override fun onBackPressed() {

        if (mCurrentFragment !== null && mCurrentFragment !== balanceFragment) {

            if (discoverFragment == null || mCurrentFragment != discoverFragment || !discoverFragment!!.onBackPressed()) {
                showBalanceFragment()
                navigation.selectedItemId = R.id.navBalance
            }
            return
        } else {
            super.onBackPressed()
        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            REQUEST_CODE_TRANSFER -> {
                if (resultCode == Activity.RESULT_OK) {
                    finish()
                }
            }
            else -> {
                mCurrentFragment?.onActivityResult(requestCode, resultCode, data)
            }
        }

    }

    fun balanceViewModel(): BalanceViewModel {
        if (balanceViewModel == null) balanceViewModel = ViewModelProviders.of(this).get(BalanceViewModel::class.java)
        return balanceViewModel
    }

    fun updateSupportTokens() {
        //更新Tokens
        supportTokenViewModel.getTokensFromServer(lifecycle) {

            //获取币rpc host
            supportTokenViewModel.getCurrencyRpcHOST(lifecycle) {
            }
        }
    }

    private fun checkRuntimeEnvironment() {
        doAsync {
            if (CheckDeviceUtil.instance.checkDeviceIDS(this@MainActivity)
                    || CheckDeviceUtil.instance.checkEmulatorBuild(this@MainActivity)
                    || CheckDeviceUtil.instance.checkEmulatorFiles()
                    || CheckDeviceUtil.instance.checkImsiIDS(this@MainActivity)
                    || CheckDeviceUtil.instance.checkQEmuDriverFile()
                    || CheckDeviceUtil.instance.checkOperatorNameAndroid(this@MainActivity)
                    || CheckDeviceUtil.instance.checkPhoneNumber(this@MainActivity)
                    || CheckDeviceUtil.instance.checkPipes()) {

                uiThread { showTipsDialog(getString(R.string.tips_title), getString(R.string.warning_running_on_virtual_device), getString(R.string.action_ok_i_know), {}) }
            }

            if (RootUtil.instance.isDeviceRooted()) {
                uiThread { showTipsDialog(getString(R.string.tips_title), getString(R.string.warning_running_on_root_device), getString(R.string.action_ok_i_know), {}) }
            }
        }
    }


}
