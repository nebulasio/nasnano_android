package io.nebulas.wallet.android.module.detail.adapter

import android.content.Context
import io.nebulas.wallet.android.R
import io.nebulas.wallet.android.base.BaseBindingAdapter
import io.nebulas.wallet.android.databinding.ItemBalanceDetailListBinding
import io.nebulas.wallet.android.module.detail.model.BalanceDetailModel

/**
 * Created by Heinoc on 2018/2/28.
 */
class BalanceDetailListAdapter(context: Context) : BaseBindingAdapter<BalanceDetailModel, ItemBalanceDetailListBinding>(context) {
    override fun getLayoutResId(viewType: Int): Int {
        return R.layout.item_balance_detail_list
    }

    override fun onBindItem(binding: ItemBalanceDetailListBinding?, item: BalanceDetailModel) {
        binding?.item = item
    }
}