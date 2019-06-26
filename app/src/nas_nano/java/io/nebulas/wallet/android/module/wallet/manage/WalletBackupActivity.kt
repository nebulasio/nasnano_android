package io.nebulas.wallet.android.module.wallet.manage

import android.app.Activity
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity
import io.nebulas.wallet.android.R
import io.nebulas.wallet.android.base.BaseActivity
import io.nebulas.wallet.android.common.DataCenter
import io.nebulas.wallet.android.common.PASSWORD_TYPE_COMPLEX
import io.nebulas.wallet.android.common.PASSWORD_TYPE_SIMPLE
import io.nebulas.wallet.android.db.DBUtil
import io.nebulas.wallet.android.dialog.VerifyPasswordDialog
import io.nebulas.wallet.android.module.wallet.create.CreateWalletActivity
import io.nebulas.wallet.android.module.wallet.create.model.Wallet
import io.nebulas.wallet.android.module.wallet.create.viewmodel.WalletViewModel
import io.nebulas.wallet.android.util.Formatter
import io.nebulas.wallet.android.util.SecurityHelper
import io.reactivex.Flowable
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.android.synthetic.nas_nano.activity_wallet_backup.*
import kotlinx.android.synthetic.nas_nano.app_bar_main.*
import kotlinx.android.synthetic.nas_nano.dialog_wallet_passphrase.*
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.startActivityForResult
import org.jetbrains.anko.uiThread
import org.jetbrains.annotations.NotNull
import org.reactivestreams.Subscription
import walletcore.Walletcore
import java.util.concurrent.TimeUnit

class WalletBackupActivity : BaseActivity() {

    companion object {
        /**
         * 当前备份类型，值在strings.xml中配置
         * R.string.wallet_backup_mnemonic
         * R.string.wallet_backup_keystore
         * R.string.wallet_backup_plain_priv
         */
        const val BACKUP_TYPE = "backupType"

        /**
         * 当前需要备份的钱包
         */
        const val WALLET = "wallet"

        /**
         * 备份详情页面requestCode
         */
        const val REQUEST_CODE_BACKUP_DETAIL = 10001

        /**
         * 启动WalletBackupActivity
         *
         * @param context
         * @param backupType
         * @param wallet
         */
        fun launch(@NotNull context: Context, requestCode: Int = 90001, @NotNull backupType: String, @NotNull wallet: Wallet) {
            (context as AppCompatActivity).startActivityForResult<WalletBackupActivity>(requestCode, BACKUP_TYPE to backupType, WALLET to wallet)
        }

        /**
         * 启动WalletBackupActivity
         *
         * @param fragment
         * @param backupType
         * @param wallet
         */
        fun launch(@NotNull fragment: Fragment, requestCode: Int = 90001, @NotNull backupType: String, @NotNull wallet: Wallet) {
            fragment.startActivityForResult(Intent(fragment.requireContext(), WalletBackupActivity::class.java).apply {
                putExtra(BACKUP_TYPE, backupType)
                putExtra(WALLET, wallet)
            }, requestCode)
        }

    }

    lateinit var backupType: String

    lateinit var wallet: Wallet

    private var verifyPWDDialog: VerifyPasswordDialog? = null

