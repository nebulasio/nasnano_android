package io.nebulas.wallet.android.atp

import android.app.Activity
import android.app.Dialog
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.support.annotation.WorkerThread
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import com.google.firebase.analytics.FirebaseAnalytics
import io.nebulas.wallet.android.R
import io.nebulas.wallet.android.app.WalletApplication
import io.nebulas.wallet.android.base.BaseActivity
import io.nebulas.wallet.android.common.Constants
import io.nebulas.wallet.android.common.DataCenter
import io.nebulas.wallet.android.common.mainHandler
import io.nebulas.wallet.android.extensions.logD
import io.nebulas.wallet.android.module.balance.model.Coin
import io.nebulas.wallet.android.module.wallet.create.model.Address
import io.nebulas.wallet.android.module.wallet.create.model.Wallet
import io.nebulas.wallet.android.network.nas.NASHttpManager
import io.nebulas.wallet.android.network.nas.api.NASApi
import io.nebulas.wallet.android.network.nas.model.*
import io.nebulas.wallet.android.network.server.HttpManager
import io.nebulas.wallet.android.network.server.model.PriceList
import io.nebulas.wallet.android.util.Formatter
import io.nebulas.wallet.android.util.NetWorkUtil
import io.nebulas.wallet.android.view.research.CurtainResearch
import kotlinx.android.synthetic.nas_nano.dialog_atp_confirm.*
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import walletcore.Walletcore
import java.math.BigDecimal
import java.math.RoundingMode

