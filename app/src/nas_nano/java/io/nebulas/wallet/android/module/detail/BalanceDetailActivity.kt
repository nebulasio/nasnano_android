package io.nebulas.wallet.android.module.detail

import android.app.Activity
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter
import android.view.MenuItem
import android.view.View
import io.nebulas.wallet.android.R
import io.nebulas.wallet.android.base.BaseActivity
import io.nebulas.wallet.android.common.Constants
import io.nebulas.wallet.android.common.DataCenter
import io.nebulas.wallet.android.db.DBUtil
import io.nebulas.wallet.android.module.balance.model.Coin
import io.nebulas.wallet.android.module.balance.viewmodel.BalanceViewModel
import io.nebulas.wallet.android.module.detail.adapter.BalanceDetailListAdapter
import io.nebulas.wallet.android.module.detail.fragment.WalletListFragment
import io.nebulas.wallet.android.module.detail.fragment.transaction.TransactionListFragment
import io.nebulas.wallet.android.module.transaction.ReceivablesActivity
import io.nebulas.wallet.android.module.transaction.transfer.TransferActivity
import io.nebulas.wallet.android.module.transaction.model.Transaction
import io.nebulas.wallet.android.module.transaction.viewmodel.TxViewModel
import io.nebulas.wallet.android.module.wallet.create.CreateWalletActivity
import io.nebulas.wallet.android.module.wallet.create.model.Wallet
import io.nebulas.wallet.android.view.BalanceDetailAppbarDelegate
import kotlinx.android.synthetic.main.app_bar_for_balance_detail.*
import kotlinx.android.synthetic.nas_nano.activity_balance_detail.*
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.startActivity
import org.jetbrains.annotations.NotNull
import org.reactivestreams.Subscription

/**
 * Created by Heinoc on 2018/2/2.
 */

class BalanceDetailActivity : BaseActivity() {

    companion object {
        /**
         * 按币种id分组后的coin index
         */
        const val COIN_GROUP_INDEX = "coinGroupIndex"

        /**
         *
         */
        const val BALANCE_DETAIL_COIN = "balanceDetailCoin"

        /**
         * 发起收款RequestCode
         */
        const val REQUEST_CODE_RECEIVABLES = 10001

        /**
         * 发起转账RequestCode
         */
        const val REQUEST_CODE_TRANSFER = 10002

        /**
         * 钱包详情RequestCode
         */
        const val REQUEST_CODE_WALLET_DETAIL = 10003

        /**
         * 启动BalanceDetailActivity
         *
         * @param context
         * @param coinGroupIndex 按币种id分组后的coin index
         */
        fun launch(@NotNull context: Context, @NotNull coin: Coin) {
            DataCenter.setData(BALANCE_DETAIL_COIN, coin)
            context.startActivity<BalanceDetailActivity>()
        }

    }

    lateinit var coin: Coin
    lateinit var balanceViewModel: BalanceViewModel
    lateinit var txViewModel: TxViewModel

    var pendingTxs = mutableListOf<Transaction>()

    lateinit var adapter: BalanceDetailListAdapter


    var modifiedWalletItemIndex = 0

    /**
     * 所有交易进行中的transaction的进度查询订阅
     */
    var subscriptions = mutableListOf<Subscription>()

