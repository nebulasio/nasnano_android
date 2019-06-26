package io.nebulas.wallet.android.module.balance.dao

import android.arch.persistence.room.*
import io.nebulas.wallet.android.module.balance.model.Coin

/**
 * Created by Heinoc on 2018/2/23.
 */
@Dao
interface CoinDao {

    @Query("select * from token where wallet_id = :walletId")
    fun getCoinsByWalletId(walletId: Int): List<Coin>

    @Query("select * from token where address_id = :addressId")
    fun getCoinsByAddressId(addressId: Int): List<Coin>

    @Query("select * from token where wallet_id = :walletId and address_id = :addressId")
    fun getCoinsByWalletIdAndAddressId(walletId: Int, addressId: Int): List<Coin>

    @Query("select * from token")
    fun loadAllCoins(): List<Coin>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertCoin(coin: Coin): Long

    @Insert
    fun insertCoins(coin: List<Coin>)

    @Update
    fun updateCoin(coin: Coin)

//    @Query("select id, wallet_id, address_id, address, token_id, symbol, name, platform, contractAddress, logo, tokenDecimals, quotation, sum(balance) as balance, sum(balanceValue) as balanceValue, displayed, type, mark from token group by symbol order by displayed")
//    fun loadAllCoinsGroupByCoinSymbol(): List<Coin>

    @Delete
    fun deleteCoin(vararg coin: Coin)

    @Delete
    fun deleteCoins(coins: List<Coin>)


}