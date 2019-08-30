package io.nebulas.wallet.android.module.transaction.transfer

import android.content.Intent
import android.os.Bundle
import com.young.binder.AbstractDataCenter
import com.young.binder.lifecycle.Data
import io.nebulas.wallet.android.common.Constants
import io.nebulas.wallet.android.common.DataCenter
import io.nebulas.wallet.android.module.balance.model.Coin
import io.nebulas.wallet.android.module.launch.H5RaiseDeliverActivity
import io.nebulas.wallet.android.module.main.MainActivity
import io.nebulas.wallet.android.module.qrscan.QRScanActivity
import io.nebulas.wallet.android.module.transaction.model.Transaction
import io.nebulas.wallet.android.module.transaction.transfer.TransferActivity.Companion.ADDRESS
import io.nebulas.wallet.android.module.transaction.transfer.TransferActivity.Companion.FROM_ACTIVITY
import io.nebulas.wallet.android.module.transaction.transfer.TransferActivity.Companion.HAS_INTENT
import io.nebulas.wallet.android.module.transaction.transfer.TransferActivity.Companion.HAS_PROTO
import io.nebulas.wallet.android.module.wallet.create.model.Wallet
import io.nebulas.wallet.android.network.server.model.GasPriceResp
import io.nebulas.wallet.android.util.Formatter
import org.json.JSONObject
import walletcore.Payload
import walletcore.Walletcore
import java.math.BigDecimal

class TransferDataCenter : AbstractDataCenter() {

    companion object {
        const val ERROR_NO_ERROR = 0
        const val ERROR_UNKNOW_ERROR = -1
        const val ERROR_COIN_NOT_SUPPORT = 10001
        const val ERROR_INVALID_ADDRESS = 10002

        const val EVENT_LOADING_STATUS_CHANGED = "event_loading_status_changed"
        const val EVENT_TRANSACTION_INFO_CHANGED = "event_transaction_info_changed"
        const val EVENT_ADDRESS_CHANGED = "event_address_changed"
        const val EVENT_VALUE_CHANGED = "event_value_changed"
        const val EVENT_COIN_CHANGED = "event_coin_changed"
        const val EVENT_CORE_COIN_CHANGED = "event_core_coin_changed"
        const val EVENT_ADDRESS_EDITABLE_CHANGED = "event_address_editable_changed"
    }

    var isLoading: Boolean = false
        set(value) {
            field = value
            notifyDataChanged(EVENT_LOADING_STATUS_CHANGED)
        }

    var error: Data<Int> = Data(ERROR_NO_ERROR)

    val autoConfirm: Data<Boolean> = Data(false)

    var paySuccess: Data<Boolean> = Data(false)
    var payError: Data<String> = Data()

    val maxBalance:Data<String> = Data("0")
    val maxBalanceFormatted:Data<String> = Data("0")

    var addressEditable: Boolean = true
        set(value) {
            field = value
            notifyDataChanged(EVENT_ADDRESS_EDITABLE_CHANGED)
        }

    var to: String? = null
        set(value) {
            field = value
            notifyDataChanged(EVENT_ADDRESS_CHANGED)
        }
    var value: String? = null
        set(value) {
            field = value
            notifyDataChanged(EVENT_VALUE_CHANGED)
        }
    var serialNumber: String? = null
    var callback: String? = null
    var currency: String? = null
    var gasPriceResp: GasPriceResp? = null
        set(value) {
            field = value
            transaction?.gasPrice = gasPriceResp?.gasPriceMin ?: "0"
            transaction?.gasLimit = (BigDecimal(gasPriceResp?.estimateGas
                    ?: "0") + BigDecimal(10000)).stripTrailingZeros().toPlainString()
        }
    var raiseUpPayload: Payload? = null
    var raiseGasPrice: String? = null
    var raiseGasLimit: String? = null
    var gotoTransferDialog: Boolean = false
    var dAppTransferJson: JSONObject? = null

    var coin: Coin? = null
        set(value) {
            field = value
            notifyDataChanged(EVENT_COIN_CHANGED)
            updateTransactionInfo()
        }
    var coreCoin: Coin? = null

