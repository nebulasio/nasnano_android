package io.nebulas.wallet.android.module.launch

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import io.nebulas.wallet.android.common.Constants
import io.nebulas.wallet.android.common.DataCenter
import org.jetbrains.anko.AnkoLogger
import org.json.JSONException
import org.json.JSONObject
import java.net.URLDecoder


class H5RaiseDeliverActivity : AppCompatActivity(), AnkoLogger {

    companion object {
        private const val REQUEST_CODE_TRANSFER = 65533
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        /**
         * App唤起协议
         *
         * openapp.nasnano://virtual?params={"category":"jump","des":"confirmTransfer","pageParam":{"productid":"547147","goodsid":""}}
         *
         * @category 唤起类别：jump：跳转到指定（@des）页面；
         * @des 跳转页面类型
         * @pageParam 页面跳转参数的json串
         */
        val intent = intent
        val scheme = intent.scheme
        val uri = intent.data

        Log.d("H5RaiseDeliverActivity", "scheme:$scheme")

        if (uri != null) {
            val host = uri.host
            val dataString = intent.dataString
            val id = uri.getQueryParameter("id")
            val path = uri.path
            val path1 = uri.encodedPath
            val queryString = URLDecoder.decode(uri.query, "utf-8")

            Log.d("H5RaiseDeliverActivity", "host:$host")
            Log.d("H5RaiseDeliverActivity", "dataString:$dataString")
            Log.d("H5RaiseDeliverActivity", "id:$id")
            Log.d("H5RaiseDeliverActivity", "path:$path")
            Log.d("H5RaiseDeliverActivity", "path1:$path1")
            Log.d("H5RaiseDeliverActivity", "queryString:$queryString")

            try {
                val jsonObject = if (queryString.indexOf("{") >= 0) {
                    JSONObject(queryString.substring(queryString.indexOf("{")))
                } else {
                    JSONObject()
                }

                Log.d("H5RaiseDeliverActivity", jsonObject.toString())

                if ("jump" == jsonObject.optString("category")) {

                    val des = jsonObject.optString("des")

                    if ("confirmTransfer" == des) {

                        /**
                         * 将交易信息存储到DataCenter
                         */
                        val pageParams: JSONObject? = jsonObject.optJSONObject("pageParams")
                        val gotoTransfer = pageParams?.run {
                            DataCenter.setData(Constants.KEY_DAPP_TRANSFER_JSON, jsonObject.optJSONObject("pageParams").toString())

                            true
                        } ?: false

//                        if (null != pageParams) {
                            LaunchActivity.launch(this, gotoTransfer)
//                        }


//                        if (isAppIsInBackground(this)) {

//                        } else {
//                            TransferActivity.launch(this,
//                                    REQUEST_CODE_TRANSFER,
//                                    true,
//                                    H5RaiseDeliverActivity::class.java.name,
//                                    "",
//                                    true)
//                        }

                    }

                    this.finish()

                }

            } catch (e: JSONException) {
                e.printStackTrace()
            }

        }

    }

    private fun isAppIsInBackground(context: Context): Boolean {
        var isInBackground = true
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT_WATCH) {
            val runningProcesses = am.runningAppProcesses
            for (processInfo in runningProcesses) {
                //前台程序
                if (processInfo.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                    for (activeProcess in processInfo.pkgList) {
                        if (activeProcess == context.getPackageName()) {
                            isInBackground = false
                        }
                    }
                }
            }
        } else {
            val taskInfo = am.getRunningTasks(1)
            val componentInfo = taskInfo[0].topActivity
            if (componentInfo.packageName == context.getPackageName()) {
                isInBackground = false
            }
        }

        return isInBackground
    }

}
