package io.nebulas.wallet.android.module.transaction.dao

import android.arch.persistence.room.*
import io.nebulas.wallet.android.module.transaction.model.Transaction

/**
 * Created by Heinoc on 2018/2/23.
 */
@Dao
interface TransactionDao {

    @Query("select * from tx where hash = :hash")
    fun getTransactionByHash(hash: String): Transaction

    @Query("select * from tx")
    fun loadAllTransactions(): List<Transaction>

    @Query("select * from tx group by currencyId order by blockTimestamp desc limit :count")
    fun getRecentlySentRecordWithDiffCoin(count: Int): List<Transaction>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertTransaction(transaction: Transaction): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertTransactions(transactions: List<Transaction>)

    @Delete
    fun deleteTransaction(vararg transaction: Transaction)

    @Delete
    fun deleteTransactions(transactions: List<Transaction>)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    fun updateTransaction(vararg transaction: Transaction)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    fun updateTransactions(transactions: List<Transaction>)


}