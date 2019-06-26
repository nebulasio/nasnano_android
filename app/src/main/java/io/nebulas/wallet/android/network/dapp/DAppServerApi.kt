package io.nebulas.wallet.android.network.dapp

import io.nebulas.wallet.android.network.server.api.ApiResponse
import retrofit2.Call
import retrofit2.http.POST
import retrofit2.http.Query

interface DAppServerApi {

    /**
     * 向DApp服务器发送指定payId的txHash
     *
     * 注：url没有数据就加 . 或者 /
     */
    @POST(".")
    fun sendTxHashToDAppServer(@Query("payId") payId: String, @Query("txHash") txHash: String): Call<ApiResponse<Any>>
}