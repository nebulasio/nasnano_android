package io.nebulas.wallet.android.network.exception

import io.nebulas.wallet.android.R
import io.nebulas.wallet.android.app.WalletApplication
import java.lang.RuntimeException

/**
 * Created by Heinoc on 2018/2/6.
 */

class ApiException(resultCode: Int, msg: String) : RuntimeException(getApiExceptionMessage(resultCode, msg)) {
    companion object {
        val Code_TimeOut = 1000
        val Code_UnConnected = 1001
        val Code_MalformedJson = 1020
        val Code_Default = 1003
        var CONNECT_EXCEPTION: String = ""
            get() = WalletApplication.INSTANCE.activity?.getString(R.string.network_connect_exception) ?: ""
        var UNKNOWN_HOST_EXCEPTION = ""
            get() = WalletApplication.INSTANCE.activity?.getString(R.string.unknown_host_exception) ?: ""
        var SOCKET_TIMEOUT_EXCEPTION = ""
            get() = WalletApplication.INSTANCE.activity?.getString(R.string.network_timeout_exception) ?: ""
        var MALFORMED_JSON_EXCEPTION = ""
            get() = WalletApplication.INSTANCE.activity?.getString(R.string.json_parse_failed) ?: ""
        var NULL_POINTER_EXCEPTION = ""
            get() = WalletApplication.INSTANCE.activity?.getString(R.string.null_pointer_exception) ?: ""
    }

    init {
        var result = getApiExceptionMessage(resultCode, msg)
    }
}

fun getApiExceptionMessage(resultCode: Int, msg: String): String {
    val message: String
    when (resultCode) {
        else -> message = resultCode.toString() + "#" + msg
    }
    return message
}