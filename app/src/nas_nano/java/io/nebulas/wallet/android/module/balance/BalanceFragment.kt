package io.nebulas.wallet.android.module.balance

import android.app.Activity
import android.arch.lifecycle.Observer
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.DefaultItemAnimator
import android.support.v7.widget.LinearLayoutManager
import android.text.TextPaint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.nebulas.wallet.android.R
import io.nebulas.wallet.android.atp.AtpHolder
import io.nebulas.wallet.android.base.BaseActivity
import io.nebulas.wallet.android.base.BaseBindingAdapter
import io.nebulas.wallet.android.base.BaseFragment
import io.nebulas.wallet.android.common.Constants
import io.nebulas.wallet.android.common.DataCenter
import io.nebulas.wallet.android.common.firebaseAnalytics
import io.nebulas.wallet.android.common.mainHandler
import io.nebulas.wallet.android.db.DBUtil
import io.nebulas.wallet.android.extensions.errorToast
import io.nebulas.wallet.android.module.balance.adapter.BalanceRecyclerViewAdapter
import io.nebulas.wallet.android.module.balance.model.BalanceListModel
import io.nebulas.wallet.android.module.balance.model.Coin
import io.nebulas.wallet.android.module.detail.BalanceDetailActivity
import io.nebulas.wallet.android.module.detail.WalletDetailActivity
import io.nebulas.wallet.android.module.html.HtmlActivity
import io.nebulas.wallet.android.module.main.MainActivity
import io.nebulas.wallet.android.module.qrscan.ScannerActivity
import io.nebulas.wallet.android.module.qrscan.ScannerDispatcher
import io.nebulas.wallet.android.module.staking.dashboard.StakingDashboardActivity
import io.nebulas.wallet.android.module.staking.StakingContractHolder
import io.nebulas.wallet.android.module.swap.SwapRouter
import io.nebulas.wallet.android.module.transaction.ReceivablesActivity
import io.nebulas.wallet.android.module.transaction.TxDetailActivity
import io.nebulas.wallet.android.module.transaction.model.Transaction
import io.nebulas.wallet.android.module.transaction.transfer.TransferActivity
import io.nebulas.wallet.android.module.wallet.create.CreateWalletActivity
import io.nebulas.wallet.android.module.wallet.create.model.Wallet
import io.nebulas.wallet.android.network.server.HttpManager
import io.nebulas.wallet.android.util.Formatter
import io.nebulas.wallet.android.util.Util
import kotlinx.android.synthetic.nas_nano.app_bar_balance.*
import kotlinx.android.synthetic.nas_nano.fragment_balance.*
import org.jetbrains.anko.dip
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import java.math.BigDecimal


/**
 * Created by Heinoc on 2018/1/31.
 */

class BalanceFragment : BaseFragment() {

    companion object {
        const val REQUEST_CODE_BACKUP_WALLET = 10001
        const val REQUEST_CODE_CREATE_WALLET = 10002
        const val REQUEST_CODE_WALLET_DETAIL = 10003
        const val REQUST_CODE_FROM_HOME_PAGE = 1004
        const val REQUEST_CODE_SCANNER = 10005
    }

    private var ctx: Context? = null

    private lateinit var adapter: BalanceRecyclerViewAdapter

    //lateinit var balanceViewModel: BalanceViewModel

