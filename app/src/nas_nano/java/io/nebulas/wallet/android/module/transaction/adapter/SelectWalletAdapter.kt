package io.nebulas.wallet.android.module.transaction.adapter

import android.content.Context
import io.nebulas.wallet.android.R
import io.nebulas.wallet.android.base.BaseBindingAdapter
import io.nebulas.wallet.android.databinding.ItemSelectWalletRecyclerviewBinding
import io.nebulas.wallet.android.module.wallet.create.model.Wallet

/**
 * Created by Heinoc on 2018/3/23.
 */
class SelectWalletAdapter(context: Context): BaseBindingAdapter<Wallet, ItemSelectWalletRecyclerviewBinding>(context) {
    override fun getLayoutResId(viewType: Int): Int {
        return R.layout.item_select_wallet_recyclerview

    }

    override fun onBindItem(binding: ItemSelectWalletRecyclerviewBinding?, item: Wallet) {
        binding?.item = item
    }

}