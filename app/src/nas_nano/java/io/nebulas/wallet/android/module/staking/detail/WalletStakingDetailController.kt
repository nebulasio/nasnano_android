package io.nebulas.wallet.android.module.staking.detail

import android.arch.lifecycle.LifecycleOwner
import android.content.Context
import android.support.annotation.WorkerThread
import com.alibaba.fastjson.JSON
import com.young.binder.lifecycle.LifecycleController
import io.nebulas.wallet.android.BuildConfig
import io.nebulas.wallet.android.R
import io.nebulas.wallet.android.common.DataCenter
import io.nebulas.wallet.android.extensions.logD
import io.nebulas.wallet.android.module.staking.AddressProfits
import io.nebulas.wallet.android.module.staking.StakingConfiguration
import io.nebulas.wallet.android.module.staking.StakingContractHolder
import io.nebulas.wallet.android.module.wallet.create.model.Address
import io.nebulas.wallet.android.network.nas.NASHttpManager
import io.nebulas.wallet.android.network.nas.model.ContractCall
import io.nebulas.wallet.android.network.nas.model.EstimateGasRequest
import io.nebulas.wallet.android.network.nas.model.NasAccountState
import io.nebulas.wallet.android.network.server.HttpManager
import io.nebulas.wallet.android.util.Formatter
import org.jetbrains.anko.doAsync
import walletcore.Payload
import walletcore.Response
import walletcore.Walletcore
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.concurrent.Future
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock

class WalletStakingDetailController(lifecycleOwner: LifecycleOwner,
                                    private val context: Context,
                                    private val dataCenter: WalletStakingDetailDataCenter) : LifecycleController(lifecycleOwner, context) {

    private var future: Future<Unit>? = null
    private val lock: Lock = ReentrantLock()

    override fun onDestroyed() {
        future?.apply {
            if (!isCancelled && !isDone) {
                cancel(true)
            }
        }
    }

    fun loadData() {
        doAsync {
            try {
                if (!lock.tryLock()) {
                    logD("loadData is processing. canceled.")
                    return@doAsync
                }
                val profits = getAddressProfits()
                if (profits == null) {
                    dataCenter.error.value = context.getString(R.string.network_connect_exception)
                    dataCenter.isSwipeRefresh.value = false
                    return@doAsync
                }
                dataCenter.addressProfits.value = profits
                val records = profits.profits
                if (records != null) {
                    var exists = dataCenter.profitsRecords.value
                    if (exists == null) {
                        exists = mutableListOf()
                    }
                    if (dataCenter.currentPage == 1) {
                        exists.clear()
                    }
                    exists.addAll(records)
                    dataCenter.profitsRecords.value = exists
                }
                dataCenter.currentPage++
                dataCenter.isSwipeRefresh.value = false
            } catch (e: Exception) {

            } finally {
                lock.unlock()
                dataCenter.isSwipeLoadingMore = false
            }
        }
    }

    fun getEstimateGasFee(){
        doAsync {
            try{
                dataCenter.isCenterLoading.value = true
                val gasFee = getEstimateGas()
                dataCenter.estimateGasFee = gasFee
                dataCenter.showCancelPledgeTipDialog.value = true
            }catch (e:Exception){

            }finally {
                dataCenter.isCenterLoading.value = false
            }
        }
    }

    fun cancelPledge(password:String){
        doAsync {
            dataCenter.isCenterLoading.value = true
            val addressStr = dataCenter.address.value?:return@doAsync
            val address = getAddress(addressStr)?:return@doAsync
            val response = generateRawTransaction(password, address)
            if (response==null){
                dataCenter.error.value = context.getString(R.string.tip_password_error_to_lock)
                return@doAsync
            }
            if (response.errorCode!=0L){
                Formatter.formatWalletErrorMsg(context, response.errorMsg)
                return@doAsync
            }
            val hash = sendTransaction(response.rawTransaction)
            if (hash==null){
                dataCenter.error.value = context.getString(R.string.tip_password_error_to_lock)
                return@doAsync
            }
            StakingConfiguration.savePledgingWallet(context, address.walletId.toString(), hash, StakingConfiguration.OperationType.CancelPledge)
            dataCenter.isCenterLoading.value = false
            dataCenter.cancelPledgeComplete.value = true
        }
    }

    @WorkerThread
    private fun generateRawTransaction(password: String, address:Address): Response?{
        val contract = StakingContractHolder.stakingProxyContract?:return null
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
                    nasFunction = "cancel"
                    nasArgs = "[]"
                    nasType= Walletcore.TxPayloadCallType
                    nasSourceType = "js"
                    nasSource = ""
                },
                dataCenter.gasPrice,
                gasLimit)
    }

    private fun getAddress(address: String):Address?{
        return DataCenter.addresses.find { it.address==address && it.platform==Walletcore.NAS }
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
    private fun getAccountState(address: String): NasAccountState?{
        val api = NASHttpManager.getApi()
        var retryCount = 0
        var accountState: NasAccountState?=null
        while (accountState==null && retryCount<3) {
            val accountStateResponse = api.getAccountState(mapOf("address" to address)).execute()
            accountState = accountStateResponse.body()?.result
            retryCount++
        }
        return accountState
    }

    @WorkerThread
    private fun getEstimateGas(): String {
        val address = dataCenter.address.value?: return dataCenter.defaultEstimateGasFee
        val contract = StakingContractHolder.stakingProxyContract ?: return dataCenter.defaultEstimateGasFee
        val api = NASHttpManager.getApi()
        val estimateGasRequest = EstimateGasRequest()
        estimateGasRequest.from = address
        estimateGasRequest.to = contract
        estimateGasRequest.contract = ContractCall("cancel", JSON.toJSONString(emptyList<String>()))
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

    @WorkerThread
    private fun getAddressProfits(): AddressProfits? {
        val address = dataCenter.address.value ?: return null
        val api = HttpManager.getServerApi()
        var retryCount = 0
        var addressProfits: AddressProfits? = null
        while (addressProfits == null && retryCount < 3) {
            val response = api.getProfitsInfo(HttpManager.getHeaderMap(), address, dataCenter.currentPage).execute()
            addressProfits = response.body()?.data
            retryCount++
        }
        return addressProfits
    }

}