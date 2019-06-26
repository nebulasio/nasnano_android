package io.nebulas.wallet.android.module.setting.adapter

import android.content.Context
import io.nebulas.wallet.android.R
import io.nebulas.wallet.android.base.BaseBindingAdapter
import io.nebulas.wallet.android.databinding.ItemDefaultWalletLayoutBinding
import io.nebulas.wallet.android.module.setting.model.DefaultWalletListModel

/**
 * Created by Alina on 2018/6/22
 */
class DefaultWalletAdapter(context:Context):BaseBindingAdapter<DefaultWalletListModel,ItemDefaultWalletLayoutBinding>(context) {


    override fun getLayoutResId(viewType: Int): Int {
        return R.layout.item_default_wallet_layout
    }

    override fun onBindItem(binding: ItemDefaultWalletLayoutBinding?, item: DefaultWalletListModel) {
        binding?.item = item
    }


}