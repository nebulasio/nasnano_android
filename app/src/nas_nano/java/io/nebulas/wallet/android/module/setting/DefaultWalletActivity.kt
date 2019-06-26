package io.nebulas.wallet.android.module.setting

import android.content.Context
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.view.View
import io.nebulas.wallet.android.R
import io.nebulas.wallet.android.base.BaseActivity
import io.nebulas.wallet.android.base.BaseBindingAdapter
import io.nebulas.wallet.android.common.DataCenter
import io.nebulas.wallet.android.module.me.WalletFragment
import io.nebulas.wallet.android.module.setting.adapter.DefaultWalletAdapter
import io.nebulas.wallet.android.module.setting.model.DefaultWalletListModel
import io.nebulas.wallet.android.module.wallet.create.CreateWalletActivity
import io.nebulas.wallet.android.util.Util
import kotlinx.android.synthetic.nas_nano.activity_default_wallet.*
import kotlinx.android.synthetic.nas_nano.app_bar_main_no_underline.*
import org.jetbrains.anko.startActivityForResult

/**
 * Created by Alina on 2018/6/22
 */
class DefaultWalletActivity : BaseActivity() {
    companion object {
        /**
         * activity title
         */
        const val TITLE = "title"

        fun launch(context: Context, requestCode: Int, title: Int) {
            (context as AppCompatActivity).startActivityForResult<DefaultWalletActivity>(requestCode, TITLE to title)
        }
    }

    var defaultWalletId: Long? = null
    var adapter: DefaultWalletAdapter? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_default_wallet)
        initView()
    }

    override fun initView() {

        showBackBtn(true, toolbar)
        titleTV.setText(intent.getIntExtra(TITLE, R.string.payment_wallet))
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = DefaultWalletAdapter(this)
        recyclerView.adapter = adapter
        adapter?.setOnClickListener(object : BaseBindingAdapter.OnItemClickListener {
            override fun onItemClick(view: View, position: Int) {
                run breakpoint@{
                    adapter!!.items!!.forEach {
                        if (defaultWalletId == it.wallet.id) {
                            it.selected = false
                            return@breakpoint
                        }
                    }
                }
                val selectWallet = adapter!!.items[position]
                selectWallet.selected = true
                adapter!!.notifyDataSetChanged()
                setDefaultWallet(selectWallet.wallet.id)
            }

            override fun onItemLongClick(view: View, position: Int) {
            }
        })
        emptyAddWalletBtn.setOnClickListener {
            CreateWalletActivity.launch(this, WalletFragment.REQUEST_CODE_CREATE_WALLET, true)
        }
    }

    override fun onResume() {
        super.onResume()
        loadData()
    }

    /**
     * 设置默认钱包
     */
    fun setDefaultWallet(id: Long) {
        defaultWalletId = id
        when (titleTV.text.toString()) {
            getString(R.string.payment_wallet) -> {
                Util.setDefaultPaymentWallet(this, id)
            }
            getString(R.string.receiving_wallet) -> {
                Util.setDefaultReceivingWallet(this, id)
            }
        }
    }

    private fun loadData() {
        if (DataCenter.wallets.size == 0) {
            layoutEmpty.visibility = View.VISIBLE
            return
        } else {
            layoutEmpty.visibility = View.GONE
        }
        val list = mutableListOf<DefaultWalletListModel>()
        when (titleTV.text.toString()) {
            getString(R.string.payment_wallet) -> {
                defaultWalletId = Util.getDefaultPaymentWallet(this)
            }
            getString(R.string.receiving_wallet) -> {
                defaultWalletId = Util.getDefaultReceivingWallet(this)
            }
        }
        DataCenter.wallets.forEach {
            if (it.id == defaultWalletId) {
                list.add(DefaultWalletListModel(wallet = it, selected = true))
            } else {
                list.add(DefaultWalletListModel(wallet = it, selected = false))
            }
        }
        adapter?.changeDataSource(list)
    }

}