package io.nebulas.wallet.android.network.subscriber

import android.arch.lifecycle.Lifecycle
import android.arch.lifecycle.LifecycleObserver
import android.arch.lifecycle.OnLifecycleEvent
import com.google.gson.stream.MalformedJsonException
import com.jakewharton.retrofit2.adapter.rxjava2.HttpException
import io.nebulas.wallet.android.network.callback.OnResultCallBack
import io.nebulas.wallet.android.network.exception.ApiException
import io.reactivex.Observer
import io.reactivex.disposables.Disposable
import io.reactivex.exceptions.CompositeException
import org.json.JSONObject
import java.lang.Exception
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * Created by Heinoc on 2018/2/6.
 */

class HttpSubscriber<T>(listener: OnResultCallBack<T>, lifecycle: Lifecycle) : Observer<T>, LifecycleObserver {

    var mOnResultListener: OnResultCallBack<T>? = null

    lateinit var mDisposable: Disposable

    init {
        mOnResultListener = listener
        lifecycle.addObserver(this)
    }

    override fun onNext(t: T) {
        mOnResultListener?.onSuccess(t)
        mDisposable.dispose()
    }

    override fun onSubscribe(d: Disposable) {
        mDisposable = d
    }

    override fun onError(e: Throwable) {
        if (e is CompositeException) {
            e.exceptions.forEach {
                if (it is SocketTimeoutException) {
                    mOnResultListener?.onError(ApiException.Code_TimeOut, ApiException.SOCKET_TIMEOUT_EXCEPTION)
                } else if (it is ConnectException) {
                    mOnResultListener?.onError(ApiException.Code_UnConnected, ApiException.CONNECT_EXCEPTION)
                } else if (it is UnknownHostException) {
                    mOnResultListener?.onError(ApiException.Code_UnConnected, ApiException.UNKNOWN_HOST_EXCEPTION)
                } else if (it is MalformedJsonException) {
                    mOnResultListener?.onError(ApiException.Code_MalformedJson, ApiException.MALFORMED_JSON_EXCEPTION)
                }
            }
        } else if (e is SocketTimeoutException) {
            mOnResultListener?.onError(ApiException.Code_TimeOut, ApiException.SOCKET_TIMEOUT_EXCEPTION)
        } else if (e is ConnectException) {
            mOnResultListener?.onError(ApiException.Code_UnConnected, ApiException.CONNECT_EXCEPTION)
        } else if (e is UnknownHostException) {
            mOnResultListener?.onError(ApiException.Code_UnConnected, ApiException.UNKNOWN_HOST_EXCEPTION)
        } else if (e is KotlinNullPointerException) {
            mOnResultListener?.onError(ApiException.Code_UnConnected, ApiException.NULL_POINTER_EXCEPTION)
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
                mOnResultListener?.onError(code, msg.split("#")[1])
            } else {
                code = ApiException.Code_Default
                mOnResultListener?.onError(code, msg)
            }
        }
    }

    private fun getErrorBodyString(e:HttpException):String?{
        return try {
            e.response().errorBody()?.string()
        }catch (exception:Exception){
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

    override fun onComplete() {
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    fun unSubscribe() {
        try {
            if (!mDisposable.isDisposed) {
                mDisposable.dispose()
            }
        } catch (e: Exception) {
            error(e.message!!)
        }
    }
}