    private fun updateTransactionInfo() {
        val t = transaction ?: return
        val finalCoin = coin ?: return
        t.apply {
            currencyId = finalCoin.tokenId
            coinSymbol = finalCoin.symbol
            platform = finalCoin.platform
            contractAddress = finalCoin.contractAddress
            tokenDecimals = finalCoin.tokenDecimals
        }
        transaction = t
    }

    lateinit var gasSymbol: String
    var transaction: Transaction? = null
        set(value) {
            field = value
            notifyDataChanged(EVENT_TRANSACTION_INFO_CHANGED)
        }

    /**
     * 是否为App内支付
     */
    var innerPay = false
    /**
     * token兑换法币费率
     */
    var coinPrice: BigDecimal? = BigDecimal(0)
    var gasCoinPrice: Data<BigDecimal> = Data(BigDecimal.ZERO)
    lateinit var currentWallet: Wallet


    fun handleIntent(intent: Intent) {
        if (intent.getBooleanExtra("isInvoke", false)) {
            //新版本SDK唤起
            gotoTransferDialog = true
            val parameters = intent.extras ?: Bundle()
            serialNumber = parameters.getString("serialNumber")
            callback = parameters.getString("callback")

            to = parameters.getString("to")
            addressEditable = false
            value = parameters.getString("value")
            currency = parameters.getString("currency")

            raiseGasPrice = parameters.getString("gasPrice")
            raiseGasLimit = parameters.getString("gasLimit")

            raiseUpPayload = Payload().apply {
                nasSource = parameters.getString("source")    //智能合约源码
                nasSourceType = parameters.getString("sourceType")    //智能合约源码Type - 透传参数，不用处理
                nasFunction = parameters.getString("function")
                nasArgs = parameters.getString("args")
                nasType = parameters.getString("type")    //交易类型：普通转账、智能合约部署、智能合约调用
            }

            var hasCoin = false
            run breakPoint@{
                DataCenter.coins.forEach {
                    if (it.symbol == currency) {
                        hasCoin = true
                        coin = it
                        return@breakPoint
                    }
                }
            }
            if (!hasCoin) {
                error.value = ERROR_COIN_NOT_SUPPORT
                return
            }
        } else {
            coin = intent.getSerializableExtra(TransferActivity.KEY_COIN) as Coin?
            //老版本逻辑
            if (intent.getBooleanExtra(HAS_INTENT, false)) {
                when (intent.getStringExtra(FROM_ACTIVITY)) {
                    QRScanActivity::class.java.name -> {    //扫描二维码而来
                        gotoTransferDialog = intent.getBooleanExtra(HAS_PROTO, false)
                        if (gotoTransferDialog) {
                            addressEditable = false
                            handleProto()
                        } else {
                            to = intent.getStringExtra(ADDRESS)
                        }
                    }
                    "SelectTransferTargetActivity" -> {  //转账历史而来
                        to = intent.getStringExtra(ADDRESS)
                        addressEditable = false
                    }
                    MainActivity::class.java.name, H5RaiseDeliverActivity::class.java.name -> { //首页而来（通过应用唤起跳转过来）
                        gotoTransferDialog = intent.getBooleanExtra(HAS_PROTO, false)
                        if (gotoTransferDialog) {
                            addressEditable = false
                            handleProto()
                        }
                    }
                }
            }
        }
        val finalCoin = coin
        if (finalCoin == null) {
            error.value = ERROR_COIN_NOT_SUPPORT
            return
        }

        coreCoin = DataCenter.coins.find {
            it.type == 1 && it.platform == finalCoin.platform
        }

        if (coreCoin == null) {
            error.value = ERROR_UNKNOW_ERROR
            return
        }

        gasSymbol = if (finalCoin.type == 1)
            finalCoin.symbol
        else {
            val temp = DataCenter.coins.filter {
                it.platform == finalCoin.platform && it.type == 1
            }
            if (temp.isNotEmpty())
                temp[0].symbol
            else
                ""
        }
        gasPriceResp = when (finalCoin.platform) {
            Walletcore.NAS -> {
                GasPriceResp(gasPriceMin = "20000000000", estimateGas = "20000", gasPriceMax = "10000000")
            }
            Walletcore.ETH -> {
                GasPriceResp(gasPriceMin = "1000000000", estimateGas = "21000", gasPriceMax = "10000000000")
            }
            else -> {
                GasPriceResp(gasPriceMin = "20000000000", estimateGas = "20000", gasPriceMax = "10000000")
            }
        }
        transaction = Transaction(
                currencyId = finalCoin.tokenId,
                coinSymbol = finalCoin.symbol,
                blockChainType = finalCoin.platform,
                account = finalCoin.address,
                targetAddress = to,
                contractAddress = finalCoin.contractAddress,
                amount = "0",
                txData = "",
                gasPrice = gasPriceResp?.gasPriceMin ?: "0",
                gasLimit = (BigDecimal(gasPriceResp?.estimateGas
                        ?: "0") + BigDecimal(10000)).stripTrailingZeros().toPlainString(),
                isSend = true
        )
        if (null != raiseUpPayload)
            transaction?.payload = raiseUpPayload
        coin?.apply {
            transaction?.payload?.nasType = if (this.type==1) Walletcore.TxPayloadBinaryType else Walletcore.TxPayloadCallType
        }
        transaction?.tokenDecimals = finalCoin.tokenDecimals
        transaction?.nonce = gasPriceResp?.nonce ?: "0"
    }

