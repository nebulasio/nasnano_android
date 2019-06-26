package io.nebulas.wallet.android.module.wallet.create.model

import android.arch.persistence.room.Entity
import android.arch.persistence.room.Ignore
import android.arch.persistence.room.PrimaryKey
import io.nebulas.wallet.android.app.WalletApplication
import io.nebulas.wallet.android.base.BaseEntity
import io.nebulas.wallet.android.db.AppDBHelper
import io.nebulas.wallet.android.util.KeyStoreUtil
import java.io.Serializable

/**
 * Created by Heinoc on 2018/2/10.
 */
@Entity(tableName = "wallet")
data class Wallet constructor(

        @PrimaryKey(autoGenerate = true)
        var id: Long = 0,
        /**
         * 钱包名称
         */
        var walletName: String = "",
        /**
         * 助记词
         */
        var mnemonic: ByteArray? = byteArrayOf(),
        /**
         * 钱包密码是否为复杂密码
         */
        var isComplexPwd: Boolean = false,
        /**
         * 钱包是否由助记词创建
         */
        var createdByMnemonic: Boolean = false,
        @Ignore
        var canPayTx: Boolean = false,
        @Ignore
        var selected: Boolean = false,

        /**
         * 钱包是否被锁定
         */
        var isLock: Boolean = false,

        /**
         * 钱包锁定时间
         */
        var lockedTime: Long = 0,

        /**
         * 钱包密码错误次数
         */
        var wrongPasswordTimes: Int = 0,

        /**
         * 输错密码的时间记录 : "Long,Long,Long"
         */
        var wrongPasswordTimesString: String = ""



) : BaseEntity(), Serializable {
    constructor(walletName: String) : this(0) {
        this.walletName = walletName
    }

    fun setMnemonic(mnemonic: String) {
        if (mnemonic.isNullOrEmpty()) {
            this.mnemonic = byteArrayOf()
        }
        this.mnemonic = KeyStoreUtil.encryptString(mnemonic)
    }

    fun isNeedBackup(): Boolean {
        return mnemonic != null && mnemonic!!.isNotEmpty()
    }

    fun getPlainMnemonic(): String {
        return if (null == this.mnemonic) "" else KeyStoreUtil.decryptString(this.mnemonic!!)
    }

    fun resetLockStatus() {
        isLock = false
        wrongPasswordTimes = 0
        wrongPasswordTimesString = ""
        lockedTime = 0
    }

    companion object {
        /**
         * 获取数据库表中存储的全部钱包
         */
        fun getWallets(): List<Wallet> {
            return AppDBHelper.getInstance(WalletApplication.INSTANCE).appDB.walletDao().loadAllWallet()
        }

    }


}