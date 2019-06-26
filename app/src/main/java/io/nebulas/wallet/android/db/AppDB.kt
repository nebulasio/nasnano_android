package io.nebulas.wallet.android.db

import android.arch.persistence.db.SupportSQLiteDatabase
import android.arch.persistence.room.Database
import android.arch.persistence.room.Room
import android.arch.persistence.room.RoomDatabase
import android.arch.persistence.room.migration.Migration
import android.content.Context
import io.nebulas.wallet.android.BuildConfig
import io.nebulas.wallet.android.module.balance.dao.CoinDao
import io.nebulas.wallet.android.module.balance.model.Coin
import io.nebulas.wallet.android.module.transaction.dao.TransactionDao
import io.nebulas.wallet.android.module.transaction.model.Transaction
import io.nebulas.wallet.android.module.wallet.create.dao.AddressDao
import io.nebulas.wallet.android.module.wallet.create.dao.SupportTokenDao
import io.nebulas.wallet.android.module.wallet.create.dao.WalletDao
import io.nebulas.wallet.android.module.wallet.create.model.Address
import io.nebulas.wallet.android.module.wallet.create.model.SupportToken
import io.nebulas.wallet.android.module.wallet.create.model.Wallet

/**
 * DataBase
 *
 * Created by Heinoc on 2018/2/10.
 */
@Database(entities = arrayOf(
        Wallet::class,
        Address::class,
        Coin::class,
        SupportToken::class,
        Transaction::class
), version = 5)
abstract class AppDB : RoomDatabase() {

    abstract fun walletDao(): WalletDao
    abstract fun addressDao(): AddressDao
    abstract fun coinDao(): CoinDao
    abstract fun supportTokenDao(): SupportTokenDao
    abstract fun transactionDao(): TransactionDao

    companion object {

        @Volatile
        private var INSTANCE: AppDB? = null

        fun getInstance(context: Context): AppDB =
                INSTANCE ?: synchronized(this) {
                    INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
                }

        private fun buildDatabase(context: Context) =
                Room.databaseBuilder(context.applicationContext, AppDB::class.java, BuildConfig.DB_NAME)
                        .addMigrations(MIGRATION_1_2,
                                MIGRATION_2_3,
                                MIGRATION_3_4,
                                MIGRATION_4_5)
                        .build()


        private val MIGRATION_1_2 = object : Migration(1, 2){
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE `token` ADD COLUMN `isShow` INTEGER NOT NULL DEFAULT 1")
//                database.execSQL("CREATE TABLE IF NOT EXISTS `tx` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `account` TEXT, `amount` TEXT, `blockHeight` TEXT, `blockTimestamp` INTEGER, `confirmed` INTEGER NOT NULL, `confirmedCnt` INTEGER NOT NULL, `currencyId` TEXT, `hash` TEXT, `libTimestamp` INTEGER, `maxConfirmCnt` INTEGER NOT NULL, `receiver` TEXT, `sendTimestamp` INTEGER, `sender` TEXT, `status` TEXT, `txFee` TEXT, `name` TEXT, `coinSymbol` TEXT, `platform` TEXT NOT NULL, `tokenDecimals` TEXT NOT NULL, `contractAddress` TEXT NOT NULL, `nonce` TEXT NOT NULL, `txData` TEXT NOT NULL, `gasPrice` TEXT NOT NULL, `gasLimit` TEXT NOT NULL, `remark` TEXT NOT NULL)")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3){
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE `wallet` ADD COLUMN `isComplexPwd` INTEGER NOT NULL DEFAULT 1")
                database.execSQL("ALTER TABLE `wallet` ADD COLUMN `createdByMnemonic` INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE `support_token` ADD COLUMN `currencyPrice` TEXT NOT NULL DEFAULT '0.00'")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4){
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("UPDATE `wallet` SET `isComplexPwd`=1")
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5){
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE `wallet` ADD COLUMN `wrongPasswordTimesString` TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE `wallet` ADD COLUMN `wrongPasswordTimes` INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE `wallet` ADD COLUMN `lockedTime` INTEGER NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE `wallet` ADD COLUMN `isLock` INTEGER NOT NULL DEFAULT 0")
            }
        }

    }

}