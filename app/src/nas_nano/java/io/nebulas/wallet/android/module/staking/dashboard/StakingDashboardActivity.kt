package io.nebulas.wallet.android.module.staking.dashboard

import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.view.View
import com.young.binder.whenEvent
import io.nebulas.wallet.android.R
import io.nebulas.wallet.android.base.BaseActivity
import io.nebulas.wallet.android.common.DataCenter
import io.nebulas.wallet.android.extensions.errorToast
import io.nebulas.wallet.android.module.staking.StakingConfiguration
import io.nebulas.wallet.android.module.staking.detail.WalletStakingDetailActivity
import io.nebulas.wallet.android.module.staking.pledge.PledgeActivity
import io.nebulas.wallet.android.module.wallet.create.model.Wallet
import kotlinx.android.synthetic.nas_nano.activity_stacking.*
import org.jetbrains.anko.toast
import walletcore.Walletcore
import java.math.BigDecimal

class StakingDashboardActivity : BaseActivity(), StakingDashboardAdapter.ActionListener {

    companion object {
        fun launch(context: Context) {
            context.startActivity(Intent(context, StakingDashboardActivity::class.java))
        }
    }

    private lateinit var dataCenter: StakingDashboardDataCenter
    private lateinit var controller: StakingDashboardController
    private lateinit var adapter: StakingDashboardAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (window.decorView.systemUiVisibility != View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_STABLE) {
            //状态栏图标和文字颜色为浅色
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        }
        dataCenter = ViewModelProviders.of(this).get(StakingDashboardDataCenter::class.java)
        controller = StakingDashboardController(this, this, dataCenter)
        adapter = StakingDashboardAdapter(this, this, dataCenter)
        setContentView(R.layout.activity_stacking)
    }

    override fun initView() {
        ibGoBack.setOnClickListener {
            finish()
        }
        ibHelp.setOnClickListener {
            showHelpDialog()
        }
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
        swipeRefreshLayout.setOnRefreshListener {
            controller.loadData()
        }
        bind()
    }

    override fun onStart() {
        super.onStart()
        dataCenter.isLoading.value = true
        controller.loadData()
        controller.sync()
    }

    override fun actionPledge() {
        if (StakingConfiguration.hasReadStackingRule(this)) {
            toPledge()
        } else {
            showHelpDialog {
                toPledge()
            }
        }
    }

    override fun actionToWalletProfitDetail(wallet: Wallet) {
        val address = DataCenter.addresses.find { it.walletId == wallet.id && it.platform == Walletcore.NAS }
                ?: return
        val pledgeDetailList = dataCenter.pledgeDetailList
        val pledgeDetail = pledgeDetailList.find { it.address == address.address } ?: return
        val pledgedInfo = pledgeDetail.info ?: return

        WalletStakingDetailActivity.launch(this,
                wallet.walletName,
                address.address,
                pledgedInfo.value ?: "0",
                pledgedInfo.t ?: "0")
    }

    private fun bind() {
        dataCenter.isLoading.observe(this) {
            swipeRefreshLayout.isRefreshing = it ?: false
        }
        dataCenter.stakingFailed.observe(this) {
            if (it == true) {
                toast(R.string.text_fail_to_pledge)
            }
        }
        dataCenter.stakingSuccess.observe(this) {
            if (it == true) {
                toast(R.string.text_success_to_pledge)
            }
        }
        dataCenter.stakingCancelFailed.observe(this) {
            if (it == true) {
                toast(R.string.text_fail_to_cancel_pledge)
            }
        }
        dataCenter.stakingCancelSuccess.observe(this) {
            if (it == true) {
                toast(R.string.text_success_to_cancel_pledge)
            }
        }
        dataCenter.error.observe(this) {
            it ?: return@observe
            if (!it.isEmpty()) {
                errorToast(it)
            }
        }
        dataCenter.whenEvent(StakingDashboardDataCenter.EVENT_DATA_SOURCE_CHANGED) {
            val pledgedWallets = mutableListOf<StakingDashboardAdapter.WalletWrapper>()
            dataCenter.pledgeDetailList.forEach { pledgeDetail ->
                val info = pledgeDetail.info ?: return@forEach
                val pledgedValue = info.value ?: return@forEach
                try {
                    if (BigDecimal(pledgedValue) == BigDecimal.ZERO) {
                        return@forEach
                    }
                } catch (e: Exception) {
                    return@forEach
                }
                val address = DataCenter.addresses.find { it.address == pledgeDetail.address }
                address ?: return@forEach
                val wallet = DataCenter.wallets.find { it.id == address.walletId }
                wallet ?: return@forEach
                pledgedWallets.add(StakingDashboardAdapter.WalletWrapper(wallet, StakingDashboardAdapter.WalletStatusType.Normal))
            }
            dataCenter.inOperationWallets.forEach { pledgingWalletWrapper ->
                val wallet = DataCenter.wallets.find { it.id == pledgingWalletWrapper.walletId }
                        ?: return@forEach
                val existWallet = pledgedWallets.find { it.wallet.id == wallet.id }
                val status = when (pledgingWalletWrapper.type) {
                    StakingConfiguration.OperationType.Pledge -> StakingDashboardAdapter.WalletStatusType.InPledging
                    StakingConfiguration.OperationType.CancelPledge -> StakingDashboardAdapter.WalletStatusType.InCancel
                }
                if (existWallet == null) {
                    pledgedWallets.add(StakingDashboardAdapter.WalletWrapper(wallet, status))
                } else {
                    existWallet.status = status
                }
            }
            adapter.updateDataSource(pledgedWallets)
        }


        dataCenter.stakingSummary.observe(this) {
            it ?: return@observe
            adapter.notifyDataSetChanged()
        }
    }

    private fun toPledge() {
        PledgeActivity.launchForResult(this)
    }

    private fun showHelpDialog(block: () -> Unit = {}) {
        showTipsDialog(
                getString(R.string.text_pledge_rules),
                getString(R.string.text_pledge_rules_details),
                getString(R.string.text_got_it)) {
            block()
        }
        if (!StakingConfiguration.hasReadStackingRule(this)) {
            StakingConfiguration.markRuleHasBeenRead(this)
        }
    }

}
