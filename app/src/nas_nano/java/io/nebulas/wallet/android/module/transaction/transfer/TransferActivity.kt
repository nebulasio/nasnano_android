package io.nebulas.wallet.android.module.transaction.transfer

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import com.alibaba.fastjson.JSON
import io.nebulas.wallet.android.R
import io.nebulas.wallet.android.base.BaseActivity
import io.nebulas.wallet.android.common.*
import io.nebulas.wallet.android.dialog.VerifyPasswordDialog
import io.nebulas.wallet.android.invoke.InvokeHelper
import io.nebulas.wallet.android.module.balance.model.Coin
import io.nebulas.wallet.android.module.detail.BalanceDetailActivity
import io.nebulas.wallet.android.module.qrscan.NormalScannerResultActivity
import io.nebulas.wallet.android.module.qrscan.QRScanActivity
import io.nebulas.wallet.android.module.qrscan.ScannerDispatcher
import io.nebulas.wallet.android.module.staking.StakingContractHolder
import io.nebulas.wallet.android.module.transaction.TxDetailActivity
import io.nebulas.wallet.android.module.transaction.view.ConfirmTransferDialog
import io.nebulas.wallet.android.module.transaction.view.GasAdjustDialog
import io.nebulas.wallet.android.module.vote.VoteActivity
import io.nebulas.wallet.android.module.wallet.create.model.Address
import io.nebulas.wallet.android.network.server.HttpManager
import io.nebulas.wallet.android.util.Formatter
import io.nebulas.wallet.android.util.SecurityHelper
import io.nebulas.wallet.android.util.Util
import io.nebulas.wallet.android.view.research.CurtainResearch
import kotlinx.android.synthetic.nas_nano.app_bar_main_no_underline.*
import org.jetbrains.anko.*
import org.jetbrains.annotations.NotNull
import walletcore.Walletcore
import java.math.BigDecimal
import java.util.concurrent.Future

class TransferActivity : BaseActivity() {

    companion object {

        /**
         * 是否有携带地址信息的intent
         */
        const val HAS_INTENT = "hasIntent"
        /**
         * 从哪个activity跳转而来
         */
        const val FROM_ACTIVITY = "fromActivity"
        /**
         * 转账的目标地址
         */
        const val ADDRESS = "account"

        const val KEY_COIN = "key_coin"

        /**
         * 是否有唤起协议跳转
         */
        const val HAS_PROTO = "hasProto"
        /**
         * open QRScanActivity request code
         */
        const val REQUEST_CODE_SCAN_QRCODE = 11001
        /**
         * open singleInputActivity request code
         */
        const val REQUEST_CODE_CHOOSE_TOKEN = 11002

        /**
         * 启动TransferActivity
         *
         * @param context
         * @param requestCode
         * @param hasIntent
         * @param fromActivity
         * @param address
         */
        fun launch(@NotNull context: Activity,
                   @NotNull requestCode: Int,
                   @NotNull hasIntent: Boolean = false,
                   @NotNull fromActivity: String = "",
                   @NotNull address: String = "",
                   hasProto: Boolean = false,
                   coin: Coin? = null) {
            context.startActivityForResult<TransferActivity>(
                    requestCode,
                    HAS_INTENT to hasIntent,
                    FROM_ACTIVITY to fromActivity,
                    ADDRESS to address,
                    HAS_PROTO to hasProto,
                    KEY_COIN to coin)
        }


        fun launchByInvoke(context: Context, parameters: Bundle) {
            val intent = Intent(context, TransferActivity::class.java)
            intent.putExtras(parameters)
            intent.putExtra("isInvoke", true)
            context.startActivity(intent)
            if (context is Activity) {
                context.overridePendingTransition(0, 0)
            }
        }
    }

    private val dataCenter: TransferDataCenter = TransferDataCenter()
    private lateinit var controller: TransferController
    private lateinit var binder: TransferBinder

    private var gasAdjustDialog: GasAdjustDialog? = null
    private var confirmTransferDialog: ConfirmTransferDialog? = null
    private var verifyPWDDialog: VerifyPasswordDialog? = null

