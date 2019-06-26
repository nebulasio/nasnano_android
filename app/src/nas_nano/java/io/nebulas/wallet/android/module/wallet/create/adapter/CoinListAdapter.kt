package io.nebulas.wallet.android.module.wallet.create.adapter

import android.content.Context
import io.nebulas.wallet.android.R
import io.nebulas.wallet.android.base.BaseBindingAdapter
import io.nebulas.wallet.android.databinding.ItemCoinListBinding
import io.nebulas.wallet.android.module.wallet.create.model.CoinListModel

/**
 * Created by Heinoc on 2018/2/22.
 */
class CoinListAdapter(context: Context) : BaseBindingAdapter<CoinListModel,ItemCoinListBinding>(context) {
    override fun getLayoutResId(viewType: Int): Int {
        return R.layout.item_coin_list
    }

    override fun onBindItem(binding: ItemCoinListBinding?, item: CoinListModel) {
        binding?.item = item
    }
}