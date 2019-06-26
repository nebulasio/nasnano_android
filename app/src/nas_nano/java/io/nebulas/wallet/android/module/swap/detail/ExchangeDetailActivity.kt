package io.nebulas.wallet.android.module.swap.detail

import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.text.Html
import android.view.Menu
import android.view.MenuItem
import android.view.View
import io.nebulas.wallet.android.R
import io.nebulas.wallet.android.base.BaseActivity
import io.nebulas.wallet.android.common.Constants
import io.nebulas.wallet.android.common.DataCenter
import io.nebulas.wallet.android.extensions.logD
import io.nebulas.wallet.android.module.swap.SwapHelper
import io.nebulas.wallet.android.module.swap.SwapRouter
import io.nebulas.wallet.android.module.swap.viewmodel.SwapTransferViewModel
import io.nebulas.wallet.android.network.server.model.SwapTransferResp
import io.nebulas.wallet.android.util.Formatter
import kotlinx.android.synthetic.nas_nano.activity_exchange_detail.*
import org.jetbrains.anko.startActivityForResult
import org.jetbrains.annotations.NotNull
import java.math.BigDecimal
import java.math.BigInteger
import java.util.*

class ExchangeDetailActivity : BaseActivity() {

    companion object {
        const val SWAP_TRANSACTION = "swap_transaction"
        private const val PARAMETER_STATUS = "parameter_status"

        fun launch(@NotNull context: Context, requestCode: Int, swapTransactionInfo: SwapHelper.SwapTransactionInfo, status: Int = SwapHelper.SWAP_STATUS_IN_PROCESSING) {
            (context as AppCompatActivity).startActivityForResult<ExchangeDetailActivity>(requestCode,
                    SWAP_TRANSACTION to swapTransactionInfo,
                    PARAMETER_STATUS to status)
        }
    }

    private lateinit var swapTransferViewModel: SwapTransferViewModel
    private lateinit var swapTransactionInfo: SwapHelper.SwapTransactionInfo

