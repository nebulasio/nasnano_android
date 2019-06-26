package io.nebulas.wallet.android.util

import android.annotation.TargetApi
import android.app.Activity
import android.content.Context
import android.os.Build
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import io.nebulas.wallet.android.R
import io.nebulas.wallet.android.base.BaseActivity
import io.nebulas.wallet.android.common.Constants
import io.nebulas.wallet.android.common.DataCenter
import io.nebulas.wallet.android.module.html.HtmlActivity
import io.nebulas.wallet.android.module.launch.H5RaiseDeliverActivity
import io.nebulas.wallet.android.module.transaction.transfer.TransferActivity
import io.nebulas.wallet.android.module.wallet.create.CreateWalletActivity
import org.json.JSONObject
import java.net.URLDecoder

/**
 * Created by Heinoc on 2018/5/24.
 */
open class BaseWebViewClient(var context: Context) : WebViewClient() {

    override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
        return if (handleUrl(url!!))
            true
        else
            return super.shouldOverrideUrlLoading(view, url)
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {

        return if (handleUrl(request!!.url.toString()))
            true
        else
            return super.shouldOverrideUrlLoading(view, request)
    }

    private fun handleUrl(url: String): Boolean {
//        Log.d("overrideUrl", url)

        if (isRaiseUpApp(url)) {

//            var intent = Intent()
//            intent.action = "android.intent.action.VIEW"
//            intent.data = Uri.parse(url)
//            context.startActivity(intent)

            var decodeUrl = URLDecoder.decode(url, "utf-8")
            val jsonObject = JSONObject(decodeUrl.substring(decodeUrl.indexOf("{")))
            if ("jump" == jsonObject.optString("category")) {
                val des = jsonObject.optString("des")

                val pageParams = jsonObject.optJSONObject("pageParams")

                when (des) {
                    "confirmTransfer" -> {

                        if (DataCenter.wallets.isEmpty()) {
                            (context as BaseActivity).showTipsDialog(context.getString(R.string.tips_title),
                                    context.getString(R.string.no_wallet_now),
                                    context.getString(R.string.no_wallet_cancel_btn),
                                    {},
                                    context.getString(R.string.no_wallet_add_btn),
                                    {
                                        CreateWalletActivity.launch(context as BaseActivity, CreateWalletActivity.CREATE_CHECK_FROM_WEB,showBackBtn = true)
                                    })

                        } else {

                            /**
                             * 增加App内部支付字段
                             */
                            pageParams.put("innerPay", true)
                            /**
                             * 将交易信息存储到DataCenter
                             */
                            DataCenter.setData(Constants.KEY_DAPP_TRANSFER_JSON, pageParams.toString())
                            TransferActivity.launch(context as Activity,
                                    36271,
                                    true,
                                    H5RaiseDeliverActivity::class.java.name,
                                    "",
                                    true)
                        }
                    }

                    "Html" -> {
                        HtmlActivity.launch(context, pageParams.optString("url"), pageParams.optString("title"))

                    }

                }
            }

            return true
        }

        return false
    }


    private fun isRaiseUpApp(url: String): Boolean {
        return url.toLowerCase().startsWith("openapp.nasnano")
    }

}