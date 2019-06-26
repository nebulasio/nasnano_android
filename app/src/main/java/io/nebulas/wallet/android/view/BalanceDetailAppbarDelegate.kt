package io.nebulas.wallet.android.view

import android.annotation.SuppressLint
import android.graphics.Paint
import android.graphics.Rect
import android.support.annotation.ColorInt
import android.support.design.widget.AppBarLayout
import android.util.TypedValue
import android.view.View
import android.widget.TextView
import io.nebulas.wallet.android.extensions.doOnEnd
import io.nebulas.wallet.android.extensions.withValueAnimator
import io.nebulas.wallet.android.util.Util
import kotlinx.android.synthetic.main.app_bar_for_balance_detail.view.*
import org.jetbrains.anko.dip

/**
 * Created by young on 2018/6/28.
 */
class BalanceDetailAppbarDelegate(private val appBar: AppBarLayout, @ColorInt private var walletId: Long? = null) {

    enum class ContentType(private val desc: String) {
        TYPE_LEGAL_TENDER_VALUE("法币总价值"),
        TYPE_TOKEN_VALUE("代币总价值"),
        TYPE_UNKNOWN("未知")
    }

    init {
        appBar.addOnOffsetChangedListener { appBarLayout, verticalOffset ->
            val totalOffset = appBarLayout.height - appBar.toolbar.height
            if (Math.abs(verticalOffset) > totalOffset / 2) {
                showSub()
            } else {
                hideSub()
            }
        }
        setContent(ContentType.TYPE_UNKNOWN, "0.00", "")
    }

    fun setTitleInToolbar(title: String) {
        appBar.titleTV.text = title
    }

    fun setSubTitleInToolbar(subTitle: String) {
        appBar.tvSubTitleInToolBar.text = subTitle
    }


    /**
     * 设置总价值
     * @param   contentType 类型（法币、代币）
     * @param   valueString 价值（如：6,892.0012 / 7,128.00）
     * @param   symbol      货币/代币 符号（法币：$/￥ ; 代币：NAS/ETH/NAT）
     */
    @SuppressLint("SetTextI18n")
    fun setContent(contentType: ContentType, valueString: String, symbol: String) {
        when (contentType) {
            ContentType.TYPE_LEGAL_TENDER_VALUE -> {
                checkTextSize(arrayOf(TextViewWrapper(appBar.tvCurrencySymbol, "≈" + symbol), TextViewWrapper(appBar.tvBalanceTotalValue, valueString)))
                appBar.tvTokenSymbol.visibility = View.GONE
                appBar.tvCurrencySymbol.visibility = View.VISIBLE
                appBar.tvCurrencySymbol.text = "≈" + symbol
            }
            ContentType.TYPE_TOKEN_VALUE -> {
                checkTextSize(arrayOf(TextViewWrapper(appBar.tvTokenSymbol, symbol), TextViewWrapper(appBar.tvBalanceTotalValue, valueString)))
                appBar.tvTokenSymbol.visibility = View.VISIBLE
                appBar.tvCurrencySymbol.visibility = View.GONE
                appBar.tvTokenSymbol.text = symbol
            }
            ContentType.TYPE_UNKNOWN -> {
                appBar.tvTokenSymbol.visibility = View.GONE
                appBar.tvCurrencySymbol.visibility = View.GONE
            }
        }
        appBar.tvBalanceTotalValue.text = valueString
    }

    fun setSubContent(subContent: String) {
        appBar.tvSubContent.text = subContent
    }

    class TextViewWrapper(val textView: TextView, val content: String, var textSize: Float = textView.textSize)

    private fun checkTextSize(textViewWrappers: Array<TextViewWrapper>) {
        var totalWidth = 0
        val paint = Paint()
        val rect = Rect(0, 0, 0, 0)
        textViewWrappers.forEach {
            paint.textSize = it.textSize
            paint.getTextBounds(it.content, 0, it.content.length, rect)
            totalWidth += rect.width()
        }

        val maxWidth = Util.screenWidth(appBar.context) * 0.8 - appBar.context.dip(18 * 2)
        while (totalWidth > maxWidth) {
            totalWidth = 0
            textViewWrappers.forEach {
                it.textSize = it.textSize - 6
                paint.textSize = it.textSize
                paint.getTextBounds(it.content, 0, it.content.length, rect)
                totalWidth += rect.width()
            }
        }

        textViewWrappers.forEach {
            it.textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, it.textSize)
        }
    }

    private fun showSub() {
        if (appBar.tvSubTitleInToolBar.alpha > 0) {
            return
        }
        val total = appBar.context.dip(10)
        appBar.tvSubTitleInToolBar.alpha = 0.01f
        appBar.tvSubTitleInToolBar.withValueAnimator(0f, 1f, 300L) {
            alpha = it
            appBar.titleTV.translationY = -total * it
        }.doOnEnd {
            appBar.tvSubTitleInToolBar.alpha = 1f
        }.start()
    }

    private fun hideSub() {
        if (appBar.tvSubTitleInToolBar.alpha < 1) {
            return
        }
        val total = appBar.context.dip(10)
        appBar.tvSubTitleInToolBar.alpha = 0.99f
        appBar.tvSubTitleInToolBar.withValueAnimator(1f, 0f, 300L) {
            alpha = it
            appBar.titleTV.translationY = -total * it
        }.doOnEnd {
            appBar.tvSubTitleInToolBar.alpha = 0f
        }.start()
    }

}