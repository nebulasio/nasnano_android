package io.nebulas.wallet.android.module.transaction.transfer

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.util.TypedValue
import android.view.View
import android.widget.TextView
import com.young.binder.bind
import com.young.binder.whenEvent
import io.nebulas.wallet.android.R
import io.nebulas.wallet.android.common.Constants
import io.nebulas.wallet.android.image.ImageUtil
import io.nebulas.wallet.android.module.wallet.create.CoinListActivity
import io.nebulas.wallet.android.util.Formatter
import io.nebulas.wallet.android.util.TextChange
import io.nebulas.wallet.android.util.Util
import kotlinx.android.synthetic.nas_nano.activity_transfer.view.*
import java.math.BigDecimal
import java.math.RoundingMode

class TransferBinder(val activity: TransferActivity, val controller: TransferController, private val dataCenter: TransferDataCenter) {

    var watcher: TextWatcher? = null

    fun bind(view: View) {
        setupInvariableViews(view)

        view.progressBar.bind(dataCenter, TransferDataCenter.EVENT_LOADING_STATUS_CHANGED) {
            visibility = if (dataCenter.isLoading) {
                View.VISIBLE
            } else {
                View.GONE
            }
        }

        view.amountType.bind(dataCenter, TransferDataCenter.EVENT_TRANSACTION_INFO_CHANGED) {
            text = dataCenter.transaction?.coinSymbol ?: ""
        }

        view.toAddressET.bind(dataCenter, TransferDataCenter.EVENT_ADDRESS_CHANGED) {
            setText(dataCenter.to)
        }

        view.amountET.bind(dataCenter, TransferDataCenter.EVENT_VALUE_CHANGED) {
            setText(dataCenter.value)
        }

        view.ivTokenIcon.bind(dataCenter, TransferDataCenter.EVENT_COIN_CHANGED) ivTokenIcon@{
            val coin = dataCenter.coin ?: return@ivTokenIcon
            ImageUtil.load(context, this, coin.logo)
        }

        dataCenter.gasCoinPrice.observe(activity) {
            refreshGasFee(view)
        }

        dataCenter.maxBalanceFormatted.observe(activity) {
            view.tvMaxBalance.text = it?:"0"
        }

        dataCenter.whenEvent(TransferDataCenter.EVENT_COIN_CHANGED) {
            view.amountET.setText("")
        }

        dataCenter.whenEvent(TransferDataCenter.EVENT_TRANSACTION_INFO_CHANGED) {
            refreshGasFee(view)
        }

        dataCenter.whenEvent(TransferDataCenter.EVENT_ADDRESS_EDITABLE_CHANGED) {
            view.toAddressET.clearFocus()
            view.toAddressET.isCursorVisible = false
            view.toAddressET.isFocusable = false
            view.toAddressET.isFocusableInTouchMode = false

            view.amountET.clearFocus()
            view.amountET.isCursorVisible = false
            view.amountET.isFocusable = false
            view.amountET.isFocusableInTouchMode = false
        }

        changeConfirmState(view)
    }

    private fun setupInvariableViews(view: View) {
        if (!dataCenter.addressEditable) {
            view.toAddressET.setText(dataCenter.to)
            view.toAddressET.clearFocus()
            view.toAddressET.isCursorVisible = false
            view.toAddressET.isFocusable = false
            view.toAddressET.isFocusableInTouchMode = false
        }
        if (!dataCenter.value.isNullOrEmpty() && dataCenter.coin != null) {
            val value = dataCenter.value
            view.amountET.setText(value)
            view.amountET.clearFocus()
            view.amountET.isCursorVisible = false
            view.amountET.isFocusable = false
            view.amountET.isFocusableInTouchMode = false
        }
        view.adjustGasTV.setOnClickListener {
            activity.adjustGas()
        }

        view.tvMax.setOnClickListener {
            val currentCoin = dataCenter.coin
            currentCoin?:return@setOnClickListener
            // coin type==1(币种为NAS) 需要减去gas消耗  else 直接取最大值即可
            val maxBalance = BigDecimal(dataCenter.maxBalance.value?:"0")
            val maxValueWei = if (currentCoin.type==1) {
                val gasInfo = dataCenter.gasPriceResp
                gasInfo?:return@setOnClickListener
                val gasPrice = BigDecimal(gasInfo.gasPriceMin?:"0")
                val estimateGas = BigDecimal(gasInfo.estimateGas?:"0")
                val estimateGasFee = gasPrice.multiply(estimateGas)
                maxBalance.subtract(estimateGasFee)
            } else {
                BigDecimal(maxBalance.toPlainString())
            }

            val maxValue = Formatter.amountFormat(maxValueWei.toPlainString(), currentCoin.tokenDecimals.toInt())
            view.amountET.setText(maxValue, TextView.BufferType.EDITABLE)
            view.amountET.setSelection(view.amountET.text.length)
        }

        view.layout_coin_info.setOnClickListener {
            if (!dataCenter.addressEditable) {
                return@setOnClickListener
            }
            val coin = dataCenter.coin ?: return@setOnClickListener
            CoinListActivity.launch(activity, TransferActivity.REQUEST_CODE_CHOOSE_TOKEN, coin.id)
        }

        view.toAddressET.addTextChangedListener(TextChange({ changeConfirmState(view) }, {}, {}))
        /**
         * 转账金额监听
         */
        watcher = object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val currentValue = view.amountET.text.toString()
                view.amountET.paint.isFakeBoldText = currentValue.isNotEmpty()
                if (view.toAddressET.text.isNotEmpty() && view.amountET.text.isNotEmpty()) {
                    view.nextStepBtn.setBackgroundResource(R.drawable.btn_import_bg)
                    view.nextStepBtn.isClickable = true
                } else {
                    view.nextStepBtn.setBackgroundResource(R.drawable.btn_import_disable_bg)
                    view.nextStepBtn.isClickable = false
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val coin = dataCenter.coin ?: return
                var curValues = view.amountET.text.toString()
                if (curValues.isNotEmpty()) {
                    view.amountET.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
                } else {
                    view.amountET.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
                }
                if (curValues.contains(".") && curValues.indexOf(".") == 0) {
                    curValues = "0$curValues"
                }

                val numbers = curValues.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                val selectionStart = view.amountET.selectionStart
                if (numbers.size > 1) {
                    if (numbers[1].length > coin.tokenDecimals.toInt()) {
                        curValues = numbers[0] + "." + numbers[1].substring(0, coin.tokenDecimals.toInt())
                        view.amountET.setText(curValues)
                        view.amountET.setSelection(Math.min(selectionStart, view.amountET.text.length))
                    }
                } else if (curValues != view.amountET.text.toString()) {
                    view.amountET.setText(curValues)
                    view.amountET.setSelection(Math.min(selectionStart, view.amountET.text.length))
                }
            }
        }
        view.amountET.addTextChangedListener(watcher)

