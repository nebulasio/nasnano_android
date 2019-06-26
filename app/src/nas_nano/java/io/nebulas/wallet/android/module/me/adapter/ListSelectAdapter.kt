package io.nebulas.wallet.android.module.me.adapter

import android.content.Context
import io.nebulas.wallet.android.R
import io.nebulas.wallet.android.base.BaseBindingAdapter
import io.nebulas.wallet.android.databinding.ItemListSelectBinding
import io.nebulas.wallet.android.module.me.model.ListSelectModel

/**
 * Created by Heinoc on 2018/4/7.
 */
class ListSelectAdapter(context: Context): BaseBindingAdapter<ListSelectModel, ItemListSelectBinding>(context) {
    override fun getLayoutResId(viewType: Int): Int {
        return R.layout.item_list_select

    }

    override fun onBindItem(binding: ItemListSelectBinding?, item: ListSelectModel) {
        binding?.item = item
    }
}