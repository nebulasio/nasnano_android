package io.nebulas.wallet.android.configuration

import android.content.Context

object Configuration {

    private const val FILE_CONFIGURATION = "file_configuration"
    private const val KEY_FINGERPRINT_ENABLED = "key_fingerprint_enabled"
    private const val KEY_TRANSACTION_NOTIFICATION_ENABLED = "key_transaction_notification_enabled"
    private const val KEY_NOTICE_NOTIFICATION_ENABLED = "key_notice_notification_enabled"

    fun enableFingerprint(context: Context) {
        setFingerprintEnabled(context, true)
    }
    fun disableFingerprint(context: Context) {
        setFingerprintEnabled(context, false)
    }

    fun isFingerprintEnabled(context: Context):Boolean {
        val preferences = context.getSharedPreferences(FILE_CONFIGURATION, Context.MODE_PRIVATE or Context.MODE_MULTI_PROCESS)
        return preferences.getBoolean(KEY_FINGERPRINT_ENABLED, false)
    }

    fun enableTransactionNotification(context: Context) {
        setTransactionNotificationEnabled(context, true)
    }

    fun disableTransactionNotification(context: Context) {
        setTransactionNotificationEnabled(context, false)
    }

    fun isTransactionNotificationEnabled(context: Context): Boolean {
        val preferences = context.getSharedPreferences(FILE_CONFIGURATION, Context.MODE_PRIVATE or Context.MODE_MULTI_PROCESS)
        return preferences.getBoolean(KEY_TRANSACTION_NOTIFICATION_ENABLED, true)
    }

    fun enableNoticeNotification(context: Context) {
        setNoticeNotificationEnabled(context, true)
    }

    fun disableNoticeNotification(context: Context) {
        setNoticeNotificationEnabled(context, false)
    }

    fun isNoticeNotificationEnabled(context: Context): Boolean {
        val preferences = context.getSharedPreferences(FILE_CONFIGURATION, Context.MODE_PRIVATE or Context.MODE_MULTI_PROCESS)
        return preferences.getBoolean(KEY_NOTICE_NOTIFICATION_ENABLED, true)
    }

    private fun setTransactionNotificationEnabled(context: Context, isEnabled: Boolean) {
        val preferences = context.getSharedPreferences(FILE_CONFIGURATION, Context.MODE_PRIVATE or Context.MODE_MULTI_PROCESS)
        preferences.edit()
                .putBoolean(KEY_TRANSACTION_NOTIFICATION_ENABLED, isEnabled)
                .apply()
    }

    private fun setNoticeNotificationEnabled(context: Context, isEnabled: Boolean) {
        val preferences = context.getSharedPreferences(FILE_CONFIGURATION, Context.MODE_PRIVATE or Context.MODE_MULTI_PROCESS)
        preferences.edit()
                .putBoolean(KEY_NOTICE_NOTIFICATION_ENABLED, isEnabled)
                .apply()
    }

    private fun setFingerprintEnabled(context:Context, isEnabled:Boolean){
        val preferences = context.getSharedPreferences(FILE_CONFIGURATION, Context.MODE_PRIVATE or Context.MODE_MULTI_PROCESS)
        preferences.edit()
                .putBoolean(KEY_FINGERPRINT_ENABLED, isEnabled)
                .apply()
    }

}