package io.nebulas.wallet.android.module.wallet.create.viewmodel

import android.arch.lifecycle.ViewModel
import android.content.Context
import io.nebulas.wallet.android.R
import io.nebulas.wallet.android.app.WalletApplication
import io.nebulas.wallet.android.common.Constants
import io.nebulas.wallet.android.common.DataCenter
import io.nebulas.wallet.android.db.DBUtil
import io.nebulas.wallet.android.extensions.logD
import io.nebulas.wallet.android.module.balance.model.Coin
import io.nebulas.wallet.android.module.wallet.create.model.Address
import io.nebulas.wallet.android.module.wallet.create.model.Wallet
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import walletcore.Walletcore
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Created by Heinoc on 2018/2/24.
 */
class CreateWalletViewModel : ViewModel() {

    /**
     * 通过助记词创建钱包
     */
    private var createWalletFromMnemonic = false
    /**
     * 通过新助记词创建钱包
     */
    private var createWalletWithNewMnemonic = false
    private var newMnemonic: String? = null

    /**
     * 通过助记词获取明文私钥
     *
     * @param mnemonic
     * @param path
     * @param onComplete
     * @param onFailed
     */
    fun getPlainPrivateKeyFromMnemonic(mnemonic: String, path: String, onComplete: (plainPrivateKey: String) -> Unit, onFailed: (errMsg: String) -> Unit) {
        doAsync {
            newMnemonic = mnemonic

            var result = Walletcore.getPlainPrivateKeyFromMnemonic(newMnemonic, path)

            if (result.errorCode != 0L) {

                uiThread {
                    onFailed(WalletApplication.INSTANCE.activity!!.getString(R.string.mnemonic_analysis_failed) + "\n" + result.errorMsg)
                }
                return@doAsync
            }

            uiThread { onComplete(result.plainPrivateKey) }

        }
    }

    /**
     * 通过KeyJson获取明文私钥
     *
     * @param coinTypeOfKeyJson
     * @param keyJson
     * @param passPhrase
     * @param onComplete
     * @param onFailed
     */
    fun getPlainPrivateKeyFromKeyJson(coinTypeOfKeyJson: String, keyJson: String, passPhrase: String, onComplete: (plainPrivateKey: String) -> Unit, onFailed: (errMsg: String) -> Unit) {
        doAsync {

            if (!keyJson.isEmpty()) {
                var result = Walletcore.getPlainPrivateKeyFromKeyJson(coinTypeOfKeyJson, keyJson, passPhrase)

                if (result.errorCode != 0L) {
                    uiThread {
                        //                        toastErrors(WalletApplication.INSTANCE.activity!!.getString(R.string.keyjson_analysis_failed) + "\n" + result.errorMsg, onComplete)
                        onFailed(result.errorMsg)
                    }
                    return@doAsync
                }

                uiThread { onComplete(result.plainPrivateKey) }
            }

        }
    }

    /**
     * 根据私钥获取各平台的地址字符串
     *
     * @param plainPrivateKey
     * @param passPhrase
     * @param onComplete
     * @param onFailed
     */
    fun getAddressFromPlainPrivateKey(plainPrivateKey: String, passPhrase: String, onComplete: (addresses: List<String>) -> Unit, onFailed: (errMsg: String) -> Unit) {
        doAsync {
            var addresses = mutableListOf<String>()
            Constants.PLATFORMS.forEach { platform ->
                var walletAccount = Walletcore.importWalletFromPrivateKey(platform, plainPrivateKey, passPhrase)
                if (walletAccount.errorCode != 0L) {
                    uiThread {
                        onFailed(platform + WalletApplication.INSTANCE.activity!!.getString(R.string.wallet_create_failed) + "\n" + walletAccount.errorMsg)
                    }
                    return@doAsync
                } else {
                    addresses.add(walletAccount.address)
                }
            }

            uiThread { onComplete(addresses) }

        }
    }

//    /**
//     * 新创建钱包
//     *
//     * @param passPhrase 密码
//     * @param coinList 需要创建地址的数字货币类型
//     * @param coinList 需要创建的数字货币列表
//     * @param platform 平台标识
//     * @param onComplete 钱包创建完成后的回调
//     */
//    fun generateWallet(walletName: String, passPhrase: String, coinList: List<Coin>, onComplete: (success: Boolean) -> Unit, onFailed: (errMsg: String) -> Unit) {
//        generateWallet(walletName, "", "", passPhrase, coinList, onComplete, onFailed)
//    }

