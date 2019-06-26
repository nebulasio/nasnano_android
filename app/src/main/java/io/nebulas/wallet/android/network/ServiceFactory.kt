package io.nebulas.wallet.android.network

import io.nebulas.wallet.android.BuildConfig
import io.nebulas.wallet.android.common.URLConstants
import io.nebulas.wallet.android.network.server.api.ServerApi
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Created by Heinoc on 2018/2/6.
 */
object ServiceFactory {

    fun getClient():OkHttpClient{
        var builder = OkHttpClient.Builder()

        if (BuildConfig.DEBUG){
            //log interceptor
            var interceptor = HttpLoggingInterceptor()
            interceptor.level = HttpLoggingInterceptor.Level.BODY
            builder.addNetworkInterceptor(interceptor)
        }

        return builder.build()

    }

    fun getTransactionService(): ServerApi {
        return Retrofit.Builder()
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                .baseUrl(URLConstants.URL_HOST)
                .client(getClient())
                .build()
                .create(ServerApi::class.java)
    }

}