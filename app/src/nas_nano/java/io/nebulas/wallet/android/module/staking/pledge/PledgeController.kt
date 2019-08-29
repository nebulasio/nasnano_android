package io.nebulas.wallet.android.module.staking.pledge

import android.arch.lifecycle.LifecycleOwner
import android.content.Context
import android.support.annotation.WorkerThread
import com.alibaba.fastjson.JSON
import com.young.binder.lifecycle.LifecycleController
import io.nebulas.wallet.android.BuildConfig
import io.nebulas.wallet.android.R
import io.nebulas.wallet.android.common.DataCenter
import io.nebulas.wallet.android.extensions.logD
import io.nebulas.wallet.android.extensions.logE
import io.nebulas.wallet.android.module.staking.PledgeDetail
import io.nebulas.wallet.android.module.staking.StakingConfiguration
import io.nebulas.wallet.android.module.staking.StakingContractHolder
import io.nebulas.wallet.android.module.wallet.create.model.Address
import io.nebulas.wallet.android.module.wallet.create.model.Wallet
import io.nebulas.wallet.android.network.nas.NASHttpManager
import io.nebulas.wallet.android.network.nas.api.NASApi
import io.nebulas.wallet.android.network.nas.model.ContractCall
import io.nebulas.wallet.android.network.nas.model.EstimateGasRequest
import io.nebulas.wallet.android.network.nas.model.NasAccountState
import io.nebulas.wallet.android.network.nas.model.NasTransactionReceipt
import org.jetbrains.anko.doAsync
import walletcore.Payload
import walletcore.Response
import walletcore.Walletcore
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.concurrent.Future

