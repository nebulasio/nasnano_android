package io.nebulas.wallet.android.module.transaction

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.databinding.DataBindingUtil
import android.graphics.Paint
import android.net.Uri
import android.os.Bundle
import android.support.annotation.WorkerThread
import android.view.View
import com.young.polling.PollingFutureTask
import com.young.polling.SyncManager
import io.nebulas.wallet.android.R
import io.nebulas.wallet.android.base.BaseActivity
import io.nebulas.wallet.android.common.Constants
import io.nebulas.wallet.android.common.DataCenter
import io.nebulas.wallet.android.common.TransactionPollingTaskInfo
import io.nebulas.wallet.android.common.URLConstants
import io.nebulas.wallet.android.databinding.ActivityTxDetailBinding
import io.nebulas.wallet.android.db.DBUtil
import io.nebulas.wallet.android.module.balance.model.Coin
import io.nebulas.wallet.android.module.balance.viewmodel.BalanceViewModel
import io.nebulas.wallet.android.module.transaction.model.Transaction
import io.nebulas.wallet.android.module.transaction.viewmodel.TxViewModel
import io.nebulas.wallet.android.network.server.HttpManager
import io.nebulas.wallet.android.util.Converter
import io.nebulas.wallet.android.util.Formatter
import io.nebulas.wallet.android.util.Util
import kotlinx.android.synthetic.nas_nano.activity_tx_detail.*
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.startActivity
import org.jetbrains.anko.uiThread
import org.jetbrains.annotations.NotNull
import org.reactivestreams.Subscription
import java.math.BigDecimal

class TxDetailActivity : BaseActivity(), PollingFutureTask.PollingResultCallback<Transaction>, PollingFutureTask.PollingCompleteCallback<Transaction> {
    override fun onCompleted(tId: String, lastPollingResult: Transaction?) {
        lastPollingResult?.apply {
            txViewModel.setTx(this)
        }
    }

    override fun onPollingResult(tId: String, result: Transaction?) {
        result?.apply {
            txViewModel.setTx(this)
        }
    }

    companion object {
        const val COIN = "coin"
        const val TRANSACTION = "transaction"

        /**
         * 启动TxDetailActivity
         *
         * @param context
         * @param coin
         * @param tx
         */
        fun launch(@NotNull context: Context, @NotNull coin: Coin, @NotNull tx: Transaction) {
            DataCenter.setData(TRANSACTION, tx)
            context.startActivity<TxDetailActivity>(COIN to coin)
        }
    }

    var binding: ActivityTxDetailBinding? = null

    lateinit var balanceViewModel: BalanceViewModel
    lateinit var txViewModel: TxViewModel

    lateinit var coin: Coin
    lateinit var tx: Transaction
    var isSend = false

    /**
     * token兑换法币费率
     */
    var coinPrice: BigDecimal? = null

    private var gasCoinPrice: BigDecimal? = null

    private lateinit var coreCoin: Coin

    var gasSymbol: String = ""

