package io.nebulas.wallet.android.push.message.channel.firebase

import android.app.Application
import com.google.firebase.iid.FirebaseInstanceId
import com.google.firebase.messaging.FirebaseMessaging
import io.nebulas.wallet.android.extensions.logD
import io.nebulas.wallet.android.push.message.channel.ChannelEnum
import io.nebulas.wallet.android.push.message.channel.PushChannel

class PushChannelFirebase: PushChannel {

    override fun getToken(): String {
        return FirebaseInstanceId.getInstance().token?:""
    }

    override fun getChannelEnum(): ChannelEnum = ChannelEnum.Firebase

    override fun init(application: Application) {
        FirebaseMessaging.getInstance().isAutoInitEnabled = true
    }

    override fun subscribeTopic(application: Application, topic: String, what: String?) {
        FirebaseMessaging.getInstance().subscribeToTopic(topic)
                .addOnCompleteListener {
                    val msg = if(it.isSuccessful) "注册topic: $topic - 成功" else "注册topic: $topic - 失败"
                    logD(msg)
                }
    }

    override fun unsubscribeTopic(application: Application, topic: String, what: String?) {
        FirebaseMessaging.getInstance().unsubscribeFromTopic(topic)
                .addOnCompleteListener {
                    val msg = if(it.isSuccessful) "注销topic: $topic - 成功" else "注销topic: $topic - 失败"
                    logD(msg)
                }
    }
}