        view.nextStepBtn.setOnClickListener {
            if (view.toAddressET.text.isNullOrEmpty() || view.amountET.text.isNullOrEmpty()) {
                return@setOnClickListener
            }

            val coin = dataCenter.coin ?: return@setOnClickListener
            if (!Util.checkAddress(view.toAddressET.text.toString(), coin.platform)) {
                dataCenter.error.value = TransferDataCenter.ERROR_INVALID_ADDRESS
                return@setOnClickListener
            }

            /**
             * GA
             */
            activity.firebaseAnalytics?.logEvent(Constants.kATransferConfirmClick, Bundle())

            val transaction = dataCenter.transaction ?: return@setOnClickListener
            transaction.receiver = view.toAddressET.text.toString()
            /**
             * 发送交易传递的amount需×10^18
             */
            val amountString = view.amountET.text.toString().replace(",", "")
            val amount = BigDecimal(amountString).multiply(BigDecimal(10).pow(coin.tokenDecimals.toInt())).stripTrailingZeros().toPlainString()
            transaction.amount = amount
            transaction.remark = view.remarksET.text.toString()
            activity.interceptConfirmTransaction(amount)
            if (!TextUtils.isEmpty(view.remarksET.text.toString())) {
                activity.firebaseAnalytics?.logEvent(Constants.kATransferMemoClick, Bundle())
            }
        }

        if (dataCenter.raiseUpPayload != null) {
            val payload = dataCenter.raiseUpPayload!!
            if (payload.nasFunction.length > 0) {
                view.remarksTitle.setText(R.string.tx_detail_remark_func)
                view.remarksET.setText(payload.nasFunction)

                view.remarksET.clearFocus()
                view.remarksET.isCursorVisible = false
                view.remarksET.isFocusable = false
                view.remarksET.isFocusableInTouchMode = false
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun refreshGasFee(view: View) {
        val transaction = dataCenter.transaction ?: return
        val gasPriceResp = dataCenter.gasPriceResp ?: return
        val coreCoin = dataCenter.coreCoin ?: return
        val gasFee = Formatter.getGas(transaction.gasPrice,
                gasPriceResp.estimateGas ?: "20000", coreCoin.tokenDecimals.toInt())
        view.gasFeeET.text = gasFee + dataCenter.gasSymbol + "（≈" + Constants.CURRENCY_SYMBOL + Formatter.gasPriceFormat(BigDecimal(gasFee).multiply(dataCenter.gasCoinPrice.value).setScale(coreCoin.tokenDecimals.toInt(), RoundingMode.DOWN)) + "）"
    }

    private fun changeConfirmState(view: View) {
        val currentValue = view.amountET.text.toString()
        view.amountET.paint.isFakeBoldText = currentValue.isNotEmpty()
        if (view.toAddressET.text.isNotEmpty() && view.amountET.text.isNotEmpty()) {
            view.nextStepBtn.setBackgroundResource(R.drawable.btn_import_bg)
            view.nextStepBtn.isClickable = true
        } else {
            view.nextStepBtn.setBackgroundResource(R.drawable.btn_import_disable_bg)
            view.nextStepBtn.isClickable = false
        }
    }
}