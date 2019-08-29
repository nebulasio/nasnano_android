package io.nebulas.wallet.android.network.server.api

import io.nebulas.wallet.android.base.BaseEntity
import io.nebulas.wallet.android.common.URLConstants
import io.nebulas.wallet.android.module.staking.AddressProfits
import io.nebulas.wallet.android.module.staking.StakingSummary
import io.nebulas.wallet.android.module.transaction.model.Transaction
import io.nebulas.wallet.android.network.server.model.*
import io.reactivex.Observable
import io.reactivex.Single
import retrofit2.Call
import retrofit2.http.*

/**
 * Created by Heinoc on 2018/2/6.
 */
interface ServerApi {

    /**
     * 获取应用版本信息
     */
    @GET(URLConstants.VERSION)
    fun getVersion(@HeaderMap headers: Map<String, String>,
                   @Query("versionCode") versionName: String): Observable<ApiResponse<VersionResp>>

    /**
     * 获取首页feed流
     */
    @POST(URLConstants.FEED_FLOW)
    fun getHomeFeedFlow(@HeaderMap headers: Map<String, String>,
                        @Body body: BalanceReq): Observable<ApiResponse<FeedFlowResp>>

    /**
     * 获取balance
     */
    @POST(URLConstants.BALANCE)
    fun getBalance(@HeaderMap headers: Map<String, String>,
                   @Body body: BalanceReq): Observable<ApiResponse<BalanceResp>>

    /**
     * 获取balance
     */
    @POST(URLConstants.BALANCE)
    fun getBalanceWithoutRX(@HeaderMap headers: Map<String, String>,
                            @Body body: BalanceReq): Call<ApiResponse<BalanceResp>>

    /**
     * 获取币列表
     */
    @GET(URLConstants.CURRENCY)
    fun getCurrencyList(@HeaderMap headers: Map<String, String>): Observable<ApiResponse<CurrencyResp>>

    /**
     * 获取币rpc host
     */
    @GET(URLConstants.CURRENCY_COIN)
    fun getCurrencyRpcHOST(@HeaderMap headers: Map<String, String>): Observable<ApiResponse<CurrencyRpcHOSTResp>>

    /**
     * 获取币的法币费率
     */
    @GET(URLConstants.CURRENCY_PRICE)
    fun getCurrencyPrice(@HeaderMap headers: Map<String, String>,
                         @Query("currencyId") currencyIds: List<String>): Observable<ApiResponse<CurrencyPriceResp>>

    /**
     * 获取币的法币费率
     */
    @GET(URLConstants.CURRENCY_PRICE)
    fun getCurrencyPriceWithoutRX(@HeaderMap headers: Map<String, String>,
                                  @Query("currencyId") currencyIds: List<String>): Call<ApiResponse<CurrencyPriceResp>>


    /**
     * 获取transactioin交易记录
     */
    @GET(URLConstants.TX)
    fun getTxRecords(@HeaderMap headers: Map<String, String>,
                     @Query("address") address: String,
                     @Query("currencyId") currencyId: String,
                     @Query("page") page: Int,
                     @Query("pageSize") pageSize: Int): Observable<ApiResponse<TxResp>>


    /**
     * 获取transactioin交易记录
     */
    @GET(URLConstants.TX)
    fun getTxRecordsWithoutRX(@HeaderMap headers: Map<String, String>,
                              @Query("address") address: String,
                              @Query("currencyId") currencyId: String,
                              @Query("page") page: Int,
                              @Query("pageSize") pageSize: Int): Call<ApiResponse<TxResp>>

    /**
     * 获取指定交易的交易详情
     */
    @GET(URLConstants.TX + "/{hash}")
    fun getTxDetail(@HeaderMap headers: Map<String, String>,
                    @Path("hash") hash: String,
                    @Query("currencyId") currencyId: String): Observable<ApiResponse<Transaction>>

    /**
     * 获取指定交易的交易详情
     */
    @GET(URLConstants.TX + "/{hash}")
    fun getTxDetailWithoutRX(@HeaderMap headers: Map<String, String>,
                             @Path("hash") hash: String,
                             @Query("currencyId") currencyId: String): Call<ApiResponse<Transaction>>

    /**
     * 获取单个钱包的交易详情
     */
    @POST(URLConstants.TX_WALLET)
    fun getTxRecordsByWallet(@HeaderMap headers: Map<String, String>,
                             @Body walletReq: WalletReq): Observable<ApiResponse<MutableList<Transaction>>>