class PledgeController(lifecycleOwner: LifecycleOwner,
                       val context: Context,
                       val dataCenter: PledgeDataCenter) : LifecycleController(lifecycleOwner, context) {

    private var future: Future<Unit>? = null
    private var pledgeFuture: Future<Unit>? = null

    private var walletCheckFuture:Future<Unit>? = null

    private val cachedAddress = mutableMapOf<Long, Address?>()

    override fun onDestroyed() {
        future?.apply {
            if (!isDone && !isCancelled) {
                cancel(true)
            }
        }
        pledgeFuture?.apply {
            if (!isDone && !isCancelled) {
                cancel(true)
            }
        }
        walletCheckFuture?.apply {
            if (!isDone && !isCancelled) {
                cancel(true)
            }
        }
    }

    fun loadData() {
        future?.apply {
            if (!isDone && !isCancelled) {
                cancel(true)
            }
        }
        future = null
        future = doAsync {
            val wallet = dataCenter.walletData.value ?: return@doAsync
            val address = getWalletAddress(wallet) ?: return@doAsync
            dataCenter.isLoading.value = true
            dataCenter.estimateGasFee.value = getEstimateGas()
            val pledgedDetail = getWalletPledgedInfo(address.address)
            if (pledgedDetail == null) {
                dataCenter.error.value = context.getString(R.string.network_connect_exception)
                dataCenter.isLoading.value = false
                return@doAsync
            }
            dataCenter.pledgedInfo.value = pledgedDetail
            dataCenter.isLoading.value = false
        }
    }

    fun pledge(nas:String, password:String){
        if (nas.isBlank()){
            return
        }
        val wallet = dataCenter.walletData.value ?: return
        pledgeFuture = doAsync {
            try {
                val buttonStatusBackup = dataCenter.buttonStatus.value
                dataCenter.buttonStatus.value = PledgeDataCenter.ButtonStatus.UploadingToChain
                val nasDecimal = BigDecimal(nas)
                val weiDecimal = nasDecimal.multiply(BigDecimal.TEN.pow(18))
                val rawTransaction = generateRawTransaction(weiDecimal.toPlainString(), password)
                if (rawTransaction==null){
                    dataCenter.error.value = context.getString(R.string.tip_password_error_to_lock)
                    dataCenter.buttonStatus.value = buttonStatusBackup
                    return@doAsync
                }
                if (rawTransaction.errorCode!=0L){
                    dataCenter.error.value = rawTransaction.errorMsg
                    dataCenter.buttonStatus.value = buttonStatusBackup
                    return@doAsync
                }
                val hash = sendTransaction(rawTransaction.rawTransaction)
                if (hash==null){
                    dataCenter.error.value = context.getString(R.string.tip_password_error_to_lock)
                    dataCenter.buttonStatus.value = buttonStatusBackup
                    return@doAsync
                }

                StakingConfiguration.savePledgingWallet(context, wallet.id.toString(), hash, StakingConfiguration.OperationType.Pledge)

                while (true) {
                    Thread.sleep(5000)
                    val transactionReceipt = checkPledgeTransactionStatus(hash)
                    if (transactionReceipt!=null){
                        val status = transactionReceipt.status
                        if (status==0) {    //交易失败
                            dataCenter.error.value = context.getString(R.string.tip_password_error_to_lock)
                            dataCenter.buttonStatus.value = buttonStatusBackup
                            StakingConfiguration.pledgeComplete(context, hash)
                            break
                        } else if (status==1) { //交易成功
                            dataCenter.pledgeResult.value = true
                            StakingConfiguration.pledgeComplete(context, hash)
                            break
                        }
                    }
                }

                logD("Pledge success: $hash")
            }catch (e:Exception){
                logE("Pledge error: $e")
            }finally {
                dataCenter.isLoading.value = false
            }
        }
    }

    fun scheduleWalletStatus(wallet: Wallet){
        walletCheckFuture?.apply {
            if (!isDone && !isCancelled) {
                cancel(true)
            }
        }
        walletCheckFuture = null
        walletCheckFuture = doAsync {
            while (true){
                val pledgingWallets = StakingConfiguration.getPledgingWallets(context)
                val w = pledgingWallets.find { it.walletId==wallet.id }
                if (w!=null){
                    if (dataCenter.buttonStatus.value != PledgeDataCenter.ButtonStatus.UploadingToChain) {
                        dataCenter.buttonStatus.value = PledgeDataCenter.ButtonStatus.UploadingToChain
                    }
                } else {
                    dataCenter.buttonStatus.value = PledgeDataCenter.ButtonStatus.Disabled
                    loadData()
                    break
                }
                Thread.sleep(1000)
            }
        }
    }

    @WorkerThread
    private fun checkPledgeTransactionStatus(hash:String):NasTransactionReceipt?{
        val api = NASHttpManager.getApi()
        var transactionReceipt:NasTransactionReceipt?=null
        var retryCount = 0
        while (transactionReceipt==null && retryCount<3) {
            val response = api.getTransactionReceipt(mapOf("hash" to hash)).execute()
            transactionReceipt = response.body()?.result
            retryCount++
        }
        return transactionReceipt
    }

    @WorkerThread
    private fun generateRawTransaction(wei:String, password: String):Response?{
        val wallet = dataCenter.walletData.value ?: return null
        val contract = StakingContractHolder.stakingProxyContract?:return null
        val address = getWalletAddress(wallet)?:return null
        val accountState = getAccountState(address.address)?:return null
        val gasLimit = try{
            BigDecimal(dataCenter.estimateGas).multiply(BigDecimal.TEN).toPlainString()
        }catch (e:Exception){
            "200000"
        }

        return Walletcore.getRawTransaction(Walletcore.NAS,
                BuildConfig.NAS_CHAIN_ID.toString(),
                address.address,
                password,
                address.getKeyStore(),
                contract,
                "0",
                (accountState.nonce+1).toString(),
                Payload().apply {
                    nasFunction = "staking"
                    nasArgs = "[\"$wei\"]"
                    nasType=Walletcore.TxPayloadCallType
                    nasSourceType = "js"
                    nasSource = ""
                },
                dataCenter.gasPrice,
                gasLimit)
    }

    @WorkerThread
    private fun sendTransaction(rawTransaction:String):String?{
        val api = NASHttpManager.getApi()
        var hash:String? = null
        var retryCount = 0
        while (hash==null && retryCount<3) {
            val response = api.syncSendRawTransaction(mapOf("data" to rawTransaction)).execute()
            hash = response.body()?.result?.txhash
            retryCount++
        }
        return hash
    }

    @WorkerThread
    private fun getAccountState(address: String):NasAccountState?{
        val api = NASHttpManager.getApi()
        var retryCount = 0
        var accountState:NasAccountState?=null
        while (accountState==null && retryCount<3) {
            val accountStateResponse = api.getAccountState(mapOf("address" to address)).execute()
            accountState = accountStateResponse.body()?.result
            retryCount++
        }
        return accountState
    }

    @WorkerThread
    private fun getWalletPledgedInfo(address: String): PledgeDetail? {
        val contract = StakingContractHolder.dataContract ?: return null
        val api = NASHttpManager.getApi()
        var result: List<PledgeDetail>? = null
        var retryCount = 0
        while (result == null && retryCount < 3) {
            val response = api.call(
                    NASApi.CallParam(contract, contract, "0", "1", "20000000000", "2000000", mapOf(
                            Pair("function", "getCurrentStakingsStatistic"),
                            Pair("args", JSON.toJSONString(listOf(listOf(address), null)))
                    ))
            ).execute()
            val arrayString = response.body()?.result?.result ?: return null
            result = try {
                JSON.parseArray(arrayString, PledgeDetail::class.java)
            } catch (e: Exception) {
                null
            }
            retryCount++
        }
        return if (result == null || result.isEmpty()) null else result[0]
    }

    @WorkerThread
    private fun getEstimateGas(): String {
        val wallet = dataCenter.walletData.value ?: return dataCenter.defaultEstimateGasFee
        val address = getWalletAddress(wallet) ?: return dataCenter.defaultEstimateGasFee
        val contract = StakingContractHolder.stakingProxyContract ?: return dataCenter.defaultEstimateGasFee
        val api = NASHttpManager.getApi()
        val estimateGasRequest = EstimateGasRequest()
        estimateGasRequest.from = address.address
        estimateGasRequest.to = contract
        val fiveNas = "5000000000000000000"
        estimateGasRequest.contract = ContractCall("staking", JSON.toJSONString(listOf(fiveNas)))
        var retryCount1 = 0
        var estimateGas:String?=null
        while (estimateGas==null && retryCount1<3) {
            val estimateGasResponse = api.getEstimateGas(estimateGasRequest).execute()
            estimateGas = estimateGasResponse.body()?.result?.gas
            retryCount1++
        }
        if (estimateGas.isNullOrEmpty()){
            estimateGas = "200000"
        }

        var retryCount2 = 0
        var gasPrice:String?=null
        while (gasPrice==null && retryCount2<3) {
            val gasPriceResponse = api.getGasPrice().execute()
            gasPrice = gasPriceResponse.body()?.result?.gas_price
            retryCount2++
        }
        if (gasPrice.isNullOrEmpty()){
            gasPrice = "20000000000"
        }

        return try{
            dataCenter.estimateGas = estimateGas?:"20000"
            dataCenter.gasPrice = gasPrice?:"20000000000"
            val gasFee = BigDecimal(estimateGas).multiply(BigDecimal(gasPrice)).divide(BigDecimal.TEN.pow(18), 18, RoundingMode.FLOOR)
            gasFee.stripTrailingZeros().toPlainString()
        }catch (e:Exception){
            "0.004"
        }
    }

    private fun getWalletAddress(wallet: Wallet): Address? {
        if (cachedAddress.containsKey(wallet.id)) {
            return cachedAddress[wallet.id]
        }
        val address = DataCenter.addresses.find { it.walletId == wallet.id && it.platform == Walletcore.NAS }
        cachedAddress[wallet.id] = address
        return address
    }
}