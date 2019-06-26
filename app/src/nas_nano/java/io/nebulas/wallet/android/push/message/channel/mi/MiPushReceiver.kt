package io.nebulas.wallet.android.push.message.channel.mi

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.text.TextUtils
import android.util.Log
import com.google.gson.Gson
import com.xiaomi.mipush.sdk.*
import io.nebulas.wallet.android.app.WalletApplication
import io.nebulas.wallet.android.extensions.logD
import io.nebulas.wallet.android.extensions.logE
import io.nebulas.wallet.android.module.launch.LaunchActivity
import io.nebulas.wallet.android.push.message.KEY_PUSH_MESSAGE
import io.nebulas.wallet.android.push.message.Message
import io.nebulas.wallet.android.push.message.PushManager
import io.nebulas.wallet.android.push.message.channel.ChannelEnum
import org.json.JSONObject
import java.lang.Exception


class MiPushReceiver : PushMessageReceiver() {

    private val tag = "MiPushReceiver"
    private var mRegId: String? = null
    private val mResultCode: Long = -1
    private val mReason: String? = null
    private val mCommand: String? = null
    private var mMessage: String? = null
    private var mTopic: String? = null
    private var mAlias: String? = null
    private var mUserAccount: String? = null
    private var mStartTime: String? = null
    private var mEndTime: String? = null

    private val gson = Gson()

    override fun onReceivePassThroughMessage(context: Context, message: MiPushMessage) {
        mMessage = message.content
        if (!TextUtils.isEmpty(message.topic)) {
            mTopic = message.topic
        } else if (!TextUtils.isEmpty(message.alias)) {
            mAlias = message.alias
        } else if (!TextUtils.isEmpty(message.userAccount)) {
            mUserAccount = message.userAccount
        }
        Log.d(tag, "onReceivePassThroughMessage : $message")
    }

    override fun onNotificationMessageClicked(context: Context, message: MiPushMessage) {
        mMessage = message.content
        if (!TextUtils.isEmpty(message.topic)) {
            mTopic = message.topic
        } else if (!TextUtils.isEmpty(message.alias)) {
            mAlias = message.alias
        } else if (!TextUtils.isEmpty(message.userAccount)) {
            mUserAccount = message.userAccount
        }

        try {
            val content = message.content
            val contentJson = JSONObject(content)
            val ctx:Context = if (context is WalletApplication) {
                context.activity?:context
            } else {
                context
            }
            ctx.startActivity(Intent(ctx, LaunchActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                putExtra(KEY_PUSH_MESSAGE, Message(
                        channel = ChannelEnum.MiPush,
                        type = contentJson.optString("type"),
                        data = contentJson.optString("data")
                ))
            })
            Log.d(tag, "onNotificationMessageClicked : $message")
        } catch (e: Exception) {
            Log.e(tag, "MiPusherReceiver - Process Message Error: ${e.message?:e.toString()}")
        }
    }

    override fun onNotificationMessageArrived(context: Context, message: MiPushMessage) {
        mMessage = message.content
        if (!TextUtils.isEmpty(message.topic)) {
            mTopic = message.topic
        } else if (!TextUtils.isEmpty(message.alias)) {
            mAlias = message.alias
        } else if (!TextUtils.isEmpty(message.userAccount)) {
            mUserAccount = message.userAccount
        }
        Log.d(tag, "onNotificationMessageArrived : $message")
    }

    override fun onCommandResult(context: Context, message: MiPushCommandMessage) {
        val command = message.command
        val arguments = message.commandArguments
        val cmdArg1 = if (arguments != null && arguments.size > 0) arguments[0] else null
        val cmdArg2 = if (arguments != null && arguments.size > 1) arguments[1] else null
        if (MiPushClient.COMMAND_REGISTER == command) {
            if (message.resultCode == ErrorCode.SUCCESS.toLong()) {
                mRegId = cmdArg1
                logD("Mi Push Token: $cmdArg1")
                PushManager.updateToken(cmdArg1)
//                MiPushClient.subscribe(context, "Pixel", "")
            } else {
                logD("Mi Push Get Token Error: ${message.reason}")
            }
        } else if (MiPushClient.COMMAND_SET_ALIAS == command) {
            if (message.resultCode == ErrorCode.SUCCESS.toLong()) {
                mAlias = cmdArg1
            }
        } else if (MiPushClient.COMMAND_UNSET_ALIAS == command) {
            if (message.resultCode == ErrorCode.SUCCESS.toLong()) {
                mAlias = cmdArg1
            }
        } else if (MiPushClient.COMMAND_SUBSCRIBE_TOPIC == command) {
            if (message.resultCode == ErrorCode.SUCCESS.toLong()) {
                mTopic = cmdArg1
                logD("Topic subscribe onCommandResult: ${gson.toJson(message)}")
            } else {
                logD("Topic subscribe onCommandResult: ${gson.toJson(message)}")
            }
        } else if (MiPushClient.COMMAND_UNSUBSCRIBE_TOPIC == command) {
            if (message.resultCode == ErrorCode.SUCCESS.toLong()) {
                logD("Topic unsubscribe onCommandResult: ${gson.toJson(message)}")
            } else {
                logD("Topic unsubscribe onCommandResult: ${gson.toJson(message)}")
            }
        } else if (MiPushClient.COMMAND_SET_ACCEPT_TIME == command) {
            if (message.resultCode == ErrorCode.SUCCESS.toLong()) {
                mStartTime = cmdArg1
                mEndTime = cmdArg2
            }
        }
    }

    override fun onReceiveRegisterResult(context: Context, message: MiPushCommandMessage) {
        val command = message.command
        val arguments = message.commandArguments
        val cmdArg1 = if (arguments != null && arguments.size > 0) arguments[0] else null
        val cmdArg2 = if (arguments != null && arguments.size > 1) arguments[1] else null
        if (MiPushClient.COMMAND_REGISTER == command) {
            if (message.resultCode == ErrorCode.SUCCESS.toLong()) {
                mRegId = cmdArg1
                logD("regId : $mRegId")
            }
        }
    }

    override fun onRequirePermissions(context: Context?, permissions: Array<String>?) {
        super.onRequirePermissions(context, permissions)
        permissions ?: return
        Log.e(tag,
                "onRequirePermissions is called. need permission" + arrayToString(permissions))

        if (Build.VERSION.SDK_INT >= 23 && context!!.applicationInfo.targetSdkVersion >= 23) {
            val intent = Intent()
            intent.putExtra("permissions", permissions)
            intent.component = ComponentName(context.packageName, PermissionActivity::class.java.canonicalName)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
            context.startActivity(intent)
        }
    }

    fun arrayToString(strings: Array<String>): String {
        var result = " "
        for (str in strings) {
            result = "$result$str "
        }
        return result
    }
}