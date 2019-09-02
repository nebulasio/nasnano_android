package io.nebulas.wallet.android.network.server

import android.os.Build
import android.util.Log
import com.jakewharton.retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import io.nebulas.wallet.android.BuildConfig
import io.nebulas.wallet.android.app.WalletApplication
import io.nebulas.wallet.android.common.Constants
import io.nebulas.wallet.android.module.transaction.model.Transaction
import io.nebulas.wallet.android.network.cache.CacheInterceptor
import io.nebulas.wallet.android.network.parser.DefaultUrlParser
import io.nebulas.wallet.android.network.parser.UrlParser
import io.nebulas.wallet.android.network.server.api.ApiResponse
import io.nebulas.wallet.android.network.server.api.ServerApi
import io.nebulas.wallet.android.network.server.convert.CustomGsonConverterFactory
import io.nebulas.wallet.android.network.server.model.*
import io.nebulas.wallet.android.util.Util
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
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.*
import java.util.concurrent.TimeUnit
import javax.net.ssl.*

/**
 * Created by Heinoc on 2018/2/6.
 */

object HttpManager {

    lateinit var mRetrofit: Retrofit
    private var okHttpClient: OkHttpClient

    private lateinit var serverApi: ServerApi

    /**
     * timeout
     */
    const val DEFAULT_TIMEOUT: Long = 20L

    private var headerMap = mutableMapOf<String, String>()

    private val mDomainNameHub = HashMap<String, HttpUrl>()
    private var mUrlParser: UrlParser = DefaultUrlParser()

    init {
        cacheCustomDomain()

        val builder = OkHttpClient.Builder()
        builder.connectTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .addInterceptor(initRedirectInterceptor())
                .addInterceptor(initHttpLoggingInterceptor())
                .addInterceptor { chain ->
                    val original = chain.request()
                    val requestBuilder = original.newBuilder().header("Content-Type", "application/json")
                    val request = requestBuilder.build()
                    chain.proceed(request)
                }
                .addInterceptor(CacheInterceptor()) //自定义缓存处理拦截器
                .hostnameVerifier(TrustAllHostnameVerifier())

        val sslFactory = createSSLSocketFactory()
        sslFactory?.apply {
            builder.sslSocketFactory(this)
        }

        okHttpClient = builder.build()

        setUrlHost()

    }

    fun getHeaderMap(): Map<String, String> {
        val map = mutableMapOf<String, String>()
        map["Content-Type"] = "application/json"
        map["Accept"] = "application/json;charset=UTF-8"
        map["x-neb-os"] = "android"
        map["x-neb-appid"] = "nasnano"
        map["x-neb-version"] = Util.appVersion(WalletApplication.INSTANCE)
        map["x-neb-cunit"] = Constants.CURRENCY_SYMBOL_NAME
        map["x-neb-language"] = Util.getCurLanguage()
        map["x-neb-deviceid"] = WalletApplication.INSTANCE.uuid
        map["x-neb-channel"] = WalletApplication.INSTANCE.channel
        map["x-neb-model"] = Build.MODEL
        return map

    }

    class TrustAllCerts : X509TrustManager {
        override fun checkClientTrusted(p0: Array<out X509Certificate>?, p1: String?) {}

        override fun checkServerTrusted(p0: Array<out X509Certificate>?, p1: String?) {}

        override fun getAcceptedIssuers(): Array<X509Certificate> {
            return emptyArray()
        }
    }

    class TrustAllHostnameVerifier : HostnameVerifier {
        override fun verify(p0: String?, p1: SSLSession?): Boolean {
            return true
        }
    }

    private fun createSSLSocketFactory(): SSLSocketFactory? {
        return try {
            val sslContext = SSLContext.getInstance("TLS")
            sslContext.init(null, arrayOf(TrustAllCerts()), SecureRandom())
            sslContext.socketFactory
        } catch (e: Exception) {
            null
        }
    }

    fun getServerApi(): ServerApi = serverApi

