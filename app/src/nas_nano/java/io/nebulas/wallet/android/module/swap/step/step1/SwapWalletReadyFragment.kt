package io.nebulas.wallet.android.module.swap.step.step1

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.nebulas.wallet.android.R
import io.nebulas.wallet.android.common.DataCenter
import io.nebulas.wallet.android.module.swap.SwapHelper
import io.nebulas.wallet.android.module.wallet.create.model.Address
import io.nebulas.wallet.android.module.wallet.create.model.Wallet
import io.nebulas.wallet.android.module.wallet.manage.WalletBackupActivity
import kotlinx.android.synthetic.nas_nano.fragment_swap_wallet_ready.view.*
import walletcore.Walletcore

class SwapWalletReadyFragment : Fragment() {

    companion object {
        private const val REQUEST_CODE_BACKUP = 10001
        private const val ARG_SWAP_WALLET_INFO = "arg_swap_wallet_info"

        @JvmStatic
        fun newInstance(swapWalletInfo: SwapHelper.SwapWalletInfo) =
                SwapWalletReadyFragment().apply {
                    arguments = Bundle().apply {
                        putSerializable(ARG_SWAP_WALLET_INFO, swapWalletInfo)
                    }
                }
    }

    interface OnNextListener {
        fun onNextClicked()
    }

    private lateinit var swapWalletInfo: SwapHelper.SwapWalletInfo
    private var listener: OnNextListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            swapWalletInfo = it.getSerializable(ARG_SWAP_WALLET_INFO) as SwapHelper.SwapWalletInfo
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_swap_wallet_ready, container, false)
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_BACKUP && resultCode == Activity.RESULT_OK) {
            SwapHelper.swapWalletBackupSuccess(requireContext())
            swapWalletInfo = SwapHelper.getSwapWalletInfo(requireContext())!!
            initViews()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews()
    }

    private fun initViews(){
        val view = view?:return
        if (swapWalletInfo.swapWalletWords.isNotEmpty()) {
            view.layout_backup_tip.visibility = View.VISIBLE
            view.layout_operations.visibility = View.GONE
            view.tv_backup_now.setOnClickListener {
                val wallet = Wallet("Swap-Wallet")
                wallet.id = -10001
                wallet.setMnemonic(swapWalletInfo.swapWalletWords)
                wallet.isComplexPwd = swapWalletInfo.isComplexPassword
                val address = Address(swapWalletInfo.swapWalletAddress, swapWalletInfo.swapWalletKeystore, Walletcore.ETH)
                DataCenter.swapAddress = address
                WalletBackupActivity.launch(this, 10001, resources.getString(R.string.wallet_backup_mnemonic), wallet)
            }
        } else {
            view.layout_backup_tip.visibility = View.GONE
            view.layout_operations.visibility = View.VISIBLE
            view.tv_backup_again.setOnClickListener {
                BackupAgainActivity.launch(requireContext(), swapWalletInfo)
            }
            view.iv_next.setOnClickListener {
                listener?.onNextClicked()
            }
        }
        view.tv_nas_wallet_name.text = getNasWalletName(swapWalletInfo.nasWalletAddress)
        view.tv_swap_address.text = swapWalletInfo.swapWalletAddress
    }

    private fun getNasWalletName(address: String): String {
        var walletId: Long? = null
        for (it in DataCenter.addresses) {
            if (it.address == address) {
                walletId = it.walletId
            }
        }
        walletId ?: return ""
        for (it in DataCenter.wallets) {
            if (it.id == walletId) {
                return it.walletName
            }
        }
        return ""
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnNextListener) {
            listener = context
        } else {
            throw RuntimeException(context.toString() + " must implement OnFragmentInteractionListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }
}
