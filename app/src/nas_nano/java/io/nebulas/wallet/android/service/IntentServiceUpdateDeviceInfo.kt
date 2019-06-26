package io.nebulas.wallet.android.service

import android.app.IntentService
import android.content.Intent
import android.content.Context
import io.nebulas.wallet.android.app.WalletApplication
import io.nebulas.wallet.android.common.DataCenter
import io.nebulas.wallet.android.configuration.Configuration
import io.nebulas.wallet.android.extensions.logD
import io.nebulas.wallet.android.extensions.logE
import io.nebulas.wallet.android.network.server.HttpManager
import io.nebulas.wallet.android.network.server.model.DeviceInfoRequest
import io.nebulas.wallet.android.push.message.PushManager
import io.nebulas.wallet.android.util.Util
import java.lang.Exception
import java.util.*

private const val ACTION_UPDATE_TOKEN = "io.nebulas.wallet.android.service.action.DEVICE_UPDATE_TOKEN"
private const val ACTION_UPDATE_ADDRESS = "io.nebulas.wallet.android.service.action.DEVICE_UPDATE_ADDRESS"

private const val EXTRA_PARAM_TOKEN = "io.nebulas.wallet.android.service.extra.PARAM_TOKEN"

private const val FILE_WALLET_PREFERENCE = "file_wallet_preference_for_device_info"
private const val KEY_ADDRESSES = "key_addresses"

class IntentServiceUpdateDeviceInfo : IntentService("IntentServiceUpdateDeviceInfo") {

    override fun onHandleIntent(intent: Intent?) {
        when (intent?.action) {
            ACTION_UPDATE_TOKEN -> {
                val token = intent.getStringExtra(EXTRA_PARAM_TOKEN)
                handleActionUpdateToken(token)
            }
            ACTION_UPDATE_ADDRESS -> {
                handleActionUpdateAddresses()
            }
        }
    }

    private fun handleActionUpdateToken(token: String) {
        val deviceInfo = DeviceInfoRequest()
        deviceInfo.deviceId = WalletApplication.INSTANCE.uuid
        deviceInfo.language = Util.getCurLanguage()
        deviceInfo.pushToken = token
        deviceInfo.pushChannel = PushManager.getCurrentChannelName()
        if (token.isEmpty() || deviceInfo.pushChannel.isEmpty()) {
            logD("token or channel is empty: token - $token **** channel - ${deviceInfo.pushChannel}")
            return
        }
        if (DataCenter.addresses.isNotEmpty()) {
            val addresses = mutableListOf<String>()
            DataCenter.addresses.forEach {
                addresses.add(it.address)
            }
            deviceInfo.addresses = addresses
        }
        updateDeviceInfo(deviceInfo)
    }

    private fun handleActionUpdateAddresses() {
        val preference = getSharedPreferences(FILE_WALLET_PREFERENCE, Context.MODE_PRIVATE)
        val cachedAddresses = preference.getString(KEY_ADDRESSES, "")
        val addresses = mutableListOf<String>()
        if (DataCenter.addresses.isNotEmpty()) {
            DataCenter.addresses.forEach {
                addresses.add(it.address)
            }
        }
        addresses.sort()
        val addressesString = Arrays.toString(addresses.toTypedArray())
        if (cachedAddresses == addressesString) {
            logD("钱包地址没有变化，无需更新")
            return
        }
        val deviceInfo = DeviceInfoRequest()
        deviceInfo.deviceId = WalletApplication.INSTANCE.uuid
        deviceInfo.language = Util.getCurLanguage()
        deviceInfo.pushToken = PushManager.getCurrentToken()
        deviceInfo.pushChannel = PushManager.getCurrentChannelName()
        deviceInfo.addresses = addresses
        deviceInfo.transactionPushOn = if (Configuration.isTransactionNotificationEnabled(this)) 1 else 0
        updateDeviceInfo(deviceInfo)
    }

    private fun updateDeviceInfo(deviceInfoRequest: DeviceInfoRequest) {
        val api = HttpManager.getServerApi()
        val call = api.updateDeviceInfo(HttpManager.getHeaderMap(), deviceInfoRequest)
        logD("开始更新设备信息: $deviceInfoRequest")
        try {
            val response = call.execute().body()
            val code = response?.code ?: -1
            if (code == 0) {
                val preference = getSharedPreferences(FILE_WALLET_PREFERENCE, Context.MODE_PRIVATE)
                val addressesString = Arrays.toString(deviceInfoRequest.addresses.toTypedArray())
                preference.edit().putString(KEY_ADDRESSES, addressesString).apply()
            }
        } catch (e: Exception) {
            logE(e.message ?: e.toString())
        }
    }

    companion object {
        /**
         * Starts this service to perform action Foo with the given parameters. If
         * the service is already performing a task this action will be queued.
         *
         * @see IntentService
         */
        @JvmStatic
        fun startActionUpdateToken(context: Context, token: String) {
            logD("准备更新设备Push token: $token")
            val intent = Intent(context, IntentServiceUpdateDeviceInfo::class.java).apply {
                action = ACTION_UPDATE_TOKEN
                putExtra(EXTRA_PARAM_TOKEN, token)
            }
            context.startService(intent)
        }

        @JvmStatic
        fun startActionUpdateAddresses(context: Context) {
            logD("准备更新设备Address")
            val intent = Intent(context, IntentServiceUpdateDeviceInfo::class.java).apply {
                action = ACTION_UPDATE_ADDRESS
            }
            context.startService(intent)
        }
    }
}
