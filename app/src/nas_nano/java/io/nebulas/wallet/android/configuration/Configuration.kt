package io.nebulas.wallet.android.configuration

import android.content.Context
import io.nebulas.wallet.android.common.Constants

object Configuration {

    private const val FILE_CONFIGURATION = "file_configuration"
    private const val KEY_FINGERPRINT_ENABLED = "key_fingerprint_enabled"
    private const val KEY_TRANSACTION_NOTIFICATION_ENABLED = "key_transaction_notification_enabled"
    private const val KEY_NOTICE_NOTIFICATION_ENABLED = "key_notice_notification_enabled"

    private val configurationsFromServer = mutableMapOf<String, String>()

    fun enableFingerprint(context: Context) {
        setFingerprintEnabled(context, true)
    }

    fun disableFingerprint(context: Context) {
        setFingerprintEnabled(context, false)
    }

    fun isFingerprintEnabled(context: Context): Boolean {
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

    private fun setFingerprintEnabled(context: Context, isEnabled: Boolean) {
        val preferences = context.getSharedPreferences(FILE_CONFIGURATION, Context.MODE_PRIVATE or Context.MODE_MULTI_PROCESS)
        preferences.edit()
                .putBoolean(KEY_FINGERPRINT_ENABLED, isEnabled)
                .apply()
    }

    private const val KEY_STAKING_STATUS = "staking_status"
    private const val KEY_STAKING_BANNER_URL = "staking_banner_url"

    fun resetConfigurationsFromServer(context: Context?, newConfigurations: Map<String, String>?) {
        newConfigurations ?: return
        for ((k, v) in newConfigurations) {
            configurationsFromServer[k] = v
        }
        if (newConfigurations.containsKey(KEY_NAX_VOTE_CONTRACTS)) {
            updateVoteContracts(context, newConfigurations[KEY_NAX_VOTE_CONTRACTS])
        }
    }

    fun isStakingOpened(): Boolean {
        val status = configurationsFromServer[KEY_STAKING_STATUS] ?: return true
        return status == "1"
    }

    /**
     * @param lang 当前App语言(en/cn/kr)
     */
    fun getStakingBannerUrl(lang:String): String {
        return configurationsFromServer["${KEY_STAKING_BANNER_URL}_$lang"] ?: return ""
    }


    private const val KEY_NAX_VOTE_CONTRACTS = "nax_vote_contracts"

    /**
     * 更新投票合约信息
     * @param contracts 格式-   Contract,Contract
     */
    fun updateVoteContracts(context: Context?, contracts:String?){
        context?:return
        contracts?:return
        val preferences = context.getSharedPreferences(FILE_CONFIGURATION, Context.MODE_PRIVATE or Context.MODE_MULTI_PROCESS)
        preferences.edit()
                .putString(KEY_NAX_VOTE_CONTRACTS, contracts)
                .apply()
        Constants.voteContracts = getVoteContracts(context)
        Constants.voteContractsMap = getVoteContractsMap(context)
    }

    fun getVoteContracts(context: Context): List<String>{
        val map = getVoteContractsMap(context)
        if (map.isEmpty()){
            return Constants.defaultVoteContracts
        }
        return map.keys.toList()
    }

    fun getVoteContractsMap(context: Context): Map<String, String> {
        val preferences = context.getSharedPreferences(FILE_CONFIGURATION, Context.MODE_PRIVATE or Context.MODE_MULTI_PROCESS)
        val contracts = preferences.getString(KEY_NAX_VOTE_CONTRACTS, "")
        val keyValue = contracts.split(",").toList()
        val map = mutableMapOf<String, String>()
        keyValue.forEach {
            map[it] = "NAX"
        }
        if (map.isEmpty()){
            return Constants.defaultVoteContractsMap
        }
        map[Constants.VOTE_CONTRACT_NAT] = "NAT"
        return map
    }

}