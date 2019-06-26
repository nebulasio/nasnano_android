package io.nebulas.wallet.android.network.nas

import android.util.Log
import com.jakewharton.retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import io.nebulas.wallet.android.common.URLConstants
import io.nebulas.wallet.android.module.transaction.model.BlockStateModel
import io.nebulas.wallet.android.network.nas.api.NASApi
import io.nebulas.wallet.android.network.nas.api.NASResponse
import io.nebulas.wallet.android.network.nas.convert.NASGsonConverterFactory
import io.nebulas.wallet.android.network.nas.model.NASTransactionModel
import io.nebulas.wallet.android.network.nas.model.NasBalanceModel
import io.nebulas.wallet.android.network.parser.DefaultUrlParser
import io.nebulas.wallet.android.network.parser.UrlParser
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
import retrofit2.Call
import retrofit2.Retrofit
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * Created by Heinoc on 2018/3/2.
 */
object NASHttpManager {

    private lateinit var mRetrofit: Retrofit
    private var okHttpClient: OkHttpClient

    private lateinit var nasApi: NASApi

    val DEFAULT_TIMEOUT: Long = 10L
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
                    val original = chain!!.request()
                    val requestBuilder = original.newBuilder().header("Content-Type", "application/json")
                    val request = requestBuilder.build()
                    chain.proceed(request)
                }

        okHttpClient = builder.build()

        setUrlHost(URLConstants.NAS_URL_HOST)

    }

    fun getApi(): NASApi = nasApi

    /**
     * 配置url host
     */
    fun setUrlHost(host: String){
        mRetrofit = Retrofit.Builder()
                .addConverterFactory(NASGsonConverterFactory.create())
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .baseUrl(host)
                .client(okHttpClient)
                .build()

        nasApi = mRetrofit.create(NASApi::class.java)

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


    private fun <T> toSubscribe(o: Observable<NASResponse<T>>, s: Observer<T>) {
        o.subscribeOn(Schedulers.io())
                .map(Function<NASResponse<T>, T> { response ->
                    response.result
                }).unsubscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(s)
    }

    /**
     * 获取余额
     */
    fun getBalance(body: Map<String, String>, subscriber: Observer<NasBalanceModel>) {
        toSubscribe(nasApi.getBalance(body), subscriber)
    }

    /**
     * 发送交易
     */
    fun sendRawTransaction(body: Map<String, String>, subscriber: Observer<NASTransactionModel>){
        toSubscribe(nasApi.sendRawTransaction(body), subscriber)
    }

    /**
     * 获取当前星云链的状态
     */
    fun getNebState(subscriber: Observer<BlockStateModel>){
        toSubscribe(nasApi.getNebState(), subscriber)
    }

    /**
     * 获取当前星云链的状态
     */
    fun getNebStateWithoutRX(): Call<NASResponse<BlockStateModel>> {
        return nasApi.getNebStateWithoutRX()
    }

}