package io.nebulas.wallet.android.push.message.channel.firebase

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.support.v4.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.google.gson.Gson
import io.nebulas.wallet.android.R
import io.nebulas.wallet.android.extensions.logD
import io.nebulas.wallet.android.extensions.logE
import io.nebulas.wallet.android.module.launch.LaunchActivity
import io.nebulas.wallet.android.push.message.KEY_PUSH_MESSAGE
import io.nebulas.wallet.android.push.message.Message
import io.nebulas.wallet.android.push.message.channel.ChannelEnum
import org.json.JSONObject

class NasFirebaseMessagingService : FirebaseMessagingService() {
    override fun onMessageReceived(message: RemoteMessage?) {
        message?.apply {
//            val gSon = Gson()
//            logD("PUSH : FCM : RemoteMessage from : ${message.from}")
//            logD("PUSH : FCM : RemoteMessage to : ${message.to}")
//            logD("PUSH : FCM : RemoteMessage data : ${gSon.toJson(message.data)}")
//            logD("PUSH : FCM : RemoteMessage messageType : ${message.messageType}")
//            logD("PUSH : FCM : RemoteMessage messageId : ${message.messageId}")
//            logD("PUSH : FCM : RemoteMessage collapseKey : ${message.collapseKey}")
//            logD("PUSH : FCM : RemoteMessage sentTime : ${message.sentTime}")
//            logD("PUSH : FCM : RemoteMessage ttl : ${message.ttl}")
//            logD("PUSH : FCM : RemoteMessage notification : ${gSon.toJson(message.notification)}")

            sendNotification(transferNotification(message))
        }
    }

    private fun sendNotification(notification: Notification?) {
        notification?.apply {
            flags = Notification.FLAG_AUTO_CANCEL
            val notificationManager: NotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
//            logD("PUSH : FCM : 拿到了Notification : ${notification.extras}")
            notificationManager.notify(1, this)
        }
    }

    private fun transferNotification(msg: RemoteMessage): Notification? {
        val notificationOrigin = msg.notification ?: return null
        val notificationManager: NotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val id = packageName
            val desc = "转账"
            val nChannel: NotificationChannel? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel(id, desc, NotificationManager.IMPORTANCE_DEFAULT)
            } else {
                null
            }
            nChannel?.apply {
                notificationManager.createNotificationChannel(this)
            }
        }
        val builder = NotificationCompat.Builder(this, packageName)
                .setSmallIcon(R.mipmap.ic_nebulas_start)
                .setContentTitle(notificationOrigin.title)
                .setContentText(notificationOrigin.body)
        val intent = Intent(this, LaunchActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        if (msg.data != null && msg.data.isNotEmpty()) {
            val content = msg.data["push_content"]
            try {
                val contentJson = JSONObject(content)
                intent.putExtra(KEY_PUSH_MESSAGE, Message(
                        channel = ChannelEnum.Firebase,
                        type = contentJson.optString("type"),
                        data = contentJson.optString("data")
                ))
            } catch (e: Exception) {
                logE("NasFirebaseMessagingService - Process Message Error: ${e.message?:e.toString()}")
            }
        }
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_ONE_SHOT)
        builder.setContentIntent(pendingIntent)
        return builder.build()
    }
}
