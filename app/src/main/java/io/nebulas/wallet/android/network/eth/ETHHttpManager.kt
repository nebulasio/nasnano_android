package io.nebulas.wallet.android.network.eth

import android.util.Log
import com.jakewharton.retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import io.nebulas.wallet.android.BuildConfig
import io.nebulas.wallet.android.common.Constants
import io.nebulas.wallet.android.common.URLConstants
import io.nebulas.wallet.android.network.eth.api.ETHApi
import io.nebulas.wallet.android.network.eth.api.ETHResponse
import io.nebulas.wallet.android.network.eth.convert.ETHGsonConverterFactory
import io.nebulas.wallet.android.network.eth.model.ETHRequestBody
import io.nebulas.wallet.android.network.eth.model.TransactionDetail
import io.nebulas.wallet.android.network.parser.DefaultUrlParser
import io.nebulas.wallet.android.network.parser.UrlParser
import io.nebulas.wallet.android.util.Formatter
import io.reactivex.Observable
import io.reactivex.Observer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.functions.Function
import io.reactivex.schedulers.Schedulers
import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * Created by Heinoc on 2018/2/6.
 */

object ETHHttpManager {

    private lateinit var mRetrofit: Retrofit
    private lateinit var okHttpClient: OkHttpClient
    val DEFAULT_TIMEOUT: Long = 10L
    private val mDomainNameHub = HashMap<String, HttpUrl>()
    private var mUrlParser: UrlParser = DefaultUrlParser()

    private lateinit var ethApi: ETHApi

    init {
        cacheCustomDomain()
        val builder = OkHttpClient.Builder()
        builder.connectTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .addInterceptor(initRedirectInterceptor())
                .addInterceptor(initHttpLoggingInterceptor())
                .addInterceptor { chain ->
                    val original = chain!!.request()
                    val requestBuilder = original.newBuilder().header("Content-Type", "application/json")
                    val request = requestBuilder.build()
                    chain.proceed(request)
                }

        okHttpClient = builder.build()

        setUrlHost(URLConstants.ETH_URL_HOST)

    }

    /**
     * 配置url host
     */
    fun setUrlHost(host: String) {
        mRetrofit = Retrofit.Builder()
                .addConverterFactory(ETHGsonConverterFactory.create())
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .baseUrl(host)
                .client(okHttpClient)
                .build()

        ethApi = mRetrofit.create(ETHApi::class.java)

    }

    /**
     * 缓存url
     */
    private fun cacheCustomDomain() {
        //在这里加入你要动态修改的URL
//        mDomainNameHub.put(DOMAIN_TEST, checkUrl(SECOND_URL))
    }

    /**
     * 重定向拦截器
     */
    private fun initRedirectInterceptor(): Interceptor {
        return Interceptor { it.proceed(processRequest(it.request())) }
    }

    /**
     * 打印调试信息
     */
    private fun initHttpLoggingInterceptor(): HttpLoggingInterceptor {
        val loggingInterceptor = HttpLoggingInterceptor(HttpLoggingInterceptor.Logger { message -> Log.i("HttpManager", message) })
        loggingInterceptor.level = HttpLoggingInterceptor.Level.BODY
        return loggingInterceptor
    }

    /**
     *解析Request的Header
     */
    private fun processRequest(request: Request): Request {
        var newBuilder = request.newBuilder()
//        var domainName = obtainDomainNameFromHeaders(request)
        var httpUrl: HttpUrl? = null
//        if (domainName.isNotEmpty()) {
//            httpUrl = fetchDomain(domainName)
//            newBuilder.removeHeader(DOMAIN)
//        }
        if (null != httpUrl) {
            val newUrl = mUrlParser.parseUrl(httpUrl, request.url())
            return newBuilder.url(newUrl).build()
        }
        return newBuilder.build()
    }

    /**
     * 解析请求的Header
     */
//    private fun obtainDomainNameFromHeaders(request: Request): String {
//        val headers = request.headers(DOMAIN)
//        if (headers == null || headers.size == 0)
//            return ""
//        if (headers.size > 1)
//            throw IllegalArgumentException("Only one Domain-Name in the headers")
//        return request.header(DOMAIN)
//    }

    /**
     *获得Header对应的HttpUrl
     */
    fun fetchDomain(domainName: String): HttpUrl? {
        return mDomainNameHub[domainName]
    }

    /**
     * 包装url类型String->HttpUrl
     */
    fun checkUrl(url: String): HttpUrl {
        val parseUrl = HttpUrl.parse(url)
        return parseUrl!!
    }

