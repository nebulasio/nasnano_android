package io.nebulas.wallet.android.module.swap.step

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.Menu
import android.view.MenuItem
import android.view.View
import io.nebulas.wallet.android.R
import io.nebulas.wallet.android.base.BaseActivity
import io.nebulas.wallet.android.common.Constants
import io.nebulas.wallet.android.common.DataCenter
import io.nebulas.wallet.android.extensions.withValueAnimator
import io.nebulas.wallet.android.module.swap.SwapHelper
import io.nebulas.wallet.android.module.swap.detail.ExchangeRecordsActivity
import io.nebulas.wallet.android.module.swap.step.step1.SwapAddressGuideActivity
import io.nebulas.wallet.android.module.swap.step.step1.SwapChooseWalletFragment
import io.nebulas.wallet.android.module.swap.step.step1.SwapWalletReadyFragment
import io.nebulas.wallet.android.module.swap.step.step2.SwapTransferFragment
import io.nebulas.wallet.android.module.wallet.create.model.Address
import io.nebulas.wallet.android.module.wallet.create.model.Wallet
import kotlinx.android.synthetic.nas_nano.activity_swap_step.*

class SwapStepActivity : BaseActivity(), SwapChooseWalletFragment.OnWalletSelectedCallback, SwapWalletReadyFragment.OnNextListener {

    companion object {
        private const val STEP_1 = 1
        private const val STEP_2 = 2

        private const val REQUEST_CODE_BACKUP = 10001

        private const val KEY_IS_RE_EXCHANGE = "key_is_re_exchange"

        fun launch(activity: Activity) {
            activity.startActivity(Intent(activity, SwapStepActivity::class.java))
            activity.overridePendingTransition(R.anim.enter_bottom_in, android.R.anim.fade_out)
        }

        fun reExchange(context: Context) {
            SwapHelper.setInReExchangeProcessing(context, true)
            SwapHelper.setCurrentSwapStatus(context, SwapHelper.SWAP_STATUS_NONE)
            SwapHelper.clearLastSwapTransactionInfo(context)
            context.startActivity(Intent(context, SwapStepActivity::class.java).apply {
                putExtra(KEY_IS_RE_EXCHANGE, true)
            })
        }
    }

    private var currentFragment: Fragment? = null
    private var isReExchange = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (window.decorView.systemUiVisibility != View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_STABLE) {
            //状态栏图标和文字颜色为浅色
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        }

        isReExchange = intent.getBooleanExtra(KEY_IS_RE_EXCHANGE, false)
        if (!isReExchange) {
            isReExchange = SwapHelper.isInReExchangeProcessing(this)
        }

        setContentView(R.layout.activity_swap_step)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        toolbar.setNavigationOnClickListener {
            finish()
            overridePendingTransition(android.R.anim.fade_in, R.anim.exit_bottom_out)
        }

