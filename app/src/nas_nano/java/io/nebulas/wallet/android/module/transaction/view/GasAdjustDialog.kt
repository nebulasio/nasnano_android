package io.nebulas.wallet.android.module.transaction.view

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.os.Bundle
import android.view.Gravity
import android.view.WindowManager
import io.nebulas.wallet.android.R
import io.nebulas.wallet.android.common.Constants
import io.nebulas.wallet.android.module.balance.model.Coin
import io.nebulas.wallet.android.util.Formatter
import kotlinx.android.synthetic.nas_nano.dialog_gas_adjust.*
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat

/**
 * Created by Heinoc on 2018/3/23.
 */
class GasAdjustDialog(activity: Activity,
                      private val coin: Coin,
                      private val gasPriceMin: String,
                      private val gasPriceMax: String,
                      private val gasUsed: String,
                      private val rate: String,
                      private val gasSymbol:String,
                      private val coinPrice:BigDecimal?,
                      private val onConfirm: (gasPrice: String) -> Unit) : Dialog(activity, R.style.DialogStyle) {

    private var gasPrice: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setGravity(Gravity.BOTTOM)

        setContentView(R.layout.dialog_gas_adjust)

        initView()

    }

    private fun initView() {
        gasPrice = gasPriceMin
        setGasFee(gasPriceMin)

        var sections = arrayListOf<String>()
        sections.add("")
        sections.add("")
        sections.add("")
        sections.add("")
        sections.add("")
        gasSeekBar.initData(sections)

        gasSeekBar.setResponseOnTouch {
            gasPrice = (BigDecimal(gasPriceMin) + ((BigDecimal(gasPriceMax) - BigDecimal(gasPriceMin))
                    .divide(BigDecimal(sections.size - 1))
                    .multiply(BigDecimal(it)))).toPlainString()

            setGasFee(gasPrice)
        }

        closeIV.setOnClickListener {
            dismiss()

        }

        confirmBtn.setOnClickListener {
            onConfirm(gasPrice)
            dismiss()
        }

    }

    @SuppressLint("SetTextI18n")
    private fun setGasFee(gasPrice: String){
        val gasFee = Formatter.getGas(gasPrice, gasUsed, coin.tokenDecimals.toInt())
        gasFeeTV.text = gasFee + gasSymbol + "(≈" + Constants.CURRENCY_SYMBOL + Formatter.gasPriceFormat(BigDecimal(gasFee).multiply(coinPrice).setScale(coin.tokenDecimals.toInt(), RoundingMode.DOWN)) + ")"
    }

    override fun show() {
        super.show()

        var lp = window.attributes
        lp.width = WindowManager.LayoutParams.MATCH_PARENT
        lp.height = WindowManager.LayoutParams.WRAP_CONTENT
        window.attributes = lp

    }

    override fun dismiss() {
        if (isShowing) {
            super.dismiss()
        }

    }


}