    /**
     * 获取gas price
     */
    @GET(URLConstants.GAS_PRICE)
    fun getGasPrice(@HeaderMap headers: Map<String, String>,
                    @Query("currencyId") currencyId: String,
                    @Query("address") address: String,
                    @Query("type") type: Int,
                    @Query("contract") contractJsonString: String): Observable<ApiResponse<GasPriceResp>>

    /**
     * 获取gas price
     */
    @GET(URLConstants.GAS_PRICE)
    fun getGasPriceWithoutRX(@HeaderMap headers: Map<String, String>,
                             @Query("currencyId") currencyId: String,
                             @Query("address") address: String,
                             @Query("type") type: Int,
                             @Query("contract") contractJsonString: String): Call<ApiResponse<GasPriceResp>>


    /**
     * 向DApp服务器发送指定payId的txHash
     *
     * 注：url没有数据就加 . 或者 /
     */
    @POST(".")
    fun sendTxHashToDAppServer(@Query("payId") payId: String, @Query("txHash") txHash: String): Observable<ApiResponse<Any>>

    //    @GET("tx")
//    fun getTxRecordList(@QueryMap params: Map<String,String>): Call<Transaction>
    @GET("tx")
    fun getTxRecordList(@QueryMap params: Map<String, String>): Observable<ApiResponse<Transaction>>

    @POST(URLConstants.BIND_SWAP_ADDRESS)
    fun bindSwapAddressWithoutRX(@HeaderMap headers: Map<String, String>, @Body body: BindSwapAddressRequest): Call<ApiResponse<String>>

    /**
     * 获取换币详情
     */
    @GET(URLConstants.SWAP_TRANSFER_DETAIL)
    fun getSwapTransferDetail(@HeaderMap headers: Map<String, String>,
                              @Query("eth_tx_hash") ethTxHash: String): Observable<ApiResponse<SwapTransferResp>>

    /**
     * 获取换币列表
     */
    @GET(URLConstants.SWAP_TRANSFER_LIST)
    fun getSwapTransferList(@HeaderMap headers: Map<String, String>,
                            @Query("eth_address") ethAddress: String,
                            @Query("page") page: String,
                            @Query("page_size") pageSize: String): Observable<ApiResponse<MutableList<SwapTransferResp>>>


    /**
     * 首页换币入口
     */
    @POST(URLConstants.SWAP_ENTRANCE)
    fun swapEntrance(@HeaderMap headers: Map<String, String>, @Body eth_address: SwapEntranceReq): Observable<ApiResponse<SwapEntranceResp>>


    /**
     * 首页换币入口
     */
    @POST(URLConstants.SWAP_HASH_UPLOAD)
    fun uploadSwapHash(@HeaderMap headers: Map<String, String>, @Body eth_address: SwapTransactionInfoRequest): Single<ApiResponse<String>>

    /**
     * 更新设备信息
     */
    @POST(URLConstants.URL_UPDATE_DEVICE_INFO)
    fun updateDeviceInfo(@HeaderMap headers: Map<String, String>, @Body deviceInfoRequest: DeviceInfoRequest): Call<ApiResponse<BaseEntity>>

    /**
     * 更新推送开关（目前只处理交易推送）
     */
    @POST(URLConstants.URL_NOTIFICATION_SWITCH)
    fun notificationSwitch(@HeaderMap headers: Map<String, String>, @Body notificationSwitchRequest: NotificationSwitchRequest): Call<ApiResponse<BaseEntity>>

    /**
     * 获取质押相关的合约地址等信息(NAX)
     */
    @GET(URLConstants.URL_GET_STAKING_CONTRACTS)
    fun getStakingContracts(@HeaderMap headers: Map<String, String>): Call<ApiResponse<StakingContractsResponse>>

    /**
     * 获取质押相关汇总信息(NAX)
     */
    @GET(URLConstants.URL_GET_STAKING_SUMMARY)
    fun getStakingSummaryInfo(@HeaderMap headers: Map<String, String>, @Query("addresses") addresses: String): Call<ApiResponse<StakingSummary>>

    /**
     * 获取但钱包质押收益列表(NAX)
     */
    @GET(URLConstants.URL_GET_NAX_PROFITS)
    fun getProfitsInfo(@HeaderMap headers: Map<String, String>,
                       @Query("address") address: String,
                       @Query("page") page: Int,
                       @Query("pageSize") pageSize: Int = 10): Call<ApiResponse<AddressProfits>>
}