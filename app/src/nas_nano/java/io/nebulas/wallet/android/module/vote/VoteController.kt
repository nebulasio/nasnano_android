package io.nebulas.wallet.android.module.vote

import android.app.Activity
import android.arch.lifecycle.LifecycleOwner
import android.support.annotation.WorkerThread
import com.young.binder.lifecycle.LifecycleController
import io.nebulas.wallet.android.R
import io.nebulas.wallet.android.common.Constants
import io.nebulas.wallet.android.common.DataCenter
import io.nebulas.wallet.android.db.DBUtil
import io.nebulas.wallet.android.module.transaction.TxDetailActivity
import io.nebulas.wallet.android.module.transaction.model.Transaction
import io.nebulas.wallet.android.module.wallet.create.model.Wallet
import io.nebulas.wallet.android.network.callback.OnResultCallBack
import io.nebulas.wallet.android.network.nas.NASHttpManager
import io.nebulas.wallet.android.network.nas.model.ContractCall
import io.nebulas.wallet.android.network.nas.model.EstimateGasRequest
import io.nebulas.wallet.android.network.nas.model.NASTransactionModel
import io.nebulas.wallet.android.network.subscriber.HttpSubscriber
import io.nebulas.wallet.android.util.Formatter
import io.nebulas.wallet.android.util.SecurityHelper
import io.nebulas.wallet.android.view.research.CurtainResearch
import org.jetbrains.anko.doAsync
import walletcore.Payload
import walletcore.Walletcore
import java.lang.Exception
import java.math.BigDecimal
import java.util.concurrent.Future

class VoteController(private val viewModel: VoteViewModel,
                     private val lifecycleOwner: LifecycleOwner,
                     private val activity: Activity) : LifecycleController(lifecycleOwner, activity) {

    val api = NASHttpManager.getApi()

    var future: Future<*>? = null
    var gasFuture: Future<*>? = null

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

    fun getGas() {
        gasFuture = doAsync {
            val gasPrice = api.getGasPrice().execute().body()?.result
            if (gasPrice != null) {
                viewModel.gasPrice.value = gasPrice.gas_price
            }
            val gas = api.getEstimateGas(EstimateGasRequest().apply {
                from = viewModel.contractAddress
                to = viewModel.contractAddress
                contract = ContractCall(function = viewModel.function, args = viewModel.args)
            }).execute().body()?.result
            if (gas != null) {
                try {
                    if (BigDecimal(gas.gas) >= BigDecimal("300000")) {
                        viewModel.estimateGas.value = gas.gas
                    } else {
                        viewModel.estimateGas.value = "300000"
                    }
                }catch (e:Exception){
                    viewModel.estimateGas.value = "300000"
                }
            }
        }
    }

    fun doTransfer(wallet: Wallet, passPhrase: String) {
        val token = Constants.voteContractsMap[viewModel.contractAddress]
        future = doAsync {
            val coin = DataCenter.coins.find {
                it.walletId == wallet.id && it.tokenId.equals(token, true)
            } ?: return@doAsync
            val address = DataCenter.addresses.find {
                it.id == coin.addressId
            } ?: return@doAsync
            val gasFeeMax = calculateMaxGasFee()
            val gasLimit = if (BigDecimal(coin.balance) < gasFeeMax) {
                viewModel.estimateGas.value ?: "300000"
            } else {
                BigDecimal(viewModel.estimateGas.value ?: "0")
                        .add(BigDecimal("10000"))
                        .stripTrailingZeros()
                        .toPlainString()
            }
            val decimal = coin.tokenDecimals.toInt()
            val transaction = Transaction(
                    currencyId = coin.tokenId,
                    coinSymbol = coin.symbol,
                    blockChainType = Walletcore.NAS,
                    account = coin.address,
                    targetAddress = viewModel.contractAddress,
                    contractAddress = viewModel.contractAddress,
                    amount = BigDecimal(viewModel.amountNAT).multiply(BigDecimal.TEN.pow(decimal)).toPlainString(),
                    txData = "",
                    gasPrice = viewModel.gasPrice.value ?: "0",
                    gasLimit = gasLimit,
                    isSend = true
            )
            transaction.maxConfirmCnt = 15
            transaction.txFee = calculateGasFeeWEI().toPlainString()
            transaction.payload = Payload()

            transaction.payload?.nasType = Walletcore.TxPayloadCallType
            transaction.payload?.nasFunction = viewModel.function
//                    transaction.payload?.nasArgs = "[\"${transaction.receiver}\",\"${transaction.amount}\"]"
            transaction.payload?.nasArgs = viewModel.args
            transaction.payload?.nasSource = ""
            transaction.payload?.nasSourceType = "js"

            transaction.sender = coin.address
            val nonce = getNonce(coin.address)
            transaction.nonce = (nonce + 1).toString()

            val rawTransaction = Walletcore.getRawTransaction(transaction.platform,
                    Constants.NAS_CHAIN_ID.toString(),
                    transaction.account,
                    passPhrase,
                    address.getKeyStore(),
                    viewModel.contractAddress,
                    "0",
                    transaction.nonce,
                    transaction.payload,
                    transaction.gasPrice,
                    transaction.gasLimit)
            if (rawTransaction == null) {
                viewModel.payError.value = activity.getString(R.string.status_fail)
                return@doAsync
            } else {
                if (rawTransaction.errorCode != 0L) {
                    viewModel.payError.value = Formatter.formatWalletErrorMsg(activity, rawTransaction.errorMsg)
                    return@doAsync
                }
            }
            SecurityHelper.walletCorrectPassword(wallet)

            transaction.signedData = rawTransaction.rawTransaction
            transaction.sendNASRawTransaction(HttpSubscriber(object : OnResultCallBack<NASTransactionModel> {
                override fun onSuccess(t: NASTransactionModel) {
                    transaction.hash = t.txhash!!
                    doAsync {
                        if (transaction.txData.isNullOrEmpty()) {
                            transaction.txData = ""
                        }
                        DBUtil.appDB.transactionDao().insertTransaction(transaction)
                        TxDetailActivity.launch(activity,
                                coin,
                                transaction)
                        activity.finish()
                        activity.overridePendingTransition(R.anim.enter_bottom_in, R.anim.exit_bottom_out)
                    }
                }

                override fun onError(code: Int, errorMsg: String) {
                    super.onError(code, errorMsg)
                    viewModel.payError.value = errorMsg
                }
            }, lifecycleOwner.lifecycle))

        }
    }

    @WorkerThread
    fun getNonce(address: String?): Long {
        address ?: return -1L
        val accountState = api.getAccountState(mapOf(Pair("address", address))).execute().body()?.result
        if (accountState == null) {
            return -1L
        } else {
            return accountState.nonce
        }
    }

    fun calculateGasFeeWEI(): BigDecimal {
        val gasPrice = BigDecimal(viewModel.gasPrice.value)
        val estimateGas = BigDecimal(viewModel.estimateGas.value)
        return gasPrice.multiply(estimateGas)
    }

    fun calculateGasFee(): BigDecimal {
        val gasFeeWEI = calculateGasFeeWEI()
        return gasFeeWEI.divide(BigDecimal.TEN.pow(18))
    }

    fun calculateMaxGasFee(): BigDecimal {
        val gasPrice = BigDecimal(viewModel.gasPrice.value)
        val estimateGas = BigDecimal(viewModel.estimateGas.value).add(BigDecimal("10000"))
        val gasFeeWEI = gasPrice.multiply(estimateGas)
        return gasFeeWEI.divide(BigDecimal.TEN.pow(18))
    }

}