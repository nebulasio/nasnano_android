package io.nebulas.wallet.android.network.server.model

import io.nebulas.wallet.android.base.BaseEntity

/**
 * /api/account/balance
 *
 * Created by Heinoc on 2018/3/12.
 */
class BalanceResp : BaseEntity() {
    var totalBalanceValue: String = "0.00"
    var currencyList: List<TokenBalanceList> = listOf()
    var accountFloat: AccountFloat = AccountFloat()
}

class TokenBalanceList : BaseEntity() {
    var address: String = ""
    var balanceValue: String = ""
    var currencies: List<TokenList> = listOf()
}

class TokenList : BaseEntity() {
    var logo: String = ""
    var currencyPrice: String = ""
    var balance: String = ""
    var balanceValue: String = ""
    var currencyId: String = ""
    var tokenDecimals: String = ""
}

class AccountFloat : BaseEntity() {
    var offsetTime: String = ""
    var floatInfo: String = ""
}