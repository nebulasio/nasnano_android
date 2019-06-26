package io.nebulas.wallet.android.module.token

import android.content.Context
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.view.inputmethod.EditorInfo
import io.nebulas.wallet.android.R
import io.nebulas.wallet.android.base.BaseActivity
import io.nebulas.wallet.android.common.Constants
import io.nebulas.wallet.android.common.DataCenter
import io.nebulas.wallet.android.extensions.initCollapsingToolbar
import io.nebulas.wallet.android.extensions.removeKeyBoard
import io.nebulas.wallet.android.module.token.adapter.ManageTokenRecyclerViewAdapter
import io.nebulas.wallet.android.module.token.model.ManageTokenListModel
import kotlinx.android.synthetic.nas_nano.activity_manage_token.*
import org.jetbrains.anko.startActivity
import org.jetbrains.annotations.NotNull

class ManageTokenActivity : BaseActivity() {

    companion object {
        fun launch(@NotNull context: Context){
            context.startActivity<ManageTokenActivity>()
        }
    }

    lateinit var adapter: ManageTokenRecyclerViewAdapter

    var tokenList = mutableListOf<ManageTokenListModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manage_token)
    }

    override fun initView() {

        initCollapsingToolbar(getString(R.string.manage_assets_title), {
            if (adapter.items.size != tokenList.size){
                adapter.items.clear()
                adapter.items.addAll(tokenList)
            }else
                onBackPressed()
        })

        manageTokenRecyclerView.layoutManager = LinearLayoutManager(this)

        adapter = ManageTokenRecyclerViewAdapter(this)
        manageTokenRecyclerView.adapter = adapter

        DataCenter.coinsGroupByCoinSymbol.forEach {
            tokenList.add(ManageTokenListModel(currencyName = Constants.CURRENCY_SYMBOL_NAME, currencySymbol = Constants.CURRENCY_SYMBOL, coin = it))
        }

        adapter.items.addAll(tokenList)


        searchET.setOnEditorActionListener { textView, actionId, event ->
            when (actionId) {
                EditorInfo.IME_ACTION_SEARCH -> {
                    removeKeyBoard()

                    val keyword = searchET.text.toString()
                    if (keyword.trim().isNotEmpty()) {

                        val searchList = mutableListOf<ManageTokenListModel>()
                        DataCenter.coinsGroupByCoinSymbol.filter {
                            it.symbol.toLowerCase().contains(keyword.toLowerCase())
                        }.forEach {
                            searchList.add(ManageTokenListModel(currencyName = Constants.CURRENCY_SYMBOL_NAME, currencySymbol = Constants.CURRENCY_SYMBOL, coin = it))
                        }

                        if (searchList.isEmpty()){
                            searchList.add(ManageTokenListModel(emptyView = true))
                        }else{
                            searchList.add(ManageTokenListModel(category = getString(R.string.manage_token_search_result)))
                        }

                        adapter.items.clear()
                        adapter.items.addAll(searchList)

                    }

                    true
                }
                else -> false
            }
        }

    }

}
