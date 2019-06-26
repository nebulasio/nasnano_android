package io.nebulas.wallet.android.push.message

import android.app.Application
import android.content.Context
import android.content.Intent
import android.util.Log
import io.nebulas.wallet.android.configuration.Configuration
import io.nebulas.wallet.android.extensions.logD
import io.nebulas.wallet.android.extensions.logE
import io.nebulas.wallet.android.push.message.channel.ChannelEnum
import io.nebulas.wallet.android.service.IntentServiceUpdateDeviceInfo
import io.nebulas.wallet.android.util.Util
import org.json.JSONObject

object PushManager {

    private const val tag = "PushManager"

    const val TOPIC_NOTICE = "notice"

    private const val filePushPreference = "file_push_preference"

    private const val keyPushChannelEnum = "key_push_channel"
    private const val keyPushChannelToken = "key_push_channel_token"

    private var application: Application? = null

    fun init(application: Application) {
        if (PushManager.application == null) {
            PushManager.application = application
        }
        val pushChannel = PushChannelFactory.getPushChannel(application)
        saveChannel(pushChannel.getChannelEnum())
        pushChannel.init(application)
        val token = getCurrentToken()
        if (token.isNotEmpty()) {
            updateTopics(application)
        }
    }

    fun reInit() {
        val app = application ?: return
        val pushChannel = PushChannelFactory.getPushChannel(app)
        pushChannel.init(app)
    }

    fun updateToken(token: String?) {
        token ?: return
        if (application == null) {
            Log.w(tag, "Please call #PushManager.init first")
            return
        }
        val app = application ?: return
        val pref = app.getSharedPreferences(filePushPreference, Context.MODE_PRIVATE)
        pref.edit().putString(keyPushChannelToken, token).apply()
        IntentServiceUpdateDeviceInfo.startActionUpdateToken(app, token)
        updateTopics(app)
    }

    fun subscribeTopic(topic: String) {
        logD("开始注册topic: $topic")
        if (application == null) {
            Log.w(tag, "Please call #PushManager.init first")
            return
        }
        val app = application ?: return
        val pushChannel = PushChannelFactory.getPushChannel(app)
        pushChannel.subscribeTopic(app, topic)
    }

    fun unsubscribeTopic(topic: String) {
        logD("开始注销topic: $topic")
        if (application == null) {
            Log.w(tag, "Please call #PushManager.init first")
            return
        }
        val app = application ?: return
        val pushChannel = PushChannelFactory.getPushChannel(app)
        pushChannel.unsubscribeTopic(app, topic)
    }

    fun interceptPushMessage(intent: Intent) {
        try {
            if (intent.hasExtra("push_content")) {
                val content = intent.getStringExtra("push_content")
                val contentJson = JSONObject(content)
                val message = Message(
                        channel = ChannelEnum.MiPush,
                        type = contentJson.optString("type"),
                        data = contentJson.optString("data")
                )
                intent.putExtra(KEY_PUSH_MESSAGE, message)
            }
        } catch (e: Exception) {
            logE("PushManager interceptPushMessage error: ${e.message ?: e.toString()}")
        }
    }

    fun getCurrentToken(): String {
        if (application == null) {
            Log.w(tag, "Please call #PushManager.init first")
            return ""
        }
        val app = application ?: return ""
        val pref = app.getSharedPreferences(filePushPreference, Context.MODE_PRIVATE)
        var cached = pref.getString(keyPushChannelToken, "")
        if (cached.isEmpty()) {
            val channel = PushChannelFactory.getPushChannel(ChannelEnum.valueOf(getCurrentChannelName()))
            cached = channel.getToken()
        }
        return cached
    }

    fun getCurrentChannelName(): String {
        if (application == null) {
            Log.w(tag, "Please call #PushManager.init first")
            return ""
        }
        val app = application ?: return ""
        val pref = app.getSharedPreferences(filePushPreference, Context.MODE_PRIVATE)
        return pref.getString(keyPushChannelEnum, "")
    }

    private fun saveChannel(channelEnum: ChannelEnum) {
        if (application == null) {
            Log.w(tag, "Please call #PushManager.init first")
            return
        }
        val app = application ?: return
        val pref = app.getSharedPreferences(filePushPreference, Context.MODE_PRIVATE)
        pref.edit().putString(keyPushChannelEnum, channelEnum.name).apply()
    }

    private fun updateTopics(application: Application) {
        updateLanguageTopic()
        updateNoticeTopic(application)
    }

    private fun updateLanguageTopic() {
        val lang = Util.getCurLanguage()
        subscribeTopic(lang)
    }

    private fun updateNoticeTopic(application: Application) {
        val isOpen = Configuration.isNoticeNotificationEnabled(application)
        if (isOpen) {
            subscribeTopic(TOPIC_NOTICE)
        }
    }

}