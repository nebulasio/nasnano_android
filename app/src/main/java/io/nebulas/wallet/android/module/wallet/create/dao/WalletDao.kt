package io.nebulas.wallet.android.module.wallet.create.dao

import android.arch.persistence.room.*
import io.nebulas.wallet.android.module.wallet.create.model.Wallet

/**
 * Created by Heinoc on 2018/2/10.
 */
@Dao
interface WalletDao {

    @Query("select * from wallet where id = :walletId")
    fun loadWalletById(walletId: Long): Wallet

    @Query("select * from wallet where id>0")
    fun loadAllWallet(): List<Wallet>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertWallet(wallet: Wallet): Long

    @Insert
    fun insertWallets(wallet: List<Wallet>)

    @Delete
    fun deleteWallet(wallet: Wallet)

}