    private fun handleProto() {
        dAppTransferJson = JSONObject(DataCenter.getData(Constants.KEY_DAPP_TRANSFER_JSON) as String)
        if (dAppTransferJson?.has("innerPay") == true) {
            innerPay = dAppTransferJson?.get("innerPay") as Boolean
        }
        raiseUpPayload = Payload()

        coin = DataCenter.coins.find {
            it.symbol == dAppTransferJson!!.optJSONObject("pay").optString("currency")
        }

        if (null == coin) {
            error.value = ERROR_COIN_NOT_SUPPORT
            return
        }
        val finalCoin = coin!!
        if (dAppTransferJson?.optJSONObject("pay")?.has("gasPrice") == true) {
            raiseGasPrice = dAppTransferJson?.optJSONObject("pay")?.optString("gasPrice")
        }
        if (dAppTransferJson?.optJSONObject("pay")?.has("gasLimit") == true) {
            raiseGasLimit = dAppTransferJson?.optJSONObject("pay")?.optString("gasLimit")
        }
        callback = dAppTransferJson?.optString("callback")
        serialNumber = dAppTransferJson?.optString("serialNumber")
        val payloadJson = dAppTransferJson?.optJSONObject("pay")?.optJSONObject("payload")
        raiseUpPayload?.nasSource = payloadJson?.optString("source")    //智能合约源码
        raiseUpPayload?.nasSourceType = payloadJson?.optString("sourceType")    //智能合约源码Type - 透传参数，不用处理
        raiseUpPayload?.nasFunction = payloadJson?.optString("function")
        raiseUpPayload?.nasArgs = payloadJson?.optString("args")
        raiseUpPayload?.nasType = payloadJson?.optString("type")    //交易类型：普通转账、智能合约部署、智能合约调用

        if (finalCoin.symbol != "NAS" && raiseUpPayload?.nasFunction == "transfer") {
            val args = raiseUpPayload?.nasArgs!!.replace("[", "").replace("]", "").replace("\"", "").split(",")
            to = args[0]
            value = Formatter.tokenFormat(BigDecimal(Formatter.amountFormat(args[1], finalCoin.tokenDecimals.toInt()))).replace(",", "")
        } else {
            to = dAppTransferJson?.optJSONObject("pay")?.optString("to") ?: ""
            value = Formatter.tokenFormat(BigDecimal(Formatter.amountFormat(dAppTransferJson?.optJSONObject("pay")?.optString("value")
                    ?: "0",
                    finalCoin.tokenDecimals.toInt()))).replace(",", "")
        }
    }

    override fun destroy() {
        super.destroy()

        DataCenter.removeData(Constants.KEY_DAPP_TRANSFER_JSON)

    }
}