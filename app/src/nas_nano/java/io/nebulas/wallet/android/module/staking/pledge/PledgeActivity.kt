package io.nebulas.wallet.android.module.staking.pledge

import android.app.Activity
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import io.nebulas.wallet.android.R
import io.nebulas.wallet.android.base.BaseActivity
import io.nebulas.wallet.android.common.DataCenter
import io.nebulas.wallet.android.common.RequestCodes
import io.nebulas.wallet.android.dialog.NasBottomListDialog
import io.nebulas.wallet.android.extensions.errorToast
import io.nebulas.wallet.android.extensions.logD
import io.nebulas.wallet.android.module.staking.StakingTools
import io.nebulas.wallet.android.module.wallet.create.model.Wallet
import io.nebulas.wallet.android.module.wallet.manage.WalletBackupActivity
import io.nebulas.wallet.android.util.Util
import io.nebulas.wallet.android.util.getWalletColorCircleDrawable
import kotlinx.android.synthetic.nas_nano.activity_pledge.*
import kotlinx.android.synthetic.nas_nano.app_bar_main_no_underline.*
import walletcore.Walletcore
import java.math.BigDecimal
import java.math.RoundingMode

class PledgeActivity : BaseActivity() {

    companion object {
        fun launchForResult(activity: Activity) {
            activity.startActivityForResult(Intent(activity, PledgeActivity::class.java), RequestCodes.CODE_PLEDGE_ACTIVITY)
        }
    }

    private lateinit var dataCenter: PledgeDataCenter
    private lateinit var controller: PledgeController
    private var selectedWalletWrapper: WalletWrapper? = null

    data class WalletWrapper(val wallet: Wallet) : NasBottomListDialog.ItemWrapper<Wallet> {
        override fun getDisplayText(): String = wallet.walletName
        override fun getOriginObject(): Wallet = wallet
        override fun isShow(): Boolean = wallet.isNeedBackup()
    }

