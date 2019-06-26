package io.nebulas.wallet.android.network.nas.convert

import com.google.gson.Gson
import io.nebulas.wallet.android.network.exception.ApiException
import io.nebulas.wallet.android.network.exception.ApiException.Companion.Code_Default
import io.nebulas.wallet.android.network.nas.api.NASResponse
import okhttp3.ResponseBody
import retrofit2.Converter
import java.io.IOException
import java.lang.reflect.Type


/**
 * Created by Heinoc on 2018/3/2.
 */
internal class NASGsonResponseBodyConverter<T>(private val gson: Gson, private val type: Type) : Converter<ResponseBody, T> {
    @Throws(IOException::class)
    override fun convert(value: ResponseBody?): T {
        var response = value?.string()

        value.use { value ->
            val result = (gson.fromJson<Any>(response, NASResponse::class.java) as NASResponse<T>)
            return if (result.error.isNullOrEmpty()) {
                gson.fromJson<T>(response, type)
            } else {
                throw ApiException(Code_Default, result.error!!)
            }
        }

    }
}