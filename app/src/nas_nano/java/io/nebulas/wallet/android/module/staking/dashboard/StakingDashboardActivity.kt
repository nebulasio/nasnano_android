package io.nebulas.wallet.android.module.staking.dashboard

import android.app.Activity
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.view.Menu
import android.view.MenuItem
import com.young.binder.whenEvent
import com.young.polling.SyncManager
import io.nebulas.wallet.android.R
import io.nebulas.wallet.android.base.BaseActivity
import io.nebulas.wallet.android.common.DataCenter
import io.nebulas.wallet.android.common.RequestCodes
import io.nebulas.wallet.android.extensions.errorToast
import io.nebulas.wallet.android.module.staking.pledge.PledgeActivity
import io.nebulas.wallet.android.module.staking.StakingConfiguration
import io.nebulas.wallet.android.module.staking.detail.WalletStakingDetailActivity
import io.nebulas.wallet.android.module.wallet.create.model.Wallet
import kotlinx.android.synthetic.nas_nano.activity_stacking.*
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
        dataCenter = ViewModelProviders.of(this).get(StakingDashboardDataCenter::class.java)
        controller = StakingDashboardController(this, this, dataCenter)
        adapter = StakingDashboardAdapter(this, this, dataCenter)
        setContentView(R.layout.activity_stacking)
    }

    override fun initView() {
        showBackBtn(toolbar = toolbar)
        tvTitle.text = "质押"
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
        swipeRefreshLayout.setOnRefreshListener {
            controller.loadData()
        }
        bind()
//        controller.loadData()
    }

    override fun onStart() {
        super.onStart()
        dataCenter.isLoading.value = true
        controller.loadData()
        controller.sync()
    }

//    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
//        super.onActivityResult(requestCode, resultCode, data)
//        when(requestCode) {
//            RequestCodes.CODE_PLEDGE_ACTIVITY -> {
////                if (resultCode== Activity.RESULT_OK){
//                    dataCenter.isLoading.value = true
//                    controller.loadData()
////                } else {
////                    dataCenter.inOperationWallets = StakingConfiguration.getPledgingWallets(this)
////                }
//            }
//        }
//    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_help, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        item ?: return false
        when (item.itemId) {
            R.id.actionHelp -> {
                showHelpDialog()
            }
        }
        return super.onOptionsItemSelected(item)
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
        val pledgeDetail = pledgeDetailList.find { it.address==address.address }?:return
        val pledgedInfo = pledgeDetail.info?:return

        WalletStakingDetailActivity.launch(this,
                wallet.walletName,
                address.address,
                pledgedInfo.value?:"0",
                pledgedInfo.t?:"0")
    }

    private fun bind() {
        dataCenter.isLoading.observe(this) {
            swipeRefreshLayout.isRefreshing = it ?: false
        }
        dataCenter.error.observe(this) {
            it ?: return@observe
            if (!it.isEmpty()) {
                errorToast(it)
            }
        }
        dataCenter.whenEvent(StakingDashboardDataCenter.EVENT_DATA_SOURCE_CHANGED){
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
            dataCenter.inOperationWallets.forEach {pledgingWalletWrapper->
                val wallet = DataCenter.wallets.find { it.id== pledgingWalletWrapper.walletId}?:return@forEach
                val existWallet = pledgedWallets.find { it.wallet.id==wallet.id }
                val status = when(pledgingWalletWrapper.type){
                    StakingConfiguration.OperationType.Pledge -> StakingDashboardAdapter.WalletStatusType.InPledging
                    StakingConfiguration.OperationType.CancelPledge -> StakingDashboardAdapter.WalletStatusType.InCancel
                }
                if (existWallet==null){
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
        val msg = "用户参与质押的NAS依然存在用户的钱包中，NAX会根据NAS的质押数量动态发放，按天计算，用户所质押的连续天数越长获得NAX数量越多，取消质押后再次重新质押则恢复到初始状态，当用户钱包余额小于实际质押额则质押状态自动解除。"
        showTipsDialog("质押规则", msg, "知道了") {
            block()
        }
        if (!StakingConfiguration.hasReadStackingRule(this)) {
            StakingConfiguration.markRuleHasBeenRead(this)
        }
    }

}