    private var selectWalletDialog: NasBottomListDialog<Wallet, WalletWrapper>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        dataCenter = ViewModelProviders.of(this).get(PledgeDataCenter::class.java)
        controller = PledgeController(this, this, dataCenter)
        setContentView(R.layout.activity_pledge)
        bind()
        initData()
    }

    private fun initData() {
        val wallet = getDefaultPaymentWallet()
        wallet ?: return
        dataCenter.walletData.value = wallet
    }

    override fun initView() {
        showBackBtn(true, toolbar)
        titleTV.textSize = 18f
        titleTV.text = getString(R.string.text_start_pledge)
        tvChangeWallet.setOnClickListener {
            changeWallet()
        }
        layoutConfirmPledge.setOnClickListener {
            val nas = etPledgeValue.text.toString()
            controller.pledge(nas, "000000")
        }
        etPledgeValue.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val len = s?.length ?: 0
                dataCenter.buttonStatus.value = if (len > 0) {
                    PledgeDataCenter.ButtonStatus.Enabled
                } else {
                    PledgeDataCenter.ButtonStatus.Disabled
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun changeWallet() {
        if (null == selectWalletDialog) {
            val allWallets = mutableListOf<WalletWrapper>()
            DataCenter.wallets.forEach {
                val wrapper = WalletWrapper(it)
                allWallets.add(wrapper)
                if (it.id == dataCenter.walletData.value?.id) {
                    selectedWalletWrapper = wrapper
                }
            }
            selectWalletDialog = NasBottomListDialog(
                    context = this,
                    title = getString(R.string.select_wallet_title),
                    dataSource = allWallets,
                    initSelectedItem = selectedWalletWrapper)
            selectWalletDialog?.onItemSelectedListener = object : NasBottomListDialog.OnItemSelectedListener<Wallet, WalletWrapper> {
                override fun onItemSelected(itemWrapper: WalletWrapper) {
                    if (selectedWalletWrapper == itemWrapper) {
                        return
                    }
                    selectedWalletWrapper = itemWrapper
                    dataCenter.walletData.value = itemWrapper.wallet
                }
            }
        }
        selectWalletDialog?.selectedItem = selectedWalletWrapper
        selectWalletDialog?.show()
    }

    private fun bind() {
        dataCenter.walletData.observe(this, true) {
            it ?: return@observe
            ivWalletIcon.setImageDrawable(getWalletColorCircleDrawable(this@PledgeActivity,
                    it.id, it.walletName[0], 20))
            tvWalletName.text = it.walletName
            tvBalance.text = "余额：${getBalance(it)} NAS"
            if (it.isNeedBackup()) {
                showWalletBackupDialog(it)
            } else {
                controller.scheduleWalletStatus(it)
            }
        }
        dataCenter.pledgedInfo.observe(this) {
            layoutPledgedInfo.visibility = if (it == null) {
                View.GONE
            } else {
                val pledgedNas = StakingTools.nasFormat(dataCenter.pledgedInfo.value?.info?.value)
                if (pledgedNas.toDouble() == 0.toDouble()) {
                    View.GONE
                } else {
                    tvPledgedNas.text = pledgedNas.toPlainString()
                    View.VISIBLE
                }
            }
        }
        dataCenter.error.observe(this) {
            if (it != null && it.isNotEmpty()) {
                errorToast(it)
            }
        }
        dataCenter.isLoading.observe(this) {
            loadingView.visibility = if (it == true) View.VISIBLE else View.GONE
        }
        dataCenter.estimateGasFee.observe(this) {
            tvGasFee.text = "${it ?: dataCenter.defaultEstimateGasFee} NAS 矿工费"
        }
        dataCenter.pledgeResult.observe(this) {
            if (it==true) {
                setResult(Activity.RESULT_OK)
                finish()
            }
        }
        dataCenter.buttonStatus.observe(this) {
            val status = it ?: PledgeDataCenter.ButtonStatus.Disabled
            when (status) {
                PledgeDataCenter.ButtonStatus.Disabled -> {
                    layoutConfirmPledge.isEnabled = false
                    tvConfirmPledge.text = "确认质押"
                    progressBarOnButton.visibility = View.GONE
                    etPledgeValue.isEnabled = true
                    tvChangeWallet.isClickable = true
                }
                PledgeDataCenter.ButtonStatus.Enabled -> {
                    layoutConfirmPledge.isEnabled = true
                    tvConfirmPledge.text = "确认质押"
                    progressBarOnButton.visibility = View.GONE
                    etPledgeValue.isEnabled = true
                    tvChangeWallet.isClickable = true
                }
                PledgeDataCenter.ButtonStatus.UploadingToChain -> {
                    layoutConfirmPledge.isEnabled = true
                    layoutConfirmPledge.isClickable = false
                    tvConfirmPledge.text = "上链中"
                    progressBarOnButton.visibility = View.VISIBLE
                    etPledgeValue.isEnabled = false
                    tvChangeWallet.isClickable = false
                }
            }
        }
    }

    private fun getBalance(wallet: Wallet): BigDecimal {
        val coin = DataCenter.coins.find {
            it.walletId == wallet.id && it.platform == Walletcore.NAS && it.type == 1
        }
        coin ?: return BigDecimal.ZERO
        return BigDecimal(coin.balance).divide(BigDecimal.TEN.pow(18), 2, RoundingMode.FLOOR)
    }

    private fun getDefaultPaymentWallet(): Wallet? {
        if (DataCenter.wallets.size == 0) {
            return null
        }
        val defaultPaymentWalletId = Util.getDefaultPaymentWallet(this)
        if (defaultPaymentWalletId < 0) {
            return DataCenter.wallets.find { !it.isNeedBackup() } ?: DataCenter.wallets[0]
        }
        for (wallet in DataCenter.wallets) {
            if (wallet.id == defaultPaymentWalletId) {
                return wallet
            }
        }
        return null
    }

    private fun showWalletBackupDialog(targetWallet: Wallet) {
        showTipsDialogWithIcon(title = getString(R.string.swap_title_important_tip),
                icon = R.drawable.icon_notice,
                message = getString(R.string.backup_for_rec),
                negativeTitle = getString(R.string.backup_tip_cancel),
                onCancel = {
                    finish()
                },
                positiveTitle = getString(R.string.backup_tip_confirm),
                onConfirm = {
                    WalletBackupActivity.launch(this,
                            WalletBackupActivity.REQUEST_CODE_BACKUP_DETAIL,
                            getString(R.string.wallet_backup_mnemonic),
                            targetWallet)
                },
                onCustomBackPressed = {}
        )
    }
}
