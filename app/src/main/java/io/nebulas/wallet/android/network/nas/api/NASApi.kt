package io.nebulas.wallet.android.network.nas.api

import io.nebulas.wallet.android.common.URLConstants
import io.nebulas.wallet.android.module.transaction.model.BlockStateModel
import io.nebulas.wallet.android.network.nas.model.*
import io.reactivex.Observable
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

/**
 * Created by Heinoc on 2018/3/2.
 */
interface NASApi {

    @POST(URLConstants.NAS_GET_BALANCE)
    fun getBalance(@Body body: Map<String, String>): Observable<NASResponse<NasBalanceModel>>

    @POST(URLConstants.NAS_SEND_RAW_TRANSACTION)
    fun sendRawTransaction(@Body body: Map<String, String>): Observable<NASResponse<NASTransactionModel>>

    @POST(URLConstants.NAS_SEND_RAW_TRANSACTION)
    fun syncSendRawTransaction(@Body body: Map<String, String>): Call<NASResponse<NASTransactionModel>>

    @POST(URLConstants.NAS_ESTIMATE_GAS)
    fun getEstimateGas(@Body body: EstimateGasRequest): Call<NASResponse<NasEstimateGas>>

    @GET(URLConstants.NAS_NEB_STATE)
    fun getNebState(): Observable<NASResponse<BlockStateModel>>

    @GET(URLConstants.NAS_NEB_STATE)
    fun getNebStateWithoutRX(): Call<NASResponse<BlockStateModel>>

    @POST(URLConstants.NAS_ACCOUNT_STATE)
    fun getAccountState(@Body body: Map<String, String>): Call<NASResponse<NasAccountState>>

    @GET(URLConstants.NAS_GET_GAS_PRICE)
    fun getGasPrice(): Call<NASResponse<NasGasPrice>>

    @POST(URLConstants.NAS_GET_TRANSACTION_RECEIPT)
    fun getTransactionReceipt(@Body body: Map<String, String>): Call<NASResponse<NasTransactionReceipt>>


    data class CallParam(
            val from:String,
            val to:String,
            val value:String,
            val nonce:String,
            val gasPrice:String,
            val gasLimit:String,
            val contract:Map<String, String>
    )

    class CallResponse{
        var result:String?=null
    }
    @POST(URLConstants.NAS_CALL)
    fun call(@Body body: CallParam): Call<NASResponse<CallResponse>>
}