    private var swapWalletInfo: SwapHelper.SwapWalletInfo? = null
    private var swapStatus = SwapHelper.SWAP_STATUS_IN_PROCESSING

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (window.decorView.systemUiVisibility != View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_STABLE) {
            //状态栏图标和文字颜色为浅色
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        }
        setContentView(R.layout.activity_exchange_detail)
    }

    override fun initView() {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        titleTV.text = getString(R.string.token_swap_details)
        swapWalletInfo = SwapHelper.getSwapWalletInfo(this)
        swapTransactionInfo = intent.getSerializableExtra(SWAP_TRANSACTION) as SwapHelper.SwapTransactionInfo
        swapTransferViewModel = ViewModelProviders.of(this).get(SwapTransferViewModel::class.java)
        initData()
        confirmBtn.setOnClickListener {
            SwapRouter.reExchange(this)
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        loadData()
    }

    private fun loadData() {
        isHasNetWork()
        loadingView.visibility = View.VISIBLE
        swapTransferViewModel.getSwapTransferDetail(swapTransactionInfo.transactionHash, lifecycle, {
            if (it.status == 1) { //换币成功，直接更新UI
                SwapHelper.setCurrentSwapStatus(this, SwapHelper.SWAP_STATUS_SUCCESS)
            } else { //请求RPC拉取ETH交易状态
                getEthTransactionStatus()
            }
            setData(it)
        }, {
            //toastErrorMessage(it)
            loadingView.visibility = View.GONE
        })
    }

    private fun getEthTransactionStatus() {
        swapTransferViewModel.getEthTransactionDetailByHash(swapTransactionInfo.transactionHash, lifecycle, {
            loadingView.visibility = View.GONE
            val status = it.status ?: return@getEthTransactionDetailByHash
            val realIntStatus = status.replace("0x", "")
            try {
                val realStatus = BigInteger(realIntStatus)
                if (realStatus.toInt() == 0) {
                    //ETH交易失败
                    SwapHelper.setCurrentSwapStatus(this, SwapHelper.SWAP_STATUS_FAILED)
                    swapFailed()
                } else {
                    //成功或其他未知状态，不做处理
                }
            } catch (e: Exception) {

            }
        }, {
            loadingView.visibility = View.GONE
            logD("ERROR : $it")
        })
    }

    private fun initData() {
        swapStatus = intent.getIntExtra(PARAMETER_STATUS, SwapHelper.SWAP_STATUS_IN_PROCESSING)
        if (swapStatus != SwapHelper.SWAP_STATUS_SUCCESS) {
            swapStatus = SwapHelper.getCurrentSwapStatus(this)
        }
        resultDes.visibility = View.VISIBLE
        var imgRes = R.drawable.ic_pendding
        var statusStringRes = R.string.swap_state_waiting
        when (swapStatus) {
            SwapHelper.SWAP_STATUS_SUCCESS -> {
                imgRes = R.drawable.icon_success
                statusStringRes = R.string.swap_state_success
                resultDes.visibility = View.INVISIBLE
            }
            SwapHelper.SWAP_STATUS_FAILED -> {
                imgRes = R.drawable.icon_failed
                statusStringRes = R.string.swap_status_failed
                resultDes.setText(R.string.swap_state_fail_des)
            }
            else -> {
                val desc = SwapHelper.getProcessingDescription(this)
                resultDes.text = desc
            }
        }
        resultTV.text = getString(statusStringRes)
        swapStateImg.setImageResource(imgRes)
        confirmBtn.text = getString(R.string.swap_again)
        confirmBtn.isClickable = false
        confirmBtn.isEnabled = false
        addressTV.text = swapWalletInfo?.nasWalletAddress
        amount.text = Formatter.tokenFormat(BigDecimal(Formatter.amountFormat(swapTransactionInfo.erc20Amount, Constants.TOKEN_SCALE)), Constants.TOKEN_FORMAT_ETH)
        gasFee.text = Formatter.tokenFormat(BigDecimal(Formatter.amountFormat(swapTransactionInfo.gasFee, Constants.TOKEN_SCALE)), Constants.TOKEN_FORMAT_ETH)
        createTime.text = Formatter.timeFormat("yyyyMMMdkkmmss", swapTransactionInfo.startTime, true)
        try {
            val coin = DataCenter.coins.first { it.address == swapWalletInfo?.nasWalletAddress }
            walletNameTV.text = DataCenter.wallets.first { coin.walletId == it.id }?.walletName
        } catch (e: Exception) {
            finish()
        }
    }

    fun setData(swapTransferResp: SwapTransferResp) {
        confirmBtn.isClickable = true
        confirmBtn.isEnabled = true
        loadingView.visibility = View.GONE
        if (swapTransferResp.status == 1) {
            resultDes.visibility = View.INVISIBLE
            resultTV.text = getString(R.string.swap_state_success)
            swapStateImg.setImageResource(R.drawable.icon_success)
            confirmBtn.text = getString(R.string.swap_again)
        } else if (swapTransferResp.status == 0) {
            swapFailed()
        } else {
//            val currentSwapStatus = SwapHelper.getCurrentSwapStatus(this)
//            val imgRes = when (currentSwapStatus) {
//                SwapHelper.SWAP_STATUS_FAILED -> R.drawable.ic_fail
//                else -> R.drawable.ic_pendding
//            }
            resultDes.visibility = View.VISIBLE
            val desc = SwapHelper.getProcessingDescription(this)
            resultDes.text = desc
            resultTV.text = getString(R.string.swap_state_waiting)
            swapStateImg.setImageResource(R.drawable.ic_pendding)
            confirmBtn.text = getString(R.string.swap_again)
            confirmBtn.isClickable = false
            confirmBtn.isEnabled = false
        }

        amount.text = Formatter.tokenFormat(BigDecimal(Formatter.amountFormat(swapTransferResp.amount
                ?: swapTransactionInfo.erc20Amount, Constants.TOKEN_SCALE)), Constants.TOKEN_FORMAT_ETH)

        val gas = if (!swapTransferResp.eth_gas_used.isNullOrEmpty()
                && swapTransferResp.eth_gas_used != "UNKOWN"
                && !swapTransferResp.eth_gas_price.isNullOrEmpty()
                && swapTransferResp.eth_gas_price != "UNKOWN") {
            BigDecimal(swapTransferResp.eth_gas_used).multiply(BigDecimal(swapTransferResp.eth_gas_price))
        } else {
            BigDecimal(swapTransactionInfo.gasFee)
        }
        gasFee.text = Formatter.tokenFormat(BigDecimal(Formatter.amountFormat(gas.toPlainString(), Constants.TOKEN_SCALE)), Constants.TOKEN_FORMAT_ETH)
        val startTime = if (swapTransferResp.send_erc_timestamp == null || swapTransferResp.send_erc_timestamp == 0L) {
            swapTransactionInfo.startTime
        } else {
            swapTransferResp.send_erc_timestamp ?: 0L
        }
        createTime.text = Formatter.timeFormat("yyyyMMMdkkmmss", startTime, true)

        if (gas != null && startTime > 0L) {
            SwapHelper.updateLastSwapTransactionInfo(context = this@ExchangeDetailActivity, startTime = startTime, gasFee = gas.toPlainString())
        }
    }


    private fun swapFailed() {
        resultTV.text = getString(R.string.swap_state_fail)
        resultDes.visibility = View.VISIBLE
        resultDes.setText(R.string.swap_state_fail_des)
        swapStateImg.setImageResource(R.drawable.icon_failed)
        confirmBtn.text = getString(R.string.swap_retry)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.swap_records_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.swapRecords -> {
                ExchangeRecordsActivity.launch(this, ethAddress = swapWalletInfo?.swapWalletAddress!!)
            }
        }
        return super.onOptionsItemSelected(item)
    }
}