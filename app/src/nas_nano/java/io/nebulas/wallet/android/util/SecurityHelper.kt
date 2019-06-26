package io.nebulas.wallet.android.util

import io.nebulas.wallet.android.db.DBUtil
import io.nebulas.wallet.android.extensions.logD
import io.nebulas.wallet.android.module.wallet.create.model.Wallet
import org.jetbrains.anko.doAsync
import java.lang.Exception
import java.lang.StringBuilder

object SecurityHelper {

    private const val MAX_WRONG_PASSWORD_COUNT = 5
    private const val TEN_MINUTE = 60 * 1000 * 10
    private const val ONE_HOUR = 60 * 1000 * 60L

    /**
     * 钱包密码输入错误
     * 于数据库记录错误次数和错误发生的时间，并更新锁定状态
     *
     * @return 钱包是否锁定(true:锁定 false:未锁定)
     */
    fun walletWrongPassword(wallet: Wallet): Boolean {
        if (wallet.id<0){
            return false
        }
        val now = System.currentTimeMillis()
        val timeArray = wallet.wrongPasswordTimesString.split(",")
        if (timeArray.isEmpty()) {
            wallet.wrongPasswordTimes = 1
            wallet.wrongPasswordTimesString = now.toString()
        } else {
            val timeList = mutableListOf<String>().apply {
                addAll(timeArray)
            }
            val iterator = timeList.iterator()
            while (iterator.hasNext()) {
                val t = iterator.next()
                val tLong = try {
                    t.toLong()
                } catch (e: Exception) {
                    0L
                }
                if (now - tLong > ONE_HOUR) {
                    iterator.remove()
                }
            }
            timeList.add(now.toString())
            wallet.wrongPasswordTimes = timeList.size
            wallet.wrongPasswordTimesString = listToString(timeList)
        }

        if (wallet.wrongPasswordTimes >= MAX_WRONG_PASSWORD_COUNT) {
            wallet.isLock = true
            wallet.lockedTime = System.currentTimeMillis()
        }
        doAsync {
            DBUtil.appDB.walletDao().insertWallet(wallet)
        }
        return wallet.isLock
    }

    /**
     * 钱包密码输入错误
     * 于数据库记录错误次数，并更新锁定状态
     *
     * @return 钱包是否锁定(true:锁定 false:未锁定)
     */
    fun walletCorrectPassword(wallet: Wallet) {
        if (wallet.id<0){
            return
        }
        wallet.wrongPasswordTimesString = ""
        wallet.wrongPasswordTimes = 0
        wallet.lockedTime = 0
        doAsync {
            DBUtil.appDB.walletDao().insertWallet(wallet)
        }
    }

    /**
     * 钱包是否被锁定
     */
    fun isWalletLocked(wallet: Wallet): Boolean {
        if (wallet.id < 0) {
            return false
        }
        if (!wallet.isLock) {
            return false
        }
        val lockedTime = wallet.lockedTime
        val now = System.currentTimeMillis()
        if (now - lockedTime > TEN_MINUTE) {
            wallet.resetLockStatus()
            doAsync {
                DBUtil.appDB.walletDao().insertWallet(wallet)
            }
            return false
        }
        return true
    }

    private fun listToString(list: List<String>): String {
        val builder = StringBuilder()
        for (str in list) {
            builder.append(",").append(str)
        }
        if (builder.isNotEmpty()) {
            builder.deleteCharAt(0)
        }
        return builder.toString()
    }

}