    /**
     * 根据给定的keyJson文件新创建钱包
     *
     * @param platform 给定keyJson文件的数字货币类型
     * @param keyJson keyJson文本内容
     * @param passPhrase 密码
     * @param coinList 需要创建的数字货币列表
     * @param onComplete 钱包创建完成后的回调
     */
    fun generateWallet(walletName: String, platform: String, keyJson: String, passPhrase: String, isComplexPwd: Boolean, coinList: List<Coin>, onComplete: (wallet: Wallet?) -> Unit, onFailed: (errMsg: String) -> Unit) {
        doAsync {

            var plainPrivateKey = ""

            if (!keyJson.isEmpty()) {
                var result = Walletcore.getPlainPrivateKeyFromKeyJson(platform, keyJson, passPhrase)

                if (result.errorCode != 0L) {
                    uiThread {
                        //                        toastErrors(WalletApplication.INSTANCE.activity!!.getString(R.string.keyjson_analysis_failed) + "\n" + result.errorMsg, onComplete)
                        onFailed(result.errorMsg)
                    }
                    return@doAsync
                }

                plainPrivateKey = result.plainPrivateKey
            }

            generateWalletFromPrivateKey(walletName, arrayListOf(PkPlatformStruct(plainPrivateKey, platform)), passPhrase, isComplexPwd, coinList, onComplete, onFailed)

        }

    }


    /**
     * 根据助记词新创建钱包
     *
     * @param mnemonic 助记词，如果没有可以传空，方法将返回随机生成的助记词
     * @param path  助记词路径 (@deprecated)
     * @param passPhrase 密码
     * @param coinList 需要创建的数字货币列表
     * @param platforms 平台列表
     * @param onComplete 钱包创建完成后的回调
     */
    fun generateWalletFromMnemonic(walletName: String, mnemonic: String, path: String, passPhrase: String, isComplexPwd: Boolean, coinList: List<Coin>, platforms: ArrayList<String>, onComplete: (wallet: Wallet?) -> Unit, onFailed: (errMsg: String) -> Unit) {
        doAsync {

            newMnemonic = mnemonic

            val pkPlatformStructs = arrayListOf<PkPlatformStruct>()

            platforms.forEach {
                val platformMnemonicPath = when (it) {
                    Walletcore.NAS -> {
                        Constants.NAS_MNEMONIC_PATH
                    }

                    Walletcore.ETH -> {
                        Constants.ETH_MNEMONIC_PATH
                    }
                    else -> {
                        Constants.NAS_MNEMONIC_PATH
                    }

                }

                val result = Walletcore.getPlainPrivateKeyFromMnemonic(newMnemonic, platformMnemonicPath)

                if (result.errorCode != 0L) {

                    uiThread {
                        onFailed(WalletApplication.INSTANCE.activity!!.getString(R.string.mnemonic_analysis_failed) + "\n" + result.errorMsg)
                    }
                    return@doAsync
                }

                if (newMnemonic?.isEmpty() != false) {
                    createWalletWithNewMnemonic = true

                    newMnemonic = result.mnemonic
                }

                pkPlatformStructs.add(PkPlatformStruct(result.plainPrivateKey, it))
            }

            createWalletFromMnemonic = true

            generateWalletFromPrivateKey(walletName, pkPlatformStructs, passPhrase, isComplexPwd, coinList, onComplete, onFailed)

        }
    }

    /**
     * 根据给定的明文私钥新创建钱包
     *
     * @param pkPlatforms 明文私钥
     * @param passPhrase 密码
     * @param coinList 需要创建的数字货币列表
     * @param onComplete 钱包创建完成后的回调
     */
    fun generateWalletFromPrivateKey(walletName: String, pkPlatforms: ArrayList<PkPlatformStruct>, passPhrase: String, isComplexPwd: Boolean, coinList: List<Coin>, onComplete: (wallet: Wallet?) -> Unit, onFailed: (errMsg: String) -> Unit) {
        doAsync {

            generateWalletFromPrivateKeyInCurThread(walletName,
                    pkPlatforms,
                    passPhrase,
                    isComplexPwd,
                    coinList,
                    { onCompleteResult ->
                        uiThread {
                            onComplete(onCompleteResult)
                        }
                    },
                    { errMsg ->
                        uiThread {
                            onFailed(errMsg)
                        }

                    })
        }

    }

