package io.nebulas.wallet.android.module.transaction.transfer

import android.arch.lifecycle.LifecycleOwner
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.support.annotation.WorkerThread
import com.young.binder.lifecycle.LifecycleController
import io.nebulas.wallet.android.R
import io.nebulas.wallet.android.base.BaseActivity
import io.nebulas.wallet.android.common.Constants
import io.nebulas.wallet.android.common.DataCenter
import io.nebulas.wallet.android.db.DBUtil
import io.nebulas.wallet.android.extensions.logD
import io.nebulas.wallet.android.extensions.logE
import io.nebulas.wallet.android.module.balance.model.Coin
import io.nebulas.wallet.android.module.balance.viewmodel.BalanceViewModel
import io.nebulas.wallet.android.module.detail.fragment.transaction.ErrorHandler
import io.nebulas.wallet.android.module.transaction.model.Transaction
import io.nebulas.wallet.android.module.wallet.create.model.Address
import io.nebulas.wallet.android.network.callback.OnResultCallBack
import io.nebulas.wallet.android.network.dapp.APIHolder
import io.nebulas.wallet.android.network.nas.model.NASTransactionModel
import io.nebulas.wallet.android.network.server.HttpManager
import io.nebulas.wallet.android.network.server.api.ServerApi
import io.nebulas.wallet.android.network.server.model.GasPriceResp
import io.nebulas.wallet.android.network.server.model.PriceList
import io.nebulas.wallet.android.network.subscriber.HttpSubscriber
import io.nebulas.wallet.android.util.Formatter
import io.nebulas.wallet.android.util.SecurityHelper
import org.jetbrains.anko.doAsync
import walletcore.Payload
import walletcore.Response
import walletcore.Walletcore
import java.math.BigDecimal
import java.net.URLEncoder
import java.util.concurrent.Future

