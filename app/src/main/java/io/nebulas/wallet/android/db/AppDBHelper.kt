package io.nebulas.wallet.android.db

import android.arch.persistence.room.Room
import android.content.Context
import io.nebulas.wallet.android.module.wallet.create.model.Wallet

/**
 * DataBase Helper
 *
 * Created by Heinoc on 2018/2/10.
 */
class AppDBHelper constructor(context: Context) {

    val appDB = Room.databaseBuilder(context, AppDB::class.java, "nas_wallet_db").build()

    companion object {
        private var INSTANCE: AppDBHelper? = null

        fun getInstance(context: Context): AppDBHelper {
            if (INSTANCE === null) {
                synchronized(AppDBHelper::class) {
                    if (INSTANCE == null) {
                        INSTANCE = AppDBHelper(context.applicationContext)
                    }
                }
            }
            return INSTANCE!!
        }
    }

    fun insertToWallet(wallet: Wallet) {
        appDB.walletDao().insertWallet(wallet)
    }

}