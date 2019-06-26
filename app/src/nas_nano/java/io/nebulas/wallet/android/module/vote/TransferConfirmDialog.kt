package io.nebulas.wallet.android.module.vote

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.support.v4.view.ViewPager
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import io.nebulas.wallet.android.R
import io.nebulas.wallet.android.common.DataCenter
import io.nebulas.wallet.android.module.balance.model.Coin
import io.nebulas.wallet.android.module.transaction.adapter.WalletViewPagerAdapter
import io.nebulas.wallet.android.module.wallet.create.model.Wallet
import io.nebulas.wallet.android.util.Blockies
import io.nebulas.wallet.android.util.Util
import kotlinx.android.synthetic.main.dialog_transfer_confirm.*
import org.jetbrains.anko.dip
import org.jetbrains.anko.find
import walletcore.Walletcore
import java.math.BigDecimal

class TransferConfirmDialog(context: Context,
                            val title:String,
                            val targetAddress:String,
                            val amount:String,
                            val amountSymbol:String,
                            val amountTokenId:String,
                            val tokenDecimals:Int=18,
                            val gasFee:String,
                            val gasSymbol:String="NAS",
                            val gasCoinPlatform:String=Walletcore.NAS,
                            val onConfirmListener: OnConfirmListener?=null): Dialog(context, R.style.AppDialog) {

    interface OnConfirmListener{
        fun onConfirmed(wallet: Wallet)
    }

    private var selectedWallet:Wallet?=null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dialog_transfer_confirm)
        initViews()
    }

    private fun initViews(){
        tvTitle.text = title
        transferToTV.text = targetAddress
        addressIconIV.setImageBitmap(Blockies.createIcon(address = targetAddress, circleImage = true))
        amountTV.text = "$amount $amountSymbol"
        gasFeeTV.text = "$gasFee NAS"
        closeIV.setOnClickListener {
            cancel()
        }

        loadWalletViews()

        walletViewPagerAdapter = WalletViewPagerAdapter(walletViews)
        walletViewPager.adapter = walletViewPagerAdapter
        walletViewPager.pageMargin = context.dip(18)
        walletViewPager.offscreenPageLimit = 5
        indicatorView.count = walletViews.size
//
        walletViewPager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrollStateChanged(state: Int) {

            }

            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
            }

            override fun onPageSelected(position: Int) {
                indicatorView.setFocusIndex(position)

                setSelectedWallet(position)
//                curWallet = position
//                setWallet(curWallet)
            }
        })
    }

    override fun show() {
        super.show()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {//5.0 全透明实现
            window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_LAYOUT_STABLE)
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.statusBarColor = Color.TRANSPARENT
        } else {//4.4 全透明状态栏
            window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        }

        val attr = window.attributes
        attr.gravity = Gravity.BOTTOM
        attr.width = WindowManager.LayoutParams.MATCH_PARENT
        attr.height = WindowManager.LayoutParams.MATCH_PARENT
        window.decorView.setPadding(0, 0, 0, 0)
        window.attributes = attr
    }


    private var walletViewPagerAdapter: WalletViewPagerAdapter? = null
    private var walletViews = arrayListOf<View>()
    private val transferTokenMap = mutableMapOf<Long, Coin>()
    private val transferGasMap = mutableMapOf<Long, Coin>()
    private var wallets = mutableListOf<Wallet>()
    private var walletColors = arrayListOf(
            R.color.color_038AFB,
            R.color.color_8350F6,
            R.color.color_FF8F00,
            R.color.color_00CB91,
            R.color.color_FF516A
    )

    private fun loadWalletViews() {
        walletViews.clear()
        DataCenter.coins.filter {
            it.tokenId.equals(amountTokenId, true)
        }.forEach {
            transferTokenMap[it.walletId] = it
        }
//
        DataCenter.coins.filter {
            // type=1是核心币（主链币）
            it.platform == gasCoinPlatform && it.type == 1
        }.forEach {
            transferGasMap[it.walletId] = it
        }

        wallets.addAll(DataCenter.wallets.filter {
            transferTokenMap.containsKey(it.id)
        })

        val defaultWalletID = Util.getDefaultPaymentWallet(context)
        if (wallets.any { it.id == defaultWalletID }) {
            var defaultWallet: Wallet? = null
            run breakPoint@{
                wallets.forEach {
                    if (it.id == defaultWalletID) {
                        defaultWallet = it
                        return@breakPoint
                    }
                }
            }
            if (null != defaultWallet) {
                wallets.remove(defaultWallet!!)
                wallets.add(0, defaultWallet!!)
            }
        }
//
//
        wallets.forEach {
            val view = layoutInflater.inflate(R.layout.item_dialog_confirm_transfer_wallet, walletViewPager, false)

            if (walletViews.isEmpty()) {
                view.setBackgroundResource(R.drawable.shape_round_corner_ffffff_038afb)
                view.find<ImageView>(R.id.selectedWalletIV).visibility = View.VISIBLE
            } else {
                view.setBackgroundResource(R.drawable.shape_round_corner_ffffff_c5c5c5)
                view.find<ImageView>(R.id.selectedWalletIV).visibility = View.GONE
            }

            view.find<TextView>(R.id.default_tv).visibility = if (it.id == defaultWalletID) {
                View.VISIBLE
            } else {
                View.GONE
            }

            view.find<TextView>(R.id.wallet_name_tv).setTextColor(context.resources.getColor(walletColors[it.id.toInt() % 5]))
            view.find<TextView>(R.id.default_tv).setTextColor(context.resources.getColor(walletColors[it.id.toInt() % 5]))

            view.find<TextView>(R.id.wallet_name_tv).text = it.walletName
            if (transferTokenMap[it.id]?.tokenId != transferGasMap[it.id]?.tokenId) {
                view.find<TextView>(R.id.transferTokenSymbolTV).text = amountSymbol
                view.find<TextView>(R.id.transferTokenBalanceTV).text = transferTokenMap[it.id]?.balanceString
                view.find<TextView>(R.id.transferGasSymbolTV).text = "${transferGasMap[it.id]?.symbol}"
                view.find<TextView>(R.id.transferGasBalanceTV).text = transferGasMap[it.id]?.balanceString


            } else {
                view.find<TextView>(R.id.transferGasSymbolTV).text = amountSymbol
                view.find<TextView>(R.id.transferGasBalanceTV).text = transferTokenMap[it.id]?.balanceString

            }

            view.setOnClickListener {
                walletViewPager.setCurrentItem(walletViews.indexOf(view), true)
            }


            walletViews.add(view)
        }

        if (walletViews.isNotEmpty())
            setSelectedWallet(0)

    }

    private fun setSelectedWallet(index: Int) {
        selectedWallet = wallets[index]
        walletViews.forEachIndexed { inx, it ->
            it.setBackgroundResource(R.drawable.shape_round_corner_ffffff_c5c5c5)
            it.find<ImageView>(R.id.selectedWalletIV).visibility = View.GONE
            it.isClickable = inx!=index
            it.isEnabled = inx!=index
        }
        walletViews[index].setBackgroundResource(R.drawable.shape_round_corner_ffffff_038afb)
        walletViews[index].find<ImageView>(R.id.selectedWalletIV).visibility = View.VISIBLE
        val wallet = selectedWallet?:return
        if (transferTokenMap[wallet.id]?.tokenId != transferGasMap[wallet.id]?.tokenId) {
            //余额 ＞= 转账金额 && 主链余额 ＞ 当前转账gas消耗
            if (BigDecimal(transferTokenMap[wallet.id]?.balance) >= BigDecimal(amount) && BigDecimal(transferGasMap[wallet.id]?.balance) >= BigDecimal(gasFee).multiply(BigDecimal(10).pow(tokenDecimals))) {
                confirmTransferBtn.setText(R.string.confirm_transfer)
                confirmTransferBtn.setBackgroundResource(R.drawable.custom_corner_button_bg)
                confirmTransferBtn.setOnClickListener{
                    onConfirmListener?.onConfirmed(wallet)
                    dismiss()
                }
            } else {
                if (BigDecimal(transferTokenMap[wallet.id]?.balance) < BigDecimal(amount))
                    confirmTransferBtn.setText(R.string.insufficient_balance)
                else
                    confirmTransferBtn.setText(R.string.insufficient_gas)
                confirmTransferBtn.setBackgroundResource(R.drawable.custom_button_ff516a_bg)
                confirmTransferBtn.setOnClickListener(null)
            }

        } else {
            //余额 ＞= 转账金额 + 当前转账gas消耗
            if (BigDecimal(transferTokenMap[wallet.id]?.balance) >= (BigDecimal(amount) + BigDecimal(gasFee).multiply(BigDecimal(10).pow(tokenDecimals)))) {
                confirmTransferBtn.setText(R.string.confirm_transfer)
                confirmTransferBtn.setBackgroundResource(R.drawable.custom_corner_button_bg)
                confirmTransferBtn.setOnClickListener{
                    onConfirmListener?.onConfirmed(wallet)
                    dismiss()
                }
            } else {
                confirmTransferBtn.setText(R.string.insufficient_balance)
                confirmTransferBtn.setBackgroundResource(R.drawable.custom_button_ff516a_bg)
                confirmTransferBtn.setOnClickListener(null)
            }

        }
    }

}