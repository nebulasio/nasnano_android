package io.nebulas.wallet.android.module.wallet.create

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.text.Html
import io.nebulas.wallet.android.R
import io.nebulas.wallet.android.base.BaseActivity
import io.nebulas.wallet.android.common.Constants
import io.nebulas.wallet.android.common.DataCenter
import io.nebulas.wallet.android.extensions.initCollapsingToolbar
import io.nebulas.wallet.android.module.balance.model.Coin
import io.nebulas.wallet.android.module.main.MainActivity
import kotlinx.android.synthetic.nas_nano.activity_create_wallet.*
import kotlinx.android.synthetic.nas_nano.app_bar_collapse.*
import org.jetbrains.anko.startActivityForResult
import org.jetbrains.annotations.NotNull


class CreateWalletActivity : BaseActivity() {

    companion object {
        //intent value name
        const val SHOW_BACK_BTN = "showBackBtn"

        /**
         * token types,contain Walletcore.ETH/Walletcore.NAS .etc
         */
        const val PLATFORMS = "platforms"

        const val SET_PWD_REQUEST_CODE = 10001
        const val IMPORT_WALLET_REQUEST_CODE = 10002
        const val CREATE_CHECK_ALL_SUPPORT = 10003
        const val IMPORT_CHECK_ALL_SUPPORT = 10004
        const val CREATE_CHECK_FROM_WEB = 1005
        const val FROM_TYPE = "fromType"
        /**
         * 添加钱包的结果
         */
        const val WALLET_RESULT = "walletResult"

        /**
         * 启动CreateWalletActivity
         *
         * @param context
         * @param showBackBtn
         */
        fun launch(@NotNull context: Context, requestCode: Int = 10001, showBackBtn: Boolean = false) {
            (context as BaseActivity).firebaseAnalytics?.logEvent(Constants.kAAddWalletBtnClick, Bundle())
            (context as AppCompatActivity).startActivityForResult<CreateWalletActivity>( requestCode, FROM_TYPE to requestCode,SHOW_BACK_BTN to showBackBtn)
        }

    }

    var showBackBtn: Boolean = true

    var coinList: MutableList<Coin> = mutableListOf()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_wallet)
    }

    override fun initView() {

        initCollapsingToolbar(getString(R.string.add_account_title), { onBackPressed() })

        showBackBtn = intent.getBooleanExtra(SHOW_BACK_BTN, true)

        if (!showBackBtn) {
            //取消滑动返回手势功能
            swipeBackLayout.setEnableGesture(false)
            //屏蔽返回按钮
            toolbar.setNavigationIcon(null)
        }

        createCheckAllSupportBtn.text = Html.fromHtml(getString(R.string.check_all_support_coin))
        importCheckAllSupportBtn.text = Html.fromHtml(getString(R.string.check_all_support_coin))

        createCheckAllSupportBtn.setOnClickListener {

            SetPassPhraseActivity.launch(this, CREATE_CHECK_ALL_SUPPORT)

//            startActivityForResult<CoinListActivity>(CREATE_CHECK_ALL_SUPPORT,
//                    CoinListActivity.IS_CREATE to true)
        }
        importCheckAllSupportBtn.setOnClickListener {

            ImportWalletActivity.launch(this, IMPORT_WALLET_REQUEST_CODE)

//            startActivityForResult<CoinListActivity>(IMPORT_CHECK_ALL_SUPPORT,
//                    CoinListActivity.IS_CREATE to false)
        }

        createWalletBtn.setOnClickListener {

            if (DataCenter.wallets.size >= Constants.MAX_WALLET_COUNTS) {
                showTipsDialog(
                        getString(R.string.tips_title),
                        getString(R.string.too_many_wallets),
                        getString(R.string.determine_btn),
                        { })
                return@setOnClickListener
            }

            SetPassPhraseActivity.launch(this, if(intent.getIntExtra(FROM_TYPE,0) == CREATE_CHECK_FROM_WEB) CREATE_CHECK_FROM_WEB else CREATE_CHECK_ALL_SUPPORT)

//            startActivityForResult<CoinListActivity>(CREATE_CHECK_ALL_SUPPORT,
//                    CoinListActivity.IS_CREATE to true)

        }
        importWalletBtn.setOnClickListener {
            //            if (DataCenter.wallets.isNotEmpty()) {

            if (DataCenter.wallets.size >= Constants.MAX_WALLET_COUNTS) {
                showTipsDialog(
                        getString(R.string.tips_title),
                        getString(R.string.too_many_wallets),
                        getString(R.string.know_it),
                        { })
                return@setOnClickListener
            }

            ImportWalletActivity.launch(this, IMPORT_WALLET_REQUEST_CODE)
//            } else {
//                DataCenter.setData(PLATFORMS, platforms)
//
//                startActivityForResult<CoinListActivity>(IMPORT_CHECK_ALL_SUPPORT,
//                        CoinListActivity.IS_CREATE to false)
//
//            }
        }

    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            SET_PWD_REQUEST_CODE, IMPORT_WALLET_REQUEST_CODE, CREATE_CHECK_ALL_SUPPORT, IMPORT_CHECK_ALL_SUPPORT -> {
                if (Activity.RESULT_OK != resultCode)
                    return

                setResult(Activity.RESULT_OK, data)

                if (!showBackBtn)
                    MainActivity.launch(this)

                DataCenter.removeData(PLATFORMS)
                finish()
            }
            CREATE_CHECK_FROM_WEB ->{
                finish()
            }
        }

    }

}
