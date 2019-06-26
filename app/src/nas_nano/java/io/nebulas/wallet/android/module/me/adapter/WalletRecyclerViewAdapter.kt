package io.nebulas.wallet.android.module.me.adapter

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import io.nebulas.wallet.android.R
import io.nebulas.wallet.android.base.BaseActivity
import io.nebulas.wallet.android.base.BaseBindingAdapter
import io.nebulas.wallet.android.base.BaseBindingViewHolder
import io.nebulas.wallet.android.common.DataCenter
import io.nebulas.wallet.android.databinding.ItemMeWalletBinding
import io.nebulas.wallet.android.module.me.WalletFragment
import io.nebulas.wallet.android.module.me.model.MeWalletListModel
import io.nebulas.wallet.android.module.wallet.create.CreateWalletActivity
import io.nebulas.wallet.android.module.wallet.manage.WalletBackupActivity
import org.jetbrains.anko.find

/**
 * Created by Heinoc on 2018/5/14.
 */
class WalletRecyclerViewAdapter(context: Context) : BaseBindingAdapter<MeWalletListModel, ItemMeWalletBinding>(context) {

    class WalletViewHolder(itemView: View) : BaseBindingViewHolder(itemView) {

        val emptyAddWalletBtn = itemView.find<TextView>(R.id.emptyAddWalletBtn)
        val noticeLayout = itemView.find<LinearLayout>(R.id.noticeLayout)
        val addWalletBtn = itemView.find<TextView>(R.id.addWalletBtn)

    }

    override fun getViewHolder(itemView: View, viewType: Int): BaseBindingViewHolder {
        return WalletViewHolder(itemView)
    }

    override fun getLayoutResId(viewType: Int): Int {
        return R.layout.item_me_wallet
    }

    override fun onBindItem(binding: ItemMeWalletBinding?, item: MeWalletListModel) {
        binding?.item = item
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        super.onBindViewHolder(holder, position)

        if (holder is WalletViewHolder) {

            holder.emptyAddWalletBtn.setOnClickListener {
                CreateWalletActivity.launch(context as BaseActivity, WalletFragment.REQUEST_CODE_CREATE_WALLET, true)
            }

            holder.noticeLayout.setOnClickListener {
                if (null != items[position].wallet) {
                    WalletBackupActivity.launch((context as BaseActivity),
                            WalletFragment.REQUEST_CODE_BACKUP_WALLET,
                            context.getString(R.string.wallet_backup_mnemonic),
                            items[position].wallet!!)
                }
            }

            holder.addWalletBtn.setOnClickListener {
                CreateWalletActivity.launch(context as BaseActivity, WalletFragment.REQUEST_CODE_CREATE_WALLET, true)
            }

        }

    }

}