        if (isReExchange) {
            doReExchange()
        } else {
            autoStep()
        }
        firebaseAnalytics?.logEvent(Constants.Exchange_ImportTokens_Show, Bundle())
    }

    override fun initView() {}

    override fun onResume() {
        super.onResume()
        if (!isReExchange) {
            autoStep()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_record_list, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        item ?: return super.onOptionsItemSelected(item)
        when (item.itemId) {
            R.id.menu_item_record_list -> {
                val swapWalletInfo = SwapHelper.getSwapWalletInfo(this)
                val swapAddress = if (swapWalletInfo == null || swapWalletInfo.swapWalletAddress.isNullOrBlank()) {
                    null
                } else {
                    swapWalletInfo.swapWalletAddress
                }
                ExchangeRecordsActivity.launch(this, 0, ethAddress = swapAddress)
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(android.R.anim.fade_in, R.anim.exit_bottom_out)
    }

    override fun onWalletSelected(wallet: Wallet) {
        var address: Address? = null
        for (it in DataCenter.addresses) {
            if (it.platform == "nebulas" && it.walletId == wallet.id) {
                address = it
            }
        }
        address ?: return
        firebaseAnalytics?.logEvent(Constants.Exchange_SetNewAddress_Show, Bundle())
        SwapAddressGuideActivity.launch(this, address.address)
    }

    override fun onNextClicked() {
        isReExchange = false
        SwapHelper.setInReExchangeProcessing(this, false)
        SwapHelper.setReExchanging(this, true)
        animateToStep2()
    }

    private fun autoStep() {
        val swapWalletInfo = SwapHelper.getSwapWalletInfo(this)
        val fragment = when {
            swapWalletInfo == null -> {
                if (currentFragment is SwapChooseWalletFragment) {
                    return
                }
                changeIndicator(STEP_1)
                SwapChooseWalletFragment.newInstance()
            }
            swapWalletInfo.swapWalletWords.isEmpty() -> {
                //满足此条件，说明ETH钱包已经备份，直接进入转币步骤
                if (currentFragment is SwapTransferFragment) {
                    return
                }
                changeIndicator(STEP_2)
                SwapTransferFragment.newInstance(swapWalletInfo)
            }
            else -> {
                if (currentFragment is SwapWalletReadyFragment) {
                    return
                }
                changeIndicator(STEP_1)
                SwapWalletReadyFragment.newInstance(swapWalletInfo)
            }
        }
        showFragment(fragment)
    }

    private fun doReExchange() {
        if (currentFragment is SwapWalletReadyFragment) {
            return
        }
        val swapWalletInfo = SwapHelper.getSwapWalletInfo(this)
        changeIndicator(STEP_1)
        val fragment = SwapWalletReadyFragment.newInstance(swapWalletInfo ?: return)
        showFragment(fragment)
    }

    private fun showFragment(fragment: Fragment?) {
        fragment ?: return
        supportFragmentManager.beginTransaction()
                .replace(R.id.layout_container, fragment)
                .commitAllowingStateLoss()
        currentFragment = fragment
    }

    private fun animateToStep2() {
        val swapWalletInfo = SwapHelper.getSwapWalletInfo(this)
        swapWalletInfo ?: return
        val transferFragment = SwapTransferFragment.newInstance(swapWalletInfo)
        changeIndicator(STEP_2, true)
        supportFragmentManager.beginTransaction()
                .setCustomAnimations(R.anim.right_in, R.anim.left_out)
                .replace(R.id.layout_container, transferFragment)
                .commitAllowingStateLoss()
        currentFragment = transferFragment
    }

    private fun changeIndicator(step: Int, withAnim: Boolean = false) {
        val anchor = when (step) {
            STEP_1 -> {
                tv_tab_step_1.paint.isFakeBoldText = true
                tv_tab_step_2.paint.isFakeBoldText = false
                tv_tab_step_1.invalidate()
                tv_tab_step_2.invalidate()
                if (withAnim) {
                    tv_tab_step_1.withValueAnimator(0.6f, 1.0f) {
                        alpha = it
                    }.start()
                    tv_tab_step_2.withValueAnimator(1.0f, 0.6f) {
                        alpha = it
                    }.start()
                } else {
                    tv_tab_step_1.alpha = 1.0f
                    tv_tab_step_2.alpha = 0.6f
                }
                tv_tab_step_1
            }
            STEP_2 -> {
                tv_tab_step_1.paint.isFakeBoldText = false
                tv_tab_step_2.paint.isFakeBoldText = true
                tv_tab_step_1.invalidate()
                tv_tab_step_2.invalidate()
                if (withAnim) {
                    tv_tab_step_1.withValueAnimator(1.0f, 0.6f) {
                        alpha = it
                    }.start()
                    tv_tab_step_2.withValueAnimator(0.6f, 1.0f) {
                        alpha = it
                    }.start()
                } else {
                    tv_tab_step_1.alpha = 0.6f
                    tv_tab_step_2.alpha = 1.0f
                }
                tv_tab_step_2
            }
            else -> null
        }
        anchor ?: return
        view_indicator.post {
            val targetCenter = anchor.left + (anchor.width / 2)
            val width = view_indicator.width
            if (withAnim) {
                val nowX = view_indicator.x
                view_indicator.withValueAnimator(nowX, targetCenter.toFloat() - (width / 2)) { progress ->
                    translationX = progress
                }.start()
            } else {
                val x1 = targetCenter.toFloat() - (width / 2)
                view_indicator.x = x1
            }
        }

    }
}
