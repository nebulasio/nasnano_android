package io.nebulas.wallet.android.push.message

import android.annotation.SuppressLint
import android.app.Application
import android.os.Build
import android.os.Environment
import android.text.TextUtils
import android.util.Log
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import io.nebulas.wallet.android.push.message.channel.ChannelEnum
import io.nebulas.wallet.android.push.message.channel.PushChannel
import io.nebulas.wallet.android.push.message.channel.firebase.PushChannelFirebase
import io.nebulas.wallet.android.push.message.channel.mi.PushChannelMi
import java.io.File
import java.io.FileInputStream
import java.util.*

object PushChannelFactory {

    private const val tag = "PushChannelFactory"

    enum class SystemOS(desc: String) {
        GOOGLE("装有Google服务，可以走FireBase"),
        EMUI("华为EMUI"),
        MIUI("小米MIUI"),
        OTHER("其他")
    }

    private const val KEY_MIUI_VERSION_CODE = "ro.miui.ui.version.code"
    private const val KEY_MIUI_VERSION_NAME = "ro.miui.ui.version.name"
    private const val KEY_MIUI_INTERNAL_STORAGE = "ro.miui.internal.storage"
    private const val KEY_EMUI_API_LEVEL = "ro.build.hw_emui_api_level"
    private const val KEY_EMUI_VERSION = "ro.build.version.emui"
    private const val KEY_EMUI_CONFIG_HW_SYS_VERSION = "ro.confg.hw_systemversion"

    fun getPushChannel(application: Application): PushChannel {
        val os = getSystemOS(application)
        return when (os) {
            SystemOS.GOOGLE -> PushChannelFirebase()
            SystemOS.EMUI -> PushChannelMi()
            SystemOS.MIUI -> PushChannelMi()
            SystemOS.OTHER -> PushChannelMi()
        }
    }

    fun getPushChannel(channel: ChannelEnum):PushChannel {
        return when(channel) {
            ChannelEnum.Firebase -> PushChannelFirebase()
            ChannelEnum.MiPush -> PushChannelMi()
        }
    }

    private fun getSystemOS(application: Application): SystemOS {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (isEMUI26()) {
                    Log.d(tag, "设备有EMUI相关信息配置，类型为${SystemOS.EMUI}")
                    return SystemOS.EMUI
                }
                if (isMIUI26()) {
                    Log.d(tag, "设备有MIUI相关信息配置，类型为${SystemOS.MIUI}")
                    return SystemOS.MIUI
                }
            } else {
                val properties = Properties()
                properties.load(FileInputStream(File(Environment.getRootDirectory(), "build.prop")))
                if (isEMUI(properties)) {
                    Log.d(tag, "设备有EMUI相关信息配置，类型为${SystemOS.EMUI}")
                    return SystemOS.EMUI
                }
                if (isMIUI(properties)) {
                    Log.d(tag, "设备有MIUI相关信息配置，类型为${SystemOS.MIUI}")
                    return SystemOS.MIUI
                }
            }
            if (isGooglePlayServicesAvailable(application)) {
                Log.d(tag, "设备装有Google套件，类型为${SystemOS.GOOGLE}")
                return SystemOS.GOOGLE
            }
            Log.d(tag, "设备未找到相关信息的有效配置，类型为${SystemOS.OTHER}")
        } catch (e: Exception) {
            Log.d(tag, "设备信息获取时发生异常: ${e}，类型为${SystemOS.OTHER}")
        }
        return SystemOS.OTHER
    }

    private fun isGooglePlayServicesAvailable(application: Application): Boolean {
        val googleApiAvailability = GoogleApiAvailability.getInstance()
        val code = googleApiAvailability.isGooglePlayServicesAvailable(application)
        return code == ConnectionResult.SUCCESS
    }

    private fun isEMUI(properties: Properties): Boolean {
        if (properties.getProperty(KEY_EMUI_API_LEVEL, null) != null
                || properties.getProperty(KEY_EMUI_VERSION, null) != null
                || properties.getProperty(KEY_EMUI_CONFIG_HW_SYS_VERSION, null) != null) {
            return true
        }
        return false
    }

    private fun isEMUI26(): Boolean {
        if (!TextUtils.isEmpty(getSystemProperty(KEY_EMUI_API_LEVEL))
                || !TextUtils.isEmpty(getSystemProperty(KEY_EMUI_VERSION))
                || !TextUtils.isEmpty(getSystemProperty(KEY_EMUI_CONFIG_HW_SYS_VERSION))) {
            return true
        }
        return false
    }

    private fun isMIUI(properties: Properties): Boolean {
        if (properties.getProperty(KEY_MIUI_VERSION_CODE, null) != null
                || properties.getProperty(KEY_MIUI_VERSION_NAME, null) != null
                || properties.getProperty(KEY_MIUI_INTERNAL_STORAGE, null) != null) {
            return true
        }
        return false
    }

    private fun isMIUI26(): Boolean {
        if (!TextUtils.isEmpty(getSystemProperty(KEY_MIUI_VERSION_CODE))
                || !TextUtils.isEmpty(getSystemProperty(KEY_MIUI_VERSION_NAME))
                || !TextUtils.isEmpty(getSystemProperty(KEY_MIUI_INTERNAL_STORAGE))) {
            return true
        }
        return false
    }

    @SuppressLint("PrivateApi")
    private fun getSystemProperty(key: String, defaultValue: String? = null): String? {
        return try {
            val clazz = Class.forName("android.os.SystemProperties")
            val method = clazz.getMethod("get", String::class.java, String::class.java)
            method.invoke(clazz, key, defaultValue) as String
        } catch (e: Exception) {
            defaultValue
        }
    }

}