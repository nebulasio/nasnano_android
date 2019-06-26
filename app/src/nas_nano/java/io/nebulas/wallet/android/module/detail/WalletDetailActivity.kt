package io.nebulas.wallet.android.module.detail

import android.content.Context
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import io.nebulas.wallet.android.R
import io.nebulas.wallet.android.base.BaseActivity
import io.nebulas.wallet.android.common.Constants
import io.nebulas.wallet.android.common.DataCenter
import io.nebulas.wallet.android.module.balance.model.Coin
import io.nebulas.wallet.android.module.detail.adapter.WalletDetailPagerAdapter
import io.nebulas.wallet.android.module.transaction.ReceivablesActivity
import io.nebulas.wallet.android.module.transaction.transfer.TransferActivity
import io.nebulas.wallet.android.module.wallet.create.model.Wallet
import io.nebulas.wallet.android.module.wallet.manage.MnemonicBackupCheckActivity
import io.nebulas.wallet.android.module.wallet.manage.WalletBackupActivity
import io.nebulas.wallet.android.module.wallet.manage.WalletSettingActivity
import io.nebulas.wallet.android.util.Formatter
import io.nebulas.wallet.android.view.BalanceDetailAppbarDelegate
import kotlinx.android.synthetic.main.app_bar_for_balance_detail.*
import kotlinx.android.synthetic.nas_nano.activity_wallet_detail.*
import org.jetbrains.anko.startActivityForResult
import org.jetbrains.annotations.NotNull
import java.math.BigDecimal

/**
 * Created by alina on 2018/6/26.
 */
class WalletDetailActivity : BaseActivity() {
    companion object {
        const val WALLET = "walletDetailWallet"

        const val TRANSFER_FROM_WALLET_DETAIL = 40001
        const val RECEIVE_FROM_BALANCE_DETAIL = 40002
        fun launch(@NotNull context: Context, requestCode: Int, @NotNull wallet: Wallet) {
            DataCenter.setData(WALLET, wallet)
            (context as AppCompatActivity).startActivityForResult<WalletDetailActivity>(requestCode)
        }
    }

    var walletId = 0L
    private lateinit var currentWallet: Wallet
    private lateinit var walletPagerAdapter: WalletDetailPagerAdapter

    private lateinit var appbarDelegate: BalanceDetailAppbarDelegate
    lateinit var currentWalletCoins: MutableList<Coin>
    private var totalValue: BigDecimal = BigDecimal(0)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wallet_detail)
    }

    override fun initView() {
        showBackBtn(false, toolbar)

        walletPagerAdapter = WalletDetailPagerAdapter(supportFragmentManager)
        walletPagerAdapter.addFragment(WalletTokenFragment(), this.getString(R.string.wallet_detail_tokens))
        walletPagerAdapter.addFragment(WalletTransactionsFragment(), this.getString(R.string.wallet_detail_transactions))
        walletViewPager.adapter = walletPagerAdapter

        walletTabLayout.addTab(walletTabLayout.newTab().setText(R.string.wallet_detail_tokens))
        walletTabLayout.addTab(walletTabLayout.newTab().setText(R.string.wallet_detail_transactions))
        walletTabLayout.setupWithViewPager(walletViewPager)

        currentWallet = if (DataCenter.containsData(WALLET))
            DataCenter.getData(WALLET, false) as Wallet
        else {
            finish()
            return
        }

        walletId = currentWallet.id
        currentWalletCoins = mutableListOf()
        addressLayout.setOnClickListener {
            checkBackup (getString(R.string.backup_for_rec)){ ReceivablesActivity.launch(this, RECEIVE_FROM_BALANCE_DETAIL,currentWallet) }

        }

        transferLayout.setOnClickListener {
            checkBackup (getString(R.string.backup_for_tran)) {
                val coin = DataCenter.coins.first {
                    it.walletId == currentWallet.id
                }
                TransferActivity.launch(
                        context = this,
                        requestCode = TRANSFER_FROM_WALLET_DETAIL,
                        coin = coin
                )
            }
        }
    }

    private fun checkBackup(str: String,onFinish: () -> Unit) {
        if (currentWallet.isNeedBackup())
            showTipsDialogWithIcon(title = getString(R.string.swap_title_important_tip),
                    icon = R.drawable.icon_notice,
                    message = str,
                    negativeTitle = getString(R.string.backup_tip_cancel),
                    onCancel = {},
                    positiveTitle = getString(R.string.backup_tip_confirm),
                    onConfirm = {
                        firebaseAnalytics?.logEvent(Constants.Backup_WalletDetail_Click, Bundle())
                        DataCenter.setData(MnemonicBackupCheckActivity.CLICK_BACKUP_FROM,Constants.Backup_WalletDetail_Success)
                        WalletBackupActivity.launch(this,
                                WalletBackupActivity.REQUEST_CODE_BACKUP_DETAIL,
                                getString(R.string.wallet_backup_mnemonic),
                                currentWallet)
                    },
                    onCustomBackPressed = {}
            )
        else
            onFinish()

    }

    /**
     * 设置title信息
     */
    private fun setTitleInfo() {
        totalValue = BigDecimal(0)
        currentWalletCoins.forEach {
            totalValue += BigDecimal(it.balanceValueString)
        }

        appbarDelegate = BalanceDetailAppbarDelegate(appBarLayout, walletId)
        appbarDelegate.setTitleInToolbar(currentWallet.walletName)
        appbarDelegate.setContent(BalanceDetailAppbarDelegate.ContentType.TYPE_LEGAL_TENDER_VALUE, Formatter.priceFormat(totalValue), Constants.CURRENCY_SYMBOL)
        //appbarDelegate.setSubContent(getString(R.string.total_balance_des))

        appbarDelegate.setSubTitleInToolbar("≈" + Constants.CURRENCY_SYMBOL + Formatter.priceFormat(totalValue))
    }

    override fun onResume() {
        super.onResume()
        getCurrentWalletCoins()
        setTitleInfo()

    }

    private fun getCurrentWalletCoins() {
        currentWalletCoins.clear()
        DataCenter.coins.filter {
            it.walletId == walletId
        }.forEach {
            if (it.walletId == walletId) {
                currentWalletCoins.add(it)
            }
        }
        if (currentWalletCoins.isEmpty()) {
            finish()
            return
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menu?.clear()
        menuInflater.inflate(R.menu.setting_menu, menu)

        return super.onCreateOptionsMenu(menu)

    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.settingBtn -> {
                WalletSettingActivity.launch(this, wallet = currentWallet)
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onDestroy() {
        super.onDestroy()
        DataCenter.removeData(WALLET)
    }
}