    /**
     * transaction的进度查询订阅
     */
    var subscription: Subscription? = null
    var qrCodeContent: String? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView<ActivityTxDetailBinding>(this, R.layout.activity_tx_detail)
        isHasNetWork()
        initView()
//        setContentView(R.layout.activity_tx_detail)
    }

    override fun initView() {
        showBackBtn(false, toolbar)
        titleTV.setText(R.string.tx_detail_title)

        copyLinkBtn.paint.flags = Paint.UNDERLINE_TEXT_FLAG
        hashTV.paint.flags = Paint.UNDERLINE_TEXT_FLAG

        coin = intent.getSerializableExtra(COIN) as Coin
        coinPrice = BigDecimal(coin.currencyPrice)

        try {
            coreCoin = DataCenter.coins.first {
                it.type == 1 && it.platform == coin.platform
            }
        } catch (e: Exception) {
            finish()
            return
        }
        gasCoinPrice = BigDecimal(coreCoin.currencyPrice)

        // 初始化gas单位
        gasSymbol = if (coin.type == 1)
            coin.symbol
        else {
            val temp = DataCenter.coins.filter {
                it.platform == coin.platform && it.type == 1
            }
            if (temp.isNotEmpty())
                temp[0].symbol
            else
                ""
        }
        binding?.gasSymbol = gasSymbol

        tx = if (DataCenter.containsData(TRANSACTION))
            DataCenter.getData(TRANSACTION, false) as Transaction
        else
            Transaction()

        if (coinPrice?.toDouble() ?: 0.0 == 0.0) {
            updateCurrencyPrice(coin.tokenId)
        }

        isSend = tx.isSend

        amountTV.maxWidth = Util.screenWidth(this) * 8 / 10
        gasFeeTV.maxWidth = amountTV.maxWidth

        balanceViewModel = ViewModelProviders.of(this).get(BalanceViewModel::class.java)
        txViewModel = ViewModelProviders.of(this).get(TxViewModel::class.java)

        tx.currencySymbol = Constants.CURRENCY_SYMBOL
        binding?.tx = tx

        txViewModel.setTx(tx)
        qrCodeContent = if (packageName == "io.nebulas.wallet.android") {
            URLConstants.EXPLORER_TX_DETAIL_PREFIX + tx.hash
        } else {
            URLConstants.EXPLORER_TX_DETAIL_PREFIX_TEST_NET + tx.hash
        }

        Converter.loadTXQRCode(qr_code_iv, qrCodeContent)

        txViewModel.getTx().observe(this, Observer {
            if (it == null) {
                return@Observer
            }
            it.isSend = tx.isSend
            if (tx.remark.isNotEmpty())
                it.remark = tx.remark
            tx = it

            tx.account = if (isSend) tx.sender else tx.receiver

            tx.txFeeCurrency = if ((null != gasCoinPrice && gasCoinPrice != BigDecimal.ZERO) && null != tx.txFee && tx.txFee!!.isNotEmpty()) BigDecimal(tx.txFee).divide(BigDecimal(10).pow(coreCoin.tokenDecimals.toInt())).multiply(gasCoinPrice).stripTrailingZeros().toPlainString() else "0"

            tx.currencySymbol = Constants.CURRENCY_SYMBOL
            binding?.tx = tx
            val exploreUrl = tx.explorerURL
            if (null != exploreUrl && exploreUrl.isNotEmpty()) {
                if (qrCodeContent != exploreUrl) {
                    qrCodeContent = exploreUrl
                    Converter.loadTXQRCode(qr_code_iv, qrCodeContent)
                }
            }
            refreshCurrencyPriceView()
        })

        val fromAddressCopyLis = View.OnClickListener {
            (getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).primaryClip = ClipData.newPlainText("txFromAddress", from_address_tv.text)
            toastSuccessMessage(R.string.text_copy_successful)
        }
        tv_address_from_text.setOnClickListener(fromAddressCopyLis)
        from_address_tv.setOnClickListener(fromAddressCopyLis)

        val toAddressCopyLis = View.OnClickListener {
            (getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).primaryClip = ClipData.newPlainText("txTargetAddress", target_address_tv.text)
            toastSuccessMessage(R.string.text_copy_successful)
        }
        tv_address_target_text.setOnClickListener(toAddressCopyLis)
        target_address_tv.setOnClickListener(toAddressCopyLis)

        /**
         * 跳转浏览器打开交易详情页
         */
        hashTV.setOnClickListener {
            qrCodeContent?.apply {
                val intent = Intent()
                intent.action = "android.intent.action.VIEW"
                intent.data = Uri.parse(this)
                startActivity(intent)
            }
        }

        /**
         * 复制链接
         */
        copyLinkBtn.setOnClickListener {
            qrCodeContent?.apply {
                (getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).primaryClip = ClipData.newPlainText("txDetail", qrCodeContent)
                toastSuccessMessage(R.string.succ_copy_tx_hash_to_clipBoard)
            }
        }

    }

    override fun onResume() {
        super.onResume()

        progressBar.visibility = View.VISIBLE

        txViewModel.getTxDetails(tx, lifecycle) {
            progressBar.visibility = View.GONE
            txViewModel.setTx(tx)
            if (!tx.confirmed && tx.status != "fail")
                transactionRequestTimer()
        }

    }

    private fun updateCurrencyPrice(tokenId: String?) {
        tokenId ?: return
        doAsync {
            val currencyPrice = getCurrencyPrice(tokenId)
            coinPrice = try {
                BigDecimal(currencyPrice)
            } catch (e: Exception) {
                BigDecimal.ZERO
            }
            uiThread {
                refreshCurrencyPriceView()
            }
        }
    }

    private fun refreshCurrencyPriceView(){
        val value: BigDecimal = if (null != coinPrice && null != tx.amount && tx.amount!!.isNotEmpty()) BigDecimal(tx.amount).divide(BigDecimal(10).pow(tx.tokenDecimals.toInt())).multiply(coinPrice) else BigDecimal.ZERO
        amountValueTV.text = if (coinPrice == null || coinPrice?.toDouble() ?: 0.00 <= 0.00) "-" else "≈${tx.currencySymbol}${Formatter.priceFormat(value)}"
    }

    @WorkerThread
    private fun getCurrencyPrice(currencyId: String): String {
        val api = HttpManager.getServerApi()
        val call = api.getCurrencyPriceWithoutRX(HttpManager.getHeaderMap(), listOf(currencyId))
        val response = call.execute()
        val priceList = response.body()?.data?.priceList
        return if (priceList != null && priceList.isNotEmpty()) {
            priceList[0].price
        } else {
            "0.00"
        }
    }

    /**
     * 轮询查rpc接口确认交易状态
     */
    private fun transactionRequestTimer() {
        val taskInfo = TransactionPollingTaskInfo(tx, this, this, this)
        SyncManager.sync(taskInfo, this)
    }

    override fun onPause() {
        super.onPause()

        /**
         * 取消订阅事件
         */
        subscription?.cancel()

        if (!tx.confirmed) {
            doAsync {
                if (tx.txData.isNullOrEmpty()) {
                    tx.txData = ""
                }
                DBUtil.appDB.transactionDao().insertTransaction(tx)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        DataCenter.removeData(TRANSACTION)
    }

}