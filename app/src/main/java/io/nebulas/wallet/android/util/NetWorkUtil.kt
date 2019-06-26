package io.nebulas.wallet.android.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkInfo
import io.nebulas.wallet.android.app.WalletApplication

/**
 * Created by alina on 2018/7/16.
 */
class NetWorkUtil {

    companion object {
        val instance: NetWorkUtil by lazy { NetWorkUtil() }
    }


    fun isNetWorkConnected(): Boolean {
        try {
            val connectivityManager: ConnectivityManager = WalletApplication.INSTANCE.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val networkInfo: NetworkInfo = connectivityManager.activeNetworkInfo
            if (networkInfo != null) {
                return networkInfo.isAvailable
            }
        } catch (e: Exception) {
            return false
        }
        return false
    }
}