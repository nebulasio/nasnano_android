package io.nebulas.wallet.android.module.transaction.view

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.support.v4.view.ViewPager
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import android.widget.TextView
import io.nebulas.wallet.android.R
import io.nebulas.wallet.android.common.Constants
import io.nebulas.wallet.android.common.DataCenter
import io.nebulas.wallet.android.module.balance.model.Coin
import io.nebulas.wallet.android.module.transaction.adapter.WalletViewPagerAdapter
import io.nebulas.wallet.android.module.transaction.model.Transaction
import io.nebulas.wallet.android.module.wallet.create.model.Wallet
import io.nebulas.wallet.android.util.Blockies
import io.nebulas.wallet.android.util.Formatter
import io.nebulas.wallet.android.util.Util
import kotlinx.android.synthetic.nas_nano.dialog_confirm_transfer.*
import org.jetbrains.anko.dip
import org.jetbrains.anko.find
import walletcore.Walletcore
import java.math.BigDecimal


/**
 * Created by Heinoc on 2018/3/5.
 */
class ConfirmTransferDialog(var activity: Activity,
                            var curCoin: Coin,
                            val gasSymbol:String,
                            var coinPrice:BigDecimal?,
                            var gasCoinPrice:BigDecimal?,
                            var onSendTransfer: (wallet: Wallet) -> Unit,
                            var pledgedInfo:MutableMap<String, String> = mutableMapOf()) : Dialog(activity, R.style.AppDialog) {

    var transaction: Transaction? = null
    var gasFee: String? = null

    private lateinit var wallet: Wallet

    private var walletViewPagerAdapter: WalletViewPagerAdapter? = null
    private var walletViews = arrayListOf<View>()

    /**
     * 当前交易币种的钱包列表
     */
    private var wallets = mutableListOf<Wallet>()

    private var curWallet = 0

    private val transferTokenMap = mutableMapOf<Long, Coin>()
    private val transferGasMap = mutableMapOf<Long, Coin>()

    private var walletColors = arrayListOf<Int>(R.color.color_038AFB,
            R.color.color_8350F6,
            R.color.color_FF8F00,
            R.color.color_00CB91,
            R.color.color_FF516A)

    private lateinit var confirmBtnClickListener: View.OnClickListener

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.dialog_confirm_transfer)


        initView()

    }

    private fun initView() {

        loadWalletViews()

        walletViewPagerAdapter = WalletViewPagerAdapter(walletViews)
        walletViewPager.adapter = walletViewPagerAdapter
        walletViewPager.pageMargin = context.dip(18)
        walletViewPager.offscreenPageLimit = 5
        indicatorView.count = walletViews.size

        walletViewPager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrollStateChanged(state: Int) {

            }

            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
            }

            override fun onPageSelected(position: Int) {
                indicatorView.setFocusIndex(position)

                setSelectedWallet(position)
                curWallet = position
                setWallet(curWallet)

            }
        })


        /**
         * 发起交易
         */
        confirmBtnClickListener = View.OnClickListener {
            onSendTransfer(wallet)
        }

        closeIV.setOnClickListener {
            dismiss()

        }


    }

    private fun loadWalletViews() {
        walletViews.clear()

        DataCenter.coins.filter {
            it.tokenId == curCoin.tokenId
        }.forEach {
            transferTokenMap[it.walletId] = it
        }

        DataCenter.coins.filter {
            // type=1是核心币（主链币）
            it.platform == curCoin.platform && it.type == 1
        }.forEach {
            transferGasMap[it.walletId] = it
        }

        wallets.addAll(DataCenter.wallets.filter {
            transferTokenMap.containsKey(it.id)
        })

        val defaultWalletID = Util.getDefaultPaymentWallet(activity)
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
                view.find<TextView>(R.id.transferTokenSymbolTV).text = curCoin.symbol
                view.find<TextView>(R.id.transferTokenBalanceTV).text = transferTokenMap[it.id]?.balanceString
                view.find<TextView>(R.id.transferGasSymbolTV).text = "${transferGasMap[it.id]?.symbol}"
                view.find<TextView>(R.id.transferGasBalanceTV).text = transferGasMap[it.id]?.balanceString


            } else {
                view.find<TextView>(R.id.transferGasSymbolTV).text = curCoin.symbol
                view.find<TextView>(R.id.transferGasBalanceTV).text = transferTokenMap[it.id]?.balanceString

                val pledgedWEI = pledgedInfo[transferTokenMap[it.id]?.address]?:"0"
                val pledgedNAS = Formatter.amountFormat(pledgedWEI, curCoin.tokenDecimals.toInt())
                val formattedPledgedNAS = Formatter.tokenFormat(BigDecimal(pledgedNAS))
                view.find<TextView>(R.id.tvPledgedInfo).text = "dStaking:$formattedPledgedNAS"

                val finalTx = transaction
                if (finalTx!=null) {
                    val balance = BigDecimal(transferTokenMap[it.id]?.balance ?: "0")
                    val gasFee = BigDecimal(finalTx.gasPrice).multiply(BigDecimal(finalTx.gasLimit))
                    val amount = BigDecimal(finalTx.amount?:"0")
                    val pledgedAmount = BigDecimal(pledgedWEI)

                    // transaction.amount + (transaction.gasPrice * transaction.gasLimit) 转账金额+手续费
                    // transferTokenMap[it.id].balance 地址余额
                    // 如果：余额 - 当前质押数 < (转账金额+手续费)  -》变红
                    // 如果：余额 - 当前质押数 >= (转账金额+手续费)  -》变8F
                    if (pledgedAmount > BigDecimal.ZERO && balance.subtract(pledgedAmount) < amount.plus(gasFee)) {
                        view.find<TextView>(R.id.transferGasBalanceTV).setTextColor(context.resources.getColor(R.color.red))
                    } else {
                        view.find<TextView>(R.id.transferGasBalanceTV).setTextColor(context.resources.getColor(R.color.color_8F8F8F))
                    }

                }

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
        walletViews.forEachIndexed { inx, it ->
            it.setBackgroundResource(R.drawable.shape_round_corner_ffffff_c5c5c5)
            it.find<ImageView>(R.id.selectedWalletIV).visibility = View.GONE
            it.isClickable = inx!=index
            it.isEnabled = inx!=index
        }
        walletViews[index].setBackgroundResource(R.drawable.shape_round_corner_ffffff_038afb)
        walletViews[index].find<ImageView>(R.id.selectedWalletIV).visibility = View.VISIBLE
    }

    @SuppressLint("SetTextI18n")
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

        if (null != transaction) {

            var address = if (transaction?.platform == Walletcore.NAS) transaction?.receiver!! else transaction?.receiver!!.toLowerCase()
            addressIconIV.setImageBitmap(Blockies.createIcon(address, circleImage = true))

            transferToTV.text = transaction?.receiver

            amountTV.text = "${transaction?.amountString} ${transaction?.coinSymbol}"
            if (coinPrice!=null) {
                amountValueTV.text = "≈${Constants.CURRENCY_SYMBOL}${Formatter.priceFormat(BigDecimal(Formatter.amountFormat(transaction?.amount!!, (transaction?.tokenDecimals
                        ?: "18").toInt())).multiply(coinPrice))}"
            }

            if (null != gasFee) {
                var str = "$gasFee$gasSymbol"
                if (gasCoinPrice!=null) {
                    str = str.plus("(≈${Constants.CURRENCY_SYMBOL}${Formatter.gasPriceFormat(BigDecimal(gasFee).multiply(gasCoinPrice))}) ")
                }
                gasFeeTV.text = str
            }

            if (transaction!!.remark.isNotEmpty()) {
                remarkLayout.visibility = View.VISIBLE
                remarksTV.text = transaction!!.remark
            } else {
                remarkLayout.visibility = View.GONE
            }


        }

        setWallet(curWallet)


    }

    private fun setWallet(position: Int) {
        wallet = wallets[position]

        //当前转账为合约代币转账
        if (transferTokenMap[wallet.id]?.tokenId != transferGasMap[wallet.id]?.tokenId) {
            //余额 ＞= 转账金额 && 主链余额 ＞ 当前转账gas消耗
            if (BigDecimal(transferTokenMap[wallet.id]?.balance) >= BigDecimal(transaction?.amount) && BigDecimal(transferGasMap[wallet.id]?.balance) >= BigDecimal(gasFee).multiply(BigDecimal(10).pow(curCoin.tokenDecimals.toInt()))) {
                confirmTransferBtn.setText(R.string.confirm_transfer)
                confirmTransferBtn.setBackgroundResource(R.drawable.custom_corner_button_bg)
                confirmTransferBtn.setOnClickListener(confirmBtnClickListener)
            } else {
                if (BigDecimal(transferTokenMap[wallet.id]?.balance) < BigDecimal(transaction?.amount))
                    confirmTransferBtn.setText(R.string.insufficient_balance)
                else
                    confirmTransferBtn.setText(R.string.insufficient_gas)
                confirmTransferBtn.setBackgroundResource(R.drawable.custom_button_ff516a_bg)
                confirmTransferBtn.setOnClickListener(null)
            }

        } else {
            //余额 ＞= 转账金额 + 当前转账gas消耗
            if (BigDecimal(transferTokenMap[wallet.id]?.balance) >= (BigDecimal(transaction?.amount) + BigDecimal(gasFee).multiply(BigDecimal(10).pow(curCoin.tokenDecimals.toInt())))) {
                confirmTransferBtn.setText(R.string.confirm_transfer)
                confirmTransferBtn.setBackgroundResource(R.drawable.custom_corner_button_bg)
                confirmTransferBtn.setOnClickListener(confirmBtnClickListener)
            } else {
                confirmTransferBtn.setText(R.string.insufficient_balance)
                confirmTransferBtn.setBackgroundResource(R.drawable.custom_button_ff516a_bg)
                confirmTransferBtn.setOnClickListener(null)
            }

        }


    }

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        removeKeyBoard()
        return super.dispatchTouchEvent(ev)
    }

    private fun removeKeyBoard() {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        val view = currentFocus
        if (view != null) {
            imm.hideSoftInputFromWindow(view.windowToken, 0) //强制隐藏键盘
            view.clearFocus()
        }
    }

    override fun dismiss() {
        hideProgressBar()

        super.dismiss()

    }

    fun hideProgressBar() {
        progressBar.visibility = View.GONE

        confirmTransferBtn.isEnabled = true
        confirmTransferBtn.isClickable = true
    }


}