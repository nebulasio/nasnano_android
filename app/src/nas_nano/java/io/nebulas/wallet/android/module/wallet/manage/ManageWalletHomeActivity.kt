package io.nebulas.wallet.android.module.wallet.manage

import android.content.Context
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.view.View
import io.nebulas.wallet.android.R
import io.nebulas.wallet.android.base.BaseActivity
import io.nebulas.wallet.android.base.BaseBindingAdapter
import io.nebulas.wallet.android.common.DataCenter
import io.nebulas.wallet.android.module.wallet.create.CreateWalletActivity
import io.nebulas.wallet.android.module.wallet.manage.adapter.ManageWalletHomeRecyclerViewAdapter
import io.nebulas.wallet.android.module.wallet.manage.model.ManageWalletHomeModel
import kotlinx.android.synthetic.nas_nano.activity_manage_wallet_home.*
import kotlinx.android.synthetic.nas_nano.app_bar_wallet_manage.*
import org.jetbrains.anko.startActivity
import org.jetbrains.annotations.NotNull

class ManageWalletHomeActivity : BaseActivity() {

    companion object {
        /**
         * 启动ManageWalletHomeActivity
         *
         * @param context
         */
        fun launch(@NotNull context: Context) {
            context.startActivity<ManageWalletHomeActivity>()
        }
    }

    lateinit var adapter: ManageWalletHomeRecyclerViewAdapter

    var modifiedItemIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manage_wallet_home)
    }

    override fun initView() {
        showBackBtn(false, toolbar)
        desTV.setText(R.string.wallet_manage_des)
        subDesTV.setText(R.string.wallet_manage_sub_des)


        manageWalletHomeRecyclerView.layoutManager = LinearLayoutManager(this)

        adapter = ManageWalletHomeRecyclerViewAdapter(this)
        manageWalletHomeRecyclerView.adapter = adapter

        createWalletTV.setOnClickListener {
            gotoCreateWallet()
        }

        adapter.setOnClickListener(object : BaseBindingAdapter.OnItemClickListener {
            override fun onItemClick(view: View, position: Int) {
                val item = adapter.items[position]

                if (item.wallet != null) {
                    WalletSettingActivity.launch(this@ManageWalletHomeActivity, wallet = item.wallet!!)
                }

            }

            override fun onItemLongClick(view: View, position: Int) {

            }
        })


    }

    override fun onResume() {
        super.onResume()

        if (DataCenter.wallets.isNotEmpty()) {
            emptyLayout.visibility = View.GONE
            manageWalletHomeRecyclerView.visibility = View.VISIBLE
            initWalletItems()
        } else {
            emptyLayout.visibility = View.VISIBLE
            manageWalletHomeRecyclerView.visibility = View.GONE

        }

    }

    private fun gotoCreateWallet() {
        CreateWalletActivity.launch(this, showBackBtn = true)
    }

    fun initWalletItems() {

        var list = mutableListOf<ManageWalletHomeModel>()

        list.add(ManageWalletHomeModel(getString(R.string.select_need_manage_wallet)))

        DataCenter.wallets.forEach {
            list.add(ManageWalletHomeModel(wallet = it))
        }

        list.add(ManageWalletHomeModel(hasFooter = true))

        adapter.items.clear()
        adapter.items.addAll(list)

    }


}
