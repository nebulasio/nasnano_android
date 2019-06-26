package io.nebulas.wallet.android.module.wallet.create.viewmodel

import android.arch.lifecycle.ViewModel
import io.nebulas.wallet.android.common.DataCenter
import io.nebulas.wallet.android.db.DBUtil
import io.nebulas.wallet.android.module.balance.model.Coin
import io.nebulas.wallet.android.module.wallet.create.model.Address
import io.nebulas.wallet.android.module.wallet.create.model.Wallet
import io.reactivex.Completable
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import walletcore.Walletcore

/**
 * Created by Heinoc on 2018/2/22.
 */
class WalletViewModel : ViewModel() {

    fun getWallets(): List<Wallet> {
        return DBUtil.appDB.walletDao().loadAllWallet()
    }

    fun getAddresses(): List<Address> {
        return DBUtil.appDB.addressDao().loadAllAddresses()
    }

//    fun getCoinsGroupCoinSymbol(): List<Coin> {
//        return DBUtil.appDB.coinDao().loadAllCoinsGroupByCoinSymbol()
//    }

    fun getCoins(): List<Coin> {
        return DBUtil.appDB.coinDao().loadAllCoins()
    }

    fun updateWallet(wallet: Wallet): Completable {
        return Completable.fromAction {
            DBUtil.appDB.walletDao().insertWallet(wallet)
        }
    }

    fun getPlainPrivateKey(address: Address, passPhrase: String, onComplete: (privateKey: String) -> Unit, onFailed: (errMsg: String) -> Unit) {
        doAsync {
            val result = Walletcore.getPlainPrivateKeyFromKeyJson(address.platform, address.getKeyStore(), passPhrase)

            if (result.errorCode != 0L) {
                uiThread {
                    onFailed(result.errorMsg)
                }

            } else {
                uiThread {
                    onComplete(result.plainPrivateKey)

                }
            }
        }
    }

    fun verifyWalletPassPhrase(wallet: Wallet, passPhrase: String, onComplete: (success: Boolean) -> Unit, onFailed: (errMsg: String) -> Unit) {
        doAsync {
            val addresses = if (wallet.id >= 0) {
                //正常备份Nas主网钱包
                DataCenter.addresses.filter { it.walletId == wallet.id }
            } else {
                //备份换币钱包（ETH）
                val swapAddress = DataCenter.swapAddress
                if (swapAddress!=null){
                    arrayListOf(swapAddress)
                } else {
                    emptyList()
                }
            }
            if (addresses.isEmpty())
                onComplete(false)

            getPlainPrivateKey(addresses[0], passPhrase, {
                onComplete(!it.isEmpty())
            }, {
                onFailed(it)
            })

        }
    }

    fun changePassPhrase(wallet: Wallet, oldPassPhrase: String, newPassPhrase: String, onComplete: (success: Boolean) -> Unit, onFailed: (errMsg: String) -> Unit) {

        doAsync {
            val addresses = DataCenter.addresses.filter { it.walletId == wallet.id }

            kotlin.run breakPoint@{
                addresses.forEach {
                    var result = Walletcore.resetPassPhrase(it.platform, it.getKeyStore(), oldPassPhrase, newPassPhrase)
                    if (result.errorCode != 0L) {
                        uiThread {
                            onFailed(result.errorMsg)
                        }
                        return@breakPoint

                    } else {
                        it.setKeyStore(result.keyJson)
                    }
                }

                DBUtil.appDB.addressDao().updateAddresses(addresses)

                uiThread {
                    onComplete(true)

                }

            }

        }

    }

    fun deleteWallet(wallet: Wallet, passPhrase: String, onComplete: (success: Boolean) -> Unit, onFailed: (errMsg: String) -> Unit) {

        verifyWalletPassPhrase(wallet, passPhrase, {
            if (it) {
                doAsync {

                    val addresses = DataCenter.addresses.filter { it.walletId == wallet.id }
                    val coins = DataCenter.coins.filter { it.walletId == wallet.id }

                    DBUtil.appDB.coinDao().deleteCoins(coins)
                    DBUtil.appDB.coinDao().loadAllCoins()

                    DBUtil.appDB.addressDao().deleteAddresses(addresses)
                    DBUtil.appDB.walletDao().deleteWallet(wallet)

                    DataCenter.deleteWallet(wallet)
                    DataCenter.addresses.removeAll(addresses)
                    DataCenter.coins.removeAll(coins)

                    uiThread {
                        onComplete(true)
                    }

                }
            } else
                onComplete(false)
        }, {
            onFailed(it)
        })

    }


}