    var isFirstLoad = true
    private lateinit var appbarDelegate: BalanceDetailAppbarDelegate

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_balance_detail)
    }

    override fun initView() {
        showBackBtn(toolbar = toolbar)

        appbarDelegate = BalanceDetailAppbarDelegate(appBarLayout)

        try {
            coin = if (DataCenter.containsData(BALANCE_DETAIL_COIN)) {
                DataCenter.getData(BALANCE_DETAIL_COIN, false) as Coin
            } else {
                DataCenter.coinsGroupByCoinSymbol[0]
            }
        } catch (e: Exception) {
            finish()
            return
        }

        balanceViewModel = ViewModelProviders.of(this).get(BalanceViewModel::class.java)
        txViewModel = ViewModelProviders.of(this).get(TxViewModel::class.java)


        viewPager.adapter = TokenDetailPagerAdapter(supportFragmentManager)
        tabLayout.setupWithViewPager(viewPager)


        txViewModel.setCoin(coin)

        appbarDelegate.setTitleInToolbar(coin.symbol)

        layoutTransferButton.setOnClickListener {
            if (showBackupList(this, null, Constants.Backup_TokenDetail_Click, Constants.Backup_TokenDetail_Success)) {
                return@setOnClickListener
            }
            firebaseAnalytics?.logEvent(Constants.kACoinDetailTransferClick, Bundle())
            TransferActivity.launch(
                    context = this,
                    requestCode = BalanceDetailActivity.REQUEST_CODE_TRANSFER,
                    coin = coin)
        }

        layoutReceiveButton.setOnClickListener {
            ReceivablesActivity.launch(this, REQUEST_CODE_RECEIVABLES)
        }

        /**
         * 资产
         */
        balanceViewModel.getCoins().observe(this, Observer {

            /**
             * 如果币列表为0，则表明当前App内已无钱包
             */
            val tempList = DataCenter.coinsGroupByCoinSymbol.filter {
                it.tokenId == coin.tokenId
            }
            if (tempList.isEmpty()) {
                finish()
                return@Observer
            }

            coin = tempList[0]

            txViewModel.setCoin(coin)

            appbarDelegate.setContent(BalanceDetailAppbarDelegate.ContentType.TYPE_TOKEN_VALUE, coin.balanceString, coin.symbol)
            appbarDelegate.setSubContent(if (coin.noCurrencyPrice) "-" else "≈" + Constants.CURRENCY_SYMBOL + coin.formattedBalanceValueString)
            appbarDelegate.setSubTitleInToolbar(coin.balanceString)
        })

        refresh()
    }

    private fun refresh() {
        isFirstLoad = true
        loadingView.visibility = View.VISIBLE
        balanceViewModel.updateCoins(lifecycle) {
            loadingView.visibility = View.GONE
        }
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.actionAddWallet -> {
                CreateWalletActivity.launch(this, showBackBtn = true)
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            REQUEST_CODE_TRANSFER -> {
                if (resultCode == Activity.RESULT_OK) {

                }
            }
            REQUEST_CODE_WALLET_DETAIL -> {
                if (DataCenter.wallets.indexOf(adapter.items[modifiedWalletItemIndex].wallet as Wallet) < 0)//钱包已删除
                    adapter.notifyItemRemoved(modifiedWalletItemIndex)
                else//更新item
                    adapter.notifyItemChanged(modifiedWalletItemIndex)
            }
        }

    }

    override fun onDestroy() {
        super.onDestroy()

        doAsync {
            pendingTxs.forEach {
                if (it.txData.isNullOrEmpty()) {
                    it.txData = ""
                }
            }
            DBUtil.appDB.transactionDao().insertTransactions(pendingTxs)
        }

        /**
         * 取消所有的订阅时间，防止内存泄露
         */
        subscriptions.forEach {
            it.cancel()
        }

        DataCenter.removeData(BALANCE_DETAIL_COIN)
        DataCenter.removeData("txCoin")

    }


    inner class TokenDetailPagerAdapter(fm: FragmentManager) : FragmentPagerAdapter(fm) {

        private val fragmentWallet: Fragment by lazy {
            WalletListFragment.newInstance(coin.tokenId ?: "")
        }
        private val fragmentTransaction: Fragment by lazy {
            TransactionListFragment.newInstance(coin.tokenId ?: "")
        }

        override fun getPageTitle(position: Int): CharSequence? {
            return when (position) {
                0 -> getString(R.string.wallet_with_first_upper_case)
                1 -> getString(R.string.tx_records)
                else -> ""
            }
        }

        override fun getItem(position: Int): Fragment {
            return when (position) {
                0 -> fragmentWallet
                1 -> fragmentTransaction
                else -> throw RuntimeException("Position must not be more than 1")
            }
        }

        override fun getCount(): Int = 2

    }

}
