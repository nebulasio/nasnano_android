package io.nebulas.wallet.android.module.detail.fragment.transaction

import com.google.gson.stream.MalformedJsonException
import com.jakewharton.retrofit2.adapter.rxjava2.HttpException
import io.nebulas.wallet.android.network.callback.OnResultCallBack
import io.nebulas.wallet.android.network.exception.ApiException
import io.reactivex.exceptions.CompositeException
import org.json.JSONObject
import java.io.InterruptedIOException
import java.lang.Exception
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * Created by young on 2018/6/28.
 */
class ErrorHandler(ignoreSuperHandler: Boolean = false, block: ((Int, String) -> Unit)? = null) {

    private val callBack = object : OnResultCallBack<Any> {
        override fun onSuccess(t: Any) {

        }

        override fun onError(code: Int, errorMsg: String) {
            if (!ignoreSuperHandler) {
                super.onError(code, errorMsg)
            }
            block?.invoke(code, errorMsg)
        }
    }

    val defaultHandler = block@{ e: Throwable ->
        if (e is InterruptedIOException) {
            return@block
        }
        if (e is CompositeException) {
            e.exceptions.forEach {
                if (it is SocketTimeoutException) {
                    callBack.onError(ApiException.Code_TimeOut, ApiException.SOCKET_TIMEOUT_EXCEPTION)
                } else if (it is ConnectException) {
                    callBack.onError(ApiException.Code_UnConnected, ApiException.CONNECT_EXCEPTION)
                } else if (it is UnknownHostException) {
                    callBack.onError(ApiException.Code_UnConnected, ApiException.UNKNOWN_HOST_EXCEPTION)
                } else if (it is MalformedJsonException) {
                    callBack.onError(ApiException.Code_MalformedJson, ApiException.MALFORMED_JSON_EXCEPTION)
                }
            }
        } else if (e is SocketTimeoutException) {
            callBack.onError(ApiException.Code_TimeOut, ApiException.SOCKET_TIMEOUT_EXCEPTION)
        } else if (e is ConnectException) {
            callBack.onError(ApiException.Code_UnConnected, ApiException.CONNECT_EXCEPTION)
        } else if (e is UnknownHostException) {
            callBack.onError(ApiException.Code_UnConnected, ApiException.UNKNOWN_HOST_EXCEPTION)
        } else if (e is KotlinNullPointerException) {
            callBack.onError(ApiException.Code_UnConnected, ApiException.NULL_POINTER_EXCEPTION)
        } else {
            var msg: String? = null
            if (e is HttpException) {
                //可以拿到request请求时的host地址 -> #e.response().raw().request().url().host()
                msg = getErrorMessage(getErrorBodyString(e))
            }
            if (msg == null || msg.isEmpty()) {
                msg = e.message
            }
            var code: Int = 0
            if (msg!!.contains("#")) {
                code = msg.split("#")[0].toInt()
                callBack.onError(code, msg.split("#")[1])
            } else {
                code = ApiException.Code_Default
                callBack.onError(code, msg)
            }
        }
    }

    private fun getErrorBodyString(e: HttpException): String? {
        return try {
            e.response().errorBody()?.string()
        } catch (exception: Exception) {
            null
        }
    }

    private fun getErrorMessage(errorBody: String?): String? {
        if (errorBody == null) {
            return null
        }
        return try {
            val json = JSONObject(errorBody)
            var result = json.optString("msg")
            if (result == null || result.isEmpty()) {
                result = json.optString("error")
            }
            result
        } catch (e: Exception) {
            null
        }
    }


}