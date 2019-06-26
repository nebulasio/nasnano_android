package io.nebulas.wallet.android.module.swap.step.step1

import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.nebulas.wallet.android.R
import io.nebulas.wallet.android.base.BaseActivity
import io.nebulas.wallet.android.common.Constants
import io.nebulas.wallet.android.common.DataCenter
import io.nebulas.wallet.android.dialog.NasBottomDialog
import io.nebulas.wallet.android.module.wallet.create.CreateWalletActivity
import io.nebulas.wallet.android.module.wallet.create.model.Address
import io.nebulas.wallet.android.module.wallet.create.model.Wallet
import io.nebulas.wallet.android.module.wallet.manage.MnemonicBackupCheckActivity
import io.nebulas.wallet.android.module.wallet.manage.WalletBackupActivity
import kotlinx.android.synthetic.nas_nano.fragment_swap_choose_wallet.*
import kotlinx.android.synthetic.nas_nano.item_swap_choose_nas_wallet.view.*

class SwapChooseWalletFragment : Fragment() {

    companion object {
        @JvmStatic
        fun newInstance() =
                SwapChooseWalletFragment().apply {
                    arguments = Bundle().apply {
                    }
                }
    }

    interface OnWalletSelectedCallback {
        fun onWalletSelected(wallet: Wallet)
    }

    private var callback: OnWalletSelectedCallback? = null
    private val adapter = WalletAdapter()
    private val dataSource = mutableListOf<Wallet>()
    private var selectedWallet: Wallet? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnWalletSelectedCallback) {
            callback = context
        } else {
            throw RuntimeException(context.toString() + " must implement OnWalletSelectedCallback")
        }
    }

    override fun onDetach() {
        super.onDetach()
        callback = null
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_swap_choose_wallet, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        recycler_view.adapter = adapter
        recycler_view.layoutManager = LinearLayoutManager(requireContext())
        tv_choose_complete.setOnClickListener {
            val finalWallet = selectedWallet ?: return@setOnClickListener
            NasBottomDialog.Builder(requireContext())
                    .withTitle(getString(R.string.swap_title_important_tip))
                    .withContent(getString(R.string.swap_text_can_not_be_changed_warning))
                    .withIcon(R.drawable.icon_notice)
                    .withCancelButton(getString(R.string.swap_action_cancel))
                    .withConfirmButton(getString(R.string.swap_action_confirm)) { _, dialog ->
                        dialog.dismiss()
                        callback?.onWalletSelected(finalWallet)
                    }.build().show()
        }
        tv_add_wallet.setOnClickListener {
            CreateWalletActivity.launch(
                    context = requireContext(),
                    showBackBtn = true
            )
        }
    }

    override fun onResume() {
        super.onResume()
        if (hasWallet()) {
            tv_tip.visibility = View.VISIBLE
            recycler_view.visibility = View.VISIBLE
            tv_choose_complete.visibility = View.VISIBLE
            layout_empty.visibility = View.GONE
            tv_add_wallet.visibility = View.GONE
        } else {
            tv_tip.visibility = View.GONE
            recycler_view.visibility = View.GONE
            tv_choose_complete.visibility = View.GONE
            layout_empty.visibility = View.VISIBLE
            tv_add_wallet.visibility = View.VISIBLE
        }
        dataSource.clear()
        dataSource.addAll(DataCenter.wallets)
        adapter.notifyDataSetChanged()
    }

    private fun hasWallet(): Boolean = DataCenter.wallets.isNotEmpty()

    inner class WalletAdapter : RecyclerView.Adapter<VH>(), View.OnClickListener {
        override fun onClick(v: View?) {
            v ?: return
            val position: Int = v.tag as Int
            val wallet = dataSource[position]
            when (v.id) {
                R.id.layout_wallet_info -> {
                    selectedWallet = if (selectedWallet == wallet) {
                        null
                    } else {
                        wallet
                    }
                    tv_choose_complete.isEnabled = selectedWallet != null
                    notifyDataSetChanged()
                }
                R.id.layout_backup_tip -> {
                    (activity as BaseActivity).firebaseAnalytics?.logEvent(Constants.Exchange_OldBackup_Click, Bundle())
                    DataCenter.setData(MnemonicBackupCheckActivity.CLICK_BACKUP_FROM, Constants.Exchange_OldBackup_Success)
                    WalletBackupActivity.launch(requireContext(), 10000, resources.getString(R.string.wallet_backup_mnemonic), wallet)
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val view = layoutInflater.inflate(R.layout.item_swap_choose_nas_wallet, parent, false)
            return VH(view).apply {
                layout_wallet_info.setOnClickListener(this@WalletAdapter)
                layout_backup_tip.setOnClickListener(this@WalletAdapter)
            }
        }

        override fun getItemCount(): Int = dataSource.size

        override fun onBindViewHolder(holder: VH, position: Int) {
            val wallet = dataSource[position]
            val address = getWalletAddress(wallet)
            if (wallet.isNeedBackup()) {
                holder.layout_backup_tip.visibility = View.VISIBLE
                holder.iv_selected_status.visibility = View.GONE
                holder.tv_wallet_name.setTextColor(resources.getColor(R.color.color_CCCCCC))
                holder.tv_wallet_address.setTextColor(resources.getColor(R.color.color_CCCCCC))
                holder.layout_wallet_info.isClickable = false
            } else {
                holder.layout_backup_tip.visibility = View.GONE
                holder.iv_selected_status.visibility = View.VISIBLE
                holder.tv_wallet_name.setTextColor(resources.getColor(R.color.color_202020))
                holder.tv_wallet_address.setTextColor(resources.getColor(R.color.color_202020))
                holder.layout_wallet_info.isClickable = true
            }
            holder.layout_backup_tip.tag = position
            holder.layout_wallet_info.tag = position
            holder.tv_wallet_name.text = wallet.walletName
            holder.tv_wallet_address.text = address?.address
            if (selectedWallet == wallet) {
                holder.iv_selected_status.setImageResource(R.drawable.ic_radio_selected)
                holder.layout_wallet_info.background = resources.getDrawable(R.drawable.shape_round_corner_8dp_ffffff_038afb)
            } else {
                holder.iv_selected_status.setImageResource(R.drawable.ic_radio_unselected)
                holder.layout_wallet_info.background = resources.getDrawable(R.drawable.shape_round_corner_8dp_ffffff)
            }
        }

        private fun getWalletAddress(wallet: Wallet): Address? {
            val wId = wallet.id
            var address: Address? = null
            for (it in DataCenter.addresses) {
                if (it.platform == "nebulas" && it.walletId == wId) {
                    address = it
                }
            }
            return address
        }
    }

    class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tv_wallet_name = itemView.tv_wallet_name
        val tv_wallet_address = itemView.tv_wallet_address
        val layout_backup_tip = itemView.layout_backup_tip
        val layout_wallet_info = itemView.layout_wallet_info
        val iv_selected_status = itemView.iv_selected_status
    }
}