    var showBackUpTips = false
    var showBalanceNumber: Boolean = true
    var balanceValue: String? = null
    var lastClickedTime: Long = System.currentTimeMillis()

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        ctx = context
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_balance, container, false)
    }

    override fun initView() {
        showBalanceNumber = !Util.getBalanceHidden(context!!)

        (activity as AppCompatActivity).setSupportActionBar(toolbar)

        toolbar.title = ""
        (activity as AppCompatActivity).supportActionBar!!.title = ""

        //上划的极限高度
        val offSetHeight = context!!.dip(80).toFloat()
        //按钮的极限Y
        val offSetY = context!!.dip(60).toFloat()

        appBarLayout.addOnOffsetChangedListener { appBarLayout, verticalOffset ->
            val alphaScale = 1.0f + verticalOffset / offSetHeight
            val modifyY = verticalOffset / offSetHeight * offSetY

            balanceValueDesTV.alpha = alphaScale
            maskBtn.alpha = alphaScale
            approximateTV.alpha = alphaScale
            balanceValueTV.alpha = alphaScale
            ibScan.alpha = alphaScale

            receivablesBtn.pivotY = receivablesBtn.height.toFloat()
            receivablesBtn.translationY = modifyY
            receivables_layout.translationY = modifyY

            transferBtn.pivotY = transferBtn.height.toFloat()
//
            transferBtn.translationY = modifyY
            transfer_layout.translationY = modifyY
        }

        swipeRefreshLayout.setOnRefreshListener {
            swipeRefreshLayout.isRefreshing = true
            refresh {
                swipeRefreshLayout.isRefreshing = false
                (context as MainActivity).balanceViewModel().setLoading(false)
            }
        }
        // 初始刷新
        swipeRefreshLayout.isRefreshing = true

        balanceValueDesTV.text = getString(R.string.total_balance_des)

        maskBtn.setOnClickListener {
            showBalanceNumber = !showBalanceNumber
            Util.setBalanceHidden(context!!, !showBalanceNumber)

            updateHiddenStatus()

            //GA
            (activity as BaseActivity).firebaseAnalytics?.logEvent(if (showBalanceNumber) Constants.kAHomeShowDetail else Constants.kAHomeHiddenDetail, Bundle())

        }

        //收款
        receivablesBtn.setOnClickListener {
            if (DataCenter.wallets.isEmpty()) {
                doNoWallet()
                return@setOnClickListener
            }
            if (DataCenter.coins.isEmpty()) {
                return@setOnClickListener
            }
            DataCenter.setData("txCoin", DataCenter.coins[0])
            ReceivablesActivity.launch(this.requireContext(), REQUST_CODE_FROM_HOME_PAGE)
            // GA
            (context as BaseActivity).firebaseAnalytics?.logEvent(Constants.kAHomeReceiveClick, Bundle())
        }
        //转账
        transferBtn.setOnClickListener {
            if (DataCenter.wallets.isEmpty()) {
                doNoWallet()
                return@setOnClickListener
            }
            if (DataCenter.coins.isEmpty()) {
                return@setOnClickListener
            }
            if ((activity as BaseActivity).showBackupList(activity!!,
                            Constants.Backup_HomeReceive_Show,
                            Constants.Backup_HomeReceive_Click,
                            Constants.Backup_HomeReceive_Success)) {
                return@setOnClickListener
            }
            val nasCoin = DataCenter.coins.first { it.symbol == "NAS" }

            TransferActivity.launch(
                    context = this.requireActivity(),
                    requestCode = BalanceDetailActivity.REQUEST_CODE_TRANSFER,
                    coin = nasCoin)
            //GA
            (context as BaseActivity).firebaseAnalytics?.logEvent(Constants.kAHomeTransferClick, Bundle())
        }

        homeRecyclerView.layoutManager = LinearLayoutManager(context!!)
        homeRecyclerView.itemAnimator = DefaultItemAnimator()

        adapter = BalanceRecyclerViewAdapter(activity!!)
        homeRecyclerView.adapter = adapter

        adapter.setOnClickListener(object : BaseBindingAdapter.OnItemClickListener {
            override fun onItemClick(view: View, position: Int) {
                val now = System.currentTimeMillis()
                if (now - lastClickedTime < 200) {
                    return
                }
                lastClickedTime = now

                val item = adapter.items[position]
                if (item.isStacking) {
                    toStaking()
                    return
                }
                if (null != item.swapItem) {
                    SwapRouter.route(requireActivity(), item.swapItem!!.status)
                    (activity as BaseActivity).firebaseAnalytics?.logEvent(Constants.Exchange_Entrance_Click, Bundle())
                } else if (null != item.noticeItem) {

                    HtmlActivity.launch(this@BalanceFragment.activity!!, item.noticeItem?.href
                            ?: "", item.noticeItem?.title ?: "")
                    val bundle = Bundle()
                    bundle.putInt(Constants.Home_TopAds_Click_ID, item.noticeItem!!.id)
                    (context as BaseActivity).firebaseAnalytics?.logEvent(Constants.kAHomeTopAdClick, Bundle())

                } else if (null != item.feedItem) {

                    HtmlActivity.launch(this@BalanceFragment.activity!!, item.feedItem?.href
                            ?: "", item.feedItem?.title
                            ?: "")
                    val bundle = Bundle()
                    bundle.putInt(Constants.Home_News_Click_ID, item.feedItem!!.id)
                    (context as BaseActivity).firebaseAnalytics?.logEvent(Constants.kAHomeFeedClick, bundle)

                } else if (null != item.tx) {
                    val transaction = item.tx ?: return
                    if (AtpHolder.isRenderable(transaction.txData)) {
                        val address = if (transaction.isSend) {
                            transaction.sender ?: ""
                        } else {
                            transaction.receiver ?: ""
                        }
                        firebaseAnalytics.logEvent(Constants.Home_ATPAds_Click, Bundle())
                        AtpHolder.route(requireActivity(), transaction.txData, address)
                        return
                    }
                    var coin: Coin? = null
                    run breakPoint@{
                        DataCenter.coins.forEach {
                            if (it.tokenId == item.tx!!.currencyId) {
                                coin = it
                                return@breakPoint
                            }
                        }
                    }
                    if (null != coin) {
                        val tx = item.tx ?: return
                        val hash = tx.hash ?: return
                        doAsync {
                            val localTx: Transaction? = DBUtil.appDB.transactionDao().getTransactionByHash(hash)
                            localTx?.also {
                                tx.remark = it.remark   //从本地数据库读取备注信息
                                if (tx.txFee.isNullOrEmpty()) {
                                    tx.txFee = it.txFee
                                }
                            }
                            uiThread {
                                TxDetailActivity.launch(this@BalanceFragment.activity!!, coin!!, item.tx!!)
                            }
                        }
                    }
                }

            }

            override fun onItemLongClick(view: View, position: Int) {
            }
        })

        (context as MainActivity).balanceViewModel().getFeedFlowDatas().observe(this, Observer {
            if (null != it) {
                refreshAdapterItems(it)
            }
        })
        (context as MainActivity).balanceViewModel().getTotalBalance().observe(this, Observer {
            balanceValue = it

            updateBalanceNumberUI()

        })

        ibScan.setOnClickListener {
            ScannerActivity.launchByFragment(this, REQUEST_CODE_SCANNER)
        }

        updateHiddenStatus()
        Util.checkProtocol(context!!)
    }

    private fun updateHiddenStatus() {
        if (showBalanceNumber)
            maskBtn.setImageResource(R.drawable.eyeopen)
        else
            maskBtn.setImageResource(R.drawable.eyeclose)

        updateBalanceNumberUI()
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden) {
            lastClickedTime = System.currentTimeMillis()
            showBalanceNumber = !Util.getBalanceHidden(context!!)
            updateHiddenStatus()
        }
    }

    private fun doNoWallet() {
        (activity as MainActivity).showTipsDialogWithIcon(title = getString(R.string.alert_tips),
                icon = R.drawable.icon_notice,
                message = getString(R.string.no_wallet_content),
                negativeTitle = getString(R.string.cancel_btn),
                onCancel = {
                },
                positiveTitle = getString(R.string.yes_btn),
                onConfirm = {

                    CreateWalletActivity.launch(context as BaseActivity, showBackBtn = true)
                }
        )
    }


    private fun refresh(onFinish: () -> Unit) {
        isHasNetWork()
        (context as MainActivity).balanceViewModel().setLoading(true)
        (context as MainActivity).balanceViewModel().getCoinsValue(
                lifecycle = lifecycle,
                onFinish = { priceList ->
                    if (priceList.isNotEmpty()) {
                        DataCenter.coins.forEach { coinItem ->
                            run breakPoint@{
                                priceList.forEach {
                                    if (coinItem.tokenId == it.currencyId) {
                                        coinItem.currencyPrice = it.price
                                        return@breakPoint
                                    }
                                }
                            }
                        }
                    }
                    (context as MainActivity).balanceViewModel().updateCoins(lifecycle, onFinish)

                    (context as MainActivity).balanceViewModel().getHomeFeedFlow(this, context, onFinish)
                },
                onFailed = {
                    (context as MainActivity).balanceViewModel().getHomeFeedFlow(this, context, onFinish)
                })
    }


    override fun onResume() {
        super.onResume()

        lastClickedTime = System.currentTimeMillis()

        tvTransferSend.width = getReceivebalesBtnWidth().toInt()

        //GA埋点
        (activity as BaseActivity).firebaseAnalytics?.setCurrentScreen(activity!!, "BalanceFragment", null)

        updateBalanceNumberUI()

        refresh {
            swipeRefreshLayout.isRefreshing = false
            (context as MainActivity).balanceViewModel().setLoading(false)
        }

        //判断是否有钱包
        if (DataCenter.wallets.isEmpty()) {
            balanceValue = "0.00"
        } else {
            var totalBalance = BigDecimal.ZERO
            DataCenter.coins.forEach {
                totalBalance += BigDecimal(it.balanceValue)
            }
            balanceValue = Formatter.priceFormat(totalBalance)
        }
        updateBalanceNumberUI()

    }

    /**
     * 计算收币按钮文案宽度
     */
    private fun getReceivebalesBtnWidth(): Float {
        var paint = TextPaint()
        paint.textSize = tvTransferReceivables.textSize
        return paint.measureText(tvTransferReceivables.text.toString())
    }


    @Synchronized
    private fun refreshAdapterItems(items: MutableList<BalanceListModel>) {

        if (items.isNotEmpty()) {
            val first = items[0]
            if (!first.isStacking) {
                items.add(0, BalanceListModel(isStacking = true))
            }
        } else {
            items.add(0, BalanceListModel(isStacking = true))
        }

        /**
         * 检查钱包备份情况
         */
        run breakPoint@{
            DataCenter.wallets.forEach {
                showBackUpTips = false
                if (it.isNeedBackup()) {
                    showBackUpTips = true
                    return@breakPoint
                }
            }
        }
        // 是否需要展示备份提醒
        if (showBackUpTips && items.none { it.showBackUpTips }) {
            val noticeItemLastIndex = items.indexOfLast { null != it.noticeItem }
            val position = if (noticeItemLastIndex < 0) {
                var hasSwapItem = false
                var swapItemPosition = -1
                for (item in items) {
                    swapItemPosition++
                    if (item.swapItem != null) {
                        hasSwapItem = true
                        break
                    }
                }
                if (hasSwapItem) {
                    swapItemPosition + 1
                } else {
                    0
                }
            } else {
                noticeItemLastIndex + 1
            }
            items.add(position, BalanceListModel(showBackUpTips = showBackUpTips))
        }


        //判断是否有钱包
        if (DataCenter.wallets.isEmpty()) {
            //去掉底部添加钱包按钮，显示创建钱包的layout
            if (items.none { it.emptyWallet }) {
                items.removeAll(adapter.items.filter {
                    it.isFooter
                })
                items.add(BalanceListModel(emptyWallet = true))

                updateBalanceNumberUI()
            }

        } else {
            if (items.none { it.isFooter }) {
                //显示添加钱包按钮
                items.removeAll(adapter.items.filter {
                    it.emptyWallet
                })
                items.add(BalanceListModel(isFooter = true))

                swipeRefreshLayout.visibility = View.VISIBLE
            }

        }
        adapter.changeDataSource(items)
    }

    private fun updateBalanceNumberUI() {

        balanceValueDesTV.text = getString(R.string.total_balance_des)

        val it = if (showBalanceNumber) {
            approximateTV.visibility = View.VISIBLE
            approximateTV.text = "≈${Constants.CURRENCY_SYMBOL}"
            if (balanceValue != null)
                "$balanceValue"
            else
                "0.00"
        } else {
            approximateTV.visibility = View.GONE
            "****"
        }

        balanceValueTV.text = it

        adapter.items.forEach { item -> item.showBalanceDetail = showBalanceNumber }
        adapter.notifyDataSetChanged()

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            REQUEST_CODE_CREATE_WALLET -> {
                if (resultCode == Activity.RESULT_OK) {
                    if (null != data && data.hasExtra(CreateWalletActivity.WALLET_RESULT)) {
                        var wallet = data.getSerializableExtra(CreateWalletActivity.WALLET_RESULT) as Wallet

                        wallet = DataCenter.wallets.first {
                            it.id == wallet.id
                        }

                        if (null != wallet) {
                            WalletDetailActivity.launch(context!!, REQUEST_CODE_WALLET_DETAIL, wallet)
                        }

                    }
                }
            }
            REQUEST_CODE_SCANNER -> {
                if (resultCode == Activity.RESULT_OK) {
                    val result = data?.getStringExtra("result") ?: ""
                    activity?.apply {
                        ScannerDispatcher.dispatch(this, result)
                    }
                }
            }
        }

    }

    private fun toStaking() {
        StakingDashboardActivity.launch(requireContext())
    }

}