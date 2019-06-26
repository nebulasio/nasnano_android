package io.nebulas.wallet.android.extensions

import android.util.Log
import io.nebulas.wallet.android.BuildConfig

/**
 * Created by young on 2018/6/6.
 */

fun Any.logD(msg: String?) {
    if (BuildConfig.DEBUG) {
        Log.d(getLogTag(), msg ?: "null")
    }
}

fun Any.logV(msg: String?) {
    if (BuildConfig.DEBUG) {
        Log.v(getLogTag(), msg ?: "null")
    }
}

fun Any.logI(msg: String?) {
    if (BuildConfig.DEBUG) {
        Log.i(getLogTag(), msg ?: "null")
    }
}

fun Any.logE(msg: String?) {
    if (BuildConfig.DEBUG) {
        Log.e(getLogTag(), msg ?: "null")
    }
}

private fun Any.getLogTag(): String {
    val prefix = "Logger : "
    val maxLength = 23 - prefix.length
    var tag = this::class.java.name
    if (tag.isNullOrEmpty()) {
        tag = toString()
    }
    if (tag.length > maxLength) {
        tag = tag.substring(tag.length - 1 - maxLength, tag.length - 1)
        tag = "$prefix$tag"
    }
    return tag
}