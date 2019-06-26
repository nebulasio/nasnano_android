package io.nebulas.wallet.android.module.wallet.create.dao

import android.arch.persistence.room.*
import io.nebulas.wallet.android.module.wallet.create.model.Address

/**
 * Created by Heinoc on 2018/2/23.
 */
@Dao
interface AddressDao {

    @Query("select * from address where wallet_id = :walletId")
    fun getAddressesByWalletId(walletId: Int): List<Address>

    @Query("select * from address")
    fun loadAllAddresses(): List<Address>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAddress(address: Address): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAddrsses(address: List<Address>)

    @Delete
    fun deleteAddress(vararg address: Address)

    @Delete
    fun deleteAddresses(addresses: List<Address>)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    fun updateAddress(vararg address: Address)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    fun updateAddresses(addresses: List<Address>)


}