class AtpConfirmDialog(val activity: Activity,
                       val address: String,
                       private val contractInfo: AtpConfirmDialog.ContractInfo,
                       private val confirmListener: OnConfirmListener) : Dialog(activity, R.style.AppDialog) {

    interface OnConfirmListener {
        fun onConfirmedWithoutAutoDismiss(atpConfirmDialog: AtpConfirmDialog, gasWrapper: GasWrapper)
    }

    data class ContractInfo(
            val contractAddress: String,
            val function: String,
            val args: Array<String>) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as ContractInfo

            if (contractAddress != other.contractAddress) return false
            if (function != other.function) return false
            if (!args.contentEquals(other.args)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = contractAddress.hashCode()
            result = 31 * result + function.hashCode()
            result = 31 * result + args.contentHashCode()
            return result
        }
    }

    data class GasWrapper(
            var nonce: Long = 0,
            var gasPrice: String = "0",
            var estimateGas: String = "0",
            var gasLimit: String = "0"
    )

    private val api = NASHttpManager.getApi()
    private val nanoApi = HttpManager.getServerApi()
    private val gasWrapper: GasWrapper = GasWrapper()
    private var addressModel: Address? = null
    private var walletModel: Wallet? = null
    private var nasCoin: Coin? = null
    private var nasPrice: String = "0"
    private var readyToConfirm = false
    private val firebaseAnalytics = FirebaseAnalytics.getInstance(WalletApplication.INSTANCE)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dialog_atp_confirm)
        ib_close.setOnClickListener {
            dismiss()
        }
        btn_confirm.setOnClickListener {
            if (!readyToConfirm) {
                return@setOnClickListener
            }
            confirmListener.onConfirmedWithoutAutoDismiss(this, gasWrapper)
        }
        loadNecessaryData()
    }

    override fun show() {
        super.show()
        firebaseAnalytics?.logEvent(Constants.ATPAds_Gas_Show, Bundle())
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

    private fun loadNecessaryData() {
        doAsync {

            DataCenter.addresses.forEach {
                if (it.address == address) {
                    addressModel = it
                    return@forEach
                }
            }
            if (addressModel == null) {
                logD("未找到对应的钱包")
                return@doAsync
            }

            val finalAddressModel = addressModel ?: return@doAsync
            DataCenter.wallets.forEach {
                if (it.id == finalAddressModel.walletId) {
                    walletModel = it
                    return@forEach
                }
            }
            if (walletModel == null) {
                logD("未找到对应的钱包")
                return@doAsync
            }

            val finalWalletModel = walletModel ?: return@doAsync
            uiThread {
                tv_wallet_name.text = finalWalletModel.walletName
                tv_wallet_address.text = address
            }

            DataCenter.coins.forEach {
                if (it.type == 1 && it.platform == Walletcore.NAS) {
                    nasCoin = it
                    return@forEach
                }
            }
            if (nasCoin == null) {
                logD("获取NAS信息失败")
                return@doAsync
            }
            val finalNasCoin = nasCoin ?: return@doAsync

            if (!NetWorkUtil.instance.isNetWorkConnected()) {
                mainHandler.postDelayed({
                    networkError()
                }, 800)
                return@doAsync
            }

            val priceList = getCoinPrice()
            if (priceList==null){
                networkError()
                return@doAsync
            }
            priceList.forEach {
                if (it.currencyId == finalNasCoin.tokenId) {
                    nasPrice = it.price
                }
            }

            val accountState = getAccountState()
            if (accountState == null) {
                logD("获取地址信息失败")
                networkError()
                return@doAsync
            }
            gasWrapper.nonce = accountState.nonce + 1
            val balance = accountState.balance

            val nasGasPrice = getGasPrice()
            if (nasGasPrice == null) {
                logD("获取GasPrice失败")
                networkError()
                return@doAsync
            }
            gasWrapper.gasPrice = nasGasPrice.gas_price

            val estimateGas = getEstimateGas()
            if (estimateGas == null) {
                networkError()
                logD("获取EstimateGas失败")
                return@doAsync
            }
            gasWrapper.estimateGas = estimateGas.gas
            val limitGasFee = BigDecimal(gasWrapper.gasPrice).multiply(BigDecimal(gasWrapper.estimateGas).add(BigDecimal(10000)))
            if (limitGasFee.minus(BigDecimal(accountState.balance)).toDouble() < 0.0) {
                gasWrapper.gasLimit = estimateGas.gas
            } else {
                gasWrapper.gasLimit = BigDecimal(gasWrapper.estimateGas).add(BigDecimal(10000)).stripTrailingZeros().toPlainString()
            }
            uiThread {
                val gasFee = Formatter.getGas(
                        gasPrice = gasWrapper.gasPrice,
                        gasUse = gasWrapper.estimateGas,
                        tokenDecimal = Constants.TOKEN_SCALE)
                val builder = StringBuilder(gasFee).append("NAS")
                if (BigDecimal(nasPrice).toDouble() > 0) {
                    builder.append("(≈${Constants.CURRENCY_SYMBOL}${Formatter.gasPriceFormat(BigDecimal(gasFee).multiply(BigDecimal(nasPrice)).setScale(18, RoundingMode.DOWN))})")
                }
                if (BigDecimal(accountState.balance).minus(BigDecimal(gasWrapper.gasPrice).multiply(BigDecimal(gasWrapper.estimateGas))).toDouble() < 0.0) {
                    firebaseAnalytics?.logEvent(Constants.ATPAds_NoBlance_Show, Bundle())
                    btn_confirm.text = activity.getString(R.string.insufficient_balance)
                    btn_confirm.isEnabled = false
                } else {
                    btn_confirm.text = activity.getString(R.string.confirm_btn)
                    btn_confirm.isEnabled = true
                }
                tv_gas.text = builder.toString()
                val data = BigDecimal(Formatter.amountFormat(balance, 18))
                tv_balance_nas.text = Formatter.tokenFormat(data)
                view_loading.visibility = View.GONE
                readyToConfirm = true
            }
        }
    }

    private fun networkError() {
        mainHandler.post {
            tv_gas.text = "0.00"
            tv_balance_nas.text = Formatter.tokenFormat(BigDecimal.ZERO)
            view_loading.visibility = View.GONE
            CurtainResearch.create(this)
                    .withLevel(CurtainResearch.CurtainLevel.ERROR)
                    .withContent(activity.getString(R.string.network_connect_exception))
                    .show()
        }
    }

    @WorkerThread
    private fun getGasPrice(): NasGasPrice? {
        return api.getGasPrice().execute().body()?.result
    }

    @WorkerThread
    private fun getEstimateGas(): NasEstimateGas? {
        return try {
            api.getEstimateGas(
                    EstimateGasRequest().apply {
                        from = address
                        to = contractInfo.contractAddress
                        contract = ContractCall(contractInfo.function, getArgsString(contractInfo.args))
                    }
            ).execute().body()?.result
        } catch (e: Exception) {
            null
        }
    }

    @WorkerThread
    private fun getAccountState(): NasAccountState? {
        return try {
            api.getAccountState(mapOf("address" to address)).execute().body()?.result
        } catch (e: Exception) {
            null
        }
    }

    @WorkerThread
    private fun getCoinPrice(): List<PriceList>? {
        val gasTokenId = nasCoin?.tokenId ?: return null
        return try {
            val call = nanoApi.getCurrencyPriceWithoutRX(HttpManager.getHeaderMap(), listOf(gasTokenId))
            call.execute().body()?.data?.priceList
        } catch (e: Exception) {
            null
        }
    }

    private fun getArgsString(args: Array<String>): String {
        val builder = StringBuilder()
        builder.append("[")
        args.forEach {
            builder.append("\"")
            builder.append(it)
            builder.append("\"")
            builder.append(",")
        }
        if (builder.length > 1) {
            builder.deleteCharAt(builder.length - 1)
        }
        builder.append("]")
        return builder.toString()
    }

}