package io.nebulas.wallet.android.module.staking

import android.content.Context
import android.content.SharedPreferences
import android.support.annotation.WorkerThread
import io.nebulas.wallet.android.extensions.logD

private const val CONFIG_FILE = "config_file_stacking"
private const val KEY_HAS_READ_RULE = "key_has_read_rule"
private const val KEY_PLEDGING_WALLETS = "key_pledging_wallets"

object StakingConfiguration {

    enum class OperationType {
        Pledge, CancelPledge
    }

    data class PledgingWalletWrapper(val walletId: Long,
                                     val txHash: String,
                                     val type: OperationType)

    fun hasReadStackingRule(context: Context): Boolean {
        val pref = getConfigPreference(context)
        return pref.getBoolean(KEY_HAS_READ_RULE, false)
    }

    fun markRuleHasBeenRead(context: Context) {
        val pref = getConfigPreference(context)
        pref.edit().putBoolean(KEY_HAS_READ_RULE, true).apply()
    }

    fun savePledgingWallet(context: Context, walletId: String, txHash: String, type: OperationType) {
        val pref = getConfigPreference(context)
        val value = "$walletId:$txHash:$type"
        val pledgingWallets = pref.getStringSet(KEY_PLEDGING_WALLETS, mutableSetOf())
        if (!pledgingWallets.contains(value)) {
            pledgingWallets.add(value)
            pref.edit().putStringSet(KEY_PLEDGING_WALLETS, pledgingWallets).apply()
        }
    }

    fun getPledgingWallets(context: Context): List<PledgingWalletWrapper> {
        val set = getConfigPreference(context).getStringSet(KEY_PLEDGING_WALLETS, mutableSetOf())
        val result = mutableListOf<PledgingWalletWrapper>()
        val list = set.map {
            val arr = it.split(":")
            if (arr.size != 3) {
                return@map null
            }
            PledgingWalletWrapper(arr[0].toLong(), arr[1], OperationType.valueOf(arr[2]))
        }
        list.forEach {
            it ?: return@forEach
            result.add(it)
        }
        return result
    }

    fun pledgeComplete(context: Context, txHash: String) {
        val pref = getConfigPreference(context)
        val pledgingWallets = pref.getStringSet(KEY_PLEDGING_WALLETS, mutableSetOf())
        val iterator = pledgingWallets.iterator()
        while (iterator.hasNext()) {
            val item = iterator.next()
            val arr = item.split(":")
            if (arr.size != 3) {
                iterator.remove()
            } else {
                if (arr[1] == txHash) {
                    iterator.remove()
                }
            }
        }
        pref.edit().remove(KEY_PLEDGING_WALLETS).apply()
        pref.edit().putStringSet(KEY_PLEDGING_WALLETS, pledgingWallets).apply()
    }

    private fun getConfigPreference(context: Context): SharedPreferences {
        return context.getSharedPreferences(CONFIG_FILE, Context.MODE_PRIVATE)
    }

}