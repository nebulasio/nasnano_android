package io.nebulas.wallet.android.module.balance.model

import android.arch.persistence.room.*
import io.nebulas.wallet.android.base.BaseEntity
import io.nebulas.wallet.android.common.Constants
import io.nebulas.wallet.android.common.URLConstants
import io.nebulas.wallet.android.module.wallet.create.model.Address
import io.nebulas.wallet.android.module.wallet.create.model.SupportToken
import io.nebulas.wallet.android.module.wallet.create.model.Wallet
import io.nebulas.wallet.android.network.eth.ETHHttpManager
import io.nebulas.wallet.android.network.eth.model.ETHRequestBody
import io.nebulas.wallet.android.network.nas.NASHttpManager
import io.nebulas.wallet.android.network.nas.model.NasBalanceModel
import io.nebulas.wallet.android.util.Formatter
import io.reactivex.Observer
import org.json.JSONObject
import walletcore.Walletcore
import java.io.Serializable
import java.math.BigDecimal

/**
 * 数字货币model
 *
 * Created by Heinoc on 2018/2/9.
 */

@Entity(tableName = "token", foreignKeys = arrayOf(
        ForeignKey(entity = Wallet::class, parentColumns = arrayOf("id"), childColumns = arrayOf("wallet_id")),
        ForeignKey(entity = Address::class, parentColumns = arrayOf("id"), childColumns = arrayOf("address_id"))
))
class Coin constructor() : BaseEntity(), Serializable {

    /**
     * id
     */
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0
    /**
     * wallet id
     */
    @ColumnInfo(name = "wallet_id")
    var walletId: Long = 0
    /**
     * address id
     */
    @ColumnInfo(name = "address_id")
    var addressId: Long = 0
    /**
     * address
     */
    var address: String? = ""
    /**
     * tokenId from server
     */
    @ColumnInfo(name = "token_id")
    var tokenId: String? = ""
    /**
     * 数字货币符号
     */
    var symbol: String = ""
    /**
     * 数字货币名称
     */
    var name: String = ""
    /**
     * 区块链类型（属于ETH/NAS）
     */
    var platform: String = ""
        set(value) {
            field = when (value) {
                "neb" -> Walletcore.NAS
                "eth" -> Walletcore.ETH
                else -> value
            }
        }
    /**
     * 智能合约地址
     */
    var contractAddress: String = ""
    /**
     * logo
     */
    var logo: String = ""
    /**
     *
     */
    var tokenDecimals: String = "18"
    /**
     * 数字货币行情
     */
    var quotation: String = "+0%"
    /**
     * 数字货币数量
     */
    var balance: String = "0"
    @Ignore
    var balanceString: String = ""
        get() {
            if (this.balance.isNullOrEmpty())
                return "0"

            if (this.tokenDecimals.isNullOrEmpty()) {
                this.tokenDecimals = "18"
            }

            val data = BigDecimal(Formatter.amountFormat(this.balance, this.tokenDecimals.toInt()))
            return Formatter.tokenFormat(data)
        }
    /**
     * 法币汇率
     */
    @Ignore
    var currencyPrice: String = "0"

    @Ignore
    var noCurrencyPrice: Boolean = false
        get() {
            return currencyPrice == "0.00" || currencyPrice == "0" || currencyPrice == "0.0000"
        }

    @Ignore
    var currencyPriceString: String = "0.00"
        get() {
            if (this.currencyPrice.isNullOrBlank())
                return "0.00"
            return currencyPrice
//            val data = BigDecimal(this.currencyPrice)
//            return Formatter.priceFormat(data)
        }
    /**
     * 数字货币兑法币总价值
     */
    var balanceValue: String = "0.00"
    @Ignore
    var balanceValueString: String = "0.00"
        get() {
            if (this.balanceValue.isNullOrEmpty())
                return "0.00"
            val assets = BigDecimal(Formatter.amountFormat(this.balance, this.tokenDecimals.toInt()))
                    .multiply(BigDecimal(currencyPrice))
            return assets.toPlainString()
        }

    @Ignore
    var formattedBalanceValueString:String = "0.00"
        get() {
            val balance = BigDecimal(balanceValueString)
            return Formatter.priceFormat(balance)
        }
    /**
     * 列表展示排序
     */
    var displayed: Int = 0
    /**
     * 是否为核心币
     * 1：核心币
     */
    var type: Int = 0
    /**
     * 类型标识
     * CORE：核心币
     * NORMAL：常用币
     */
    var mark: String = ""

    /**
     * 是否展示该token
     */
    var isShow: Boolean = true


    constructor(id: Long, walletId: Long, addressId: Long, address: String?,
                tokenId: String?, symbol: String, name: String, platform: String,
                contractAddress: String, logo: String, tokenDecimals: String,
                quotation: String, currencyPrice: String, balance: String, balanceValue: String,
                displayed: Int, type: Int, mark: String, isShow: Boolean) : this() {
        this.id = id
        this.walletId = walletId
        this.addressId = addressId
        this.address = address
        this.tokenId = tokenId
        this.symbol = symbol
        this.name = name
        this.platform = platform
        this.contractAddress = contractAddress
        this.logo = logo
        this.tokenDecimals = tokenDecimals
        this.quotation = quotation
        this.currencyPrice = currencyPrice
        this.balance = balance
        this.balanceValue = balanceValue
        this.displayed = displayed
        this.type = type
        this.mark = mark
        this.isShow = isShow
    }


    constructor(token: SupportToken) : this() {
        this.tokenId = token.id
        this.symbol = token.symbol
        this.name = token.name
        this.platform = token.platform
        this.contractAddress = token.contractAddress
        this.logo = token.logo
        this.tokenDecimals = token.tokenDecimals
        this.quotation = if (token.quotation.isNullOrEmpty()) "+0%" else token.quotation!!
        this.displayed = token.displayed
        this.type = token.type
        this.mark = token.mark!!
        this.currencyPrice = token.currencyPrice
    }


    fun copy(): Coin {
        return Coin(this.id, this.walletId, this.addressId, this.address, this.tokenId, this.symbol, this.name,
                this.platform, this.contractAddress, this.logo, this.tokenDecimals, this.quotation, this.currencyPrice,
                this.balance, this.balanceValue, this.displayed, this.type, this.mark, if (null == this.isShow) true else this.isShow!!)
    }

    /**
     * 获取当前主链货币的balance
     */
    fun getNASBalance(subscriber: Observer<NasBalanceModel>) {
        NASHttpManager.getBalance(mapOf("address" to this.address!!), subscriber)
    }

}