package io.nebulas.wallet.android.module.wallet.create

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.view.View
import io.nebulas.wallet.android.R
import io.nebulas.wallet.android.base.BaseActivity
import io.nebulas.wallet.android.base.BaseBindingAdapter
import io.nebulas.wallet.android.common.DataCenter
import io.nebulas.wallet.android.module.balance.model.Coin
import io.nebulas.wallet.android.module.wallet.create.adapter.CoinListAdapter
import io.nebulas.wallet.android.module.wallet.create.model.CoinListModel
import kotlinx.android.synthetic.nas_nano.activity_coin_list.*
import kotlinx.android.synthetic.nas_nano.app_bar_main.*
import org.jetbrains.anko.startActivityForResult
import org.jetbrains.annotations.NotNull

class CoinListActivity : BaseActivity() {


    companion object {

        private const val KEY_SELECTED_COIN_ID = "key_selected_coin_id"
        /**
         * 启动CoinListActivity
         *
         * @param context
         * @param requestCode
         */
        fun launch(@NotNull context: Context, @NotNull requestCode: Int, selectedCoinId: Long = -1L) {
            (context as AppCompatActivity).startActivityForResult<CoinListActivity>(requestCode, KEY_SELECTED_COIN_ID to selectedCoinId)
        }

    }

    private var selectedCoinId: Long = -1L


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        selectedCoinId = intent.getLongExtra(KEY_SELECTED_COIN_ID, -1L)
        setContentView(R.layout.activity_coin_list)
    }

    override fun initView() {
        showBackBtn(true, toolbar)
        titleTV.setText(R.string.title_choose_token)
        val adapter = CoinListAdapter(this)
        coinsRecyclerView.layoutManager = LinearLayoutManager(this)
        coinsRecyclerView.adapter = adapter

        adapter.setOnClickListener(object : BaseBindingAdapter.OnItemClickListener {
            override fun onItemClick(view: View, position: Int) {
                val item = adapter.items[position]
                adapter.items.forEach {
                    it.selected = it==item
                }
                adapter.notifyDataSetChanged()
                view.postDelayed({
                    onCoinSelected(item.coin)
                }, 100)
            }

            override fun onItemLongClick(view: View, position: Int) {}
        })

        val dataSource = loadCoins()
        adapter.items.addAll(dataSource)
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (Activity.RESULT_OK == resultCode) {
            setResult(resultCode, data)
            finish()
        }
    }

    private fun loadCoins(): List<CoinListModel> {
        val dataSource: MutableList<CoinListModel> = mutableListOf()
        DataCenter.coinsGroupByCoinSymbol.forEach {
            dataSource.add(CoinListModel(it, it.id == selectedCoinId))
        }
        return dataSource
    }

    private fun onCoinSelected(selectedCoin: Coin) {
        setResult(Activity.RESULT_OK, Intent().apply {
            putExtra("coin", selectedCoin)
        })
        finish()
    }

}
