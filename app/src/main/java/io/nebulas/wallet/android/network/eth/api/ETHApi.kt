package io.nebulas.wallet.android.network.eth.api

import io.nebulas.wallet.android.network.eth.model.ETHRequestBody
import io.nebulas.wallet.android.network.eth.model.TransactionDetail
import io.reactivex.Observable
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * Created by Heinoc on 2018/3/1.
 */
interface ETHApi {

    @POST(".")
    fun sendETHRequest(@Body body: ETHRequestBody):Observable<ETHResponse<String>>

    @POST(".")
    fun getEthTransactionDetail(@Body body: ETHRequestBody):Observable<ETHResponse<TransactionDetail>>


}