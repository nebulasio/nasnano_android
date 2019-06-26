package io.nebulas.wallet.android.module.transaction

import android.content.Context
import io.nebulas.wallet.android.R
import io.nebulas.wallet.android.base.BaseBindingAdapter
import io.nebulas.wallet.android.databinding.ItemSelectTransferTargetListBinding
import io.nebulas.wallet.android.module.transaction.model.TransferTargetListModel

/**
 * Created by Heinoc on 2018/3/13.
 */
class TransferTargetListAdapter(context: Context) : BaseBindingAdapter<TransferTargetListModel, ItemSelectTransferTargetListBinding>(context){
    override fun getLayoutResId(viewType: Int): Int {
        return R.layout.item_select_transfer_target_list
    }

    override fun onBindItem(binding: ItemSelectTransferTargetListBinding?, item: TransferTargetListModel) {
        binding?.item = item
    }
}