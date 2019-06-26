package io.nebulas.wallet.android.module.transaction.view

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.view.Gravity
import android.view.View
import io.nebulas.wallet.android.R
import io.nebulas.wallet.android.base.BaseBindingAdapter
import io.nebulas.wallet.android.module.transaction.adapter.SelectWalletAdapter
import io.nebulas.wallet.android.module.wallet.create.model.Wallet
import io.nebulas.wallet.android.util.Util
import kotlinx.android.synthetic.nas_nano.dialog_select_wallet.*

/**
 * Created by Heinoc on 2018/3/23.
 */
class SelectWalletDialog(context: Context, var onWalletSelected:(index: Int) -> Unit): Dialog(context, R.style.AppDialog) {

    lateinit var adapter: SelectWalletAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.dialog_select_wallet)


        initView()

    }

    private fun initView() {
        selectWalletRecyclerView.layoutManager = LinearLayoutManager(context)

        closeIV.setOnClickListener {
            dismiss()

        }

    }


    fun show(wallets: MutableList<Wallet>) {
        super.show()

        // fix the width
        val lp = window.attributes
        window.setGravity(Gravity.BOTTOM)
        lp.width = Util.screenWidth(context)
        window.attributes = lp


        /**
         * 每次调用show()方法时，重新创建adapter，解决“选中”图标显示异常的bug
         */
        adapter = SelectWalletAdapter(context)
        selectWalletRecyclerView.adapter = adapter

        adapter.items.addAll(wallets)

        adapter.setOnClickListener(object : BaseBindingAdapter.OnItemClickListener{
            override fun onItemClick(view: View, position: Int) {
                adapter.items.forEach {
                    it.selected = false

                }

                onWalletSelected(position)

                dismiss()

            }

            override fun onItemLongClick(view: View, position: Int) {

            }
        })

    }

}