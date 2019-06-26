package io.nebulas.wallet.android.module.auth

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.view.ViewGroup
import android.widget.*
import io.nebulas.wallet.android.R
import io.nebulas.wallet.android.base.BaseActivity
import io.nebulas.wallet.android.common.DataCenter
import io.nebulas.wallet.android.invoke.InvokeHelper
import io.nebulas.wallet.android.module.wallet.create.model.Address
import io.nebulas.wallet.android.module.wallet.create.model.Wallet
import io.nebulas.wallet.android.util.getWalletColorCircleDrawable
import kotlinx.android.synthetic.nas_nano.activity_auth.*
import kotlinx.android.synthetic.nas_nano.app_bar_main_no_underline.*
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.find
import org.jetbrains.anko.uiThread
import walletcore.Walletcore

class AuthActivity : BaseActivity() {

    override fun initView() {}

    companion object {
        fun launch(context: Context, parameters: Bundle) {
            context.startActivity(Intent(context, AuthActivity::class.java).apply { putExtras(parameters) })
        }
    }

    data class WalletWrapper(val wallet: Wallet, val address: Address)

    private var appName: String? = null
    private var appIcon: ByteArray? = null
    private val adapter = WalletAdapter()
    private val wallets = mutableListOf<WalletWrapper>()
    private var selectedWalletWrapper: WalletWrapper? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_auth)
        handleIntent()
        initViews()
    }

    private fun initViews() {
        showBackBtn(true, toolbar)
        titleTV.text = "AUTH"
        tvAppName.text = appName
        btnConfirm.setOnClickListener {
            val item = selectedWalletWrapper
            if (item == null) {
                toastErrorMessage("请选择一个钱包")
                return@setOnClickListener
            }
            val bundle = Bundle().also {
                it.putString("address", item.address.address)
            }
            if (!InvokeHelper.goBack(this, bundle)) {
                finish()
            }
        }
        setupIconView()
        setupListView()
        loadWallets()
    }

    private fun handleIntent() {
        appName = intent.getStringExtra("appName")
        appIcon = intent.getByteArrayExtra("appIcon")
    }

    private fun setupIconView() {
        val bytes = appIcon ?: return
        doAsync {
            val bm = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            uiThread {
                ivAppIcon.setImageBitmap(bm)
            }
        }
    }

    private fun setupListView() {
        listView.adapter = adapter
        listView.setOnItemClickListener { _, _, position, _ ->
            val clickedItem = wallets[position]
            if (selectedWalletWrapper != clickedItem) {
                selectedWalletWrapper = clickedItem
                adapter.notifyDataSetChanged()
            }
        }
    }

    private fun loadWallets() {
        DataCenter.wallets.forEach { wallet ->
            DataCenter.addresses.forEach Address@{ address ->
                if (address.walletId == wallet.id && address.platform == Walletcore.NAS) {
                    wallets.add(WalletWrapper(wallet, address))
                    return@Address
                }
            }
        }
        adapter.notifyDataSetChanged()
    }

    inner class WalletAdapter : BaseAdapter() {

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val cell = if (convertView == null) {
                val v = layoutInflater.inflate(R.layout.item_wallet_list_for_auth, parent, false)
                v.setPadding(0, v.paddingTop, 0, v.paddingBottom)
                val holder = VH()
                holder.ivIcon = v.find(R.id.ivIcon)
                holder.tvWalletName = v.find(R.id.tvWalletName)
                holder.ivCheckedStatus = v.find(R.id.ivCheckedStatus)
                holder.tvAddress = v.find(R.id.tvAddress)
                holder.tvAddress.maxWidth = this@AuthActivity.getScreenWidth() / 2
                holder.tvAddress.ellipsize = TextUtils.TruncateAt.MIDDLE
                v.tag = holder
                v
            } else {
                convertView
            }
            val holder: VH = cell.tag as VH
            val wallet = getItem(position)
            holder.ivCheckedStatus.visibility = if (wallet == selectedWalletWrapper) View.VISIBLE else View.GONE
            holder.tvWalletName.text = wallet.wallet.walletName
            holder.tvAddress.text = wallet.address.address
            holder.ivIcon.setImageDrawable(getWalletColorCircleDrawable(this@AuthActivity, wallet.wallet.id, wallet.wallet.walletName[0]))
            return cell
        }

        override fun getItem(position: Int): WalletWrapper = wallets[position]

        override fun getItemId(position: Int): Long = getItem(position).wallet.id

        override fun getCount(): Int = wallets.size
    }

    class VH {
        lateinit var ivIcon: ImageView
        lateinit var tvWalletName: TextView
        lateinit var tvAddress: TextView
        lateinit var ivCheckedStatus: ImageView
    }
}
