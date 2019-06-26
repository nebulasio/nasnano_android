package io.nebulas.wallet.android.util

import android.webkit.JavascriptInterface
import io.nebulas.wallet.android.common.DataCenter
import org.json.JSONArray
import org.json.JSONObject
import walletcore.Walletcore

/**
 * Created by Heinoc on 2018/6/4.
 */
class NebJSInterface {


    /**
     * getAllWalletInfo
     *
     * @return result:{"result":[{"wallet_name":"Wallet 1","neb_address":"n1oXdmwuo5jJRExnZR5rbceMEyzRsPeALgm"},{"wallet_name":"Wallet 2","neb_address":"n1oXdmwuo5jJRExnZR5rbceMEyzRsPeALgm"}]}
     */
    @JavascriptInterface
    fun getAllWalletInfo(msg: Any): String{
        val resultJson = JSONObject()

        val walletJsonArray = JSONArray()

        DataCenter.wallets.forEach {
            val walletJson = JSONObject()
            walletJson.put("wallet_name", it.walletName)

            run breakPoint@{
                DataCenter.addresses.forEach {
                    if (it.platform == Walletcore.NAS){
                        walletJson.put("neb_address", it.address)
                        return@breakPoint
                    }
                }
            }

            walletJsonArray.put(walletJson)

        }

        resultJson.put("result", walletJsonArray)

        return resultJson.toString()

    }

}