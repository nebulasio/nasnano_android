package io.nebulas.wallet.android.extensions

import android.content.Context

/**
 * Created by young on 2018/6/5.
 */

inline fun <reified T : Any> Context.sharedPreference(fileName: String,
                                                      key: String,
                                                      defaultValue: T? = null): T? {
    val sp = getSharedPreferences(fileName, Context.MODE_PRIVATE)
    val all = sp.all
    return if (all.containsKey(key)) {
        all[key] as? T ?: defaultValue
    } else {
        defaultValue
    }
}