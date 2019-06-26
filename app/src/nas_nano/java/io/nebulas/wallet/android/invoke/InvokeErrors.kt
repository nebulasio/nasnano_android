package io.nebulas.wallet.android.invoke

import android.os.Bundle

open class InvokeError(val errorCode: Int, private val _errorMessage: String) {

    companion object {
        val success = InvokeError(0, "")
        val errorUnsupportedCategory: InvokeError = InvokeError(10001, "不支持的调用类型")
        val errorInvalidParameter: InvokeError = InvokeError(10002, "参数错误")
        val errorNoWallet: InvokeError = InvokeError(10003, "没有钱包")
    }

    open fun getErrorMessage(): String {
        return _errorMessage
    }

    fun toBundle(): Bundle {
        return Bundle().apply {
            putInt("errorCode", errorCode)
            putString("errorMessage", getErrorMessage())
        }
    }

}