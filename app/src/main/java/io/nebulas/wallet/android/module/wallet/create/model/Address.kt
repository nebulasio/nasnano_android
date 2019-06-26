package io.nebulas.wallet.android.module.wallet.create.model

import android.arch.persistence.room.ColumnInfo
import android.arch.persistence.room.Entity
import android.arch.persistence.room.ForeignKey
import android.arch.persistence.room.PrimaryKey
import io.nebulas.wallet.android.base.BaseEntity
import io.nebulas.wallet.android.util.KeyStoreUtil

/**
 * Created by Heinoc on 2018/2/23.
 */

@Entity(tableName = "address", foreignKeys = arrayOf(
        ForeignKey(entity = Wallet::class, parentColumns = arrayOf("id"), childColumns = arrayOf("wallet_id"))
))
class Address() : BaseEntity() {

    @PrimaryKey(autoGenerate = true)
    var id: Long = 0
    /**
     * wallet id
     */
    @ColumnInfo(name = "wallet_id")
    var walletId: Long = 0

    /**
     * 钱包地址
     */
    var address: String = ""

    /**
     * keystore
     */
    var keyJson: ByteArray = byteArrayOf()

    /**
     * platform
     */
    var platform: String = ""


    constructor(address: String, keyJson: String, platform: String) : this() {
        this.address = address
        setKeyStore(keyJson)
        this.platform = platform
    }


    companion object {
//        /**
//         * 获取数据库表中存储的全部钱包
//         */
//        fun getWallets(): Flowable<List<Wallet>> {
//            return AppDBHelper.getInstance(WalletApplication.INSTANCE).appDB.walletDao().loadAllWallet()
//        }

    }

    /**
     * 对给定keyJson文本内容进行Android keystore进行加密
     */
    fun setKeyStore(keyjson: String) {
        this.keyJson = KeyStoreUtil.encryptString(keyjson)
//        var sb = StringBuffer()
//
//        /**
//         * 因keyJson内容长度太长，通过keystore加密会报“Method threw 'javax.crypto.IllegalBlockSizeException' exception.”的异常，
//         * 故在此对keyJson内容进行切分，分批进行加密处理
//         */
//        //初始位置
//        var position = 0
//        //步长
//        var step = 128
//        var preEncryptStr: String
//        while (position < keyjson.length) {
//            preEncryptStr = if ((position + step) < keyjson.length) {
//                keyjson.substring(position, position + step)
//            } else {
//                keyjson.substring(position)
//            }
//
//            var encryptStr = KeyStoreUtil.encryptString(preEncryptStr)
//            if (encryptStr.isNotEmpty()) {
//                sb.append(";")
//                sb.append(encryptStr)
//            }
//
//            position += step
//        }
//        if (sb.isNotEmpty()){
//            sb.deleteCharAt(0)
//        }
//
//        return sb.toString()

    }

    /**
     * 获取解密后的keyJson文本
     */
    fun getKeyStore(): String {
        return KeyStoreUtil.decryptString(this.keyJson)
//        var sb = StringBuffer()
//
//        var encryptedStrings = this.keyJson.split(";")
//        encryptedStrings.forEach {
//            var result = KeyStoreUtil.decryptString(it)
//            if (result.isNotEmpty()){
//                sb.append(result)
//            }
//        }
//
//        return sb.toString()

    }

}