    /**
     * ETH ABI规范，如果智能合约调用参数位数不足64字符（32byte），则在其前边补0
     */
    private fun paddingParams(param: String): String {
        val sb = StringBuilder(param)
        while (sb.length < 64) {
            sb.insert(0, "0")
        }

        return sb.toString()
    }

    private fun <T> toSubscribe(o: Observable<ETHResponse<T>>, s: Observer<T>) {
        o.subscribeOn(Schedulers.io())
                .map(Function<ETHResponse<T>, T> { response ->
                    response.result
                }).unsubscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(s)
    }

    /**
     * 获取余额
     */
    fun getBalance(address: String, subscriber: Observer<String>) {
        val requestBody = ETHRequestBody(Constants.ETH_JSON_RPC_VERSION,
                URLConstants.ETH_GET_BALANCE,
                arrayOf(address, "latest"),
                Constants.ETH_CHAIN_ID)

        toSubscribe(ethApi.sendETHRequest(requestBody), subscriber)

    }

    /**
     * 获取指定合约货币ERC20的balance
     */
    fun getERC20Balance(contractAddress: String? = null, address: String, subscriber: Observer<String>) {
        val addressParams = address.replace("0x", "").replace("0X", "")

        val requestParams = mutableMapOf<String, String>()
        requestParams["to"] = contractAddress ?: BuildConfig.ERC20_NEBULAS_CONTRACT_ADDRESS
        // 0x70a08231 = balanceOf(address)
        requestParams["data"] = "0x70a08231${paddingParams(addressParams)}"

        val requestBody = ETHRequestBody(Constants.ETH_JSON_RPC_VERSION,
                URLConstants.ETH_CALL,
                arrayOf(requestParams, "latest"),
                Constants.ETH_CHAIN_ID)

        toSubscribe(ethApi.sendETHRequest(requestBody), subscriber)

    }

    /**
     * 获取eth_estimateGas
     */
    fun getEstimateGas(address:String, data: String, subscriber: Observer<String>) {

        val requestParams = mutableMapOf<String, String>()
        requestParams["data"] = data
        requestParams["from"] = address
        requestParams["to"] = BuildConfig.ERC20_NEBULAS_CONTRACT_ADDRESS
        requestParams["value"] = "0x0"

        val requestBody = ETHRequestBody(Constants.ETH_JSON_RPC_VERSION,
                URLConstants.ETH_ESTIMATE_GAS,
                arrayOf(requestParams),
                Constants.ETH_CHAIN_ID)

        toSubscribe(ethApi.sendETHRequest(requestBody), subscriber)

    }


    fun getNonce(address:String, subscriber: Observer<String>){
        val requestBody = ETHRequestBody(Constants.ETH_JSON_RPC_VERSION,
                URLConstants.ETH_GET_TRANSACTION_COUNT,
                arrayOf(address, "latest"),
                Constants.ETH_CHAIN_ID)
        toSubscribe(ethApi.sendETHRequest(requestBody), subscriber)
    }

    /**
     * 发送交易
     */
    fun sendRawTransaction(signedData: String, subscriber: Observer<String>) {

        val requestBody = ETHRequestBody(Constants.ETH_JSON_RPC_VERSION,
                URLConstants.ETH_SEND_RAW_TRANSACTION,
                arrayOf(signedData),
                Constants.ETH_CHAIN_ID)

        toSubscribe(ethApi.sendETHRequest(requestBody), subscriber)

    }

    /**
     * 根据hash获取交易详情
     */
    fun getTransactionDetailByHash(hash:String, subscriber: Observer<TransactionDetail>) {
        val requestBody = ETHRequestBody(Constants.ETH_JSON_RPC_VERSION,
                URLConstants.ETH_GET_TRANSACTION_RECEIPT,
                arrayOf(hash),
                Constants.ETH_CHAIN_ID)
        toSubscribe(ethApi.getEthTransactionDetail(requestBody), subscriber)
    }

    /**
     * 获取以太坊合约货币ERC20转账的payload信息
     */
    fun getETHContractPayload(toAddress: String, amount: String): String {
        val addressParams = toAddress.replace("0x", "").replace("0X", "")
        val amountHex = Formatter.stringToHex(amount)

        // 0xa9059cbb = transfer(address,uint256)
        return "0xa9059cbb${paddingParams(addressParams)}${paddingParams(amountHex)}"

    }

    /**
     * blockNumber
     */
    fun blockNumber(subscriber: Observer<String>) {

        val requestBody = ETHRequestBody(Constants.ETH_JSON_RPC_VERSION,
                URLConstants.ETH_BLOCK_NUMBER,
                arrayOf(),
                Constants.ETH_CHAIN_ID)

        toSubscribe(ethApi.sendETHRequest(requestBody), subscriber)

    }

}