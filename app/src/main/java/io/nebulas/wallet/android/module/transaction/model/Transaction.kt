package io.nebulas.wallet.android.module.transaction.model

import android.arch.persistence.room.Entity
import android.arch.persistence.room.Ignore
import android.arch.persistence.room.PrimaryKey
import android.util.Base64
import com.alibaba.fastjson.JSON
import com.google.gson.annotations.SerializedName
import io.nebulas.wallet.android.R
import io.nebulas.wallet.android.app.WalletApplication
import io.nebulas.wallet.android.base.BaseEntity
import io.nebulas.wallet.android.common.Constants
import io.nebulas.wallet.android.common.DataCenter
import io.nebulas.wallet.android.module.wallet.create.model.Wallet
import io.nebulas.wallet.android.network.eth.ETHHttpManager
import io.nebulas.wallet.android.network.nas.NASHttpManager
import io.nebulas.wallet.android.network.nas.model.NASTransactionModel
import io.nebulas.wallet.android.network.server.HttpManager
import io.nebulas.wallet.android.util.Formatter
import io.reactivex.Observer
import walletcore.Payload
import walletcore.Walletcore
import java.io.Serializable
import java.math.BigDecimal

/**
 * 交易model
 *
 * Created by Heinoc on 2018/2/6.
 */

@Entity(tableName = "tx")
class Transaction constructor() : BaseEntity(), Serializable {

    @PrimaryKey(autoGenerate = true)
    var id: Long = 0

    /**
     * 本地发生交易的钱包地址
     */
    var account: String? = null
    /**
     * 交易数量
     */
    var amount: String? = null
    @Ignore
    var amountString: String = ""
        get() {
            if (this.amount.isNullOrEmpty())
                return ""

            val tokenSymbol = if (isVoteTransaction()){
                Constants.voteContractsMap[receiver]
            } else {
                this.currencyId
            }
            DataCenter.coins.forEach {
                if (it.tokenId == tokenSymbol) {
                    this.tokenDecimals = it.tokenDecimals
                }
            }

            if (this.tokenDecimals.isNullOrEmpty()) {
                this.tokenDecimals = "18"
            }
            val data = BigDecimal(Formatter.amountFormat(this.amount!!, this.tokenDecimals.toInt()))
            return Formatter.tokenFormat(data)
        }
    /**
     * 交易产生是的区块高度
     */
    var blockHeight: String? = ""
    /**
     * 发送交易的打包时间
     */
    var blockTimestamp: Long? = null
    /**
     * 是否已确认
     */
    var confirmed: Boolean = false
    /**
     * 已确认节点数
     */
    var confirmedCnt: Int = 0
    /**
     * 货币ID
     */
    var currencyId: String? = null
    /**
     * tx hash
     */
    var hash: String? = null
    /**
     * tx到达lib时间
     */
    var libTimestamp: Long? = null
    /**
     * 节点最大确认数
     */
    var maxConfirmCnt: Int = 0
    /**
     * 交易接收方
     */
    var receiver: String? = null
    /**
     * tx发送时间
     */
    var sendTimestamp: Long? = null
    /**
     * 交易发送方
     */
    var sender: String? = null
    /**
     * 交易状态
     */
    var status: String? = null
    @Ignore
    var statusString: String? = null
        get() {
            when (this.status) {
                "success" -> {
                    return WalletApplication.INSTANCE.getString(R.string.status_success)
                }
                "waiting" -> {
                    return WalletApplication.INSTANCE.getString(R.string.status_waiting)
                }
                "pending" -> {
                    return WalletApplication.INSTANCE.getString(R.string.status_pending)

                }
                "fail" -> {
                    return WalletApplication.INSTANCE.getString(R.string.status_fail)

                }
            }
            return field
        }
    /**
     * 矿工费
     */
    var txFee: String? = null
    @Ignore
    var txFeeString: String = ""
        get() {
            if (this.txFee.isNullOrEmpty())
                return ""
            var tDecimal = "18"
            DataCenter.coins.forEach {
                if (it.platform == "nebulas" && it.type == 1) {
                    tDecimal = it.tokenDecimals
                    return@forEach
                }
            }
            val data = BigDecimal(Formatter.amountFormat(this.txFee!!, tDecimal.toInt()))
            return Formatter.tokenFormat(data)
        }
    @Ignore
    var txFeeCurrency: String = ""
        set(value) {
            val data = BigDecimal(value)
            field = Formatter.gasPriceFormat(data)
        }


    /**
     * 对方姓名
     */
    var name: String? = ""

    /**
     * 发生交易的货币符号
     */
    var coinSymbol: String? = null
        get() {
            if (field.isNullOrEmpty()) {
                run breakPoint@{
                    DataCenter.coins.forEach {
                        if (it.tokenId == this.currencyId) {
                            field = it.symbol
                            return@breakPoint
                        }
                    }
                }
            }
            return field
        }
    /**
     * 区块链类型（属于ETH/NAS）
     */
    var platform: String = ""

