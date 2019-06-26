package io.nebulas.wallet.android.network.server.convert

import com.google.gson.Gson
import io.nebulas.wallet.android.network.SUCCESS_CODE
import io.nebulas.wallet.android.network.server.api.ApiResponse
import io.nebulas.wallet.android.network.exception.ApiException
import okhttp3.ResponseBody
import retrofit2.Converter
import java.io.IOException
import java.lang.reflect.Type


internal class GsonResponseBodyConverter<T>(private val gson: Gson, private val type: Type) : Converter<ResponseBody, T> {

    @Throws(IOException::class)
    override fun convert(value: ResponseBody): T? {
        val response = value.string()
        try {
            val result = (gson.fromJson<Any>(response, ApiResponse::class.java) as ApiResponse<T>)
            val code = result.code!!
            return if (code == SUCCESS_CODE) {
                gson.fromJson<T>(response, type)
            } else {
                throw ApiException(Integer.valueOf(code), result.msg!!)
            }
        } finally {
            value.close()
        }
    }
}