class TransferController(val context: Context,
                         private val lifecycleOwner: LifecycleOwner,
                         private val dataCenter: TransferDataCenter) : LifecycleController(lifecycleOwner, context) {

    var future: Future<*>? = null
    var gasFuture: Future<*>? = null

    override fun getOwnerContext(): Context = context

    override fun onDestroyed() {
        future?.apply {
            if (!isDone && !isCancelled) {
                cancel(true)
            }
        }
        gasFuture?.apply {
            if (!isDone && !isCancelled) {
                cancel(true)
            }
        }
    }

    fun loadNecessaryData() {
        val api = HttpManager.getServerApi()
        val coin = dataCenter.coin ?: return
        val coreCoin = dataCenter.coreCoin ?: return
        val address = coin.address ?: return
        val transaction = dataCenter.transaction!!

        DataCenter.coins.forEach {
            if(it.tokenId == coin.tokenId) {
                val max = BigDecimal(dataCenter.maxBalance.value?:"0")
                if(BigDecimal(it.balance) > max){
                    dataCenter.maxBalance.value = it.balance
                    dataCenter.maxBalanceFormatted.value = it.balanceString
                }
            }
        }

        dataCenter.isLoading = true
        future = doAsync(ErrorHandler { _, _ ->
            dataCenter.isLoading = false
        }.defaultHandler) {
            val priceList = getCoinPrice(api, coin, coreCoin)

            priceList?.forEach {
                if (it.currencyId == coreCoin.tokenId) {
                    dataCenter.gasCoinPrice.value = BigDecimal(it.price)
                }
                if (it.currencyId == coin.tokenId) {
                    dataCenter.coinPrice = BigDecimal(it.price)
                }
            }

            val gasPriceResp = getGas(api, address, transaction)
            if (gasPriceResp != null) {
                if (!dataCenter.raiseGasPrice.isNullOrEmpty()) {
                    gasPriceResp.gasPriceMin = dataCenter.raiseGasPrice
                    gasPriceResp.gasPriceMax = dataCenter.raiseGasPrice + "0"
                }
                if (!dataCenter.raiseGasLimit.isNullOrEmpty()) {
                    gasPriceResp.estimateGas = dataCenter.raiseGasLimit
                }
            }
            dataCenter.gasPriceResp = gasPriceResp
            gasPriceResp?.apply {
                transaction.gasPrice = gasPriceMin ?: "0"
                transaction.gasLimit = (BigDecimal(estimateGas) + BigDecimal(10000)).stripTrailingZeros().toPlainString()
                transaction.nonce = nonce ?: "0"
                dataCenter.transaction = transaction
            }
            if (dataCenter.gotoTransferDialog) {
                val balanceViewModel = ViewModelProviders.of(context as BaseActivity).get(BalanceViewModel::class.java)
                //获取资产余额（仅适用于唤起APP并直接跳转至transfer的情况）
                balanceViewModel.requestCoinBalance(lifecycleOwner.lifecycle) {
                    dataCenter.isLoading = false
                    dataCenter.autoConfirm.value = true
                }
            } else {
                dataCenter.isLoading = false
            }
        }
    }

    fun doPay(address: Address, password: String) {
        val api = HttpManager.getServerApi()
        val transaction = dataCenter.transaction!!
        future = doAsync(ErrorHandler { _, _ ->
            dataCenter.isLoading = false
        }.defaultHandler) {
            val gasPriceResp = getGas(api, address.address, transaction)
            dataCenter.gasPriceResp = gasPriceResp
            gasPriceResp?.apply {
                //交易发起前调用gas接口，只是为了拉取最新的nonce
                transaction.nonce = nonce ?: "0"
                transaction.txFee = BigDecimal(transaction.gasPrice).multiply(BigDecimal(this.estimateGas
                        ?: "0")).stripTrailingZeros().toPlainString()
                dataCenter.transaction = transaction
            }

            val response = prepareTransaction(address, password)
            if (response == null) {
                dataCenter.payError.value = context.getString(R.string.status_fail)
                return@doAsync
            } else {
                if (response.errorCode != 0L) {
                    dataCenter.payError.value = Formatter.formatWalletErrorMsg(context, response.errorMsg)
                    return@doAsync
                }
            }
            SecurityHelper.walletCorrectPassword(dataCenter.currentWallet)
            when (transaction.platform) {
                Walletcore.NAS -> {
                    transaction.signedData = response.rawTransaction
                    transaction.sendNASRawTransaction(HttpSubscriber(object : OnResultCallBack<NASTransactionModel> {
                        override fun onSuccess(t: NASTransactionModel) {
                            transaction.hash = t.txhash!!
                            doAsync {
                                if (transaction.txData.isNullOrEmpty()) {
                                    transaction.txData = ""
                                }
                                DBUtil.appDB.transactionDao().insertTransaction(transaction)
                                val callback = dataCenter.callback
                                val serialNo = dataCenter.serialNumber
                                val hash = transaction.hash
                                if (callback != null && serialNo != null && hash != null) {
                                    callback(callback, serialNo, hash)
                                }
                                dataCenter.paySuccess.value = true
                            }
                        }

                        override fun onError(code: Int, errorMsg: String) {
                            super.onError(code, errorMsg)
                            dataCenter.payError.value = errorMsg
                        }
                    }, lifecycleOwner.lifecycle))
                }
                Walletcore.ETH -> {
                    transaction.signedData = response.rawTransaction
                    transaction.sendETHRawTransaction(HttpSubscriber(object : OnResultCallBack<String> {
                        override fun onSuccess(t: String) {
                            transaction.hash = t
                            doAsync {
                                if (transaction.txData.isNullOrEmpty()) {
                                    transaction.txData = ""
                                }
                                DBUtil.appDB.transactionDao().insertTransaction(transaction)
                                val callback = dataCenter.callback
                                val serialNo = dataCenter.serialNumber
                                val hash = transaction.hash
                                if (callback != null && serialNo != null && hash != null) {
                                    callback(callback, serialNo, hash)
                                }
                                dataCenter.paySuccess.value = true
                            }
                        }

                        override fun onError(code: Int, errorMsg: String) {
                            super.onError(code, errorMsg)
                            dataCenter.payError.value = errorMsg
                        }

                    }, lifecycleOwner.lifecycle))
                }
            }
        }
    }

    @WorkerThread
    private fun getCoinPrice(api: ServerApi, coin: Coin, gasCoin: Coin): List<PriceList>? {
        val tokenId = coin.tokenId ?: return null
        val gasTokenId = gasCoin.tokenId ?: return null
        val call = api.getCurrencyPriceWithoutRX(HttpManager.getHeaderMap(), listOf(tokenId, gasTokenId))
        return call.execute().body()?.data?.priceList
    }

    @WorkerThread
    private fun getGas(api: ServerApi, address: String, transaction: Transaction): GasPriceResp? {
        val currencyId = transaction.currencyId
        val type = if (transaction.payload?.nasType == Walletcore.TxPayloadBinaryType) 1 else 0
        val call = api.getGasPriceWithoutRX(HttpManager.getHeaderMap(),
                currencyId
                        ?: "nebulas", address, type, URLEncoder.encode(transaction.getPayloadJson(), "utf-8"))
        return call.execute().body()?.data
    }

    @WorkerThread
    private fun prepareTransaction(address: Address, password: String): Response? {
        val coin = dataCenter.coin ?: return null
        val transaction = dataCenter.transaction ?: return null
        // nebpay 唤起的payload实体
        if (null != dataCenter.raiseUpPayload) {
            transaction.payload = dataCenter.raiseUpPayload
        }
        if (coin.type != 1) {
            // nebpay 唤起的payload实体
            if (null != dataCenter.raiseUpPayload) {
                transaction.payload = dataCenter.raiseUpPayload
            } else {
                when (coin.platform) {
                    Walletcore.NAS -> {

                        transaction.payload = null
                        transaction.payload = Payload()

                        transaction.payload?.nasType = Walletcore.TxPayloadCallType
                        transaction.payload?.nasFunction = "transfer"
                        transaction.payload?.nasArgs = "[\"${transaction.receiver}\",\"${transaction.amount}\"]"
                        transaction.payload?.nasSource = ""
                        transaction.payload?.nasSourceType = "js"
                    }

                    Walletcore.ETH -> {
                        //目前只支持erc20转账
                        transaction.receiver = coin.contractAddress
                        transaction.amount = "0"

                        transaction.receiver = transaction.receiver?.replace("0x", "")?.replace("0X", "")

                        val receiverStr = String.format("%64s", transaction.receiver).replace(" ", "0")
                        val amountStr = String.format("%64s", transaction.amount).replace(" ", "0")

                        val ethContract = "0xa9059cbb$receiverStr$amountStr"

                        transaction.payload = null
                        transaction.payload = Payload()

                        transaction.payload?.ethContract = ethContract

                    }
                }
            }
        }

        val chainId: String = when (transaction.platform) {
            Walletcore.NAS -> Constants.NAS_CHAIN_ID.toString()
            Walletcore.ETH -> Constants.ETH_CHAIN_ID.toString()
            else -> ""
        }

        var to: String
        var amount: String

        if (transaction.platform == Walletcore.NAS) {
            when (transaction.payload?.nasType) {
                Walletcore.TxPayloadBinaryType -> {
                    to = transaction.receiver!!
                    amount = transaction.amount!!
                }
                Walletcore.TxPayloadCallType -> {
                    if (transaction.coinSymbol != "NAS" && transaction.payload?.nasFunction == "transfer") {
                        to = transaction.contractAddress
                        amount = "0"
                    } else {
                        to = transaction.receiver!!
                        amount = transaction.amount!!
                    }

                }
                Walletcore.TxPayloadDeployType -> {
                    to = transaction.account!!
                    amount = "0"
                }

                else -> {
                    to = ""
                    amount = "0"
                }

            }
        } else {
            to = transaction.receiver!!
            amount = transaction.amount!!
        }

        if (BigDecimal(amount) <= BigDecimal.ZERO) {
            amount = "0"
        }

        // 判断gasLimit是否需要加10000
        val coreCoin = dataCenter.coreCoin ?: Coin()
        if (coin.tokenId != coreCoin.tokenId){
            if (BigDecimal(coreCoin.balance) < BigDecimal(transaction.gasPrice).multiply(BigDecimal(transaction.gasLimit))){
                transaction.gasLimit = (BigDecimal(transaction.gasLimit) - BigDecimal(10000)).stripTrailingZeros().toPlainString()
            }
        } else {
            var amountTemp = BigDecimal(transaction.amount)
            if (BigDecimal(coin.balance) < (amountTemp + BigDecimal(transaction.gasPrice).multiply(BigDecimal(transaction.gasLimit)))) {
                transaction.gasLimit = (BigDecimal(transaction.gasLimit) - BigDecimal(10000)).stripTrailingZeros().toPlainString()
            }
        }

        return Walletcore.getRawTransaction(transaction.platform,
                chainId,
                transaction.account,
                password,
                address.getKeyStore(),
                to,
                amount,
                transaction.nonce,
                transaction.payload,
                transaction.gasPrice,
                transaction.gasLimit)

    }

    @WorkerThread
    private fun callback(callbackUrl: String, serialNo: String, hash: String): Boolean {
        return try {
            val call = APIHolder.getDAppServerAPI(callbackUrl).sendTxHashToDAppServer(serialNo, hash)
            val r = call.execute()
            true
        } catch (e: Exception) {
            false
        }
    }
}
