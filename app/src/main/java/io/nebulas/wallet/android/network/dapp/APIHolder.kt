package io.nebulas.wallet.android.network.dapp

import com.jakewharton.retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import io.nebulas.wallet.android.network.server.HttpManager
import io.nebulas.wallet.android.network.server.convert.CustomGsonConverterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

object APIHolder {

//    private val okHttpClient: OkHttpClient by lazy {
//        val builder = OkHttpClient.Builder()
//        builder.connectTimeout(HttpManager.DEFAULT_TIMEOUT, TimeUnit.SECONDS)
//                .retryOnConnectionFailure(true)
//                .addInterceptor(HttpManager.initRedirectInterceptor())
//                .addInterceptor(HttpManager.initHttpLoggingInterceptor())
//                .addInterceptor { chain ->
//                    val original = chain.request()
//                    val requestBuilder = original.newBuilder().header("Content-Type", "application/json")
//                    val request = requestBuilder.build()
//                    chain.proceed(request)
//                }
//        builder.build()
//    }

    private val okHttpClient = OkHttpClient.Builder().apply {
        retryOnConnectionFailure(true)
                .addInterceptor(HttpManager.initRedirectInterceptor())
                .addInterceptor(HttpManager.initHttpLoggingInterceptor())
                .addInterceptor { chain ->
                    val original = chain.request()
                    val requestBuilder = original.newBuilder().header("Content-Type", "application/json")
                    val request = requestBuilder.build()
                    chain.proceed(request)
                }
    }.build()

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
                .addConverterFactory(CustomGsonConverterFactory.create())
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .baseUrl("http://192.168.0.1/") //just for initialize, see #getDAppServerAPI(String)
                .client(okHttpClient)
                .build()
    }

    fun getDAppServerAPI(baseUrl: String): DAppServerApi {
        var url = baseUrl
        if (!baseUrl.endsWith("/")) {
            url = baseUrl.plus("/")
        }
        val builder = retrofit.newBuilder()
        builder.baseUrl(url)
        return builder.build().create(DAppServerApi::class.java)
    }

}