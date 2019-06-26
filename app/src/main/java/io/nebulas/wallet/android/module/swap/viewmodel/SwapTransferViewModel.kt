package io.nebulas.wallet.android.module.swap.viewmodel

import android.arch.lifecycle.Lifecycle
import android.arch.lifecycle.ViewModel
import android.text.TextUtils
import io.nebulas.wallet.android.app.WalletApplication
import io.nebulas.wallet.android.common.Constants
import io.nebulas.wallet.android.module.swap.SwapHelper
import io.nebulas.wallet.android.module.swap.model.ExchangeRecordModel
import io.nebulas.wallet.android.network.callback.OnResultCallBack
import io.nebulas.wallet.android.network.eth.ETHHttpManager
import io.nebulas.wallet.android.network.eth.model.TransactionDetail
import io.nebulas.wallet.android.network.server.HttpManager
import io.nebulas.wallet.android.network.server.model.SwapTransferResp
import io.nebulas.wallet.android.network.subscriber.HttpSubscriber
import io.nebulas.wallet.android.util.Formatter
import java.math.BigDecimal

class SwapTransferViewModel : ViewModel() {

    /**
     * 获取换币详情
     */
    fun getSwapTransferDetail(ethTxHash: String, lifecycle: Lifecycle, onFinished: (SwapTransferResp) -> Unit, onFail: (String) -> Unit) {

        HttpManager.getSwapDetail(ethTxHash, HttpSubscriber(object : OnResultCallBack<SwapTransferResp> {

            override fun onSuccess(t: SwapTransferResp) {
                onFinished(t)
            }

            override fun onError(code: Int, errorMsg: String) {
                super.onError(code, errorMsg)
                onFail(errorMsg)
            }
        }, lifecycle))
    }

    fun getEthTransactionDetailByHash(ethTxHash: String, lifecycle: Lifecycle, onFinished: (TransactionDetail) -> Unit, onFailed: (String) -> Unit) {
        ETHHttpManager.getTransactionDetailByHash(ethTxHash, HttpSubscriber(object : OnResultCallBack<TransactionDetail> {
            override fun onSuccess(t: TransactionDetail) {
                onFinished(t)
            }

            override fun onError(code: Int, errorMsg: String) {
                onFailed(errorMsg)
            }
        }, lifecycle))
    }

    /**
     * 获取换币记录
     */
    fun getSwapTransferList(ethAddress: String, page: Int, pageSize: Int, lifecycle: Lifecycle, onFinished: (any: MutableList<ExchangeRecordModel>) -> Unit, onFail: (String) -> Unit) {

        HttpManager.getSwapList(ethAddress, page.toString(), pageSize.toString(), HttpSubscriber(object : OnResultCallBack<MutableList<SwapTransferResp>> {
            override fun onSuccess(t: MutableList<SwapTransferResp>) {
                if (t.size == 0 && page == 1)
                    onFail(t.toString())
                else
                    onFinished(doTransferList(t))
            }

            override fun onError(code: Int, errorMsg: String) {
                super.onError(code, errorMsg)
                onFail(errorMsg)
            }
        }, lifecycle))
    }

    fun doTransferList(list: MutableList<SwapTransferResp>): MutableList<ExchangeRecordModel> {
        val swapTransactionInfo = SwapHelper.getLastSwapTransactionInfo(WalletApplication.INSTANCE)
        val swapTransferList: MutableList<ExchangeRecordModel> = mutableListOf()
        list.forEach {
            swapTransferList.add(
                    ExchangeRecordModel(
                            time = if (it.send_erc_timestamp == null) {
                                ""
                            } else {
                                Formatter.timeFormat("yyyyMMMdkkmmss", it.send_erc_timestamp!!, true)
                            },
                            status = it.status,
                            amount = if (TextUtils.isEmpty(it.amount) || it.amount.equals("UNKOWN")) {
                                "0.0000"
                            } else {
                                Formatter.tokenFormat(BigDecimal(Formatter.amountFormat(it.amount ?: "0", Constants.TOKEN_SCALE)), Constants.TOKEN_FORMAT_ETH)
                            },
                            gasFee = if (TextUtils.isEmpty(it.eth_gas_used) || it.eth_gas_used.equals("UNKOWN")) {
                                if (it.eth_tx_hash == swapTransactionInfo?.transactionHash) {
                                    Formatter.tokenFormat(BigDecimal(Formatter.amountFormat(swapTransactionInfo?.gasFee?:"0", Constants.TOKEN_SCALE)), Constants.TOKEN_FORMAT_ETH)
                                } else {
                                    ""
                                }
                            } else {
                                Formatter.getGas(it.eth_gas_price!!, it.eth_gas_used!!, 18)
                            },
                            address = it.neb_address
                    )
            )
        }
        return swapTransferList
    }
}