    /**
     * 配置url host
     */
    fun setUrlHost() {
        mRetrofit = Retrofit.Builder()
                .addConverterFactory(CustomGsonConverterFactory.create())
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .baseUrl(BuildConfig.URL_HOST)
                .client(okHttpClient)
                .build()



        serverApi = mRetrofit.create(ServerApi::class.java)

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
    fun initRedirectInterceptor(): Interceptor {
        return Interceptor { it.proceed(processRequest(it.request())) }
    }

    /**
     * 打印调试信息
     */
    fun initHttpLoggingInterceptor(): HttpLoggingInterceptor {
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


    private fun <T> toSubscribe(o: Observable<ApiResponse<T>>, s: Observer<T>) {
        o.subscribeOn(Schedulers.io())
                .map(Function<ApiResponse<T>, T> { response ->
                    response.data
                }).unsubscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(s)
    }

    /******************  server api  *********************/
    /**
     * 获取应用版本信息
     */
    fun getVersion(versionName: String, subscriber: Observer<VersionResp>) {
        toSubscribe(serverApi.getVersion(getHeaderMap(), versionName), subscriber)
    }

    /**
     * 获取首页feed流
     */
    fun getHomeFeedFlow(body: BalanceReq, subscriber: Observer<FeedFlowResp>) {
        toSubscribe(serverApi.getHomeFeedFlow(getHeaderMap(), body), subscriber)
    }

    /**
     * 获取balance
     */
    fun getBalance(body: BalanceReq, subscriber: Observer<BalanceResp>) {
        toSubscribe(serverApi.getBalance(getHeaderMap(), body), subscriber)
    }

    /**
     * 获取币列表
     */
    fun getCurrencyList(subscriber: Observer<CurrencyResp>) {
        toSubscribe(serverApi.getCurrencyList(getHeaderMap()), subscriber)
    }

    /**
     * 获取币rpc host
     */
    fun getCurrencyRpcHOST(subscriber: Observer<CurrencyRpcHOSTResp>) {
        toSubscribe(serverApi.getCurrencyRpcHOST(getHeaderMap()), subscriber)
    }

    /**
     * 获取币的法币费率
     */
    fun getCurrencyPrice(currencyIds: List<String>, subscriber: Observer<CurrencyPriceResp>) {
        toSubscribe(serverApi.getCurrencyPrice(getHeaderMap(), currencyIds), subscriber)
    }

    /**
     * 获取transactioin交易记录
     */
    fun getTxRecords(address: String, currencyId: String, page: Int, pageSize: Int, subscriber: Observer<TxResp>) {
        toSubscribe(serverApi.getTxRecords(getHeaderMap(), address, currencyId, page, pageSize), subscriber)
    }

    /**
     * 获取指定交易的交易详情
     */
    fun getTxDetail(hash: String, currencyId: String, subscriber: Observer<Transaction>) {
        toSubscribe(serverApi.getTxDetail(getHeaderMap(), hash, currencyId), subscriber)
    }

    /**
     * 获取单个钱包的交易详情
     */
    fun getTxRecordsByWallet(walletReq: WalletReq, subscriber: Observer<MutableList<Transaction>>) {
        toSubscribe(serverApi.getTxRecordsByWallet(getHeaderMap(), walletReq), subscriber)
    }

    /**
     * 获取gasPrice
     */
    fun getGasPrice(currencyId: String, address: String, type: Int, contractJsonString: String, subscriber: Observer<GasPriceResp>) {
        toSubscribe(serverApi.getGasPrice(getHeaderMap(), currencyId, address, type, contractJsonString), subscriber)
    }

    /**
     * 向DApp服务器发送指定payId的txHash
     */
    fun sendTxHashToDAppServer(payId: String, txHash: String, subscriber: Observer<Any>) {
        toSubscribe(serverApi.sendTxHashToDAppServer(payId, txHash), subscriber)
    }

    /******************  server api  *********************/

    fun getRecord(subscriber: Observer<Transaction>, params: Map<String, String>) {
        toSubscribe(serverApi.getTxRecordList(params), subscriber)
    }


    /**
     * 换币详情
     */
    fun getSwapDetail(ethTxHash: String, subscriber: Observer<SwapTransferResp>) {
        toSubscribe(serverApi.getSwapTransferDetail(getHeaderMap(), ethTxHash), subscriber)
    }

    /**
     * 换币列表
     */
    fun getSwapList(ethAddress: String, page: String, pageSize: String, subscriber: Observer<MutableList<SwapTransferResp>>) {
        toSubscribe(serverApi.getSwapTransferList(getHeaderMap(), ethAddress, page, pageSize), subscriber)
    }

    /**
     * 首页换币入口
     */
    fun swapEntrance(eth_address: SwapEntranceReq, subscriber: Observer<SwapEntranceResp>) {
        toSubscribe(serverApi.swapEntrance(getHeaderMap(), eth_address), subscriber)
    }
}