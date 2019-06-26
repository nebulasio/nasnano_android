package io.nebulas.wallet.android.network.eth.convert

import com.google.gson.Gson
import io.nebulas.wallet.android.network.eth.api.ETHResponse
import io.nebulas.wallet.android.network.exception.ApiException
import io.nebulas.wallet.android.network.exception.ApiException.Companion.Code_Default
import okhttp3.ResponseBody
import retrofit2.Converter
import java.io.IOException
import java.lang.reflect.Type


/**
 * Created by Heinoc on 2018/3/1.
 */
internal class ETHGsonResponseBodyConverter<T>(private val gson: Gson, private val type: Type) : Converter<ResponseBody, T> {
    @Throws(IOException::class)
    override fun convert(value: ResponseBody?): T {
        val response = value?.string()
        try {
            val result = (gson.fromJson<Any>(response, ETHResponse::class.java) as ETHResponse<T>)
            return if (null == result.error || result.error?.code == 0){
                gson.fromJson<T>(response, type)
            }else{
                throw ApiException(Code_Default, result.error?.message?:"")
            }

        }finally {
            value?.close()
        }

    }
}