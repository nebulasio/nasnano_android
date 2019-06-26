package io.nebulas.wallet.android.module.me.adapter

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.view.View
import android.widget.TextView
import io.nebulas.wallet.android.R
import io.nebulas.wallet.android.base.BaseActivity
import io.nebulas.wallet.android.base.BaseBindingAdapter
import io.nebulas.wallet.android.base.BaseBindingViewHolder
import io.nebulas.wallet.android.databinding.ItemMeTokenBinding
import io.nebulas.wallet.android.extensions.logD
import io.nebulas.wallet.android.module.me.WalletFragment
import io.nebulas.wallet.android.module.me.model.MeTokenListModel
import io.nebulas.wallet.android.module.token.ManageTokenActivity
import io.nebulas.wallet.android.module.wallet.create.CreateWalletActivity
import org.jetbrains.anko.find

/**
 * Created by Heinoc on 2018/5/14.
 */
class TokenRecyclerViewAdapter(context: Context): BaseBindingAdapter<MeTokenListModel, ItemMeTokenBinding>(context) {

    class TokenViewHolder(itemView: View) : BaseBindingViewHolder(itemView) {

        val manageAssetsBtn = itemView.find<TextView>(R.id.manageAssetsBtn)
        val emptyAddWalletBtn = itemView.find<TextView>(R.id.emptyAddWalletBtn)

    }

    override fun getViewHolder(itemView: View, viewType: Int): BaseBindingViewHolder {
        return TokenViewHolder(itemView)
    }

    override fun getLayoutResId(viewType: Int): Int {
        return R.layout.item_me_token
    }

    override fun onBindItem(binding: ItemMeTokenBinding?, item: MeTokenListModel) {
        binding?.item = item
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        super.onBindViewHolder(holder, position)

        if (holder is TokenViewHolder) {
            holder.manageAssetsBtn.setOnClickListener {
                ManageTokenActivity.launch(context)
            }

            holder.emptyAddWalletBtn.setOnClickListener {
                CreateWalletActivity.launch(context as BaseActivity, WalletFragment.REQUEST_CODE_CREATE_WALLET, true)
            }
        }

    }

}
