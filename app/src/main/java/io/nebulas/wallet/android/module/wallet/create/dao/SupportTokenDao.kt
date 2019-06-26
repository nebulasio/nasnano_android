package io.nebulas.wallet.android.module.wallet.create.dao

import android.arch.persistence.room.*
import io.nebulas.wallet.android.module.wallet.create.model.SupportToken

/**
 * Created by Heinoc on 2018/3/9.
 */
@Dao
interface SupportTokenDao {

    @Query("select * from support_token")
    fun loadAllToken(): List<SupportToken>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertToken(token: SupportToken): Long

    @Insert
    fun insertTokens(tokens: List<SupportToken>)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    fun updateToken(token: SupportToken)

    @Delete
    fun deleteToken(vararg token: SupportToken)

    @Query("delete from support_token")
    fun deleteAllData()

}