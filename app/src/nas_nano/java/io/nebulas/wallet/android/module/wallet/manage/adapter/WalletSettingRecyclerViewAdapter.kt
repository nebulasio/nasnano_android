package io.nebulas.wallet.android.module.wallet.manage.adapter

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.view.View
import android.widget.TextView
import io.nebulas.wallet.android.R
import io.nebulas.wallet.android.base.BaseBindingAdapter
import io.nebulas.wallet.android.base.BaseBindingViewHolder
import io.nebulas.wallet.android.databinding.ItemWalletSettingRecyclerviewBinding
import io.nebulas.wallet.android.module.wallet.manage.model.WalletSettingListModel
import org.jetbrains.anko.find

/**
 * Created by Heinoc on 2018/3/21.
 */
class WalletSettingRecyclerViewAdapter(context: Context, var onDeleteBtnClick:() -> Unit): BaseBindingAdapter<WalletSettingListModel, ItemWalletSettingRecyclerviewBinding>(context) {


    class WalletSettingViewHolder(itemView: View): BaseBindingViewHolder(itemView){
        val delWalletBtn = itemView.find<TextView>(R.id.delWalletBtn)
    }


    override fun getViewHolder(itemView: View, viewType: Int): BaseBindingViewHolder {
        return WalletSettingViewHolder(itemView)
    }

    override fun getLayoutResId(viewType: Int): Int {
        return R.layout.item_wallet_setting_recyclerview

    }

    override fun onBindItem(binding: ItemWalletSettingRecyclerviewBinding?, item: WalletSettingListModel) {
        binding?.item = item

    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        super.onBindViewHolder(holder, position)

        (holder as WalletSettingViewHolder).delWalletBtn.setOnClickListener {
            onDeleteBtnClick()
        }

    }

}