package io.nebulas.wallet.android.db

import android.content.Context

/**
 * Created by Heinoc on 2018/3/9.
 */
object DBUtil {

    lateinit var appDB: AppDB

    /**
     * 初始化
     */
    fun initAppDB(context: Context){
        appDB = AppDB.getInstance(context)
    }

}