    lateinit var walletViewModel: WalletViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wallet_backup)
    }

    override fun initView() {

        backupType = intent.getStringExtra(BACKUP_TYPE)
        wallet = intent.getSerializableExtra(WALLET) as Wallet

        showBackBtn(true, toolbar)
        titleTV.text = when (backupType) {
            getString(R.string.wallet_backup_mnemonic) -> {
                getString(R.string.text_wallet_backup_words)
            }
            getString(R.string.wallet_backup_plain_priv) -> {
                getString(R.string.text_wallet_backup_private_key)
            }
            getString(R.string.wallet_backup_keystore) -> {
                getString(R.string.text_wallet_backup_keystore)
            }
            else -> ""
        }

        walletViewModel = ViewModelProviders.of(this).get(WalletViewModel::class.java)

        when (backupType) {
            getString(R.string.wallet_backup_mnemonic) -> {
                val title = getString(R.string.wallet_backup_title_words)
                backupTitleTV.text = title

                backupSubTitleTV.text = getString(R.string.wallet_backup_keystore_sub_title)
                tipsTitleTV.text = getString(R.string.wallet_backup_keystore_course)
                if (backupType == getString(R.string.wallet_backup_mnemonic)) {
                    tipsTitleTV.setText(R.string.wallet_backup_offline)
                    backupSubTitleTV.text = getString(R.string.wallet_backup_sub_mu_des)
                }

                var tips = getString(R.string.wallet_backup_offline_resaon)
                tips = String.format(tips, backupType)
                tipsTV.text = tips
            }
            getString(R.string.wallet_backup_plain_priv) -> {
                val title = getString(R.string.wallet_backup_title_private_key)
                backupTitleTV.text = title

                backupSubTitleTV.text = getString(R.string.wallet_backup_keystore_sub_title)
                tipsTitleTV.text = getString(R.string.wallet_backup_keystore_course)
                if (backupType == getString(R.string.wallet_backup_mnemonic)) {
                    tipsTitleTV.setText(R.string.wallet_backup_offline)
                    backupSubTitleTV.text = getString(R.string.wallet_backup_sub_mu_des)
                }

                var tips = getString(R.string.wallet_backup_offline_resaon)
                tips = String.format(tips, backupType)
                tipsTV.text = tips
            }
            getString(R.string.wallet_backup_keystore) -> {
                val title = getString(R.string.wallet_backup_title_keystore)
                backupTitleTV.text = title

                backupSubTitleTV.setText(R.string.wallet_backup_keystore_sub_title)
                tipsTitleTV.setText(R.string.wallet_backup_keystore_course)
                tipsTV.setText(R.string.wallet_backup_keystore_course_content)

            }

            else -> {
            }
        }

        backupBtn.setOnClickListener {
            if (SecurityHelper.isWalletLocked(wallet)) {
                toastErrorMessage(getString(R.string.tip_password_has_locked))
                return@setOnClickListener
            }
            /**
             * 验证钱包密码
             */
            if (null == verifyPWDDialog) {
                verifyPWDDialog = VerifyPasswordDialog(activity = this,
                        title = getString(R.string.wallet_backup_pwd_title),
                        passwordType = if (wallet.isComplexPwd) PASSWORD_TYPE_COMPLEX else PASSWORD_TYPE_SIMPLE,
                        onNext = { passPhrase ->
                            walletViewModel.verifyWalletPassPhrase(wallet, passPhrase, {
                                if (it) {
                                    SecurityHelper.walletCorrectPassword(wallet)
                                    verifyPWDDialog?.dismiss()

                                    WalletBackupDetailActivity.launch(this,
                                            REQUEST_CODE_BACKUP_DETAIL,
                                            backupType,
                                            wallet,
                                            Walletcore.NAS,
                                            passPhrase)
                                    /*when (backupType) {
                                        getString(R.string.wallet_backup_mnemonic) -> {
                                            WalletBackupDetailActivity.launch(this,
                                                    REQUEST_CODE_BACKUP_DETAIL,
                                                    backupType,
                                                    wallet,
                                                    Walletcore.NAS,
                                                    passPhrase)
                                        }
                                        getString(R.string.wallet_backup_plain_priv),
                                        getString(R.string.wallet_backup_keystore) -> {
                                            BlockChainTypeActivity.launch(this,
                                                    REQUEST_CODE_BACKUP_DETAIL,
                                                    BlockChainTypeActivity.BlockChainTypeLaunchType.BACKUP_WALLET,
                                                    wallet,
                                                    backupType,
                                                    passPhrase,
                                                    getString(R.string.choose_wallet_type_title))
                                        }

                                        else -> {
                                        }
                                    }*/

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
                                        verifyPWDDialog?.reset()
                                    }
                                }
                            })
                        })
            }
            verifyPWDDialog?.show()
        }

        timer()

    }

    override fun onResume() {
        super.onResume()
        refreshWallet()
    }

    private fun refreshWallet(){
        val walletId = wallet.id
        backupBtn.isEnabled = false
        doAsync {
            DataCenter.deleteWallet(wallet)
            wallet = DBUtil.appDB.walletDao().loadWalletById(walletId)
            DataCenter.wallets.add(wallet)
            DataCenter.wallets.sortWith(kotlin.Comparator{ w1, w2 ->
                return@Comparator (w1.id - w2.id).toInt()
            })
            uiThread {
                backupBtn.isEnabled = true
            }
        }
    }

    private fun timer() {
        val count = 4L
        var subscription: Subscription? = null
        Flowable.interval(0, 1, TimeUnit.SECONDS)
                .onBackpressureBuffer()//背压策略
                .take(count)//循环次数
                .map { aLong ->
                    count - aLong - 1
                }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(object : org.reactivestreams.Subscriber<Long> {
                    override fun onComplete() {

                        subscription?.cancel()//取消订阅，防止内存泄漏

                        backupBtn.isEnabled = true
                        backupBtn.isClickable = true
                        backupBtn.setBackgroundResource(R.drawable.custom_corner_button_bg)
                        backupBtn.setText(R.string.wallet_backup_btn)

                    }

                    override fun onSubscribe(s: Subscription?) {

                        backupBtn.isEnabled = false
                        backupBtn.isClickable = false
                        backupBtn.setBackgroundResource(R.drawable.shape_round_corner_8dp_d4d9de)

                        subscription = s
                        s?.request(Long.MAX_VALUE)

                    }

                    override fun onNext(t: Long?) {
                        backupBtn.text = getString(R.string.wallet_backup_btn) + "（" + t + "s）"
                    }

                    override fun onError(t: Throwable?) {
                        t?.printStackTrace()
                    }
                })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            REQUEST_CODE_BACKUP_DETAIL, CreateWalletActivity.CREATE_CHECK_ALL_SUPPORT,CreateWalletActivity.CREATE_CHECK_FROM_WEB -> {
                if (resultCode == Activity.RESULT_OK) {
                    setResult(Activity.RESULT_OK)
                    finish()
                }
            }
        }


    }


}
