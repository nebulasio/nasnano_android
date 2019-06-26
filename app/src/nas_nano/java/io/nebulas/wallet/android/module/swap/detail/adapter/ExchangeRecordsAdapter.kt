package io.nebulas.wallet.android.module.swap.detail.adapter

import android.content.Context
import io.nebulas.wallet.android.R
import io.nebulas.wallet.android.base.BaseBindingAdapter
import io.nebulas.wallet.android.databinding.ItemExchangeRecordBinding
import io.nebulas.wallet.android.module.swap.model.ExchangeRecordModel

class ExchangeRecordsAdapter(context: Context) : BaseBindingAdapter<ExchangeRecordModel, ItemExchangeRecordBinding>(context) {
    override fun getLayoutResId(viewType: Int): Int {
        return R.layout.item_exchange_record
    }

    override fun onBindItem(binding: ItemExchangeRecordBinding?, item: ExchangeRecordModel) {
        binding?.item = item
    }
}