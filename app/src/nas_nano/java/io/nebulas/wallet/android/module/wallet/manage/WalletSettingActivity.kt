package io.nebulas.wallet.android.module.wallet.manage

import android.app.Activity
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.view.View
import io.nebulas.wallet.android.R
import io.nebulas.wallet.android.base.BaseActivity
import io.nebulas.wallet.android.base.BaseBindingAdapter
import io.nebulas.wallet.android.common.DataCenter
import io.nebulas.wallet.android.common.PASSWORD_TYPE_COMPLEX
import io.nebulas.wallet.android.common.PASSWORD_TYPE_SIMPLE
import io.nebulas.wallet.android.common.mainHandler
import io.nebulas.wallet.android.configuration.Configuration
import io.nebulas.wallet.android.db.DBUtil
import io.nebulas.wallet.android.dialog.VerifyPasswordDialog
import io.nebulas.wallet.android.extensions.initCollapsingToolbar
import io.nebulas.wallet.android.module.me.SingleInputActivity
import io.nebulas.wallet.android.module.swap.SwapHelper
import io.nebulas.wallet.android.module.wallet.create.model.Wallet
import io.nebulas.wallet.android.module.wallet.create.viewmodel.WalletViewModel
import io.nebulas.wallet.android.module.wallet.manage.adapter.WalletSettingRecyclerViewAdapter
import io.nebulas.wallet.android.module.wallet.manage.model.WalletSettingListModel
import io.nebulas.wallet.android.service.IntentServiceUpdateDeviceInfo
import io.nebulas.wallet.android.util.Formatter
import io.nebulas.wallet.android.util.SecurityHelper
import io.nebulas.wallet.android.util.Util
import kotlinx.android.synthetic.nas_nano.activity_wallet_setting.*
import kotlinx.android.synthetic.nas_nano.dialog_wallet_passphrase.*
import org.jetbrains.anko.contentView
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.startActivityForResult
import org.jetbrains.anko.uiThread
import org.jetbrains.annotations.NotNull
import java.math.BigDecimal

class WalletSettingActivity : BaseActivity() {

    companion object {
        const val WALLET = "walletSettingWallet"

        /**
         * backup wallet
         */
        const val REQUEST_CODE_BACKUP_MNEMONIC_WALLET = 10010

        /**
         * backup wallet
         */
        const val REQUEST_CODE_BACKUP_WALLET = 10011

        /**
         * open singleInputActivity request code
         */
        const val REQUEST_CODE_SINGLE_INPUT = 10020

        /**
         * change pwd request code
         */
        const val REQUEST_CODE_CHANGE_PWD = 10030

        /**
         * 启动WalletDetailActivity
         *
         * @param context
         * @param requestCode
         * @param wallet
         */
        fun launch(@NotNull context: Context, requestCode: Int = 10001, @NotNull wallet: Wallet) {
            DataCenter.setData(WALLET, wallet)
            (context as AppCompatActivity).startActivityForResult<WalletSettingActivity>(requestCode)
        }

    }

    var walletIndex = 0
    lateinit var wallet: Wallet
    var walletBalance = ""

    lateinit var walletViewModel: WalletViewModel

    lateinit var adapter: WalletSettingRecyclerViewAdapter

