package io.nebulas.wallet.android.module.staking.detail

import android.app.Dialog
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.View
import io.nebulas.wallet.android.R
import io.nebulas.wallet.android.base.BaseActivity
import io.nebulas.wallet.android.dialog.CommonCenterDialog
import kotlinx.android.synthetic.nas_nano.activity_wallet_stacking_detail.*
import kotlinx.android.synthetic.nas_nano.app_bar_main_no_underline.*
import org.jetbrains.anko.toast
import java.util.*

class WalletStakingDetailActivity : BaseActivity(), WalletStakingDetailAdapter.ActionListener {

    companion object {
        /**
         * 启动方法
         * @param context       Context
         * @param walletName    钱包名称
         * @param address       钱包地址
         * @param pledgedNas    已质押的NAS数量
         * @param pledgedAge    已质押的NAS币龄
         */
        fun launch(context: Context,
                   walletName: String,
                   address: String,
                   pledgedNas:String,
                   pledgedAge: String) {
            context.startActivity(
                    Intent(context, WalletStakingDetailActivity::class.java)
                            .apply {
                                putExtra(WalletStakingDetailDataCenter.PARAM_WALLET_NAME, walletName)
                                putExtra(WalletStakingDetailDataCenter.PARAM_ADDRESS, address)
                                putExtra(WalletStakingDetailDataCenter.PARAM_PLEDGED_NAS, pledgedNas)
                                putExtra(WalletStakingDetailDataCenter.PARAM_PLEDGED_AGE, pledgedAge)
                            }
            )
        }
    }

    private lateinit var dataCenter: WalletStakingDetailDataCenter
    private lateinit var controller: WalletStakingDetailController

    private lateinit var adapter: WalletStakingDetailAdapter

    private var tipDialog: Dialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        dataCenter = ViewModelProviders.of(this).get(WalletStakingDetailDataCenter::class.java)
        controller = WalletStakingDetailController(this, this, dataCenter)
        adapter = WalletStakingDetailAdapter(this, this, dataCenter)
        handleIntent()
        setContentView(R.layout.activity_wallet_stacking_detail)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener(){
            override fun onScrolled(recyclerView: RecyclerView?, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                recyclerView?:return
                val lastVisiblePosition = (recyclerView.layoutManager as LinearLayoutManager).findLastVisibleItemPosition()
                if(lastVisiblePosition >= adapter.itemCount - 1) {
                    if (dataCenter.hasMore() && !dataCenter.isSwipeLoadingMore) {
                        dataCenter.isSwipeLoadingMore = true
                        controller.loadData()
                    }
                }
            }
        })
        swipeRefreshLayout.setOnRefreshListener {
            dataCenter.currentPage = 1
            controller.loadData()
        }
        bind()
        controller.loadData()
    }

    private fun handleIntent() {
        dataCenter.handleIntent(intent)
    }

    override fun initView() {
        showBackBtn(true, toolbar)
    }

    override fun actionCancelPledge() {
        controller.getEstimateGasFee()
    }

    private fun bind() {
        dataCenter.isSwipeRefresh.observe(this) {
            val isLoading = it ?: false
            swipeRefreshLayout.isRefreshing = isLoading
        }
        dataCenter.isCenterLoading.observe(this) {
            loadingView.visibility = if (it==true) View.VISIBLE else View.GONE
        }
        dataCenter.walletName.observe(this) {
            titleTV.text = it ?: ""
        }
        dataCenter.addressProfits.observe(this) {
            it ?: return@observe
            adapter.updateDataSource(dataCenter.profitsRecords.value ?: Collections.emptyList())
        }
        dataCenter.showCancelPledgeTipDialog.observe(this) {
            if (it==true) {
                showCancelPledgeTipDialog()
            }
        }
        dataCenter.cancelPledgeComplete.observe(this) {
            if (it==true){
                finish()
            }
        }
    }

    private fun showCancelPledgeTipDialog() {
        if (tipDialog == null) {
            tipDialog = CommonCenterDialog.Builder()
                    .withTitle("提醒")
                    .withContent("当前已质押20天 取消质押后将不再获得NAX 预计消耗${dataCenter.estimateGasFee}NAS矿工费")
                    .withLeftButton("确认取消") {
                        cancelPledge()
                    }
                    .withRightButton("继续质押")
                    .build(this)
        }
        val finalTipDialog = tipDialog ?: return
        finalTipDialog.show()
    }

    private fun cancelPledge() {
        controller.cancelPledge("000000")
    }
}
