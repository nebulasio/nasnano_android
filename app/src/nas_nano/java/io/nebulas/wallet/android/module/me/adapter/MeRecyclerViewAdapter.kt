package io.nebulas.wallet.android.module.me.adapter

import android.content.Context
import io.nebulas.wallet.android.R
import io.nebulas.wallet.android.base.BaseBindingAdapter
import io.nebulas.wallet.android.databinding.ItemMeRecyclerviewBinding
import io.nebulas.wallet.android.module.me.model.MeListModel

/**
 * Created by Heinoc on 2018/3/21.
 */
class MeRecyclerViewAdapter(context: Context): BaseBindingAdapter<MeListModel, ItemMeRecyclerviewBinding>(context){
    override fun getLayoutResId(viewType: Int): Int {
        return R.layout.item_me_recyclerview
    }

    override fun onBindItem(binding: ItemMeRecyclerviewBinding?, item: MeListModel) {
        binding?.item = item
    }
}