    private var verifyPWDDialog: VerifyPasswordDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wallet_setting)
    }

    override fun initView() {

        initCollapsingToolbar(title = getString(R.string.wallet_setting_title),
                onBackBtnClick = {
                    onBackPressed()
                })

        wallet = if (DataCenter.containsData(WALLET)) {
            DataCenter.getData(WALLET, true) as Wallet
        } else {
            finish()
            return
        }

        var walletBalanceBD = BigDecimal(0)
        DataCenter.coins.forEach {
            if (it.walletId == wallet.id) {
                walletBalanceBD += BigDecimal(it.balance)
            }
        }
        walletBalance = walletBalanceBD.toPlainString()

        walletViewModel = ViewModelProviders.of(this).get(WalletViewModel::class.java)

        //recyclerView
        walletSettingRecyclerView.layoutManager = LinearLayoutManager(this)
        adapter = WalletSettingRecyclerViewAdapter(this) {
            if (SwapHelper.getTransferConfig(this)) {
                val address = DataCenter.coins.firstOrNull { it.walletId == wallet.id }?.address
                if (address != null && address.equals(SwapHelper.getSwapWalletInfo(this)?.nasWalletAddress)) {
                    showTipsDialog(
                            getString(R.string.tips_title),
                            getString(R.string.swap_wallet_delet_des),
                            getString(R.string.know_it)) {}
                    return@WalletSettingRecyclerViewAdapter
                }
            }

            if (wallet.isNeedBackup()) {

                if (walletBalance.isNotEmpty() && BigDecimal(walletBalance) > BigDecimal(0)) {
                    showTipsDialog(
                            getString(R.string.tips_title),
                            getString(R.string.have_balance_delete_tips),
                            getString(R.string.know_it)) { }

                    return@WalletSettingRecyclerViewAdapter

                } else {
                    showTipsDialog(
                            getString(R.string.tips_title),
                            getString(R.string.need_backup_to_delete_wallet),
                            getString(R.string.know_it)) { }

                    return@WalletSettingRecyclerViewAdapter
                }
            }

            if (walletBalance.isNotEmpty() && BigDecimal(walletBalance) > BigDecimal(0)) {
                showTipsDialog(
                        getString(R.string.tips_title),
                        getString(R.string.have_balance_delete_tips),
                        getString(R.string.have_balance_delete_wallet_need_confirm),
                        { },
                        getString(R.string.have_balance_delete_wallet_has_saved),
                        {
                            showDeleteWalletDialog()
                        })

                return@WalletSettingRecyclerViewAdapter

            }

            showDeleteWalletDialog()


        }

        walletSettingRecyclerView.adapter = adapter

        adapter.setOnClickListener(object : BaseBindingAdapter.OnItemClickListener {
            override fun onItemClick(view: View, position: Int) {
                val item = adapter.items[position]

                if (null != item.name) {
                    when (item.name) {
                        getString(R.string.wallet_setting_rename_wallet) -> {
                            SingleInputActivity.launch(this@WalletSettingActivity,
                                    REQUEST_CODE_SINGLE_INPUT,
                                    getString(R.string.modify_wallet_name),
                                    getString(R.string.input_wallet_name),
                                    getString(R.string.single_input_hint),
                                    wallet.walletName)
                        }
                        getString(R.string.wallet_setting_change_pwd) -> {
                            ChangePwdTypeActivity.launch(this@WalletSettingActivity, REQUEST_CODE_CHANGE_PWD, wallet)
                        }
                        getString(R.string.wallet_setting_mnemonic) -> {
                            WalletBackupActivity.launch(this@WalletSettingActivity,
                                    REQUEST_CODE_BACKUP_MNEMONIC_WALLET,
                                    getString(R.string.wallet_backup_mnemonic),
                                    wallet)
                        }
                        getString(R.string.wallet_setting_keystore) -> {
                            WalletBackupActivity.launch(this@WalletSettingActivity,
                                    REQUEST_CODE_BACKUP_WALLET,
                                    getString(R.string.wallet_backup_keystore),
                                    wallet)
                        }
                        getString(R.string.wallet_setting_plain_priv) -> {
                            WalletBackupActivity.launch(this@WalletSettingActivity,
                                    REQUEST_CODE_BACKUP_WALLET,
                                    getString(R.string.wallet_backup_plain_priv),
                                    wallet)
                        }

                        else -> {
                        }
                    }
                }


            }

            override fun onItemLongClick(view: View, position: Int) {

            }
        })

        initItems()

    }

    private fun initItems() {
        adapter.items.clear()

        val list = arrayListOf<WalletSettingListModel>()

        list.add(WalletSettingListModel(cateName = getString(R.string.wallet_setting_basic_setting)))
        list.add(WalletSettingListModel(name = getString(R.string.wallet_setting_rename_wallet), des = wallet.walletName))
        list.add(WalletSettingListModel(name = getString(R.string.wallet_setting_change_pwd)))
        list.add(WalletSettingListModel(separate = true))
        list.add(WalletSettingListModel(cateName = getString(R.string.wallet_setting_backup)))
        if (wallet.isNeedBackup())
            list.add(WalletSettingListModel(name = getString(R.string.wallet_setting_mnemonic)))
        list.add(WalletSettingListModel(name = getString(R.string.wallet_setting_keystore)))
        list.add(WalletSettingListModel(name = getString(R.string.wallet_setting_plain_priv), des = getString(R.string.wallet_setting_no_safe_tips), highlight = true))
        list.add(WalletSettingListModel(separate = true))
        list.add(WalletSettingListModel(hasFooter = true))

        adapter.items.addAll(list)
    }

    private fun showDeleteWalletDialog() {
        if (SecurityHelper.isWalletLocked(wallet)) {
            toastErrorMessage(getString(R.string.tip_password_has_locked))
            return
        }
        /**
         * 验证钱包密码
         */
        if (null == verifyPWDDialog) {
            verifyPWDDialog = VerifyPasswordDialog(activity = this,
                    title = getString(R.string.delete_wallet_dialog),
                    passwordType = if (wallet.isComplexPwd) PASSWORD_TYPE_COMPLEX else PASSWORD_TYPE_SIMPLE,
                    onNext = { passPhrase ->
                        walletViewModel.deleteWallet(wallet, passPhrase, { success ->
                            if (success) {
                                val defaultPaymentWalletId = Util.getDefaultPaymentWallet(this)
                                if (defaultPaymentWalletId == wallet.id) {
                                    Util.setDefaultPaymentWallet(this, -1)
                                }
                                val defaultReceiveWalletId = Util.getDefaultReceivingWallet(this)
                                if (defaultReceiveWalletId == wallet.id) {
                                    Util.setDefaultReceivingWallet(this, -1)
                                }
                                verifyPWDDialog?.dismiss()
                                toastSuccessMessage(R.string.tip_wallet_delete_success)
                                if (DataCenter.wallets.isEmpty()) { //删除钱包后判断钱包数量是否为0，为0则自动关闭指纹验证
                                    Configuration.disableFingerprint(this)
                                }
                                //更新设备信息
                                IntentServiceUpdateDeviceInfo.startActionUpdateAddresses(this)
                                mainHandler.postDelayed({
                                    finish()
                                }, 1000)
                            } else {
                                verifyPWDDialog?.walletPassPhraseET?.setText("")
                                verifyPWDDialog?.reset()
                            }
                        }, {
                            if (it.isNotEmpty()) {
                                if (SecurityHelper.walletWrongPassword(wallet)) {
                                    verifyPWDDialog?.dismiss()
                                    toastErrorMessage(getString(R.string.tip_password_error_to_lock))
                                } else {
                                    verifyPWDDialog?.toastErrorMessage(Formatter.formatWalletErrorMsg(this, it))
                                }
                            }
                            verifyPWDDialog?.reset()
                        })
                    })
        }
        verifyPWDDialog?.show()

    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            REQUEST_CODE_SINGLE_INPUT -> {
                if (resultCode == Activity.RESULT_OK) {
                    wallet.walletName = data!!.getStringExtra(SingleInputActivity.INPUT_TEXT)

                    val item = adapter.items.filter {
                        it.name == getString(R.string.wallet_setting_rename_wallet)
                    }[0]
                    item.des = wallet.walletName
                    adapter.notifyItemChanged(adapter.items.indexOf(item))

                    doAsync {
                        DBUtil.appDB.walletDao().insertWallet(wallet)

                    }

                }
            }
            REQUEST_CODE_BACKUP_MNEMONIC_WALLET -> {
                if (resultCode == Activity.RESULT_OK) {
                    run breakPoint@{
                        adapter.items.forEach {
                            if (it.name == getString(R.string.wallet_setting_mnemonic)) {
                                adapter.items.remove(it)
                                return@breakPoint
                            }
                        }
                    }
                }
            }
            REQUEST_CODE_CHANGE_PWD -> {
//                //如果是通过重新导入重置密码，直接finish，返回到钱包列表
//                if (resultCode == RESULT_CODE_RE_IMPORT) {
//                    finish()
//                }
            }
        }

    }

    override fun onRestart() {
        super.onRestart()
        refreshWallet()
    }

    override fun onPause() {
        super.onPause()
        setResult(Activity.RESULT_OK)
    }

    override fun onDestroy() {
        super.onDestroy()

        DataCenter.removeData(WALLET)
    }

    private fun refreshWallet() {
        val walletId = wallet.id
        contentView?.isEnabled = false
        doAsync {
            DataCenter.deleteWallet(wallet)
            wallet = DBUtil.appDB.walletDao().loadWalletById(walletId)
            DataCenter.wallets.add(wallet)
            DataCenter.wallets.sortWith(kotlin.Comparator{ w1, w2 ->
                return@Comparator (w1.id - w2.id).toInt()
            })
            uiThread {
                contentView?.isEnabled = true
            }
        }
    }


}