    private var future: Future<Unit>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_transfer)
        controller = TransferController(this, this, dataCenter)
        binder = TransferBinder(this, controller, dataCenter)
        handleIntent()
        initViews()
        controller.loadNecessaryData()
        isHasNetWork()
    }

    override fun onDestroy() {
        super.onDestroy()
        future?.apply {
            if (!isCancelled && !isDone) {
                cancel(true)
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        handleIntent()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_CODE_SCAN_QRCODE -> {
                if (resultCode == Activity.RESULT_OK) {
                    data?:return
                    val content = data.getStringExtra(QRScanActivity.SCAN_QRCODE_RESULT_TEXT)
                    try {
                        val json = JSON.parseObject(content)
                        if (json.containsKey("pageParams")) {
                            val pageParams = json.getJSONObject("pageParams")
                            if (!pageParams.containsKey("innerPay")) {
                                // 增加App内部支付字段
                                pageParams["innerPay"] = true
                            }
                            if (pageParams.containsKey("pay")) {
                                val pay = pageParams.getJSONObject("pay")
                                if (ScannerDispatcher.isVoteRequest(pay)) {
                                    val voteRequest = ScannerDispatcher.boxVoteInfo(pay)
                                    VoteActivity.launch(
                                            context = this,
                                            contractAddress = voteRequest.contractAddress,
                                            amountNAT = voteRequest.amountNAT,
                                            function = voteRequest.function,
                                            args = voteRequest.args
                                    )
                                    finish()
                                    return
                                }
                            }
                        }
                    } catch (e:Exception) {
                        // not json data
                    }
                    if (data.getBooleanExtra(QRScanActivity.NEED_GOTO_TRANSFER, false)) {
                        dataCenter.handleIntent(data)
                        controller.loadNecessaryData()
                    } else {
                        if(Util.checkAddress(content, Walletcore.NAS)) {
                            dataCenter.to = content
                        } else {
                            NormalScannerResultActivity.launch(this, content)
                        }
                    }
                }
            }
            REQUEST_CODE_CHOOSE_TOKEN -> {
                if (resultCode == Activity.RESULT_OK) {
                    val resultCoin = data?.getSerializableExtra("coin") as Coin?
                    if (resultCoin != null) {
                        val currentCoin = dataCenter.coin ?: return
                        if (currentCoin.tokenId != null && currentCoin.tokenId != resultCoin.tokenId) {
                            dataCenter.maxBalance.value = "0"
                            dataCenter.maxBalanceFormatted.value = Formatter.tokenFormat(BigDecimal.ZERO)
                            dataCenter.coin = resultCoin
                            val transaction = dataCenter.transaction ?: return
                            transaction.payload?.nasType = if (resultCoin.type == 1) {
                                Walletcore.TxPayloadBinaryType
                            } else {
                                Walletcore.TxPayloadCallType
                            }
                            val coreCoin = DataCenter.coins.first {
                                it.type == 1 && it.platform == resultCoin.platform
                            }
                            val currentCoreCoin = dataCenter.coreCoin ?: return
                            if (currentCoreCoin.tokenId != null && currentCoreCoin.tokenId != coreCoin.tokenId) {
                                dataCenter.coreCoin = coreCoin
                            }
                            controller.loadNecessaryData()
                        }
                    }
                }
            }
            BalanceDetailActivity.REQUEST_CODE_TRANSFER -> {
                if (resultCode == Activity.RESULT_OK) {
                    setResult(Activity.RESULT_OK)
                    finish()
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        firebaseAnalytics?.logEvent(Constants.kATransferVCShowed, Bundle())
    }

    override fun onResume() {
        super.onResume()

//        controller.loadNecessaryData()

    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.qr_scan_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.actionScanQR -> {
                if (!dataCenter.addressEditable) {
                    return false
                }
                QRScanActivity.launch(this, REQUEST_CODE_SCAN_QRCODE)
                firebaseAnalytics?.logEvent(Constants.sendScanClick, Bundle())
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun initView() {}

    private fun handleIntent() {
        dataCenter.handleIntent(intent)
    }

    private fun initViews() {
        showBackBtn(true, toolbar)
        titleTV.setText(R.string.transfer_btn_text)
        binder.bind(contentView!!)
        dataCenter.paySuccess.observe(this) {
            if (it == true) {
                paySuccess()
            }
        }
        dataCenter.payError.observe(this) {
            if (it != null && it.isNotEmpty()) {
                if (getString(R.string.wrong_pwd) == it) {
                    if (SecurityHelper.walletWrongPassword(dataCenter.currentWallet)){
                        verifyPWDDialog?.toastErrorMessage(getString(R.string.tip_password_error_to_lock))
                        mainHandler.postDelayed({
                            verifyPWDDialog?.dismiss()
                        }, 1800)
                    } else {
                        verifyPWDDialog?.toastErrorMessage(it)
                    }
                } else {
                    verifyPWDDialog?.toastErrorMessage(it)
                }
            }
            firebaseAnalytics?.logEvent(Constants.kATransferFailed, Bundle())
            verifyPWDDialog?.reset()
        }
        dataCenter.error.observe(this) {
            when (it) {
                TransferDataCenter.ERROR_COIN_NOT_SUPPORT -> {
                    toastErrorMessage(R.string.not_support_this_currency)
                    finish()
                }
                TransferDataCenter.ERROR_INVALID_ADDRESS -> {
                    toastErrorMessage(R.string.wrong_address_content)
                }
                TransferDataCenter.ERROR_UNKNOW_ERROR -> {
                    finish()
                }
            }
        }
        dataCenter.autoConfirm.observe(this) {
            if (it == true) {
                val amountNas = dataCenter.value ?: return@observe
                val coin = dataCenter.coin ?: return@observe
                val amountWei = BigDecimal(amountNas).multiply(BigDecimal(10)
                        .pow(coin.tokenDecimals.toInt()))
                        .setScale(0)
                        .stripTrailingZeros()
                        .toPlainString()
                interceptConfirmTransaction(amountWei)
            }
        }
    }

    private fun paySuccess() {
        if (confirmTransferDialog?.isShowing == true) {
            confirmTransferDialog?.dismiss()
        }
        firebaseAnalytics?.logEvent(Constants.kATransferSuccess, Bundle())
        verifyPWDDialog?.toastSuccessMessage(getString(R.string.transfer_send_succ))
        doAsync {
            Thread.sleep(1500)
            uiThread {
                if (null != verifyPWDDialog && verifyPWDDialog?.isShowing == true) {
                    verifyPWDDialog?.directDismiss()
                }
                val bundle = Bundle()
                dataCenter.transaction?.apply {
                    hash?.apply {
                        bundle.putString("hash", this)
                    }
                }
                if (!InvokeHelper.goBack(this@TransferActivity, bundle)) {
                    if (dataCenter.dAppTransferJson != null) {
                        if (!dataCenter.innerPay) {
                            setResult(Activity.RESULT_OK)
                            finish()
                        } else {
                            showTipsDialog("",
                                    getString(R.string.dapp_pay_succ),
                                    getString(R.string.dapp_pay_succ_detail_btn),
                                    {
                                        setResult(Activity.RESULT_OK)
                                        finish()
                                        /**
                                         * 跳转到交易详情
                                         */
                                        TxDetailActivity.launch(this@TransferActivity,
                                                dataCenter.coin!!,
                                                dataCenter.transaction!!)
                                        overridePendingTransition(R.anim.enter_bottom_in, R.anim.exit_bottom_out)
                                    },
                                    getString(R.string.dapp_pay_succ_to_dapp_btn),
                                    {
                                        setResult(Activity.RESULT_OK)
                                        finish()
                                    })
                        }
                    } else {
                        setResult(Activity.RESULT_OK)
                        finish()
                        /**
                         * 跳转到交易详情
                         */
                        TxDetailActivity.launch(this@TransferActivity,
                                dataCenter.coin!!,
                                dataCenter.transaction!!)
                        overridePendingTransition(R.anim.enter_bottom_in, R.anim.exit_bottom_out)
                    }
                }
            }
        }
    }

    fun adjustGas() {
        val gasPriceResp = dataCenter.gasPriceResp ?: return
        val transaction = dataCenter.transaction ?: return
        if (null == gasAdjustDialog) {
            gasAdjustDialog = GasAdjustDialog(this,
                    dataCenter.coreCoin!!,
                    gasPriceResp.gasPriceMin ?: "0",
                    gasPriceResp.gasPriceMax ?: "0",
                    gasPriceResp.estimateGas ?: "0",
                    "",
                    dataCenter.gasSymbol,
                    dataCenter.gasCoinPrice.value
            ) {
                transaction.gasPrice = it
                dataCenter.transaction = transaction
            }
        }
        gasAdjustDialog?.show()

        //GA
        firebaseAnalytics?.logEvent(Constants.kATransferGasClick, Bundle())
    }

    fun interceptConfirmTransaction(amount: String){
        future?.apply {
            if (!isCancelled && !isDone) {
                cancel(true)
            }
        }
        future = doAsync {
            dataCenter.isLoading = true
            val pledgedWalletInfo = controller.getPledgedWallet()
            val pledgedMap = mutableMapOf<String, String>()
            pledgedWalletInfo?.forEach {
                val address = it.address
                address?:return@forEach
                pledgedMap[address] = it.info?.value?:"0"
            }
            dataCenter.isLoading = false
            uiThread {
                confirmTransaction(amount, pledgedMap)
            }
        }
    }

    fun confirmTransaction(amount: String, pledgedInfo:MutableMap<String, String> = mutableMapOf()) {
        confirmTransferDialog = ConfirmTransferDialog(activity = this,
                curCoin = dataCenter.coin!!,
                gasSymbol = dataCenter.gasSymbol,
                coinPrice = dataCenter.coinPrice,
                gasCoinPrice = dataCenter.gasCoinPrice.value,
                onSendTransfer = { wallet ->
                    if (SecurityHelper.isWalletLocked(wallet)) {
                        val d = confirmTransferDialog
                        if (d!=null) {
                            CurtainResearch.create(d)
                                    .withLevel(CurtainResearch.CurtainLevel.ERROR)
                                    .withContent(getString(R.string.tip_password_has_locked))
                                    .show()
                        }
                        return@ConfirmTransferDialog
                    }
                    if (null == verifyPWDDialog) {
                        verifyPWDDialog = VerifyPasswordDialog(activity = this,
                                title = getString(R.string.payment_password_text),
                                passwordType = if (wallet.isComplexPwd) PASSWORD_TYPE_COMPLEX else PASSWORD_TYPE_SIMPLE,
                                onNext = { passPhrase ->
                                    dataCenter.currentWallet = wallet
                                    val address: Address = DataCenter.addresses.firstOrNull { dataCenter.coin!!.platform == it.platform && it.walletId == wallet.id }!!
                                    val transaction = dataCenter.transaction
                                            ?: return@VerifyPasswordDialog
                                    transaction.account = address.address
                                    transaction.sender = address.address

                                    if (transaction.receiver == transaction.account) {
                                        showTipsDialog(getString(R.string.alert_tips),
                                                getString(R.string.send_to_self_tips),
                                                getString(R.string.cancel_btn),
                                                {
                                                    verifyPWDDialog?.dismiss()
                                                    confirmTransferDialog?.hideProgressBar()
                                                },
                                                getString(R.string.confirm_btn),
                                                {
                                                    controller.doPay(address, passPhrase)
                                                })
                                    } else {
                                        controller.doPay(address, passPhrase)
                                    }
                                },
                                onDismiss = {
                                    confirmTransferDialog?.show()
                                })
                    }
                    confirmTransferDialog?.dismiss()
                    verifyPWDDialog?.show()
                    firebaseAnalytics?.logEvent(Constants.kATransferConfirmViewConfirmClick, Bundle())
                },
                pledgedInfo = pledgedInfo)
        confirmTransferDialog?.setCanceledOnTouchOutside(false)
        val transaction = dataCenter.transaction ?: return
        transaction.amount = amount
        confirmTransferDialog?.transaction = transaction
        confirmTransferDialog?.gasFee = Formatter.getGas(transaction.gasPrice, dataCenter.gasPriceResp?.estimateGas
                ?: "20000", dataCenter.coreCoin!!.tokenDecimals.toInt())
        confirmTransferDialog?.show()
    }
}