    /**
     * 根据给定的明文私钥新创建钱包
     *
     * @param pkPlatforms 明文私钥
     * @param passPhrase 密码
     * @param coinList 需要创建的数字货币列表
     * @param onComplete 钱包创建完成后的回调
     */
    fun generateWalletFromPrivateKeyInCurThread(walletName: String, pkPlatforms: ArrayList<PkPlatformStruct>, passPhrase: String, isComplexPwd: Boolean, coinList: List<Coin>, onComplete: (wallet: Wallet?) -> Unit, onFailed: (errMsg: String) -> Unit) {
        /**
         * 检验是否地址已经存在，如果存在，则替换现有address的keyjson即可
         */
        var existsAddress = false
        var existsWalletId = -1L

        var response: walletcore.Response

        var addresses: MutableList<Address> = mutableListOf()
        var coins: MutableList<Coin> = mutableListOf()

        //创建钱包
        var wallet = Wallet("")
        wallet.isComplexPwd = isComplexPwd

        /**
         * 是否通过新助记词创建钱包，如果是，则临时保存助记词
         */
        if (createWalletWithNewMnemonic) {
            wallet.setMnemonic(newMnemonic!!)
            createWalletWithNewMnemonic = false
            newMnemonic = null
        }

        if (createWalletFromMnemonic) {
            wallet.createdByMnemonic = true
            createWalletFromMnemonic = false
        }

        var insertWalletFlag = false
        for (pkPlatformItem in pkPlatforms) {

            //创建地址
            // 2018-6-15: 因为业务逻辑改为通过助记词新创建钱包，所以不会存在明文私钥为空的情况，该if判断分支已失效
            if (pkPlatformItem.plainPrivateKey.isEmpty()) {

                response = Walletcore.newWallet(pkPlatformItem.platform, passPhrase)
                if (response.errorCode != 0L) {
                    //wallet_create_failed
                    onFailed(pkPlatformItem.platform + WalletApplication.INSTANCE.activity!!.getString(R.string.wallet_create_failed) + "\n" + response.errorMsg)

                    continue
                }

                var tempWallet = Walletcore.getPlainPrivateKeyFromKeyJson(pkPlatformItem.platform, response.keyJson, passPhrase)
                if (tempWallet.errorCode != 0L) {
                    continue
                }

            } else {
                response = Walletcore.importWalletFromPrivateKey(pkPlatformItem.platform, pkPlatformItem.plainPrivateKey, passPhrase)
                if (response.errorCode != 0L) {
                    //wallet_create_failed
                    onFailed(pkPlatformItem.platform + WalletApplication.INSTANCE.activity!!.getString(R.string.wallet_create_failed) + "\n" + response.errorMsg)
                    continue

                }
            }

            // 遍历当前address，是否已经存在，如果存在则是修改密码，更改密码时，更新keystore 信息
            DataCenter.addresses.forEach {
                if (it.address == response.address) {
                    existsAddress = true
                    existsWalletId = it.walletId

                    it.setKeyStore(response.keyJson)
                    DBUtil.appDB.addressDao().updateAddress(it)
                }
            }
            // 如果当前是修改密码，则不再继续往下走
            if (existsAddress)
                continue

            /**
             * 走到这里时，说明一定是创建钱包，不存在修改密码的情况
             */
            //数据持久化
            if (!insertWalletFlag) {
                insertWalletFlag = true

                wallet.id = DBUtil.appDB.walletDao().insertWallet(wallet)
                wallet.walletName = walletName + " " + wallet.id.toString()
                DBUtil.appDB.walletDao().insertWallet(wallet)
            }


            var address = Address(response.address, response.keyJson, pkPlatformItem.platform)
            address.walletId = wallet.id
            //数据持久化
            address.id = DBUtil.appDB.addressDao().insertAddress(address)

            addresses.add(address)

            //添加数字货币
            coinList.forEach {
                if (pkPlatformItem.platform == it.platform) {
                    it.walletId = wallet.id
                    it.addressId = address.id
                    it.address = address.address

                    //数据持久化，插入coin
                    it.id = DBUtil.appDB.coinDao().insertCoin(it)

                    coins.add(it)
                }
            }


        }

        /**
         * 如果钱包已存在，更新钱包密码类型
         */
        if (existsAddress) {
            if (existsWalletId >= 0) {
                val existsWallet = DataCenter.wallets.find { it.id == existsWalletId }
                if (null != existsWallet) {
                    existsWallet.isComplexPwd = isComplexPwd
                    DBUtil.appDB.walletDao().insertWallet(existsWallet)
                }
            }
        } else {
            // 钱包不存在时，新增钱包，更新全局信息
            //内存备份数据
            DataCenter.wallets.add(wallet)
            DataCenter.addresses.addAll(addresses)
            DataCenter.coins.addAll(coins)
        }

        onComplete(if (addresses.size > 0 || existsAddress) wallet else null)

    }

    fun loadAllMemorizeWords(context: Context, loadComplete: (List<String>) -> Unit) {
        doAsync {
            val start = System.currentTimeMillis()
            val assetManager = context.assets
            val inputStream = assetManager.open("memorize_words.txt")
            val br = BufferedReader(InputStreamReader(inputStream, "UTF-8"))
            val list = br.readLines()
            val end = System.currentTimeMillis()
            uiThread {
                loadComplete(list)
            }
        }
    }


}

data class PkPlatformStruct(
        var plainPrivateKey: String = "",
        var platform: String = ""
) {}
