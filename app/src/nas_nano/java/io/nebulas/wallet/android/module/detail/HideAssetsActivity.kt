package io.nebulas.wallet.android.module.detail

import android.content.Context
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import io.nebulas.wallet.android.R
import io.nebulas.wallet.android.base.BaseActivity
import io.nebulas.wallet.android.common.Constants
import io.nebulas.wallet.android.common.DataCenter
import io.nebulas.wallet.android.module.token.adapter.ManageTokenRecyclerViewAdapter
import io.nebulas.wallet.android.module.token.model.ManageTokenListModel
import io.nebulas.wallet.android.util.Formatter
import kotlinx.android.synthetic.nas_nano.activity_hide_assets.*
import kotlinx.android.synthetic.nas_nano.app_bar_hide_assets.*
import org.jetbrains.anko.startActivity
import org.jetbrains.annotations.NotNull
import java.math.BigDecimal

class HideAssetsActivity : BaseActivity() {

    companion object {
        fun launch(@NotNull context: Context) {
            context.startActivity<HideAssetsActivity>()
        }
    }

    lateinit var adapter: ManageTokenRecyclerViewAdapter

    var tokenList = mutableListOf<ManageTokenListModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_hide_assets)
    }

    override fun initView() {
        showBackBtn(blackBack = false, toolbar = toolbar)
        titleTV.text = getString(R.string.hide_assets_title)

        hideAssetsRecyclerView.layoutManager = LinearLayoutManager(this)

        adapter = ManageTokenRecyclerViewAdapter(this)
        hideAssetsRecyclerView.adapter = adapter

        var totalBalanceValue = BigDecimal(0)

        DataCenter.coinsGroupByCoinSymbol.filter {
            !it.isShow
        }.forEach {
            totalBalanceValue += BigDecimal(it.balanceValue)
            tokenList.add(ManageTokenListModel(currencyName = Constants.CURRENCY_SYMBOL_NAME, currencySymbol = Constants.CURRENCY_SYMBOL, coin = it))
        }

        totalBalanceValueTV.text = "≈${Constants.CURRENCY_SYMBOL}${Formatter.priceFormat(totalBalanceValue)}"

        adapter.items.addAll(tokenList)

    }

}
