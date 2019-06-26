package io.nebulas.wallet.android.module.transaction.viewmodel

import android.arch.lifecycle.Lifecycle
import android.arch.lifecycle.ViewModel
import io.nebulas.wallet.android.BuildConfig
import io.nebulas.wallet.android.common.Constants
import io.nebulas.wallet.android.common.URLConstants
import io.nebulas.wallet.android.db.DBUtil
import io.nebulas.wallet.android.module.transaction.model.Transaction
import io.nebulas.wallet.android.module.wallet.create.model.Address
import io.nebulas.wallet.android.network.callback.OnResultCallBack
import io.nebulas.wallet.android.network.eth.ETHHttpManager
import io.nebulas.wallet.android.network.nas.model.NASTransactionModel
import io.nebulas.wallet.android.network.server.HttpManager
import io.nebulas.wallet.android.network.server.model.GasPriceResp
import io.nebulas.wallet.android.network.subscriber.HttpSubscriber
import io.nebulas.wallet.android.util.Formatter
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import walletcore.Payload
import walletcore.Walletcore
import java.net.URLEncoder

/**
 * Created by Heinoc on 2018/3/5.
 */
class TransferViewModel : ViewModel() {

    fun getGas(currencyId: String, address: String, type: Int, contractJsonString: String, lifecycle: Lifecycle, onComplete: (gasPriceResp: GasPriceResp?) -> Unit) {

        HttpManager.getGasPrice(currencyId, address, type, URLEncoder.encode(contractJsonString, "utf-8"), HttpSubscriber(object : OnResultCallBack<GasPriceResp> {
            override fun onSuccess(t: GasPriceResp) {
                onComplete(t)
            }

            override fun onError(code: Int, errorMsg: String) {
                super.onError(code, errorMsg)
                onComplete(null)
            }
        }, lifecycle))

    }

    fun getErc20Gas(address: String, data:String, lifecycle: Lifecycle, onComplete: (String) -> Unit, onFailed: (errMsg: String) -> Unit) {
        ETHHttpManager.getEstimateGas(address, data, HttpSubscriber(object :OnResultCallBack<String>{
            override fun onSuccess(t: String) {
                onComplete(Formatter.ethHexToBalance(t, 0))
            }

            override fun onError(code: Int, errorMsg: String) {
                onFailed(errorMsg)
            }
        }, lifecycle))
    }

    fun getEthNonce(address: String, lifecycle: Lifecycle, onComplete: (String) -> Unit, onFailed: (errMsg: String) -> Unit){
        ETHHttpManager.getNonce(address, HttpSubscriber(object :OnResultCallBack<String>{
            override fun onSuccess(t: String) {
                onComplete(Formatter.hexToString(t))
            }

            override fun onError(code: Int, errorMsg: String) {
                onFailed(errorMsg)
            }
        }, lifecycle))
    }

    fun sendRawTransaction(address: Address, passPhrase: String, transaction: Transaction, lifecycle: Lifecycle, onComplete: () -> Unit, onFailed: (errMsg: String) -> Unit) {
        var chainId: String = when (transaction.platform) {
            Walletcore.NAS -> Constants.NAS_CHAIN_ID.toString()
            Walletcore.ETH -> Constants.ETH_CHAIN_ID.toString()
            else -> ""
        }

        val to: String
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
        } else if (transaction.platform == Walletcore.ETH) {
            to = BuildConfig.ERC20_NEBULAS_CONTRACT_ADDRESS
            amount = "0"
            transaction.contractAddress = BuildConfig.ERC20_NEBULAS_CONTRACT_ADDRESS
        } else {
            to = transaction.receiver!!
            amount = transaction.amount!!
        }

        doAsync {
            val response = Walletcore.getRawTransaction(transaction.platform,
                    chainId,
                    transaction.account,
                    passPhrase,
                    address.getKeyStore(),
                    to,
                    amount,
                    transaction.nonce,
                    transaction.payload,
                    transaction.gasPrice,
                    transaction.gasLimit)

            if (response.errorCode != 0L) {
                uiThread {
                    onFailed(response.errorMsg)
                }
                return@doAsync
            }

            when (transaction.platform) {
                Walletcore.NAS -> {
                    transaction.signedData = response.rawTransaction

                    transaction.sendNASRawTransaction(HttpSubscriber(object : OnResultCallBack<NASTransactionModel> {
                        override fun onSuccess(t: NASTransactionModel) {

                            transaction.hash = t.txhash!!

                            storeTransaction(transaction)

                            onComplete()
                        }

                        override fun onError(code: Int, errorMsg: String) {
                            super.onError(code, errorMsg)
                            onFailed(errorMsg)
                        }
                    }, lifecycle))

                }

                Walletcore.ETH -> {

                    transaction.signedData = response.rawTransaction


                    transaction.sendETHRawTransaction(HttpSubscriber<String>(object : OnResultCallBack<String> {
                        override fun onSuccess(t: String) {
                            transaction.hash = t

                            storeTransaction(transaction)

                            onComplete()

                        }

                        override fun onError(code: Int, errorMsg: String) {
                            super.onError(code, errorMsg)
                            onFailed(errorMsg)
                        }

                    }, lifecycle))

                }
            }


        }
    }

    private fun storeTransaction(transaction: Transaction) {

        doAsync {
            if (transaction.txData.isNullOrEmpty()) {
                transaction.txData = ""
            }
            DBUtil.appDB.transactionDao().insertTransaction(transaction)
        }

    }

    fun sendTxHashToDAppServer(lifecycle: Lifecycle, payId: String, txHash: String, onComplete: () -> Unit) {
        HttpManager.sendTxHashToDAppServer(payId, txHash, HttpSubscriber(object : OnResultCallBack<Any> {
            override fun onSuccess(t: Any) {
                onComplete()
            }

            override fun onError(code: Int, errorMsg: String) {
//                        super.onError(code, errorMsg)
                onComplete()
            }
        }, lifecycle))

    }


}