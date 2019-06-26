package io.nebulas.wallet.android.module.transaction.adapter

import android.content.Context
import io.nebulas.wallet.android.R
import io.nebulas.wallet.android.base.BaseBindingAdapter
import io.nebulas.wallet.android.databinding.ItemChooseTokenRecyclerviewBinding
import io.nebulas.wallet.android.module.transaction.model.ChooseTokenListModel

/**
 * Created by Heinoc on 2018/6/21.
 */
class ChooseTokenRecyclerviewAdapter(context: Context) : BaseBindingAdapter<ChooseTokenListModel, ItemChooseTokenRecyclerviewBinding>(context) {

    override fun getLayoutResId(viewType: Int): Int {
        return R.layout.item_choose_token_recyclerview

    }

    override fun onBindItem(binding: ItemChooseTokenRecyclerviewBinding?, item: ChooseTokenListModel) {
        binding?.item = item
    }
}