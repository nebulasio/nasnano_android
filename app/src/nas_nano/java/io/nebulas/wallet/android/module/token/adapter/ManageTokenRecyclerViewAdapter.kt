package io.nebulas.wallet.android.module.token.adapter

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.view.View
import android.widget.CompoundButton
import android.widget.Switch
import io.nebulas.wallet.android.R
import io.nebulas.wallet.android.base.BaseBindingAdapter
import io.nebulas.wallet.android.base.BaseBindingViewHolder
import io.nebulas.wallet.android.common.DataCenter
import io.nebulas.wallet.android.databinding.ItemManageTokenBinding
import io.nebulas.wallet.android.db.AppDB
import io.nebulas.wallet.android.module.token.model.ManageTokenListModel
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.find

/**
 * Created by Heinoc on 2018/5/16.
 */
class ManageTokenRecyclerViewAdapter(context: Context): BaseBindingAdapter<ManageTokenListModel, ItemManageTokenBinding>(context) {

    class ManageTokenViewHolder(itemView: View) : BaseBindingViewHolder(itemView) {
        var selectSwitch = itemView.find<Switch>(R.id.selectSwitch)
    }

    override fun getViewHolder(itemView: View, viewType: Int): BaseBindingViewHolder {
        return ManageTokenViewHolder(itemView)
    }

    override fun getLayoutResId(viewType: Int): Int {
        return R.layout.item_manage_token

    }

    override fun onBindItem(binding: ItemManageTokenBinding?, item: ManageTokenListModel) {
        binding?.item = item
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        super.onBindViewHolder(holder, position)

        if (holder is ManageTokenViewHolder && null != items[position].coin) {
            holder.selectSwitch.setOnCheckedChangeListener(object : CompoundButton.OnCheckedChangeListener{
                override fun onCheckedChanged(p0: CompoundButton?, p1: Boolean) {
                    if (position >= items.size)
                        return

                    val item = items[position].coin!!
                    item.isShow = p1

                    DataCenter.coins.filter {
                        it.tokenId == item.tokenId
                    }.forEach {
                        if (it.isShow != p1) {
                            it.isShow = p1

                            //更新数据库
                            doAsync {
                                AppDB.getInstance(context).coinDao().updateCoin(it)
                            }
                        }
                    }

                    DataCenter.coinsGroupByCoinSymbol.first {
                        it.tokenId == item.tokenId
                    }.isShow = p1


                }
            })
        }

    }

}