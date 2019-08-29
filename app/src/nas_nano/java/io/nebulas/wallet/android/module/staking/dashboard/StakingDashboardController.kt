package io.nebulas.wallet.android.module.staking.dashboard

import android.arch.lifecycle.LifecycleOwner
import android.content.Context
import android.support.annotation.WorkerThread
import com.alibaba.fastjson.JSON
import com.young.binder.lifecycle.LifecycleController
import io.nebulas.wallet.android.R
import io.nebulas.wallet.android.common.DataCenter
import io.nebulas.wallet.android.module.staking.PledgeDetail
import io.nebulas.wallet.android.module.staking.StakingConfiguration
import io.nebulas.wallet.android.module.staking.StakingContractHolder
import io.nebulas.wallet.android.module.staking.StakingSummary
import io.nebulas.wallet.android.network.nas.NASHttpManager
import io.nebulas.wallet.android.network.nas.api.NASApi
import io.nebulas.wallet.android.network.nas.model.NasTransactionReceipt
import io.nebulas.wallet.android.network.server.HttpManager
import org.jetbrains.anko.doAsync
import walletcore.Walletcore
import java.util.concurrent.Future

class StakingDashboardController(lifecycleOwner: LifecycleOwner,
                                 val context: Context,
                                 val dataCenter: StakingDashboardDataCenter) : LifecycleController(lifecycleOwner, context) {

    private var future:Future<Unit>?=null
    private var syncFuture:Future<Unit>?=null

    override fun onDestroyed() {
        future?.apply {
            if (!isCancelled && !isDone){
                cancel(true)
            }
        }
        syncFuture?.apply {
            if (!isCancelled && !isDone){
                cancel(true)
            }
        }
    }

    fun loadData() {
        future?.apply {
            if (!isCancelled && !isDone){
                cancel(true)
            }
        }
        future = doAsync {
            val api = HttpManager.getServerApi()
            val response = api.getStakingContracts(HttpManager.getHeaderMap()).execute()
            if (response.code() != 200) {
                dataCenter.error.value = context.getString(R.string.network_connect_exception)
                dataCenter.isLoading.value = false
                return@doAsync
            }
            val apiResponse = response.body()
            if (apiResponse == null) {
                dataCenter.error.value = context.getString(R.string.network_connect_exception)
                dataCenter.isLoading.value = false
                return@doAsync
            }
            val stakingContractsResponse = apiResponse.data
            if (stakingContractsResponse?.stakingProxy == null
                    || stakingContractsResponse.data == null) {
                dataCenter.error.value = context.getString(R.string.network_connect_exception)
                dataCenter.isLoading.value = false
                return@doAsync
            }
            if (!stakingContractsResponse.verify()) {
                dataCenter.error.value = context.getString(R.string.network_connect_exception)
                dataCenter.isLoading.value = false
                return@doAsync
            }
            StakingContractHolder.holdStakingContractInfo(stakingContractsResponse)

            val pledgeDetailList = getPledgedWallet()
            if (pledgeDetailList == null) {
                dataCenter.error.value = context.getString(R.string.network_connect_exception)
                dataCenter.isLoading.value = false
                return@doAsync
            }

            val stakingSummary = getStakingSummaryInfo()
            if (stakingSummary == null) {
                dataCenter.error.value = context.getString(R.string.network_connect_exception)
                dataCenter.isLoading.value = false
                return@doAsync
            }

            dataCenter.inOperationWallets = StakingConfiguration.getPledgingWallets(context)
            dataCenter.pledgeDetailList = pledgeDetailList
            dataCenter.stakingSummary.value = stakingSummary
            dataCenter.isLoading.value = false
        }
    }

    fun sync() {
        syncFuture?.apply {
            if (!isCancelled && !isDone){
                cancel(true)
            }
        }
        syncFuture = doAsync {
            while (true) {
                val pledgingWallets = StakingConfiguration.getPledgingWallets(context)
                Thread.sleep(3000)
                val anyChanges = doSync(pledgingWallets)
                if (anyChanges) {
                    loadData()
                }
                if(dataCenter.inOperationWallets.isEmpty()){
                    break
                }
            }
        }
    }

    @WorkerThread
    private fun doSync(pledgingWallets:List<StakingConfiguration.PledgingWalletWrapper?>):Boolean{
        var anyChanges = false
        pledgingWallets.forEach {
            it?:return@forEach
            val transactionReceipt = checkPledgeTransactionStatus(it.txHash)?:return@forEach
            val status = transactionReceipt.status
            if (status!=2){
                anyChanges = true
                StakingConfiguration.pledgeComplete(context, it.txHash)
            }
        }
        return anyChanges
    }

    @WorkerThread
    private fun checkPledgeTransactionStatus(hash:String): NasTransactionReceipt?{
        val api = NASHttpManager.getApi()
        var transactionReceipt: NasTransactionReceipt?=null
        var retryCount = 0
        while (transactionReceipt==null && retryCount<3) {
            val response = api.getTransactionReceipt(mapOf("hash" to hash)).execute()
            transactionReceipt = response.body()?.result
            retryCount++
        }
        return transactionReceipt
    }

    @WorkerThread
    private fun getPledgedWallet(): List<PledgeDetail>? {
        val contract = StakingContractHolder.dataContract
        contract ?: return null
        val allWallets = mutableListOf<String>()
        DataCenter.addresses.forEach {
            if (it.platform == Walletcore.NAS) {
                allWallets.add(it.address)
            }
        }
        val api = NASHttpManager.getApi()

        var result: List<PledgeDetail>? = null
        var retryCount = 0
        while (result == null && retryCount < 3) {
            val response = api.call(
                    NASApi.CallParam(contract, contract, "0", "1", "20000000000", "2000000", mapOf(
                            Pair("function", "getCurrentStakingsStatistic"),
                            Pair("args", JSON.toJSONString(listOf(allWallets, null)))
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
        return result
    }

    @WorkerThread
    private fun getStakingSummaryInfo(): StakingSummary? {
        val builder = StringBuilder()
        DataCenter.addresses.forEach {
            if (it.platform==Walletcore.NAS){
                builder.append(it.address).append(",")
            }
        }
        if (builder.isNotEmpty()){
            builder.deleteCharAt(builder.length-1)
        }
        val addresses = builder.toString()
        val api = HttpManager.getServerApi()
        var stakingSummary: StakingSummary? = null
        var retryCount = 0
        while (stakingSummary == null && retryCount < 3) {
            val response = api.getStakingSummaryInfo(HttpManager.getHeaderMap(), addresses).execute()
            stakingSummary = response.body()?.data
            retryCount++
        }
        return stakingSummary
    }

}