    var tokenDecimals: String = "18"


//    /**
//     * 缩短版钱包地址
//     */
//    var shortAddress: String? = null
//    /**
//     * 交易对方地址
//     */
//    var targetAddress: String? = null
    /**
     * 智能合约地址
     */
    var contractAddress: String = ""

    /**
     * 当前账户正在交易的笔数
     */
    var nonce: String = "1"
    /**
     * data
     */
    @SerializedName("data")
    var txData: String = ""
    /**
     * gas price
     */
    var gasPrice: String = ""
    /**
     * gas limit
     */
    var gasLimit: String = ""
    /**
     * 交易备注
     */
    var remark: String = ""


    /**
     * explorer tx路径
     */
    @Ignore
    var explorerURL: String = ""
    /**
     * 是否为收款交易
     * true：转账交易
     * false：收款交易
     */
    @Ignore
    var isSend: Boolean = false
    /**
     * 私钥签名过后的数据
     */
    @Ignore
    var signedData: String = ""

    @Ignore
    var wallet: Wallet? = null
        get() {
            if (this.account.isNullOrEmpty())
                return null
            var walletId: Long = -1
            run breakPoint@{
                DataCenter.addresses.forEach {
                    if (it.address == this.account) {
                        walletId = it.walletId
                        return@breakPoint
                    }
                }
            }

            DataCenter.wallets.forEach {
                if (it.id == walletId) {
                    return it
                }
            }

            return null
        }

    /**
     * 法币符号
     */
    @Ignore
    var currencySymbol: String = ""

    /**
     * nas 智能合约对象
     */
    @Ignore
    var payload: Payload? = null


    constructor(currencyId: String?, coinSymbol: String?, blockChainType: String, account: String?, targetAddress: String?, contractAddress: String, amount: String?, txData: String, gasPrice: String, gasLimit: String, isSend: Boolean) : this() {
        this.currencyId = currencyId
        this.coinSymbol = coinSymbol
        this.platform = blockChainType
        this.account = account
        if (isSend)
            this.receiver = targetAddress
        else
            this.sender = targetAddress
        this.contractAddress = contractAddress
        this.amount = amount
        this.txData = txData
        this.gasPrice = gasPrice
        this.gasLimit = gasLimit
        this.isSend = isSend

        this.payload = Payload()
        this.payload?.nasType = Walletcore.TxPayloadBinaryType
    }

    fun getPayloadJson(): String {
        return "{\"source\":\"${this.payload?.nasSource}\",\"sourceType\":\"${this.payload?.nasSourceType}\",\"binary\":\"${this.payload?.nasBinary}\",\"function\":\"${this.payload?.nasFunction}\",\"args\":\"${this.payload?.nasArgs}\",\"type\":\"${this.payload?.nasType}\"}"
    }


    /**
     * ETH发起交易
     */
    fun sendETHRawTransaction(subscriber: Observer<String>) {
        ETHHttpManager.sendRawTransaction(this.signedData, subscriber)
    }

    /**
     * NAS发起交易
     */
    fun sendNASRawTransaction(subscriber: Observer<NASTransactionModel>) {
        NASHttpManager.sendRawTransaction(mapOf("data" to this.signedData), subscriber)
    }

    /**
     * 获取指定交易的交易详情
     */
    fun getTxDetail(subscriber: Observer<Transaction>) {
        if (hash == null || currencyId == null) {
            return
        }
        var currencyId = this.currencyId ?: Walletcore.NAS
        if (isVoteTransaction()) {
            currencyId = Walletcore.NAS
        }
        HttpManager.getTxDetail(this.hash!!, currencyId, subscriber)
    }

    private fun isVoteTransaction(): Boolean {
        if (!Constants.voteContracts.contains(receiver)) {
            return false
        }
        if (payload != null) {
            val payload = payload!!
            return payload.nasFunction == "vote"
        } else {
            val data = txData
            if (!data.isNullOrEmpty()) {
                return try {
                    val txData = String(Base64.decode(data.toByteArray(), Base64.DEFAULT))
                    val json = JSON.parseObject(txData)
                    val function = json.getString("Function")
                    function == "vote"
                } catch (e: Exception) {
                    false
                }
            }
        }
        return false
    }

    /**
     * 获取nas neb status
     */
    fun getNasNebStatus(subscriber: Observer<BlockStateModel>) {
        NASHttpManager.getNebState(subscriber)
    }

    /**
     * 获取eth block number
     */
    fun getEthTxStatus(subscriber: Observer<String>) {
        ETHHttpManager.blockNumber(subscriber)
    }


}