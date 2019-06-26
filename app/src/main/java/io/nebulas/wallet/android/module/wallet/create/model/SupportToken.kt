package io.nebulas.wallet.android.module.wallet.create.model

import android.arch.persistence.room.ColumnInfo
import android.arch.persistence.room.Entity
import android.arch.persistence.room.Ignore
import android.arch.persistence.room.PrimaryKey
import io.nebulas.wallet.android.base.BaseEntity
import io.nebulas.wallet.android.network.server.HttpManager
import io.nebulas.wallet.android.network.server.model.CurrencyResp
import io.reactivex.Observer
import walletcore.Walletcore
import java.io.Serializable

/**
 * 支持的数字货币model
 *
 * Created by Heinoc on 2018/2/9.
 */
@Entity(tableName = "support_token")
class SupportToken() : BaseEntity(), Serializable {

    /**
     * id
     */
    @PrimaryKey
    var id: String = ""
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
    var tokenDecimals: String = ""
    /**
     * 数字货币行情
     */
    @ColumnInfo()
    var quotation: String? = "+0%"
    /**
     * 是否被选中
     */
    var isSelected: Boolean = false
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
     * CORE
     * NORMAL
     */
    var mark: String? = ""
    /**
     * 类型名称
     */
    var markName: String? = ""
    /**
     * 法币汇率
     */
    var currencyPrice: String = "0.00"

    /**
     * 是否需要展示
     */
    @Ignore
    var hint: Int = 0

    constructor(id: String,
                symbol: String,
                name: String,
                platform: String,
                contractAddress: String,
                logo: String,
                tokenDecimals: String,
                quotation: String?,
                isSelected: Boolean,
                displayed: Int,
                type: Int,
                mark: String?,
                markName: String?,
                currencyPrice: String,
                hint:Int): this() {
        this.id = id
        this.symbol = symbol
        this.name = name
        this.platform = platform
        this.contractAddress = contractAddress
        this.logo = logo
        this.tokenDecimals = tokenDecimals
        this.quotation = quotation
        this.isSelected = isSelected
        this.displayed = displayed
        this.type = type
        this.mark = mark
        this.markName = markName
        this.currencyPrice = currencyPrice
        this.hint = hint
    }


    fun copy(): SupportToken {
        return SupportToken(this.id, this.symbol, this.name, this.platform,
                this.contractAddress, this.logo, this.tokenDecimals,
                this.quotation, this.isSelected, this.displayed, this.type, this.mark, this.markName, this.currencyPrice,this.hint)
    }


    companion object {
        /**
         * 从服务器获取当前支持的所有币
         */
        fun getSupportTokenList(subscriber: Observer<CurrencyResp>) {
            HttpManager.getCurrencyList(subscriber)
        }

    }

}