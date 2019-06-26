package io.nebulas.wallet.android.module.me

import android.arch.lifecycle.Observer
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.nebulas.wallet.android.R
import io.nebulas.wallet.android.base.BaseActivity
import io.nebulas.wallet.android.base.BaseFragment
import io.nebulas.wallet.android.common.DataCenter
import io.nebulas.wallet.android.module.detail.WalletDetailActivity
import io.nebulas.wallet.android.module.main.MainActivity
import io.nebulas.wallet.android.module.me.adapter.WalletAdapter
import io.nebulas.wallet.android.module.wallet.create.CreateWalletActivity
import io.nebulas.wallet.android.module.wallet.create.model.Wallet
import io.nebulas.wallet.android.module.wallet.manage.WalletBackupActivity
import io.nebulas.wallet.android.util.Util
import kotlinx.android.synthetic.nas_nano.fragment_wallet.*

/**
 * Created by Heinoc on 2018/5/10.
 */
class WalletFragment : BaseFragment(), WalletAdapter.WalletAdapterCallback {

    companion object {
        const val REQUEST_CODE_WALLET_DETAIL = 20001
        const val REQUEST_CODE_CREATE_WALLET = 20002
        const val REQUEST_CODE_BACKUP_WALLET = 20003
    }

    lateinit var adapter: WalletAdapter
    var balanceHidden: Boolean = false
    private var initialized = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_wallet, container, false)
    }

    override fun initView() {
        balanceHidden = Util.getBalanceHidden(context!!)
        walletRecyclerView.layoutManager = LinearLayoutManager(this.context)

        adapter = WalletAdapter(this, balanceHidden)
        walletRecyclerView.adapter = adapter

        loadData()

        emptyAddWalletBtn.setOnClickListener {
            onCreateWalletClicked()
        }

        (context as MainActivity).balanceViewModel().getLoading().observe(this, Observer {
            if (it == true) {

            } else {
                loadData()
            }
        })

        initialized = true
    }

    override fun onResume() {
        super.onResume()
        loadData()
    }

    override fun onCreateWalletClicked() {
        CreateWalletActivity.launch(context as BaseActivity, WalletFragment.REQUEST_CODE_CREATE_WALLET, true)
    }

    override fun onBackupClicked(wallet: Wallet) {
        WalletBackupActivity.launch((context as BaseActivity),
                WalletFragment.REQUEST_CODE_BACKUP_WALLET,
                getString(R.string.wallet_backup_mnemonic),
                wallet)
    }

    override fun onItemClicked(wallet: Wallet, position: Int) {
        WalletDetailActivity
                .launch(this@WalletFragment.context!!,
                        REQUEST_CODE_WALLET_DETAIL,
                        wallet)
    }

    fun refreshBalanceHidden(isHidden: Boolean) {
        balanceHidden = isHidden
        if (!initialized) {
            return
        }
        adapter.isBalanceHidden(isHidden)
    }

    private fun loadData() {
        adapter.setDataSource(DataCenter.wallets)
        if (adapter.itemCount == 0) {
            layoutEmpty.visibility = View.VISIBLE
        } else {
            layoutEmpty.visibility = View.GONE
        }
    }

}