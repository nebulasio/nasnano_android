package io.nebulas.wallet.android.module.wallet.manage.adapter

import android.content.Context
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.RecyclerView
import android.view.View
import android.widget.Button
import io.nebulas.wallet.android.R
import io.nebulas.wallet.android.base.BaseBindingAdapter
import io.nebulas.wallet.android.base.BaseBindingViewHolder
import io.nebulas.wallet.android.databinding.ItemManageWalletHomeRecyclerviewBinding
import io.nebulas.wallet.android.module.wallet.create.CreateWalletActivity
import io.nebulas.wallet.android.module.wallet.manage.model.ManageWalletHomeModel
import org.jetbrains.anko.find

/**
 * Created by Heinoc on 2018/3/21.
 */
class ManageWalletHomeRecyclerViewAdapter(context: Context) : BaseBindingAdapter<ManageWalletHomeModel, ItemManageWalletHomeRecyclerviewBinding>(context) {


    class ManageWalletHomeViewHolder(itemView: View): BaseBindingViewHolder(itemView){
        val addWalletBtn = itemView.find<Button>(R.id.addWalletBtn)
    }


    override fun getViewHolder(itemView: View, viewType: Int): BaseBindingViewHolder {
        return ManageWalletHomeViewHolder(itemView)
    }

    override fun getLayoutResId(viewType: Int): Int {
        return R.layout.item_manage_wallet_home_recyclerview
    }

    override fun onBindItem(binding: ItemManageWalletHomeRecyclerviewBinding?, item: ManageWalletHomeModel) {
        binding?.item = item
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        super.onBindViewHolder(holder, position)

        (holder as ManageWalletHomeViewHolder).addWalletBtn.setOnClickListener {
            CreateWalletActivity.launch(context as AppCompatActivity, showBackBtn = true)
        }

    }
}