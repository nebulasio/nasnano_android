package io.nebulas.wallet.android.push.message.channel.mi

import android.app.ActivityManager
import android.app.Application
import android.content.Context
import android.os.Process
import android.util.Log
import com.xiaomi.channel.commonutils.logger.LoggerInterface
import com.xiaomi.mipush.sdk.Logger
import com.xiaomi.mipush.sdk.MiPushClient
import io.nebulas.wallet.android.app.WalletApplication
import io.nebulas.wallet.android.push.message.channel.ChannelEnum
import io.nebulas.wallet.android.push.message.channel.PushChannel

class PushChannelMi : PushChannel {

    private val tag = "PushChannelMi"
    private val appId = "2882303761517824450"
    private val appKey = "5631782425450"

    override fun getToken(): String {
        return MiPushClient.getRegId(WalletApplication.INSTANCE) ?: ""
    }

    override fun getChannelEnum(): ChannelEnum = ChannelEnum.MiPush

    override fun init(application: Application) {
        if (shouldInit(application)) {
            MiPushClient.registerPush(application, appId, appKey)
        }
        MiPushClient.setLocalNotificationType(application, -1)
        //打开Log
        val newLogger = object : LoggerInterface {
            override fun setTag(tag: String) {
                // ignore
            }

            override fun log(content: String, t: Throwable) {
                Log.d(tag, content, t)
            }

            override fun log(content: String) {
                Log.d(tag, content)
            }
        }
        Logger.setLogger(application, newLogger)
    }

    override fun subscribeTopic(application: Application, topic: String, what: String?) {
        MiPushClient.subscribe(application, topic, what)
    }

    override fun unsubscribeTopic(application: Application, topic: String, what: String?) {
        MiPushClient.unsubscribe(application, topic, what)
    }

    private fun shouldInit(application: Application): Boolean {
        val am = application.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val processInfo = am.runningAppProcesses
        val mainProcessName = application.packageName
        val myPid = Process.myPid()
        for (info in processInfo) {
            if (info.pid == myPid && mainProcessName == info.processName) {
                return true
            }
        }
        return false
    }
}